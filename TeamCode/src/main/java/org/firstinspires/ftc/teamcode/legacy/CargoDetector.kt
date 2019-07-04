package org.firstinspires.ftc.teamcode.legacy

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.first
import org.firstinspires.ftc.robotcore.external.tfod.Recognition
import us.gotrobot.grbase.feature.Feature
import us.gotrobot.grbase.feature.FeatureConfiguration
import us.gotrobot.grbase.feature.FeatureSet
import us.gotrobot.grbase.feature.KeyedFeatureInstaller
import us.gotrobot.grbase.feature.vision.ObjectDetector
import us.gotrobot.grbase.robot.RobotContext
import kotlin.coroutines.CoroutineContext

class CargoDetector(
    private val objectDetector: ObjectDetector,
    private val parentContext: CoroutineContext
) : Feature(), CoroutineScope {

    private val job = Job(parentContext[Job])

    override val coroutineContext: CoroutineContext
        get() = parentContext + CoroutineName("CargoDetector") + job

    private val Recognition.isGold: Boolean get() = label == LABEL_GOLD_MINERAL
    private val Recognition.isSilver: Boolean get() = label == LABEL_SILVER_MINERAL
    private infix fun Recognition.leftOf(other: Recognition): Boolean = this.right < other.left
    private infix fun Recognition.rightOf(other: Recognition): Boolean = this.left > other.right
    private infix fun Recognition.between(others: Pair<Recognition, Recognition>): Boolean =
        this leftOf others.first && this rightOf others.second || this leftOf others.second && this rightOf others.first


    private val _goldPosition: Channel<GoldPosition> = Channel(Channel.CONFLATED)
    val goldPosition: ReceiveChannel<GoldPosition> get() = _goldPosition

    enum class GoldPosition {
        LEFT, CENTER, RIGHT, UNKNOWN
    }

    fun initialize() {
        updateGoldPosition(objectDetector.recognitions)
    }

    private fun CoroutineScope.updateGoldPosition(
        recognitionsChannel: ReceiveChannel<List<Recognition>>
    ) = launch {
        while (isActive) {
            val recognitions = recognitionsChannel
                .receive()
                .filter { it.width < it.imageWidth / 2 }

            val goldRecognitions = recognitions
                    .filter { it.isGold }
                    .sortedBy { it.top }
                    .reversed()

            val silverRecognitions =
                recognitions
                    .filter { it.isSilver }
                    .sortedBy { it.top }
                    .reversed()

            val gold = goldRecognitions.firstOrNull()
            val silver0 = silverRecognitions.firstOrNull()
            val silver1 = silverRecognitions.getOrNull(1)

            val position = if (gold != null && silver0 != null && silver1 != null) {
                when {
                    gold between (silver0 to silver1) -> GoldPosition.CENTER
                    gold leftOf silver0 && gold leftOf silver1 -> GoldPosition.LEFT
                    gold rightOf silver0 && gold rightOf silver0 -> GoldPosition.RIGHT
                    else -> GoldPosition.UNKNOWN
                }
            } else if (gold != null && silver0 != null) {
                when {
                    gold leftOf silver0 -> GoldPosition.LEFT
                    gold rightOf silver0 -> GoldPosition.CENTER
                    else -> GoldPosition.UNKNOWN
                }
            } else if (gold == null && silver0 != null && silver1 != null) {
                GoldPosition.RIGHT
            } else GoldPosition.UNKNOWN

            telemetry.addData("Gold", gold?.left)
            telemetry.addData("Silver0", silver0?.left)
            telemetry.addData("Silver1", silver1?.left)
            telemetry.addData("Position", position)
            telemetry.update()

            _goldPosition.offer(position)
        }
    }

    suspend fun shutdown() = objectDetector.shutdown()

    companion object Installer : KeyedFeatureInstaller<CargoDetector, Configuration>() {

        const val TFOD_MODEL_ASSET = "RoverRuckus.tflite"
        const val LABEL_GOLD_MINERAL = "Gold Mineral"
        const val LABEL_SILVER_MINERAL = "Silver Mineral"

        override val name: String = "Cargo Detector"

        override suspend fun install(
            context: RobotContext,
            featureSet: FeatureSet,
            configure: Configuration.() -> Unit
        ): CargoDetector {
            val configuration = Configuration().apply(configure)
            val objectDetector = configuration.objectDetector
            return CargoDetector(objectDetector, context.coroutineScope.coroutineContext).apply {
                initialize()
            }
        }

    }

    class Configuration : FeatureConfiguration {
        lateinit var objectDetector: ObjectDetector
    }

}

@Suppress("EXPERIMENTAL_API_USAGE")
suspend fun ReceiveChannel<CargoDetector.GoldPosition>.firstKnownPosition() =
    first { it != CargoDetector.GoldPosition.UNKNOWN }

