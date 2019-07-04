package us.gotrobot.grbase.pipeline

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

open class Pipeline<TSubject : Any, TContext : Any> {

    private val interceptors = mutableListOf<PipelineInterceptor<TSubject, TContext>>()

    suspend fun execute(subject: TSubject, context: TContext): TSubject =
        PipelineContext(
            subject,
            context,
            interceptors.toList(),
            coroutineContext
        ).proceed()

    fun intercept(interceptor: PipelineInterceptor<TSubject, TContext>) {
        interceptors.add(interceptor)
    }

}

class PipelineContext<TSubject : Any, out TContext : Any>(
    subject: TSubject,
    val context: TContext,
    private val interceptors: List<PipelineInterceptor<TSubject, TContext>>,
    override val coroutineContext: CoroutineContext
) : CoroutineScope {

    var subject: TSubject = subject
        private set

    private var index = 0

    suspend fun proceedWith(subject: TSubject): TSubject {
        this.subject = subject
        return proceed()
    }

    suspend fun proceed(): TSubject {
        while (index >= 0) {
            if (interceptors.size == index) {
                finish()
                return subject
            }
            val nextInterceptor = interceptors[index]
            index++
            nextInterceptor.invoke(this, subject)
        }
        return subject
    }

    fun finish() {
        index = -1
    }

}

typealias PipelineInterceptor<TSubject, TContext> = suspend PipelineContext<TSubject, TContext>.(TSubject) -> Unit