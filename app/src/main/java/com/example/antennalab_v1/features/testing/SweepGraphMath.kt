package com.example.antennalab_v1.features.testing

/*
########################################################################
FILE: SweepGraphMath.kt
PACKAGE: com.example.antennalab_v1.features.testing
LAYER: UI / Testing Tools / Graph Math

LAST UPDATED 14/3/2026 21:50

SYSTEM ROLE
Provides pure sweep graph helper math for screen rendering and marker
position calculations.

CURRENT DEVELOPMENT ROLE
This file is the first safe extraction point from SweepGraphScreen.kt.

It intentionally contains only stateless math helpers so it can be added
without changing runtime behaviour or moving acquisition logic.

IMPORTANT ARCHITECTURE RULE
This file must stay focused on:

• graph coordinate mapping
• numeric clamping
• safe range handling
• interpolation helpers
• marker position math

This file must NOT contain:

• sweep acquisition logic
• USB/device logic
• Compose UI
• project state management

SAFE EDIT AREA
- add more graph helpers later
- add marker interpolation helpers later
- add axis label helpers later
- add viewport helpers later
########################################################################
*/

/*
########################################################################
SECTION 1
BASIC RANGE HELPERS
########################################################################
PURPOSE
Provides safe numeric helpers used by graph rendering code.
########################################################################
*/

internal fun clampFloat(
    value: Float,
    minimum: Float,
    maximum: Float
): Float {
    return value.coerceIn(minimum, maximum)
}

internal fun clampDouble(
    value: Double,
    minimum: Double,
    maximum: Double
): Double {
    return value.coerceIn(minimum, maximum)
}

internal fun safeFloatRange(
    minimum: Float,
    maximum: Float,
    fallback: Float = 1f
): Float {
    val range = maximum - minimum
    return if (range == 0f) fallback else range
}

internal fun safeDoubleRange(
    minimum: Double,
    maximum: Double,
    fallback: Double = 1.0
): Double {
    val range = maximum - minimum
    return if (range == 0.0) fallback else range
}

/*
########################################################################
SECTION 2
NORMALISATION HELPERS
########################################################################
PURPOSE
Converts values into 0..1 graph space safely.
########################################################################
*/

internal fun normaliseFloat(
    value: Float,
    minimum: Float,
    maximum: Float
): Float {
    val range = safeFloatRange(minimum, maximum)
    return (value - minimum) / range
}

internal fun normaliseDouble(
    value: Double,
    minimum: Double,
    maximum: Double
): Double {
    val range = safeDoubleRange(minimum, maximum)
    return (value - minimum) / range
}

/*
########################################################################
SECTION 3
CANVAS POSITION HELPERS
########################################################################
PURPOSE
Maps normalised values into canvas X/Y coordinates.

Y mapping is inverted because screen coordinates grow downward.
########################################################################
*/

internal fun toCanvasX(
    value: Float,
    minimum: Float,
    maximum: Float,
    width: Float
): Float {
    val normalised = clampFloat(
        value = normaliseFloat(value, minimum, maximum),
        minimum = 0f,
        maximum = 1f
    )
    return normalised * width
}

internal fun toCanvasY(
    value: Float,
    minimum: Float,
    maximum: Float,
    height: Float
): Float {
    val normalised = clampFloat(
        value = normaliseFloat(value, minimum, maximum),
        minimum = 0f,
        maximum = 1f
    )
    return height - (normalised * height)
}

internal fun toCanvasXDouble(
    value: Double,
    minimum: Double,
    maximum: Double,
    width: Float
): Float {
    val normalised = clampDouble(
        value = normaliseDouble(value, minimum, maximum),
        minimum = 0.0,
        maximum = 1.0
    )
    return (normalised * width.toDouble()).toFloat()
}

internal fun toCanvasYDouble(
    value: Double,
    minimum: Double,
    maximum: Double,
    height: Float
): Float {
    val normalised = clampDouble(
        value = normaliseDouble(value, minimum, maximum),
        minimum = 0.0,
        maximum = 1.0
    )
    return (height.toDouble() - (normalised * height.toDouble())).toFloat()
}

/*
########################################################################
SECTION 4
LINEAR INTERPOLATION HELPERS
########################################################################
PURPOSE
Supports marker and graph calculations when future smoothing or cursor
sampling is added.
########################################################################
*/

internal fun lerpFloat(
    start: Float,
    end: Float,
    fraction: Float
): Float {
    return start + ((end - start) * fraction)
}

internal fun lerpDouble(
    start: Double,
    end: Double,
    fraction: Double
): Double {
    return start + ((end - start) * fraction)
}

/*
########################################################################
SECTION 5
INDEX HELPERS
########################################################################
PURPOSE
Provides safe helpers for converting a position or ratio into a list
index for marker placement logic.
########################################################################
*/

internal fun ratioToIndex(
    ratio: Float,
    itemCount: Int
): Int {
    if (itemCount <= 0) return 0
    val clampedRatio = clampFloat(ratio, 0f, 1f)
    val rawIndex = (clampedRatio * (itemCount - 1)).toInt()
    return rawIndex.coerceIn(0, itemCount - 1)
}

internal fun xToIndex(
    x: Float,
    width: Float,
    itemCount: Int
): Int {
    if (width <= 0f) return 0
    return ratioToIndex(
        ratio = x / width,
        itemCount = itemCount
    )
}

/*
########################################################################
SECTION 6
MIDPOINT HELPERS
########################################################################
PURPOSE
Useful for future marker delta displays and graph annotations.
########################################################################
*/

internal fun midpointFloat(
    first: Float,
    second: Float
): Float {
    return (first + second) / 2f
}

internal fun midpointDouble(
    first: Double,
    second: Double
): Double {
    return (first + second) / 2.0
}