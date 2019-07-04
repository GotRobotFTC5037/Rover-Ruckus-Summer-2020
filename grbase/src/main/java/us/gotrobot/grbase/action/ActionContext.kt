package us.gotrobot.grbase.action

class ActionContext {

    private val elements: MutableMap<Key<*>, Element> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    operator fun <E : Element> get(key: Key<E>): E = elements[key] as E

    fun add(element: Element) {
        elements[element.key] = element
    }

    operator fun <E : Element> set(key: Key<E>, element: E) {
        elements[key] = element
    }

    operator fun contains(key: Key<*>): Boolean = elements.contains(key)

    interface Key<E : Element>

    abstract class Element {
        abstract val key: Key<*>
        operator fun plus(other: Element): ActionContext {
            return ActionContext().apply {
                this.add(this@Element)
                this.add(other)
            }
        }
    }

}

infix fun Action.with(element: ActionContext.Element): Action {
    context.add(element)
    return this
}