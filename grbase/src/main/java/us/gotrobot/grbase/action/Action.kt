package us.gotrobot.grbase.action

import kotlinx.coroutines.*
import us.gotrobot.grbase.feature.Feature
import us.gotrobot.grbase.feature.FeatureKey
import us.gotrobot.grbase.robot.Robot
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KClass

typealias ActionBlock = suspend ActionScope.() -> Unit

class Action internal constructor(
    private val block: ActionBlock
) {

    var context: ActionContext = ActionContext()

    internal suspend fun run(robot: Robot) {
        val scope = ActionScope(robot, context, coroutineContext)
        block.invoke(scope)
        scope.coroutineContext[Job]?.cancel()
    }

}

class ActionScope internal constructor(
    internal val robot: Robot,
    val context: ActionContext,
    val parentContext: CoroutineContext = EmptyCoroutineContext
) : CoroutineScope {

    private val job = Job(parentContext[Job])

    override val coroutineContext: CoroutineContext
        get() = parentContext + job
}

fun <F : Feature> ActionScope.feature(key: FeatureKey<F>) = robot.features[key]
fun <F : Any> ActionScope.feature(clazz: KClass<F>) = robot.features.getAll(clazz).single()
suspend fun ActionScope.perform(action: Action) = robot.perform(action)
suspend fun Robot.perform(name: String = "(inline unnamed)", block: ActionBlock) =
    perform(action(block).apply { context.add(ActionName(name)) })

fun action(block: ActionBlock) = Action(block)
fun foreverNothing() = action {
    while (isActive) {
        yield()
    }
}

fun async(action: Action) = action { GlobalScope.launch { perform(action) } }

fun actionSequenceOf(vararg actions: Action) = action {
    for (action in actions) {
        perform(action)
    }
}.apply {
    context.add(ActionName("Action Sequence"))
}
