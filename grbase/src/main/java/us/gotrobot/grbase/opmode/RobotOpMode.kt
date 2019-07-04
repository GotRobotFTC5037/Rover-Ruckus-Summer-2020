package us.gotrobot.grbase.opmode

import us.gotrobot.grbase.action.Action
import us.gotrobot.grbase.robot.Robot

abstract class RobotOpMode : CoroutineOpMode() {

    private lateinit var robot: Robot

    abstract val action: Action

    abstract suspend fun robot(): Robot

    override suspend fun initialize() {
        robot = robot()
    }

    override suspend fun run() {
        robot.perform(action)
    }

}