package us.gotrobot.grbase.opmode

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

abstract class CoroutineOpMode : OpMode(), CoroutineScope {

    private lateinit var job: Job

    private val throwableChannel = Channel<Throwable>()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwableChannel.sendBlocking(throwable)
    }

    final override val coroutineContext: CoroutineContext
        get() = CoroutineName("OpMode") + Dispatchers.Default + job + exceptionHandler

    var isInitialized: Boolean = false
        private set

    var isStarted: Boolean = false
        private set

    var isStopped: Boolean = false
        private set

    private fun handleLoop() {
        val throwable = throwableChannel.poll()
        if (throwable != null) {
            when (throwable) {
                is RuntimeException -> throw throwable
                else -> throw RuntimeException(throwable)
            }
        }
    }

    abstract suspend fun initialize()

    abstract suspend fun run()

    private suspend fun waitForStart() {
        while (!isStarted && isActive) {
            yield()
        }
    }

    final override fun init() {
        telemetry.log().add("[OpMode] Starting initialization")
        isInitialized = true
        job = Job()
        launch {
            val time = measureTimeMillis { initialize() }
            telemetry.log().add("[OpMode] Done initialization (${(time / 1000.0).roundToInt()}s)")
            waitForStart()
            telemetry.log().add("[OpMode] Starting opmode")
            run()
            requestOpModeStop()
        }
    }

    final override fun init_loop() = handleLoop()

    final override fun start() {
        isStarted = true
    }

    final override fun loop() = handleLoop()

    final override fun stop() {
        telemetry.log().add("[OpMode] Stopping opmode")
        isStopped = true
        runBlocking { job.cancelAndJoin() }
    }

}
