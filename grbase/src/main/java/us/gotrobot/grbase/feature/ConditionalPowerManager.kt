//package us.gotrobot.grbase.feature
//
//import us.gotrobot.grbase.action.NothingPowerManager
//import us.gotrobot.grbase.action.PowerManager
//import us.gotrobot.grbase.robot.RobotContext
//
//class ConditionalPowerManager : Feature() {
//
//    companion object Installer : KeyedFeatureInstaller<ConditionalPowerManager, Configuration>() {
//        override val name: String = "Conditional Power Manager"
//
//        override suspend fun install(
//            context: RobotContext,
//            featureSet: FeatureSet,
//            configure: Configuration.() -> Unit
//        ): ConditionalPowerManager {
//            val configuration = Configuration().apply(configure)
//            val powerManagers = configuration.powerManagers
//            context.actionPipeline.intercept {
//                val action = subject
//                if (action is MoveAction && MoveActionType in action.context && PowerManager !in action.context) {
//                    action.context[PowerManager] =
//                        powerManagers[action.context[MoveActionType]] ?: NothingPowerManager
//                }
//            }
//            return ConditionalPowerManager()
//        }
//    }
//
//    class Configuration : FeatureConfiguration {
//
//        private val _powerManagers = mutableMapOf<MoveActionType, PowerManager>()
//
//        val powerManagers: Map<MoveActionType, PowerManager> get() = _powerManagers
//
//        infix fun MoveActionType.uses(powerManager: PowerManager) {
//            _powerManagers[this] = powerManager
//        }
//    }
//
//}