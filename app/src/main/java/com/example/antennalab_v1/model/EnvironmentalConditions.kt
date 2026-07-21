package com.example.antennalab_v1.model

/*
########################################################################
FILE: EnvironmentalConditions.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Model

PURPOSE
Stores environmental and installation-related conditions that may affect
real-world antenna behaviour.

DESIGN NOTES
- This model is intentionally simple for V1.
- It supports the future SWR prediction layer.
- It also supports future testing workflows and environmental diagnostics.
- Default values are conservative and beginner-safe.
- This file must remain UI-independent.

FUTURE USES
- SWR prediction adjustments
- tuning assistant context
- testing session storage
- diagnostic workflows for unknown antennas
- companion device telemetry/session sharing
########################################################################
*/

enum class GroundMoistureLevel {
    DRY,
    MEDIUM,
    WET
}

enum class GroundType {
    AVERAGE_SOIL,
    SANDY,
    CLAY,
    ROCKY,
    SALT_RICH,
    UNKNOWN
}

data class EnvironmentalConditions(
    val temperatureCelsius: Double = 20.0,
    val humidityPercent: Double = 50.0,
    val groundMoistureLevel: GroundMoistureLevel = GroundMoistureLevel.MEDIUM,
    val groundType: GroundType = GroundType.AVERAGE_SOIL,
    val antennaHeightMeters: Double = 2.0,
    val nearbyStructures: Boolean = false,
    val notes: String = ""
)