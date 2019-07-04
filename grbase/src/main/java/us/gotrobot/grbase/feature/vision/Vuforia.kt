package us.gotrobot.grbase.feature.vision

import android.content.Context
import org.firstinspires.ftc.robotcore.external.ClassFactory
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer
import us.gotrobot.grbase.feature.Feature
import us.gotrobot.grbase.feature.FeatureConfiguration
import us.gotrobot.grbase.feature.FeatureSet
import us.gotrobot.grbase.feature.KeyedFeatureInstaller
import us.gotrobot.grbase.robot.RobotContext
import us.gotrobot.grbase.util.get

class Vuforia(
    private val licenceKey: String,
    private val webcamName: WebcamName?,
    private val context: Context
) : Feature() {

    lateinit var localizer: VuforiaLocalizer

    fun initilize() {
        val classFactory = ClassFactory.getInstance()
        val parameters = VuforiaLocalizer.Parameters().apply {
            this.vuforiaLicenseKey = licenceKey
            this.cameraDirection = VuforiaLocalizer.CameraDirection.BACK
            this.cameraName = webcamName ?: classFactory.cameraManager.nameForUnknownCamera()
        }
        localizer = classFactory.createVuforia(parameters)
    }

    companion object Installer : KeyedFeatureInstaller<Vuforia, Configuration>() {
        override val name: String = "Vuforia"
        override suspend fun install(
            context: RobotContext,
            featureSet: FeatureSet,
            configure: Configuration.() -> Unit
        ): Vuforia {
            val configuration = Configuration().apply(configure)
            val webcamName = configuration.cameraName?.let {
                context.hardwareMap[WebcamName::class, it]
            }
            return Vuforia(
                configuration.licenceKey,
                webcamName,
                context.hardwareMap.appContext
            ).apply {
                initilize()
            }
        }
    }

    class Configuration : FeatureConfiguration {
        lateinit var licenceKey: String
        var cameraName: String? = null
    }

}
