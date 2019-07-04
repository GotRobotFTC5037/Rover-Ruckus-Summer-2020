package us.gotrobot.grbase.opmode

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import kotlin.reflect.full.findAnnotation

val OpMode.isAutonomous: Boolean get() = this::class.findAnnotation<Autonomous>() != null
val OpMode.isTeleOp: Boolean get() = this::class.findAnnotation<TeleOp>() != null