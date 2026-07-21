package com.example.antennalab_v1.domain.calculator

import com.example.antennalab_v1.model.CalculatedDesign
import com.example.antennalab_v1.model.ElementRole

/*
########################################################################
FILE: DesignAnalyzer.kt
PACKAGE: com.example.antennalab_v1.domain.calculator
LAYER: Domain / Design Intelligence

SYSTEM ROLE
Analyzes a CalculatedDesign and generates additional engineering insight.

This layer does NOT calculate antenna geometry.

Instead it evaluates a completed design and provides:

• tuning suggestions
• build warnings
• structural notes
• interpretation hints

This allows the application to behave more like an engineering assistant.

ARCHITECTURE POSITION

CalculationEngine
        │
        ▼
CalculatedDesign
        │
        ▼
DesignAnalyzer (THIS FILE)
        │
        ▼
AnalysisResult
        │
        ▼
UI interpretation layer (future)

DEBUG NOTES

If calculated antenna values appear correct but advice seems wrong,
verify that the CalculatedDesign fields were populated correctly.

This analyzer depends heavily on:

elements
spacings
feedRecommendation
buildGuidance
designExplanation

########################################################################
*/

data class DesignAnalysisResult(
    val tuningAdvice: List<String>,
    val buildWarnings: List<String>,
    val improvementIdeas: List<String>
)

fun analyzeDesign(design: CalculatedDesign): DesignAnalysisResult {

    val tuningAdvice = mutableListOf<String>()
    val buildWarnings = mutableListOf<String>()
    val improvementIdeas = mutableListOf<String>()

    if (design.elements.isEmpty()) {
        buildWarnings.add("No elements defined in calculated design.")
    }

    if (design.feedRecommendation.feedMethod.isBlank()) {
        buildWarnings.add("Feed method is not defined.")
    }

    val drivenElement =
        design.elements.firstOrNull { it.role == ElementRole.DRIVEN }

    if (drivenElement != null) {
        tuningAdvice.add(
            "Small adjustments to the driven element length will shift resonance frequency."
        )
    }

    if (design.elements.size == 2) {
        improvementIdeas.add(
            "This appears to be a simple dipole-style design. Consider height and feedline routing for performance."
        )
    }

    if (design.elements.size >= 3) {
        improvementIdeas.add(
            "Directional antennas benefit from careful element spacing adjustments during tuning."
        )
    }

    if (design.buildGuidance.tuningSensitivity.name == "HIGH") {
        tuningAdvice.add(
            "This antenna design is sensitive to small physical changes during tuning."
        )
    }

    return DesignAnalysisResult(
        tuningAdvice = tuningAdvice,
        buildWarnings = buildWarnings,
        improvementIdeas = improvementIdeas
    )
}