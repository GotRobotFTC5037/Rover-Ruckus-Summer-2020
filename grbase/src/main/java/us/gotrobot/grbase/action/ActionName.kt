package us.gotrobot.grbase.action

class ActionName(val name: String) : ActionContext.Element() {

    override val key: ActionContext.Key<*> = ActionName

    companion object Key : ActionContext.Key<ActionName>

}
