package us.gotrobot.grbase.util

import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.HardwareMap
import kotlin.reflect.KClass

operator fun <T : HardwareDevice> HardwareMap.get(clazz: KClass<T>, name: String): T =
    get(clazz.java, name)