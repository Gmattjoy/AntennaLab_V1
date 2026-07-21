package com.example.antennalab_v1.domain.analysis

/*
------------------------------------------------------------
EDIT SECTION 1000
FILE HEADER
------------------------------------------------------------
FILE: TuningWorkflowBuilder.kt
PACKAGE: com.example.antennalab_v1.domain.analysis
LAYER: Domain / Analysis

SYSTEM ROLE
Builds a structured step-by-step tuning workflow from the current
analysis outputs.

CURRENT DEVELOPMENT ROLE
Combines:

• AntennaBehaviorClassification
• TuningSuggestionReport
• AdjustmentEstimate

into a practical ordered workflow that can later drive:

• Tuning Assistant UI
• Engineering dashboard workflow panels
• Guided novice tuning support
• Future expert workflow tools

DESIGN GOAL
Provide a stable first-pass workflow system that is easy to understand,
safe to expand, and conservative in its recommendations.

IMPORTANT MAKER RULE
For normal maker workflow after first build and first test, tuning
should assume trim-down operation. A finished radiator that is already
too short should not be treated as a simple add-length workflow step.
Instead the workflow should stop trimming and move toward rebuild,
replacement length, or retargeting.

SAFE EDIT AREA
- add antenna-type-specific workflow steps later
- add hardware-specific verification steps later
- add confidence and gating logic later
- add novice/expert workflow variants later
------------------------------------------------------------
*/

/*
------------------------------------------------------------
EDIT SECTION 1100
STEP KIND ENUM
------------------------------------------------------------
PURPOSE
Defines the major categories of steps that may appear in a tuning
workflow.
------------------------------------------------------------
*/
enum class TuningWorkflowStepKind {
    ADJUSTMENT,
    VERIFICATION,
    RE_MEASUREMENT,
    INSPECTION,
    CONFIRMATION,
    CAUTION
}

/*
------------------------------------------------------------
EDIT SECTION 1200
STEP MODEL
------------------------------------------------------------
PURPOSE
Stable model for one step in the tuning workflow.

SAFE EDIT AREA
- add UI hint metadata later
- add blocking / optional flags later
------------------------------------------------------------
*/
data class TuningWorkflowStep(
    val stepNumber: Int,
    val kind: TuningWorkflowStepKind,
    val title: String,
    val detail: String
)

/*
------------------------------------------------------------
EDIT SECTION 1300
REPORT MODEL
------------------------------------------------------------
PURPOSE
Bundles the generated workflow into a single stable result for future UI
and assistant systems.
------------------------------------------------------------
*/
data class TuningWorkflowReport(
    val headline: String,
    val summary: String,
    val steps: List<TuningWorkflowStep>
)

/*
------------------------------------------------------------
EDIT SECTION 1400
BUILDER OBJECT
------------------------------------------------------------
PURPOSE
Public entry point for converting analysis outputs into a structured
tuning workflow.
------------------------------------------------------------
*/
object TuningWorkflowBuilder {

    /*
    ------------------------------------------------------------
    EDIT SECTION 1401
    BUILD ENTRY
    ------------------------------------------------------------
    PURPOSE
    Builds a practical ordered workflow from classification, suggestion,
    and adjustment estimate outputs.
    ------------------------------------------------------------
    */
    fun build(
        classification: AntennaBehaviorClassification,
        suggestionReport: TuningSuggestionReport,
        adjustmentEstimate: AdjustmentEstimate
    ): TuningWorkflowReport {

        /*
        ------------------------------------------------------------
        EDIT SECTION 1402
        BUILD SETUP
        ------------------------------------------------------------
        */
        val rawSteps = mutableListOf<WorkflowSeedStep>()

        /*
        ------------------------------------------------------------
        EDIT SECTION 1403
        BUILD PASSES
        ------------------------------------------------------------
        */
        addPrimaryActionStep(
            classification = classification,
            suggestionReport = suggestionReport,
            adjustmentEstimate = adjustmentEstimate,
            steps = rawSteps
        )

        addInspectionSteps(
            classification = classification,
            suggestionReport = suggestionReport,
            steps = rawSteps
        )

        addMeasurementLoopSteps(
            classification = classification,
            adjustmentEstimate = adjustmentEstimate,
            steps = rawSteps
        )

        addCautionSteps(
            classification = classification,
            adjustmentEstimate = adjustmentEstimate,
            steps = rawSteps
        )

        /*
        ------------------------------------------------------------
        EDIT SECTION 1404
        STEP FINALIZE
        ------------------------------------------------------------
        */
        val orderedSteps = rawSteps
            .distinctBy { Triple(it.kind, it.title, it.detail) }
            .mapIndexed { index, step ->
                TuningWorkflowStep(
                    stepNumber = index + 1,
                    kind = step.kind,
                    title = step.title,
                    detail = step.detail
                )
            }

        return TuningWorkflowReport(
            headline = buildHeadline(classification, adjustmentEstimate),
            summary = buildSummary(classification, suggestionReport, adjustmentEstimate),
            steps = orderedSteps
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2000
    PRIMARY ACTION BUILDER
    ------------------------------------------------------------
    PURPOSE
    Builds the first main tuning action the user should take.
    ------------------------------------------------------------
    */
    private fun addPrimaryActionStep(
        classification: AntennaBehaviorClassification,
        suggestionReport: TuningSuggestionReport,
        adjustmentEstimate: AdjustmentEstimate,
        steps: MutableList<WorkflowSeedStep>
    ) {
        when (adjustmentEstimate.direction) {
            AdjustmentDirection.SHORTEN -> {
                steps += WorkflowSeedStep(
                    kind = TuningWorkflowStepKind.ADJUSTMENT,
                    title = "Shorten the radiator slightly",
                    detail = buildAdjustmentDetail(
                        baseText = "The sweep analysis suggests the antenna is electrically long relative to target.",
                        adjustmentEstimate = adjustmentEstimate
                    )
                )
            }

            AdjustmentDirection.REBUILD_OR_REPLACE -> {
                steps += WorkflowSeedStep(
                    kind = TuningWorkflowStepKind.ADJUSTMENT,
                    title = "Stop trimming and rebuild or replace radiator length",
                    detail = "The sweep analysis suggests the finished antenna is electrically short relative to target. Do not continue trim-down tuning on this radiator. Move to rebuild, replacement length, replacement part, or another safe correction path."
                )

                steps += WorkflowSeedStep(
                    kind = TuningWorkflowStepKind.CAUTION,
                    title = "Do not try to correct this with more cutting",
                    detail = "Further trimming will worsen the error. Treat this as an over-trim or short-build condition rather than a normal tuning step."
                )
            }

            AdjustmentDirection.RETARGET_HIGHER -> {
                steps += WorkflowSeedStep(
                    kind = TuningWorkflowStepKind.ADJUSTMENT,
                    title = "Consider retargeting to a higher frequency",
                    detail = "The current radiator may be better suited to a higher operating frequency. Use this path when rebuild or replacement is not practical."
                )
            }

            AdjustmentDirection.NONE -> {
                steps += WorkflowSeedStep(
                    kind = TuningWorkflowStepKind.ADJUSTMENT,
                    title = suggestionReport.primaryRecommendation,
                    detail = "No direct length correction is currently indicated. Follow the primary tuning recommendation generated by the analysis system."
                )
            }
        }

        if (
            classification.primaryBehavior == AntennaBehaviorType.GENERALLY_WELL_MATCHED &&
            adjustmentEstimate.direction == AdjustmentDirection.NONE
        ) {
            steps += WorkflowSeedStep(
                kind = TuningWorkflowStepKind.CONFIRMATION,
                title = "Hold major physical changes",
                detail = "The antenna already appears close to target. Avoid large changes unless you are chasing a specific operating goal."
            )
        }
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2100
    INSPECTION BUILDER
    ------------------------------------------------------------
    PURPOSE
    Adds inspection-oriented steps based on behavior and suggestion
    outputs.

    SAFE EDIT AREA
    - add richer mapping later
    ------------------------------------------------------------
    */
    private fun addInspectionSteps(
        classification: AntennaBehaviorClassification,
        suggestionReport: TuningSuggestionReport,
        steps: MutableList<WorkflowSeedStep>
    ) {
        val allBehaviors = listOf(classification.primaryBehavior) + classification.supportingBehaviors

        if (
            AntennaBehaviorType.FLAT_POOR_MATCH in allBehaviors ||
            AntennaBehaviorType.FEEDLINE_INTERACTION_SUSPECTED in allBehaviors
        ) {
            steps += WorkflowSeedStep(
                kind = TuningWorkflowStepKind.INSPECTION,
                title = "Inspect feedline and feedpoint",
                detail = "Check coax routing, connectors, adapter quality, feedpoint attachment, and obvious open or short conditions before making major geometry changes."
            )
        }

        if (
            AntennaBehaviorType.BROAD_LOSSY_RESPONSE in allBehaviors ||
            AntennaBehaviorType.HIGH_LOSS_OR_WEAK_RADIATION in allBehaviors
        ) {
            steps += WorkflowSeedStep(
                kind = TuningWorkflowStepKind.INSPECTION,
                title = "Inspect likely loss sources",
                detail = "Review joints, conductor quality, counterpoise effectiveness, loading components, and surrounding materials that may flatten or degrade the response."
            )
        }

        if (
            AntennaBehaviorType.MULTIPLE_RESONANCE_CANDIDATES in allBehaviors ||
            AntennaBehaviorType.COMPLEX_MULTI_RESONANCE in allBehaviors
        ) {
            steps += WorkflowSeedStep(
                kind = TuningWorkflowStepKind.INSPECTION,
                title = "Check for multiple active resonant paths",
                detail = "Look for traps, coupled sections, nearby conductive objects, or measurement setup geometry that could be creating more than one significant dip."
            )
        }

        if (AntennaBehaviorType.IMPEDANCE_UNSTABLE in allBehaviors) {
            steps += WorkflowSeedStep(
                kind = TuningWorkflowStepKind.INSPECTION,
                title = "Review impedance sensitivity factors",
                detail = "Check whether the current tuning point sits on a steep region of the response and whether the matching arrangement or geometry is overly sensitive."
            )
        }

        if (suggestionReport.suggestions.any { it.actionType == TuningActionType.CHECK_GROUND_SYSTEM }) {
            steps += WorkflowSeedStep(
                kind = TuningWorkflowStepKind.INSPECTION,
                title = "Verify ground or counterpoise conditions",
                detail = "If this antenna depends on a ground path or counterpoise, confirm that the return-current path is behaving as intended before continuing with trimming."
            )
        }

        if (suggestionReport.suggestions.any { it.actionType == TuningActionType.CHECK_MATCHING_NETWORK }) {
            steps += WorkflowSeedStep(
                kind = TuningWorkflowStepKind.INSPECTION,
                title = "Review matching network behavior",
                detail = "Check whether matching components are centered properly and whether the network is pushing the antenna to an unstable tuning point."
            )
        }

        if (suggestionReport.suggestions.any { it.actionType == TuningActionType.REPLACE_OR_REBUILD_RADIATOR }) {
            steps += WorkflowSeedStep(
                kind = TuningWorkflowStepKind.INSPECTION,
                title = "Verify actual built radiator length",
                detail = "Check whether the finished part was cut short, whether section lengths match the intended design, and whether the active radiator length still suits the target frequency."
            )
        }
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2200
    MEASUREMENT LOOP BUILDER
    ------------------------------------------------------------
    PURPOSE
    Adds the re-measurement loop and final confirmation steps that make
    the workflow practical.
    ------------------------------------------------------------
    */
    private fun addMeasurementLoopSteps(
        classification: AntennaBehaviorClassification,
        adjustmentEstimate: AdjustmentEstimate,
        steps: MutableList<WorkflowSeedStep>
    ) {
        val incrementText = when (adjustmentEstimate.severity) {
            AdjustmentSeverity.VERY_SMALL -> "Use very small physical steps."
            AdjustmentSeverity.SMALL -> "Use small physical steps."
            AdjustmentSeverity.MEDIUM -> "Use moderate but still controlled steps."
            AdjustmentSeverity.LARGE -> "Use staged corrections rather than one large cut or rebuild."
            AdjustmentSeverity.NONE -> "Use no physical change unless later checks justify it."
        }

        steps += WorkflowSeedStep(
            kind = TuningWorkflowStepKind.VERIFICATION,
            title = "Make only one controlled change at a time",
            detail = when (adjustmentEstimate.direction) {
                AdjustmentDirection.SHORTEN ->
                    "$incrementText Change one variable, then stop and re-measure before doing anything else."

                AdjustmentDirection.REBUILD_OR_REPLACE ->
                    "Do not mix rebuild decisions with multiple other changes at once. Verify the root cause, make one controlled correction path decision, then re-measure."

                AdjustmentDirection.RETARGET_HIGHER ->
                    "Confirm the new intended operating target before making further physical changes."

                AdjustmentDirection.NONE ->
                    "$incrementText Change one variable, then stop and re-measure before doing anything else."
            }
        )

        steps += WorkflowSeedStep(
            kind = TuningWorkflowStepKind.RE_MEASUREMENT,
            title = "Re-run the sweep",
            detail = "After the change or inspection, re-run the sweep and compare resonance position, minimum SWR, bandwidth, and impedance movement."
        )

        steps += WorkflowSeedStep(
            kind = TuningWorkflowStepKind.CONFIRMATION,
            title = "Confirm movement toward target",
            detail = buildConfirmationDetail(classification, adjustmentEstimate)
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2300
    CAUTION BUILDER
    ------------------------------------------------------------
    PURPOSE
    Adds conservative workflow cautions for large or uncertain
    corrections.
    ------------------------------------------------------------
    */
    private fun addCautionSteps(
        classification: AntennaBehaviorClassification,
        adjustmentEstimate: AdjustmentEstimate,
        steps: MutableList<WorkflowSeedStep>
    ) {
        when (adjustmentEstimate.severity) {
            AdjustmentSeverity.LARGE -> {
                steps += WorkflowSeedStep(
                    kind = TuningWorkflowStepKind.CAUTION,
                    title = "Verify before major physical change",
                    detail = "A large estimated correction can point to setup issues, model mismatch, or measurement conditions. Re-check the configuration before making irreversible changes."
                )
            }

            AdjustmentSeverity.NONE -> {
                steps += WorkflowSeedStep(
                    kind = TuningWorkflowStepKind.CAUTION,
                    title = "Do not force a change without evidence",
                    detail = "No clear length adjustment is currently indicated. Use repeated measurements and inspection findings to justify the next step."
                )
            }

            AdjustmentSeverity.VERY_SMALL,
            AdjustmentSeverity.SMALL,
            AdjustmentSeverity.MEDIUM -> {
                /* No extra caution required yet. */
            }
        }

        if (
            classification.primaryBehavior == AntennaBehaviorType.FLAT_POOR_MATCH ||
            classification.primaryBehavior == AntennaBehaviorType.FEEDLINE_INTERACTION_SUSPECTED
        ) {
            steps += WorkflowSeedStep(
                kind = TuningWorkflowStepKind.CAUTION,
                title = "Prefer setup checks before cutting",
                detail = "The current behavior can be caused by measurement setup issues. Confirm feedline and feedpoint conditions before irreversible radiator changes."
            )
        }

        if (
            adjustmentEstimate.direction == AdjustmentDirection.REBUILD_OR_REPLACE ||
            classification.primaryBehavior == AntennaBehaviorType.RESONANCE_ABOVE_TARGET ||
            classification.primaryBehavior == AntennaBehaviorType.LIKELY_TOO_SHORT
        ) {
            steps += WorkflowSeedStep(
                kind = TuningWorkflowStepKind.CAUTION,
                title = "Stop normal trim workflow if the radiator is already too short",
                detail = "A finished radiator resonating above target should not be treated as a normal add-length tuning case. Use rebuild, replacement length, replacement part, or retargeting instead."
            )
        }
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 3000
    HEADLINE BUILDER
    ------------------------------------------------------------
    PURPOSE
    Produces stable top-level headline text for workflow presentation.
    ------------------------------------------------------------
    */
    private fun buildHeadline(
        classification: AntennaBehaviorClassification,
        adjustmentEstimate: AdjustmentEstimate
    ): String {
        return when (adjustmentEstimate.direction) {
            AdjustmentDirection.SHORTEN ->
                "Tuning workflow: shorten toward target"

            AdjustmentDirection.REBUILD_OR_REPLACE ->
                "Tuning workflow: rebuild or replace short radiator"

            AdjustmentDirection.RETARGET_HIGHER ->
                "Tuning workflow: retarget to higher frequency"

            AdjustmentDirection.NONE ->
                "Tuning workflow: verify and refine ${classification.primaryBehavior.name}"
        }
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 3001
    SUMMARY BUILDER
    ------------------------------------------------------------
    PURPOSE
    Produces stable top-level summary text for workflow presentation.
    ------------------------------------------------------------
    */
    private fun buildSummary(
        classification: AntennaBehaviorClassification,
        suggestionReport: TuningSuggestionReport,
        adjustmentEstimate: AdjustmentEstimate
    ): String {
        val adjustmentText = when (adjustmentEstimate.direction) {
            AdjustmentDirection.SHORTEN ->
                adjustmentEstimate.estimatedLengthChangeMm?.let {
                    "Estimated physical change is about ${String.format("%.2f mm", it)} shortening."
                } ?: "Shortening direction is indicated, but a physical trim amount needs the active radiator length measurement."

            AdjustmentDirection.REBUILD_OR_REPLACE ->
                "The radiator appears electrically short relative to target. Stop trim-down tuning and use rebuild, replacement length, replacement part, or retargeting."

            AdjustmentDirection.RETARGET_HIGHER ->
                "The current radiator may be better suited to a higher operating frequency target."

            AdjustmentDirection.NONE ->
                "No direct length correction is currently indicated."
        }

        return "Primary behaviour: ${classification.primaryBehavior.name}. " +
                "Primary recommendation: ${suggestionReport.primaryRecommendation}. " +
                adjustmentText
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 3100
    ADJUSTMENT DETAIL
    ------------------------------------------------------------
    PURPOSE
    Shared text helper for adjustment-oriented step construction.
    ------------------------------------------------------------
    */
    private fun buildAdjustmentDetail(
        baseText: String,
        adjustmentEstimate: AdjustmentEstimate
    ): String {
        val estimateText = when {
            adjustmentEstimate.estimatedLengthChangeMm != null &&
                    adjustmentEstimate.estimatedLengthChangeInches != null -> {
                "Estimated physical change is approximately " +
                        "${String.format("%.2f mm", adjustmentEstimate.estimatedLengthChangeMm)} " +
                        "(${String.format("%.3f in", adjustmentEstimate.estimatedLengthChangeInches)})."
            }

            else -> {
                "A physical trim amount is not available until the active radiator length is measured."
            }
        }

        return "$baseText $estimateText Use controlled increments and re-sweep after each change."
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 3101
    CONFIRM DETAIL
    ------------------------------------------------------------
    PURPOSE
    Shared text helper for confirmation-oriented step construction.
    ------------------------------------------------------------
    */
    private fun buildConfirmationDetail(
        classification: AntennaBehaviorClassification,
        adjustmentEstimate: AdjustmentEstimate
    ): String {
        return when (classification.primaryBehavior) {
            AntennaBehaviorType.RESONANCE_BELOW_TARGET,
            AntennaBehaviorType.RESONANCE_ABOVE_TARGET,
            AntennaBehaviorType.GENERALLY_WELL_MATCHED,
            AntennaBehaviorType.LIKELY_TOO_SHORT,
            AntennaBehaviorType.LIKELY_TOO_LONG -> {
                when (adjustmentEstimate.direction) {
                    AdjustmentDirection.SHORTEN ->
                        "Confirm that resonance moved upward toward target and that SWR did not become worse elsewhere."

                    AdjustmentDirection.REBUILD_OR_REPLACE ->
                        "Confirm that the rebuilt or replaced radiator now places resonance closer to target and that the main dip is more usable."

                    AdjustmentDirection.RETARGET_HIGHER ->
                        "Confirm that the observed resonance now aligns with the newly intended higher target frequency."

                    AdjustmentDirection.NONE ->
                        "Confirm that the current resonance remains centered where expected and that no new mismatch issue appeared."
                }
            }

            else ->
                "Confirm whether the sweep became cleaner, the main dip became easier to identify, and the response moved toward a more stable match."
        }
    }
}

/*
------------------------------------------------------------
EDIT SECTION 9000
SEED MODEL
------------------------------------------------------------
PURPOSE
Internal helper model used while composing workflow steps before final
step numbering is applied.
------------------------------------------------------------
*/
private data class WorkflowSeedStep(
    val kind: TuningWorkflowStepKind,
    val title: String,
    val detail: String
)

/*
------------------------------------------------------------
END EDIT SECTIONS 9999
------------------------------------------------------------
*/