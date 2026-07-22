package com.example.antennalab_v1.domain.testing

import kotlin.math.atan2
import kotlin.math.hypot

/*
########################################################################
FILE: Complex.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Math

SYSTEM ROLE
Minimal immutable complex-number type shared by the OSL calibration
engine, the simulated-capture debug path, and (later) the sweep
corrector. Pure math, no Android/framework refs.

NOTE
LiteVnaSweepProtocol has its own private ComplexValue for FIFO decoding;
that is intentionally left in place. This is the shared calibration-side
type.
########################################################################
*/
data class Complex(val re: Double, val im: Double) {

    operator fun plus(other: Complex): Complex =
        Complex(re + other.re, im + other.im)

    operator fun minus(other: Complex): Complex =
        Complex(re - other.re, im - other.im)

    operator fun times(other: Complex): Complex =
        Complex(
            re = re * other.re - im * other.im,
            im = re * other.im + im * other.re
        )

    operator fun div(other: Complex): Complex {
        val denom = other.re * other.re + other.im * other.im
        return Complex(
            re = (re * other.re + im * other.im) / denom,
            im = (im * other.re - re * other.im) / denom
        )
    }

    val magnitude: Double
        get() = hypot(re, im)

    val phaseDegrees: Double
        get() = Math.toDegrees(atan2(im, re))

    companion object {
        val ZERO = Complex(0.0, 0.0)
        val ONE = Complex(1.0, 0.0)

        fun ofReal(value: Double): Complex = Complex(value, 0.0)
    }
}
