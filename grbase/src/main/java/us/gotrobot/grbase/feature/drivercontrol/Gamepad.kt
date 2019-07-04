package us.gotrobot.grbase.feature.drivercontrol

import com.qualcomm.robotcore.hardware.Gamepad
import kotlin.reflect.KProperty0

class DriverControlGamepad(gamepad: Gamepad) {
    val leftStick: Joystick = Joystick(gamepad::left_stick_x, gamepad::left_stick_y)
    val rightStick: Joystick = Joystick(gamepad::right_stick_x, gamepad::left_stick_y)
}

class Joystick(
    private val _x: KProperty0<Float>,
    private val _y: KProperty0<Float>
) {
    val x: Double get() = _x.get().toDouble()
    val y: Double get() = -_y.get().toDouble()
}