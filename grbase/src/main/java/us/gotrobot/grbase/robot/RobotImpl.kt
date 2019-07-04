package us.gotrobot.grbase.robot

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.hardware.Gamepad
import com.qualcomm.robotcore.hardware.HardwareMap
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.firstinspires.ftc.robotcore.external.Telemetry
import us.gotrobot.grbase.action.Action
import us.gotrobot.grbase.action.ActionName
import us.gotrobot.grbase.feature.*
import us.gotrobot.grbase.pipeline.Pipeline
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

private class RobotImpl(
    override val telemetry: Telemetry,
    override val hardwareMap: HardwareMap,
    override val gamepads: Pair<Gamepad, Gamepad>,
    private val parentContext: CoroutineContext
) : Robot, RobotContext, CoroutineScope {

    private val job: Job = Job(parentContext[Job])

    override val coroutineContext: CoroutineContext
        get() = parentContext + CoroutineName("Robot") + job

    override val coroutineScope: CoroutineScope
        get() = this

    private val _features: MutableFeatureSet = MutableFeatureSet()

    override val features: FeatureSet
        get() = _features

    override val actionPipeline = Pipeline<Action, RobotContext>()

    override suspend fun <F : Feature, C : FeatureConfiguration> install(
        installer: FeatureInstaller<F, C>,
        key: FeatureKey<F>,
        configure: C.() -> Unit
    ): F = if (key !in features) {
        telemetry.log().add("[Robot] Installing ${installer.name}")
        val feature = installer.install(this, features, configure)
        feature.telemetry = telemetry
        _features[key] = feature
        feature
    } else {
        throw InvalidInstallKeyException()
    }

    override suspend fun perform(action: Action) = withContext(coroutineContext) {
        val name = try {
            action.context[ActionName].name
        } catch (ignore: Throwable) {
            null
        } ?: "(Unnamed)"
        telemetry.log().add("[Robot] Performing $name")
        actionPipeline.execute(action, this@RobotImpl).run(this@RobotImpl)
    }
}

suspend fun OpMode.robot(configure: suspend RobotContext.() -> Unit = {}): Robot {
    val robot = RobotImpl(telemetry, hardwareMap, Pair(gamepad1, gamepad2), coroutineContext)
    robot.configure()
    return robot
}

class InvalidInstallKeyException : RuntimeException()