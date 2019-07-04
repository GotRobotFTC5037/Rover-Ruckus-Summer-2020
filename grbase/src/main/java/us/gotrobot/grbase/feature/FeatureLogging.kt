package us.gotrobot.grbase.feature

import org.firstinspires.ftc.robotcore.external.Telemetry

class FeatureLogging(telemetry: Telemetry, private val featureName: String) {

    private val log = telemetry.log()

    fun log(message: String) {
        log.add("[$featureName] $message")
    }

}