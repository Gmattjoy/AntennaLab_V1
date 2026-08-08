package com.example.antennalab_v1

import androidx.compose.ui.graphics.Color
import com.example.antennalab_v1.ui.theme.AccentOrange
import com.example.antennalab_v1.ui.theme.AntennaLabSpacing
import com.example.antennalab_v1.ui.theme.AntennaLabTouch
import com.example.antennalab_v1.ui.theme.DarkAntennaLabSemanticColors
import com.example.antennalab_v1.ui.theme.DarkOnPrimary
import com.example.antennalab_v1.ui.theme.DarkPrimary
import com.example.antennalab_v1.ui.theme.LightAntennaLabSemanticColors
import com.example.antennalab_v1.ui.theme.LightOnPrimary
import com.example.antennalab_v1.ui.theme.LightPrimary
import com.example.antennalab_v1.ui.theme.OnAccentOrange
import com.example.antennalab_v1.ui.theme.StatusBad
import com.example.antennalab_v1.ui.theme.StatusGood
import com.example.antennalab_v1.ui.theme.StatusWarning
import com.example.antennalab_v1.ui.theme.antennaLabSemanticColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Invariants for the Phase 0 design tokens. Compose Dp/Color are JVM value types,
 * so this needs no Robolectric. It locks the structural guarantees a screen would
 * otherwise silently break — above all the gloved-use touch-target floor.
 */
class DesignTokensTest {

    // ---- Touch targets: the gloved-minimum guard -----------------------

    @Test
    fun touchTargets_allMeetMaterialFloor_andAreOrdered() {
        // 48 dp is the accessibility floor — nothing tappable may go below it.
        assertTrue("min must be >= 48 dp", AntennaLabTouch.min.value >= 48f)
        assertTrue("comfortable must be >= 48 dp", AntennaLabTouch.comfortable.value >= 48f)
        assertTrue("field must be >= 48 dp", AntennaLabTouch.field.value >= 48f)

        assertTrue(AntennaLabTouch.min <= AntennaLabTouch.comfortable)
        assertTrue(AntennaLabTouch.comfortable <= AntennaLabTouch.field)

        // Pin the current starting values so a change is deliberate.
        assertEquals(48f, AntennaLabTouch.min.value, 0f)
        assertEquals(56f, AntennaLabTouch.comfortable.value, 0f)
        assertEquals(64f, AntennaLabTouch.field.value, 0f)
    }

    // ---- Spacing: 4 dp grid, strictly increasing -----------------------

    @Test
    fun spacing_isOnFourDpGrid_andStrictlyIncreasing() {
        val scale = listOf(
            AntennaLabSpacing.xs,
            AntennaLabSpacing.sm,
            AntennaLabSpacing.md,
            AntennaLabSpacing.lg,
            AntennaLabSpacing.xl,
            AntennaLabSpacing.xxl
        )
        for (step in scale) {
            assertEquals("every step is a multiple of 4 dp", 0f, step.value % 4f, 0f)
        }
        for (i in 1 until scale.size) {
            assertTrue("scale strictly increasing", scale[i] > scale[i - 1])
        }
    }

    // ---- Semantic colours: theme-aware, distinct, dark reuses legacy ----

    @Test
    fun semantic_lightAndDarkSetsDifferAndAreInternallyDistinct() {
        assertNotEquals(LightAntennaLabSemanticColors, DarkAntennaLabSemanticColors)

        for (set in listOf(LightAntennaLabSemanticColors, DarkAntennaLabSemanticColors)) {
            val roles = listOf(set.success, set.warning, set.danger, set.neutral, set.info)
            val distinct = roles.map { it.value }.toSet()
            assertEquals("all five semantic roles are distinct hues", roles.size, distinct.size)
        }
    }

    @Test
    fun semantic_darkSetReusesLegacyStatusColours() {
        assertEquals(StatusGood, DarkAntennaLabSemanticColors.success)
        assertEquals(StatusWarning, DarkAntennaLabSemanticColors.warning)
        assertEquals(StatusBad, DarkAntennaLabSemanticColors.danger)
    }

    @Test
    fun semanticSelector_followsThemeFlag() {
        assertEquals(DarkAntennaLabSemanticColors, antennaLabSemanticColors(darkTheme = true))
        assertEquals(LightAntennaLabSemanticColors, antennaLabSemanticColors(darkTheme = false))
    }

    // ---- Selected indicator: defined in both schemes, legible, not primary ----

    @Test
    fun selectedIndicator_isTheNeonOrangeInBothSchemes() {
        val neonOrange = Color(0xFFFF5C00)
        assertEquals(neonOrange, DarkAntennaLabSemanticColors.selectedIndicator)
        assertEquals(neonOrange, LightAntennaLabSemanticColors.selectedIndicator)
    }

    @Test
    fun accentOrange_isTheOneDefinitionBehindPrimaryAndSelection() {
        // Primary and selectedIndicator deliberately resolve to the SAME hue,
        // both aliasing AccentOrange. This pins that there is one definition
        // rather than two hexes that happen to match today and drift tomorrow.
        assertEquals(AccentOrange, DarkPrimary)
        assertEquals(AccentOrange, LightPrimary)
        assertEquals(AccentOrange, DarkAntennaLabSemanticColors.selectedIndicator)
        assertEquals(AccentOrange, LightAntennaLabSemanticColors.selectedIndicator)

        assertEquals(OnAccentOrange, DarkOnPrimary)
        assertEquals(OnAccentOrange, LightOnPrimary)
    }

    @Test
    fun onPrimary_meetsWcagAaOnPrimary_inBothSchemes() {
        // Primary is now a mid-tone orange, so the light scheme's old
        // near-white onPrimary would have failed here. Guard both schemes.
        assertTrue(contrastRatio(DarkOnPrimary, DarkPrimary) >= 4.5)
        assertTrue(contrastRatio(LightOnPrimary, LightPrimary) >= 4.5)
    }

    @Test
    fun selectedIndicator_clashesWithWarningButIsNotTheSameToken() {
        // The visual clash with warning amber was reviewed and accepted; what
        // must not happen is the two silently collapsing into one value.
        for (set in listOf(LightAntennaLabSemanticColors, DarkAntennaLabSemanticColors)) {
            assertNotEquals(set.warning, set.selectedIndicator)
        }
    }

    @Test
    fun selectedIndicatorLabel_meetsWcagAaOnTheFill() {
        // If the orange is ever re-tinted, this fails and the LABEL is what
        // gets adjusted — never the orange. 4.5:1 is the AA floor for normal
        // text, which is what the button's SemiBold label counts as.
        for (set in listOf(LightAntennaLabSemanticColors, DarkAntennaLabSemanticColors)) {
            val ratio = contrastRatio(set.onSelectedIndicator, set.selectedIndicator)
            assertTrue(
                "label on selectedIndicator must clear WCAG AA, was $ratio",
                ratio >= 4.5
            )
        }
    }

    /** WCAG 2.x contrast ratio; order-independent. */
    private fun contrastRatio(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double =
        0.2126 * linearize(color.red) +
            0.7152 * linearize(color.green) +
            0.0722 * linearize(color.blue)

    private fun linearize(channel: Float): Double {
        val c = channel.toDouble()
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
}
