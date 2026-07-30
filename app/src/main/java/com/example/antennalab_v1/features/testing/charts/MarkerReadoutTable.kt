package com.example.antennalab_v1.features.testing.charts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.domain.testing.MarkerReadout
import com.example.antennalab_v1.domain.testing.SweepMarkerMath
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.ui.theme.AntennaLabTheme
import com.example.antennalab_v1.ui.theme.AntennaLab_V1Theme

/*
########################################################################
FILE: MarkerReadoutTable.kt
PACKAGE: com.example.antennalab_v1.features.testing.charts
LAYER: Features / Testing / Shared chart components

SYSTEM ROLE
The first-class marker readout table from UI redesign spec 2.3 — the
values a NanoVNA-Saver user expects at each marker: |Z|, R + jX, Q,
Cs / Ls, return loss, phase, band.

THIS FILE COMPUTES NOTHING.
Every value is a pre-formatted string off MarkerReadout, built by
domain/testing/SweepMarkerMath.buildMarkerReadout. If a change here
needs arithmetic, it belongs in SweepMarkerMath instead.

Phase 3 ships this as a component with previews; the Phase-4 Sweep
Viewer decides where it docks (spec open question 2 — dock vs overlay vs
bottom sheet in Full mode on a tablet).
########################################################################
*/

/*
--------------------------------------------------------------------
One marker column
EDIT SECTION 1001
--------------------------------------------------------------------
*/
private val ROW_LABEL_WIDTH = 92.dp
private val MARKER_COLUMN_WIDTH = 148.dp

/*
--------------------------------------------------------------------
Readout table
EDIT SECTION 1002
--------------------------------------------------------------------
Renders one column per marker so A and B can be read side by side, with
the derived quantity names down the left. Scrolls horizontally rather
than compressing: these numbers are useless if truncated.

`markers` is ordered; labels default to A, B, C... Pass an empty list and
the table renders its empty state instead of an unexplained blank.
--------------------------------------------------------------------
*/
@Composable
fun MarkerReadoutTable(
    markers: List<MarkerReadout>,
    modifier: Modifier = Modifier,
    markerLabels: List<String> = DEFAULT_MARKER_LABELS,
    title: String = "Marker readout"
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AntennaLabTheme.spacing.md),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(AntennaLabTheme.spacing.lg)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (markers.isEmpty()) {
                Text(
                    modifier = Modifier.padding(top = AntennaLabTheme.spacing.sm),
                    text = "No marker placed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                MarkerHeaderRow(markers = markers, markerLabels = markerLabels)

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = AntennaLabTheme.spacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                READOUT_FIELDS.forEach { field ->
                    MarkerValueRow(
                        label = field.label,
                        values = markers.map(field.value)
                    )
                }
            }
        }
    }
}

/*
--------------------------------------------------------------------
Header — frequency and band per marker
EDIT SECTION 1003
--------------------------------------------------------------------
*/
@Composable
private fun MarkerHeaderRow(
    markers: List<MarkerReadout>,
    markerLabels: List<String>
) {
    Row(
        modifier = Modifier.padding(top = AntennaLabTheme.spacing.sm),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            modifier = Modifier.width(ROW_LABEL_WIDTH),
            text = "",
            style = MaterialTheme.typography.labelSmall
        )

        markers.forEachIndexed { index, marker ->
            Column(modifier = Modifier.width(MARKER_COLUMN_WIDTH)) {
                Text(
                    text = markerLabels.getOrElse(index) { "M${index + 1}" },
                    style = MaterialTheme.typography.labelSmall,
                    color = AntennaLabTheme.semantic.info,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = marker.frequencyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = marker.bandLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/*
--------------------------------------------------------------------
One derived quantity across every marker
EDIT SECTION 1004
--------------------------------------------------------------------
*/
@Composable
private fun MarkerValueRow(
    label: String,
    values: List<String>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AntennaLabTheme.spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            modifier = Modifier.width(ROW_LABEL_WIDTH),
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        values.forEach { value ->
            Text(
                modifier = Modifier.width(MARKER_COLUMN_WIDTH),
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/*
--------------------------------------------------------------------
The spec 2.3 row set
EDIT SECTION 1005
--------------------------------------------------------------------
Order matters: it is the order a VNA operator scans. Each entry is a
label plus a pure accessor onto an already-formatted string — no
formatting happens in this file.
--------------------------------------------------------------------
*/
private class ReadoutField(
    val label: String,
    val value: (MarkerReadout) -> String
)

private val READOUT_FIELDS: List<ReadoutField> = listOf(
    ReadoutField("SWR") { it.swrText },
    ReadoutField("Return loss") { it.returnLossText },
    ReadoutField("|Z|") { it.impedanceMagnitudeText },
    ReadoutField("R + jX") { it.impedanceText },
    ReadoutField("Q") { it.qText },
    ReadoutField("Cs / Ls") { it.seriesEquivalentText },
    ReadoutField("Phase") { it.phaseText }
)

private val DEFAULT_MARKER_LABELS = listOf("Marker A", "Marker B", "Marker C", "Marker D")

/*
====================================================================
PREVIEWS
EDIT SECTION 1006
====================================================================
Two markers straddling resonance: an inductive point above 50 ohm and a
capacitive one, so Q, Cs/Ls and phase all show non-trivial values.
--------------------------------------------------------------------
*/
private fun previewMarkers(): List<MarkerReadout> = listOf(
    SweepMarkerMath.buildMarkerReadout(
        SweepPoint(
            frequencyMHz = 14.200,
            swr = 2.618,
            returnLossDb = -7.0,
            resistance = 50.0,
            reactance = 50.0
        )
    ),
    SweepMarkerMath.buildMarkerReadout(
        SweepPoint(
            frequencyMHz = 14.050,
            swr = 1.12,
            returnLossDb = -25.4,
            resistance = 47.5,
            reactance = -6.2
        )
    )
)

@Preview(name = "Marker readout — dark", showBackground = true, widthDp = 380)
@Composable
private fun MarkerReadoutTableDarkPreview() {
    AntennaLab_V1Theme(darkTheme = true) {
        MarkerReadoutTable(
            markers = previewMarkers(),
            modifier = Modifier.padding(AntennaLabTheme.spacing.lg)
        )
    }
}

@Preview(name = "Marker readout — light", showBackground = true, widthDp = 380)
@Composable
private fun MarkerReadoutTableLightPreview() {
    AntennaLab_V1Theme(darkTheme = false) {
        MarkerReadoutTable(
            markers = previewMarkers(),
            modifier = Modifier.padding(AntennaLabTheme.spacing.lg)
        )
    }
}

@Preview(name = "Marker readout — empty", showBackground = true, widthDp = 380)
@Composable
private fun MarkerReadoutTableEmptyPreview() {
    AntennaLab_V1Theme(darkTheme = true) {
        MarkerReadoutTable(
            markers = emptyList(),
            modifier = Modifier.padding(AntennaLabTheme.spacing.lg)
        )
    }
}
