package us.gotrobot.grbase.pipeline

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class ChannelPipeline<TSubject : Any> {

    private val sendChannel: Channel<TSubject> = Channel()
    private var receiveChannel: ReceiveChannel<TSubject> = sendChannel

    fun intercept(interceptor: (ReceiveChannel<TSubject>) -> ReceiveChannel<TSubject>) {
        receiveChannel = interceptor(sendChannel)
    }

    suspend fun execute(subject: TSubject): TSubject {
        sendChannel.send(subject)
        return receiveChannel.receive()
    }

}
