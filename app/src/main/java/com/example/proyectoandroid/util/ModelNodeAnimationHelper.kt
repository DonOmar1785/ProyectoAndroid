package com.example.proyectoandroid.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class Position3D(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Position3D) = Position3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Position3D) = Position3D(x - other.x, y - other.y, z - other.z)
}

object ModelNodeAnimationHelper {

    fun animatePosition(
        start: Position3D,
        target: Position3D,
        durationMs: Long = 350L,
        steps: Int = 20
    ): Flow<Position3D> = flow {
        val delayPerStep = durationMs / steps
        for (i in 0..steps) {
            val fraction = i.toFloat() / steps.toFloat()
            // Smooth ease-in-ease-out cubic interpolation
            val smoothFraction = fraction * fraction * (3 - 2 * fraction)
            val currentX = start.x + (target.x - start.x) * smoothFraction
            val currentY = start.y + (target.y - start.y) * smoothFraction
            val currentZ = start.z + (target.z - start.z) * smoothFraction
            emit(Position3D(currentX, currentY, currentZ))
            if (i < steps) {
                delay(delayPerStep)
            }
        }
    }
}
