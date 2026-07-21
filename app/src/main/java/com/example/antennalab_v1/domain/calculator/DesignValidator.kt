package com.example.antennalab_v1.domain.calculator

import com.example.antennalab_v1.model.CalculatedDesign

/*
########################################################################
FILE: DesignValidator.kt
PACKAGE: com.example.antennalab_v1.domain.calculator
LAYER: Domain / Design Validation

SYSTEM ROLE
Validates a CalculatedDesign before it is used by UI or analysis systems.

This ensures antenna designs are structurally correct and prevents
UI errors or misleading engineering insights.

ARCHITECTURE POSITION

CalculationEngine
        │
        ▼
CalculatedDesign
        │
        ▼
DesignValidator (THIS FILE)
        │
        ▼
DesignAnalyzer
        │
        ▼
UI Rendering

VALIDATION GOALS

Detect:

• missing antenna elements
• impossible element lengths
• empty feed configuration
• invalid spacing values

This validator does NOT change the design.
It only reports problems.
########################################################################
*/

data class DesignValidationResult(
    val isValid: Boolean,
    val issues: List<String>
)

fun validateDesign(design: CalculatedDesign): DesignValidationResult {

    val issues = mutableListOf<String>()

    if (design.elements.isEmpty()) {
        issues.add("No antenna elements defined.")
    }

    if (design.elements.any { it.lengthMm <= 0 }) {
        issues.add("One or more antenna elements have invalid length.")
    }

    if (design.spacings.any { it.distanceMm <= 0 }) {
        issues.add("One or more element spacings are invalid.")
    }

    if (design.feedRecommendation.feedMethod.isBlank()) {
        issues.add("Feed method is not defined.")
    }

    return DesignValidationResult(
        isValid = issues.isEmpty(),
        issues = issues
    )
}