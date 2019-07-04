package us.gotrobot.grbase.opmode

interface OpModeEvent

class OpModeEvents {

    fun subscribe(block: () -> Unit) {

    }


}

class OpModeEventDefinition

var OpModeInitilized = OpModeEventDefinition()
val OpModeStarted = OpModeEventDefinition()
val OpModeStopped = OpModeEventDefinition()