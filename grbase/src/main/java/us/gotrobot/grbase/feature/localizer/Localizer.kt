package us.gotrobot.grbase.feature.localizer

import kotlinx.coroutines.channels.ReceiveChannel
import org.firstinspires.ftc.robotcore.external.navigation.Orientation

interface LinearLocalizer {

}

interface LateralLocalizer {

}

interface OrientationLocalizer : HeadingLocalizer {
    suspend fun orientation(): Orientation
}

interface HeadingLocalizer {
    suspend fun headingChannel(): ReceiveChannel<Double>
}
