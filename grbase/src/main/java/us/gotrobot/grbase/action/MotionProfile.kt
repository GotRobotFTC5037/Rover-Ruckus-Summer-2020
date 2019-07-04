package us.gotrobot.grbase.action

import kotlin.math.pow
import kotlin.math.roundToInt

data class MotionState(
    var position: Double,
    var speed: Double,
    var acceleration: Double = 0.0
)

class MotionProfile private constructor(private val segments: List<Segment>) {

    private class Segment(val startState: MotionState, val duration: Int) {

        val endState: MotionState
            get() = motionStateAt(duration)

        fun motionStateAt(duration: Int): MotionState {
            val durationSeconds = duration.toDouble() / 1000
            return MotionState(
                position = (startState.acceleration / 2) * durationSeconds.pow(2) + startState.speed * durationSeconds + startState.position,
                speed = startState.acceleration * durationSeconds + startState.speed,
                acceleration = startState.acceleration
            )
        }

    }

    val duration: Int get() = segments.sumBy { it.duration }

    val endState: MotionState get() = stateAt(this.duration)

    fun stateAt(duration: Int): MotionState {
        var adjustedDuration = duration.coerceIn(0, this.duration)
        for (segment in segments) {
            if (adjustedDuration <= segment.duration) {
                return segment.motionStateAt(adjustedDuration)
            }
            adjustedDuration -= segment.duration
        }
        return segments.last().endState
    }

    companion object {

        val Empty = MotionProfile(listOf(Segment(MotionState(0.0, 0.0, 0.0), Int.MAX_VALUE)))

        fun generate(
            positionDelta: Double,
            maximumSpeed: Double,
            maximumAcceleration: Double
        ): MotionProfile {
            val segments = mutableListOf<Segment>()
            val timeToMaxSpeed = (maximumSpeed * 1000 / maximumAcceleration).roundToInt()
            val segment0 = Segment(MotionState(0.0, 0.0, maximumAcceleration), timeToMaxSpeed)
            val segment0Delta = segment0.endState.position
            if (segment0.endState.position < positionDelta / 2) {
                val segment1Delta = positionDelta - (segment0Delta * 2)
                val segment1Duration = (segment1Delta * 1000 / maximumSpeed).roundToInt()
                val segment1 = Segment(MotionState(segment0Delta, maximumSpeed), segment1Duration)
                val accumulatedDelta = segment1.endState.position
                val segment2 = Segment(MotionState(accumulatedDelta, maximumSpeed, -maximumAcceleration), timeToMaxSpeed)
                segments.addAll(listOf(segment0, segment1, segment2))
            } else {
                TODO()
            }
            return MotionProfile(segments)
        }
    }
}
