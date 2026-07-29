package com.example.antennalab_v1

import com.example.antennalab_v1.ui.theme.AntennaLabSpacing
import com.example.antennalab_v1.ui.theme.AntennaLabTouch
import com.example.antennalab_v1.ui.theme.DarkAntennaLabSemanticColors
import com.example.antennalab_v1.ui.theme.LightAntennaLabSemanticColors
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
}
