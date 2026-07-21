package com.example.antennalab_v1.domain.analysis

/*
------------------------------------------------------------
EDIT SECTION 1000
FILE HEADER
------------------------------------------------------------
FILE: TuningSuggestionEngine.kt
PACKAGE: com.example.antennalab_v1.domain.analysis
LAYER: Domain / Analysis

SYSTEM ROLE
Converts sweep behaviour classification into practical tuning guidance.

CURRENT DEVELOPMENT ROLE
This engine provides the stable first-pass recommendation layer that
turns analysis results into suggested next actions for the user.

It supports:

• SweepGraphScreen guidance panels
• Tuning workflow generation
• Engineering dashboard recommendations
• Educational RF explanations
• Future semi-automatic tuning coaching

DESIGN GOAL
Keep recommendations conservative, understandable, and physically
plausible.

IMPORTANT MAKER RULE
For typical first-build wire or cable antennas, post-build tuning should
assume trim-down operation. After the first cut/build, the maker usually
cannot practically add length back to the same finished radiator.

SAFE EDIT AREA
- add confidence scoring later
- add hardware-specific guidance later
- add antenna-type-specific rules later
- add matching-network-specific actions later
------------------------------------------------------------
*/

/*
------------------------------------------------------------
EDIT SECTION 1100
PRIORITY ENUM
------------------------------------------------------------
PURPOSE
Provides a simple priority scale so future UI can sort or highlight
important recommendations.
------------------------------------------------------------
*/
enum class TuningSuggestionPriority {
    LOW,
    MEDIUM,
    HIGH
}

/*
------------------------------------------------------------
EDIT SECTION 1200
ACTION ENUM
------------------------------------------------------------
PURPOSE
Defines reusable recommendation action types.

SAFE EDIT AREA
- add more actions as tuning workflows expand
------------------------------------------------------------
*/
enum class TuningActionType {
    SHORTEN_RADIATOR,
    REPLACE_OR_REBUILD_RADIATOR,
    RETARGET_TO_HIGHER_FREQUENCY,
    STOP_TRIM_WORKFLOW,
    CHECK_FEEDPOINT,
    CHECK_CONNECTOR_AND_COAX,
    CHECK_GROUND_SYSTEM,
    CHECK_MATCHING_NETWORK,
    CHECK_NEARBY_COUPLING,
    CHECK_TRAPS_OR_MULTIPLE_ELEMENTS,
    REPEAT_SWEEP_WITH_WIDER_RANGE,
    VERIFY_MECHANICAL_DIMENSIONS,
    REVIEW_LOSS_SOURCES,
    REDUCE_ADJUSTMENT_STEP_SIZE,
    REVIEW_MULTI_RESONANCE_STRUCTURE,
    NO_MAJOR_CHANGE_RECOMMENDED
}

/*
------------------------------------------------------------
EDIT SECTION 1300
SUGGESTION MODEL
------------------------------------------------------------
PURPOSE
Stable output structure for UI and future workflow systems.

SAFE EDIT AREA
- add confidence
- add estimated adjustment amount
- add ordered workflow groups later
------------------------------------------------------------
*/
data class TuningSuggestion(
    val actionType: TuningActionType,
    val priority: TuningSuggestionPriority,
    val title: String,
    val detail: String
)

/*
------------------------------------------------------------
EDIT SECTION 1400
REPORT MODEL
------------------------------------------------------------
PURPOSE
Bundles the full recommendation output into a single stable result for
UI and higher-level assistant systems.
------------------------------------------------------------
*/
data class TuningSuggestionReport(
    val primaryRecommendation: String,
    val summary: String,
    val suggestions: List<TuningSuggestion>
)

/*
------------------------------------------------------------
EDIT SECTION 1500
ENGINE OBJECT
------------------------------------------------------------
PURPOSE
Public entry point for converting behaviour classification into
practical tuning guidance.
------------------------------------------------------------
*/
object TuningSuggestionEngine {

    /*
    ------------------------------------------------------------
    EDIT SECTION 1501
    GENERATE ENTRY
    ------------------------------------------------------------
    PURPOSE
    Builds a stable recommendation report from antenna behaviour
    classification and target/resonance relationship.
    ------------------------------------------------------------
    */
    fun generate(
        classification: AntennaBehaviorClassification,
        targetFrequencyMHz: Double,
        detectedResonanceMHz: Double?
    ): TuningSuggestionReport {

        /*
        ------------------------------------------------------------
        EDIT SECTION 1502
        BUILD SETUP
        ------------------------------------------------------------
        */
        val suggestions = mutableListOf<TuningSuggestion>()

        /*
        ------------------------------------------------------------
        EDIT SECTION 1503
        BUILD PASSES
        ------------------------------------------------------------
        */
        addPrimaryBehaviorSuggestions(
            classification = classification,
            targetFrequencyMHz = targetFrequencyMHz,
            detectedResonanceMHz = detectedResonanceMHz,
            suggestions = suggestions
        )

        addSupportingBehaviorSuggestions(
            classification = classification,
            suggestions = suggestions
        )

        /*
        ------------------------------------------------------------
        EDIT SECTION 1504
        FALLBACK RULE
        ------------------------------------------------------------
        */
        if (suggestions.isEmpty()) {
            suggestions += TuningSuggestion(
                actionType = TuningActionType.REPEAT_SWEEP_WITH_WIDER_RANGE,
                priority = TuningSuggestionPriority.LOW,
                title = "Repeat sweep with wider range",
                detail = "Current analysis did not produce a strong tuning direction. Repeat the sweep over a wider frequency span and review the impedance movement again."
            )
        }

        /*
        ------------------------------------------------------------
        EDIT SECTION 1505
        RESULT BUILD
        ------------------------------------------------------------
        */
        val distinctSuggestions = suggestions.distinctBy { suggestion ->
            suggestion.actionType to suggestion.title
        }

        val primaryRecommendation = distinctSuggestions.first().title
        val summary = buildSummary(
            classification = classification,
            targetFrequencyMHz = targetFrequencyMHz,
            detectedResonanceMHz = detectedResonanceMHz,
            suggestions = distinctSuggestions
        )

        return TuningSuggestionReport(
            primaryRecommendation = primaryRecommendation,
            summary = summary,
            suggestions = distinctSuggestions
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2000
    PRIMARY RULES
    ------------------------------------------------------------
    PURPOSE
    Adds first-pass recommendations based on the primary classified
    behaviour.

    SAFE EDIT AREA
    - refine thresholds and wording
    - introduce antenna-type-specific logic later
    ------------------------------------------------------------
    */
    private fun addPrimaryBehaviorSuggestions(
        classification: AntennaBehaviorClassification,
        targetFrequencyMHz: Double,
        detectedResonanceMHz: Double?,
        suggestions: MutableList<TuningSuggestion>
    ) {
        when (classification.primaryBehavior) {
            AntennaBehaviorType.RESONANCE_BELOW_TARGET -> {
                suggestions += buildResonanceBelowTargetSuggestion(
                    targetFrequencyMHz = targetFrequencyMHz,
                    detectedResonanceMHz = detectedResonanceMHz
                )
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.VERIFY_MECHANICAL_DIMENSIONS,
                    priority = TuningSuggestionPriority.MEDIUM,
                    title = "Verify radiator length",
                    detail = "The main resonance appears below target frequency, which usually points to an electrically long structure. Check total element length, added wire tails, loading effects, and end geometry."
                )
            }

            AntennaBehaviorType.RESONANCE_ABOVE_TARGET -> {
                suggestions += buildResonanceAboveTargetSuggestion(
                    targetFrequencyMHz = targetFrequencyMHz,
                    detectedResonanceMHz = detectedResonanceMHz
                )
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.STOP_TRIM_WORKFLOW,
                    priority = TuningSuggestionPriority.HIGH,
                    title = "Stop trim-down tuning on this radiator",
                    detail = "Do not continue a normal trim workflow when the finished radiator is already resonating above target. More cutting will move it further away. Treat this as an over-trim or short-build condition."
                )
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.VERIFY_MECHANICAL_DIMENSIONS,
                    priority = TuningSuggestionPriority.HIGH,
                    title = "Treat this as over-trim or short build",
                    detail = "For normal maker workflow, a finished radiator that already resonates above target is usually too short and cannot be corrected by simple trim tuning. Check whether the part was cut too short, then rebuild or replace that length or retarget the antenna to a higher frequency."
                )
            }

            AntennaBehaviorType.HIGH_Q_NARROW_RESONANCE -> {
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.REDUCE_ADJUSTMENT_STEP_SIZE,
                    priority = TuningSuggestionPriority.MEDIUM,
                    title = "Use small adjustment steps",
                    detail = "The response is narrow and sharp. Small physical changes can move resonance quickly, so adjust in small trim increments and re-sweep after each change."
                )
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.REPEAT_SWEEP_WITH_WIDER_RANGE,
                    priority = TuningSuggestionPriority.LOW,
                    title = "Check operating bandwidth",
                    detail = "A narrow high-Q response may be efficient but bandwidth-limited. Sweep a wider range to confirm how fast match quality falls away from resonance."
                )
            }

            AntennaBehaviorType.BROAD_LOSSY_RESPONSE -> {
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.REVIEW_LOSS_SOURCES,
                    priority = TuningSuggestionPriority.HIGH,
                    title = "Inspect likely loss sources",
                    detail = "A broad shallow response can indicate system loss. Check conductor quality, loading components, poor joints, undersized wire, and any materials that may be dissipating energy."
                )
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.CHECK_GROUND_SYSTEM,
                    priority = TuningSuggestionPriority.MEDIUM,
                    title = "Review ground or counterpoise effectiveness",
                    detail = "If this antenna depends on a ground path or counterpoise, poor ground conditions can flatten and degrade the response."
                )
            }

            AntennaBehaviorType.MULTIPLE_RESONANCE_CANDIDATES -> {
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.CHECK_TRAPS_OR_MULTIPLE_ELEMENTS,
                    priority = TuningSuggestionPriority.HIGH,
                    title = "Inspect multiple resonant structures",
                    detail = "Multiple low-SWR regions may indicate traps, coupled elements, unintended parasitic behaviour, or more than one active resonant path."
                )
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.CHECK_NEARBY_COUPLING,
                    priority = TuningSuggestionPriority.MEDIUM,
                    title = "Check nearby conductive coupling",
                    detail = "Nearby metal, feedline placement, or adjacent antenna parts may be creating secondary resonance effects."
                )
            }

            AntennaBehaviorType.FLAT_POOR_MATCH -> {
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.CHECK_CONNECTOR_AND_COAX,
                    priority = TuningSuggestionPriority.HIGH,
                    title = "Check connector and feedline integrity",
                    detail = "A flat poor match can mean the antenna is not being seen properly by the instrument. Inspect connectors, coax continuity, shorts, opens, and adapter quality."
                )
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.CHECK_FEEDPOINT,
                    priority = TuningSuggestionPriority.HIGH,
                    title = "Inspect the feedpoint connection",
                    detail = "Check for bad solder joints, incorrect feedpoint attachment, reversed connections, or a severe mismatch at the feedpoint."
                )
            }

            AntennaBehaviorType.FEEDLINE_INTERACTION_SUSPECTED -> {
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.CHECK_CONNECTOR_AND_COAX,
                    priority = TuningSuggestionPriority.HIGH,
                    title = "Review feedline routing and common-mode effects",
                    detail = "Interaction patterns can come from feedline position, common-mode current, measurement setup geometry, or unintended coupling through the coax."
                )
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.REPEAT_SWEEP_WITH_WIDER_RANGE,
                    priority = TuningSuggestionPriority.MEDIUM,
                    title = "Repeat sweep after setup cleanup",
                    detail = "Reposition the feedline, reduce nearby coupling influences, then repeat the sweep to see whether the interaction pattern remains."
                )
            }

            AntennaBehaviorType.IMPEDANCE_UNSTABLE -> {
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.CHECK_MATCHING_NETWORK,
                    priority = TuningSuggestionPriority.MEDIUM,
                    title = "Review matching network behaviour",
                    detail = "Strong impedance movement may indicate a sensitive or poorly centered match. Check matching components, feed arrangement, and resonance alignment."
                )
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.REPEAT_SWEEP_WITH_WIDER_RANGE,
                    priority = TuningSuggestionPriority.MEDIUM,
                    title = "Sweep wider to understand trend",
                    detail = "A wider sweep can show whether the current region is part of a single steep resonance or a more complicated impedance pattern."
                )
            }

            AntennaBehaviorType.GENERALLY_WELL_MATCHED -> {
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.NO_MAJOR_CHANGE_RECOMMENDED,
                    priority = TuningSuggestionPriority.LOW,
                    title = "No major tuning change recommended",
                    detail = "The antenna appears reasonably close to target. Only use small trim changes if you are chasing a very specific resonance shift or bandwidth improvement."
                )
            }

            AntennaBehaviorType.LIKELY_TOO_SHORT -> {
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.REPLACE_OR_REBUILD_RADIATOR,
                    priority = TuningSuggestionPriority.HIGH,
                    title = "Replace or rebuild the short radiator",
                    detail = "The analysis suggests the finished antenna is electrically too short for the target frequency. In normal maker workflow this usually means the part was cut too far. Replace or rebuild that radiator length or part, or retarget the antenna to a higher frequency."
                )
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.STOP_TRIM_WORKFLOW,
                    priority = TuningSuggestionPriority.HIGH,
                    title = "Stop trimming and reassess target",
                    detail = "Do not keep trimming when the antenna already appears too short. Further cutting will worsen the error. Move to rebuild, replacement, or retarget decisions."
                )
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.RETARGET_TO_HIGHER_FREQUENCY,
                    priority = TuningSuggestionPriority.MEDIUM,
                    title = "Consider retargeting to a higher frequency",
                    detail = "If rebuilding is not practical, the existing radiator may be better suited to a higher operating frequency than originally planned."
                )
            }

            AntennaBehaviorType.LIKELY_TOO_LONG -> {
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.SHORTEN_RADIATOR,
                    priority = TuningSuggestionPriority.HIGH,
                    title = "Shorten the radiator slightly",
                    detail = "The analysis suggests the antenna is electrically too long for the target frequency. Remove a small amount of length, then re-sweep."
                )
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.VERIFY_MECHANICAL_DIMENSIONS,
                    priority = TuningSuggestionPriority.MEDIUM,
                    title = "Verify added effective length",
                    detail = "Check for unintended added length, end effects, loading components, wire tails, or nearby conductive effects that increased electrical length."
                )
            }

            AntennaBehaviorType.HIGH_LOSS_OR_WEAK_RADIATION -> {
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.REVIEW_LOSS_SOURCES,
                    priority = TuningSuggestionPriority.HIGH,
                    title = "Inspect loss and radiation efficiency limits",
                    detail = "The analysis suggests either significant system loss or weak radiation coupling. Check conductors, joints, loading components, lossy materials, and whether the antenna is being effectively excited."
                )
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.CHECK_GROUND_SYSTEM,
                    priority = TuningSuggestionPriority.MEDIUM,
                    title = "Review return path or counterpoise",
                    detail = "Poor ground, counterpoise, or return-current conditions can make the antenna appear weak, broad, or inefficient."
                )
            }

            AntennaBehaviorType.COMPLEX_MULTI_RESONANCE -> {
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.REVIEW_MULTI_RESONANCE_STRUCTURE,
                    priority = TuningSuggestionPriority.HIGH,
                    title = "Inspect multiple interacting resonances",
                    detail = "The sweep suggests a more complex multi-resonance structure. Check for coupled elements, traps, multiple active paths, or geometry that creates more than one strong resonant mode."
                )
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.CHECK_NEARBY_COUPLING,
                    priority = TuningSuggestionPriority.MEDIUM,
                    title = "Reduce coupling variables before retest",
                    detail = "Move nearby conductors, simplify the setup, and repeat the sweep to confirm whether the complex response is intrinsic or caused by coupling."
                )
            }

            AntennaBehaviorType.INCONCLUSIVE -> {
                suggestions += TuningSuggestion(
                    actionType = TuningActionType.REPEAT_SWEEP_WITH_WIDER_RANGE,
                    priority = TuningSuggestionPriority.MEDIUM,
                    title = "Gather more sweep information",
                    detail = "The current sweep pattern is not decisive. Use a wider frequency range, confirm setup quality, and compare repeated measurements."
                )
            }
        }
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2100
    SUPPORTING RULES
    ------------------------------------------------------------
    PURPOSE
    Adds secondary recommendations from supporting behaviour indicators.

    SAFE EDIT AREA
    - tune prioritisation later
    - avoid redundant advice when richer logic is added later
    ------------------------------------------------------------
    */
    private fun addSupportingBehaviorSuggestions(
        classification: AntennaBehaviorClassification,
        suggestions: MutableList<TuningSuggestion>
    ) {
        classification.supportingBehaviors.forEach { behavior ->
            when (behavior) {
                AntennaBehaviorType.RESONANCE_BELOW_TARGET -> {
                    suggestions += TuningSuggestion(
                        actionType = TuningActionType.SHORTEN_RADIATOR,
                        priority = TuningSuggestionPriority.MEDIUM,
                        title = "Consider a small shortening step",
                        detail = "A supporting indicator suggests the antenna may be electrically long. Use a small physical shortening change, then re-sweep."
                    )
                }

                AntennaBehaviorType.RESONANCE_ABOVE_TARGET -> {
                    suggestions += TuningSuggestion(
                        actionType = TuningActionType.STOP_TRIM_WORKFLOW,
                        priority = TuningSuggestionPriority.MEDIUM,
                        title = "Stop trim-down changes on this build",
                        detail = "A supporting indicator suggests the finished antenna may already be electrically short. Do not continue cutting. Prefer rebuilding or replacing the affected length or part rather than assuming length can be added back neatly."
                    )
                    suggestions += TuningSuggestion(
                        actionType = TuningActionType.REPLACE_OR_REBUILD_RADIATOR,
                        priority = TuningSuggestionPriority.MEDIUM,
                        title = "Do not rely on adding length after build",
                        detail = "A supporting indicator suggests the finished antenna may already be electrically short. In normal maker workflow, prefer rebuilding or replacing the affected length or part rather than assuming length can be added back neatly."
                    )
                }

                AntennaBehaviorType.MULTIPLE_RESONANCE_CANDIDATES -> {
                    suggestions += TuningSuggestion(
                        actionType = TuningActionType.CHECK_NEARBY_COUPLING,
                        priority = TuningSuggestionPriority.MEDIUM,
                        title = "Reduce unintended coupling variables",
                        detail = "Temporarily simplify the setup, move nearby conductors away, and confirm whether the extra resonance features remain."
                    )
                }

                AntennaBehaviorType.FEEDLINE_INTERACTION_SUSPECTED -> {
                    suggestions += TuningSuggestion(
                        actionType = TuningActionType.CHECK_CONNECTOR_AND_COAX,
                        priority = TuningSuggestionPriority.MEDIUM,
                        title = "Verify coax influence on the measurement",
                        detail = "Try a more controlled cable route and inspect whether the measurement shifts substantially with feedline position."
                    )
                }

                AntennaBehaviorType.IMPEDANCE_UNSTABLE -> {
                    suggestions += TuningSuggestion(
                        actionType = TuningActionType.CHECK_MATCHING_NETWORK,
                        priority = TuningSuggestionPriority.MEDIUM,
                        title = "Check whether the match is centered correctly",
                        detail = "Unstable impedance may mean the current tuning point is sitting on a steep part of the response curve rather than at a robust center."
                    )
                }

                AntennaBehaviorType.BROAD_LOSSY_RESPONSE -> {
                    suggestions += TuningSuggestion(
                        actionType = TuningActionType.REVIEW_LOSS_SOURCES,
                        priority = TuningSuggestionPriority.MEDIUM,
                        title = "Review efficiency-related losses",
                        detail = "Lossy behaviour can come from conductors, components, joints, mounting effects, or poor return-current paths."
                    )
                }

                AntennaBehaviorType.FLAT_POOR_MATCH -> {
                    suggestions += TuningSuggestion(
                        actionType = TuningActionType.CHECK_FEEDPOINT,
                        priority = TuningSuggestionPriority.MEDIUM,
                        title = "Re-check feedpoint wiring",
                        detail = "A supporting flat-poor-match indicator makes it worth checking whether the antenna is connected as intended at the feedpoint."
                    )
                }

                AntennaBehaviorType.LIKELY_TOO_SHORT -> {
                    suggestions += TuningSuggestion(
                        actionType = TuningActionType.REPLACE_OR_REBUILD_RADIATOR,
                        priority = TuningSuggestionPriority.MEDIUM,
                        title = "Rebuild or replace the short section",
                        detail = "A supporting indicator suggests the antenna may be electrically too short. Prefer replacing or rebuilding that length rather than assuming length can be added neatly after build."
                    )
                    suggestions += TuningSuggestion(
                        actionType = TuningActionType.STOP_TRIM_WORKFLOW,
                        priority = TuningSuggestionPriority.MEDIUM,
                        title = "Do not continue cutting this radiator",
                        detail = "A supporting indicator suggests the radiator may already be too short. Stop trim-down tuning and move to rebuild, replacement, or retarget decisions."
                    )
                }

                AntennaBehaviorType.LIKELY_TOO_LONG -> {
                    suggestions += TuningSuggestion(
                        actionType = TuningActionType.SHORTEN_RADIATOR,
                        priority = TuningSuggestionPriority.MEDIUM,
                        title = "Try a small shortening step",
                        detail = "A supporting indicator suggests the antenna may be electrically too long. Remove a small amount of length, then re-sweep."
                    )
                }

                AntennaBehaviorType.HIGH_LOSS_OR_WEAK_RADIATION -> {
                    suggestions += TuningSuggestion(
                        actionType = TuningActionType.REVIEW_LOSS_SOURCES,
                        priority = TuningSuggestionPriority.MEDIUM,
                        title = "Check for weak radiation or excess loss",
                        detail = "A supporting indicator suggests the system may be losing energy or coupling poorly. Review conductors, joints, loading parts, and return-current conditions."
                    )
                }

                AntennaBehaviorType.COMPLEX_MULTI_RESONANCE -> {
                    suggestions += TuningSuggestion(
                        actionType = TuningActionType.REVIEW_MULTI_RESONANCE_STRUCTURE,
                        priority = TuningSuggestionPriority.MEDIUM,
                        title = "Review interacting resonant paths",
                        detail = "A supporting indicator suggests multiple resonant structures or coupled behaviour. Inspect traps, nearby elements, and geometry interactions."
                    )
                }

                AntennaBehaviorType.HIGH_Q_NARROW_RESONANCE,
                AntennaBehaviorType.GENERALLY_WELL_MATCHED,
                AntennaBehaviorType.INCONCLUSIVE -> {
                    /* No extra supporting rule required yet. */
                }
            }
        }
    }
    /*
    ------------------------------------------------------------
    EDIT SECTION 2200
    OFFSET HELPERS
    ------------------------------------------------------------
    PURPOSE
    Builds more useful recommendations when target and detected
    resonance are both known.

    SAFE EDIT AREA
    - replace rough physical guidance with antenna-type-aware logic later
    ------------------------------------------------------------
    */
    private fun buildResonanceBelowTargetSuggestion(
        targetFrequencyMHz: Double,
        detectedResonanceMHz: Double?
    ): TuningSuggestion {
        val detail = if (detectedResonanceMHz != null && targetFrequencyMHz > 0.0) {
            val offsetMHz = targetFrequencyMHz - detectedResonanceMHz

            "The detected resonance is below target, which usually means the antenna is electrically long. Consider shortening the radiator slightly. Current offset is ${String.format("%.3f MHz", offsetMHz)}. Use the actual radiator length measurement later if you want a physical trim amount in mm."
        } else {
            "The detected resonance is below target, which usually means the antenna is electrically long. Consider shortening the radiator slightly and re-sweeping."
        }

        return TuningSuggestion(
            actionType = TuningActionType.SHORTEN_RADIATOR,
            priority = TuningSuggestionPriority.HIGH,
            title = "Shorten the radiator slightly",
            detail = detail
        )
    }

    private fun buildResonanceAboveTargetSuggestion(
        targetFrequencyMHz: Double,
        detectedResonanceMHz: Double?
    ): TuningSuggestion {
        val detail = if (detectedResonanceMHz != null && targetFrequencyMHz > 0.0) {
            val offsetMHz = detectedResonanceMHz - targetFrequencyMHz

            "The detected resonance is above target, which usually means the finished antenna is electrically short. In normal maker workflow this is usually not a trim-up situation. Current offset is ${String.format("%.3f MHz", offsetMHz)}. Prefer replacing or rebuilding that length or part, or retargeting the antenna to a higher frequency."
        } else {
            "The detected resonance is above target, which usually means the finished antenna is electrically short. In normal maker workflow, prefer rebuilding or replacing that length or part rather than assuming length can be added back."
        }

        return TuningSuggestion(
            actionType = TuningActionType.REPLACE_OR_REBUILD_RADIATOR,
            priority = TuningSuggestionPriority.HIGH,
            title = "Replace or rebuild the short radiator",
            detail = detail
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 3000
    SUMMARY BUILDER
    ------------------------------------------------------------
    PURPOSE
    Produces a short human-readable summary for UI display.
    ------------------------------------------------------------
    */
    private fun buildSummary(
        classification: AntennaBehaviorClassification,
        targetFrequencyMHz: Double,
        detectedResonanceMHz: Double?,
        suggestions: List<TuningSuggestion>
    ): String {
        val resonanceText =
            if (detectedResonanceMHz != null) {
                String.format(
                    "Target %.3f MHz, detected resonance %.3f MHz.",
                    targetFrequencyMHz,
                    detectedResonanceMHz
                )
            } else {
                String.format(
                    "Target %.3f MHz. Detected resonance unavailable.",
                    targetFrequencyMHz
                )
            }

        val primaryText = "Primary behaviour: ${classification.primaryBehavior.name}."
        val firstActionText = " First recommendation: ${suggestions.firstOrNull()?.title ?: "Gather more sweep data"}."

        return primaryText + " " + resonanceText + firstActionText
    }
}

/*
------------------------------------------------------------
END EDIT SECTIONS 3999
------------------------------------------------------------
*/