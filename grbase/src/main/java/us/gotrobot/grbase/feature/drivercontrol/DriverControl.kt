package us.gotrobot.grbase.feature.drivercontrol

import com.qualcomm.robotcore.hardware.Gamepad
import kotlinx.coroutines.*
import us.gotrobot.grbase.action.ActionScope
import us.gotrobot.grbase.action.feature
import us.gotrobot.grbase.feature.Feature
import us.gotrobot.grbase.feature.FeatureSet
import us.gotrobot.grbase.feature.KeyedFeatureInstaller
import us.gotrobot.grbase.robot.RobotContext
import kotlin.coroutines.CoroutineContext

class DriverControl(
    private val gamepads: Pair<Gamepad, Gamepad>,
    parentContext: CoroutineContext
) : Feature(), CoroutineScope {

    private val job: Job = Job(parentContext[Job])

    override val coroutineContext: CoroutineContext =
        parentContext + CoroutineName("Driver Control") + job

    fun loop(block: DriverControlScope.() -> Unit) {
        val scope = DriverControlScope(gamepads.first, gamepads.second)
        launch {
            while (isActive) {
                scope.block()
            }
        }
    }

    companion object Installer : KeyedFeatureInstaller<DriverControl, Nothing>() {
        override val name: String = "Driver Control"
        override suspend fun install(
            context: RobotContext,
            featureSet: FeatureSet,
            configure: Nothing.() -> Unit
        ): DriverControl {
            val gamepads = context.gamepads
            return DriverControl(gamepads, context.coroutineScope.coroutineContext)
        }
    }

}

fun ActionScope.driverControl(block: DriverControl.() -> Unit) {
    val driverControl = feature(DriverControl)
    block.invoke(driverControl)
}

class DriverControlScope(
    gamepad0: Gamepad,
    gamepad1: Gamepad
) {
    val driver = DriverControlGamepad(gamepad0)
    val gunner = DriverControlGamepad(gamepad1)
}