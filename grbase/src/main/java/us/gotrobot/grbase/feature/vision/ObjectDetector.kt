package us.gotrobot.grbase.feature.vision

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer
import org.firstinspires.ftc.robotcore.external.tfod.Recognition
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector
import org.firstinspires.ftc.robotcore.internal.tfod.TFObjectDetectorImpl
import us.gotrobot.grbase.feature.Feature
import us.gotrobot.grbase.feature.FeatureConfiguration
import us.gotrobot.grbase.feature.FeatureSet
import us.gotrobot.grbase.feature.KeyedFeatureInstaller
import us.gotrobot.grbase.robot.RobotContext
import kotlin.coroutines.CoroutineContext

@Suppress("FunctionName")
fun TFObjectDetector(
    parameters: TFObjectDetector.Parameters,
    vuforiaLocalizer: VuforiaLocalizer
) = TFObjectDetectorImpl(parameters, vuforiaLocalizer)

class ObjectDetector(
    private val vuforia: Vuforia,
    private val appContext: Context,
    private val parentContext: CoroutineContext
) : Feature(), CoroutineScope {

    private val job = Job(parentContext[Job])

    override val coroutineContext: CoroutineContext
        get() = CoroutineName("Cargo Detector") + parentContext + job

    private lateinit var objectDetector: TFObjectDetector

    private val _recognitions = Channel<List<Recognition>>(Channel.CONFLATED)
    val recognitions: ReceiveChannel<List<Recognition>> get() = _recognitions

    private fun CoroutineScope.updateRecognitions() = launch {
        while (isActive) {
            val recognitions = objectDetector.updatedRecognitions
            if (recognitions != null) {
                _recognitions.offer(recognitions)
            } else {
                yield()
            }
        }
    }

    data class AssetData(
        val model: String,
        val labels: List<String>
    )

    private fun TFObjectDetector.loadModelFromAssetData(data: AssetData) =
        loadModelFromAsset(data.model, *data.labels.toTypedArray())

    fun initilize(data: AssetData) {
        val viewID =
            appContext.resources.getIdentifier(MONITOR_VIEW_ID, "id", appContext.packageName)
        val parameters = TFObjectDetector.Parameters(viewID)
        objectDetector = TFObjectDetector(parameters, vuforia.localizer).apply {
            loadModelFromAssetData(data)
            activate()
        }
        updateRecognitions()
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    suspend fun shutdown() = withContext(newSingleThreadContext("Shutdown")) {
        objectDetector.shutdown()
    }

    companion object Installer : KeyedFeatureInstaller<ObjectDetector, Configuration>() {

        private const val MONITOR_VIEW_ID = "tfodMonitorViewId"

        override val name: String = "ObjectDetector"

        override suspend fun install(
            context: RobotContext,
            featureSet: FeatureSet,
            configure: Configuration.() -> Unit
        ): ObjectDetector {
            val configuration = Configuration().apply(configure)
            val vuforia = configuration.vuforia
            val data = configuration.data
            val appContext = context.hardwareMap.appContext
            val parentCoroutineContext = context.coroutineScope.coroutineContext
            return ObjectDetector(vuforia, appContext, parentCoroutineContext).apply {
                initilize(data)
            }
        }

    }

    class Configuration : FeatureConfiguration {
        lateinit var vuforia: Vuforia
        lateinit var data: AssetData
    }
}