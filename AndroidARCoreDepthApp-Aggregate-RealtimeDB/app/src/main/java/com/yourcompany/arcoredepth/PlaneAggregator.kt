
package com.yourcompany.arcoredepth

import com.google.ar.core.*
import kotlin.math.max

/**
 * Aggregates per-frame plane extents and produces stable L×W estimates.
 * Strategy: track the largest horizontal plane seen (by area) across frames,
 * keep a sliding window of extents, and compute a robust median.
 */
class PlaneAggregator(private val windowSize:Int = 30) {
    private val lengths = ArrayDeque<Double>()
    private val widths  = ArrayDeque<Double>()

    fun accumulate(frame: Frame) {
        val planes = frame.getUpdatedTrackables(Plane::class.java)
            .filter { it.trackingState == TrackingState.TRACKING }
        // Pick the plane with maximum extent area among horizontals
        val candidate = planes
            .filter { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING || it.type == Plane.Type.HORIZONTAL_DOWNWARD_FACING }
            .maxByOrNull { (it.extentX * it.extentZ) }
        candidate?.let { p ->
            pushSample(max(p.extentX.toDouble(), p.extentZ.toDouble()),
                       minOf(p.extentX.toDouble(), p.extentZ.toDouble()))
        }
    }

    private fun pushSample(L: Double, W: Double) {
        lengths.addLast(L); widths.addLast(W)
        while (lengths.size > windowSize) lengths.removeFirst()
        while (widths.size  > windowSize) widths.removeFirst()
    }

    fun hasStableEstimate(minSamples:Int = 10) = lengths.size >= minSamples && widths.size >= minSamples

    fun estimateDims(): Pair<Double, Double> {
        fun median(d: List<Double>): Double {
            if (d.isEmpty()) return 0.0
            val s = d.sorted(); val m = s.size / 2
            return if (s.size % 2 == 1) s[m] else (s[m-1] + s[m]) / 2.0
        }
        return median(lengths.toList()) to median(widths.toList())
    }
}
