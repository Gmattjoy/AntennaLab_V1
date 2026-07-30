package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.SweepExportNaming
import com.example.antennalab_v1.domain.testing.SweepExportPlan
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coverage for [SweepExportPlan] — the API-tier decision behind sweep export.
 *
 * This is the whole reason the plan/writer split exists: the branch is a pure
 * function of `sdkInt`, so the code path least likely to be exercised by hand
 * (API 26-28, where MediaStore.Downloads does not exist) is an ordinary unit
 * test instead of a device hunt. Plain JVM, no Android imports.
 */
class SweepExportPlanTest {

    private fun sweep() = SweepResult(
        startFrequencyMHz = 14.0,
        endFrequencyMHz = 14.4,
        stepMHz = 0.01,
        points = listOf(
            SweepPoint(
                frequencyMHz = 14.2,
                swr = 1.5,
                returnLossDb = -10.0,
                resistance = 50.0,
                reactance = 0.0
            )
        )
    )

    // ------------------------------------------------------------------
    // API 29+ — MediaStore public Downloads
    // ------------------------------------------------------------------

    @Test
    fun api29AndAbove_useMediaStorePublicDownloads() {
        listOf(29, 30, 34, 36).forEach { sdkInt ->
            val plan = SweepExportPlan.planFor(sdkInt, "sweep.s1p")
            assertEquals(
                "sdk $sdkInt should use MediaStore",
                SweepExportPlan.Strategy.MEDIA_STORE,
                plan.strategy
            )
            assertTrue("sdk $sdkInt is a public save", plan.isPublicDownloads)
            // MediaStore's DIRECTORY_DOWNLOADS value is the singular "Download".
            assertEquals("Download/AntennaLab", plan.relativePath)
        }
    }

    // ------------------------------------------------------------------
    // API 26-28 — app-specific fallback
    // ------------------------------------------------------------------

    @Test
    fun api26To28_fallBackToAppSpecificStorage() {
        // MediaStore.Downloads does not exist before 29, and public storage
        // there would need legacy WRITE_EXTERNAL_STORAGE, which this app
        // deliberately never requests.
        listOf(26, 27, 28).forEach { sdkInt ->
            val plan = SweepExportPlan.planFor(sdkInt, "sweep.s1p")
            assertEquals(
                "sdk $sdkInt should fall back",
                SweepExportPlan.Strategy.APP_SPECIFIC,
                plan.strategy
            )
            assertFalse(
                "sdk $sdkInt must NOT claim a public Downloads save",
                plan.isPublicDownloads
            )
            assertEquals("AntennaLab", plan.relativePath)
        }
    }

    @Test
    fun theTierBoundaryIsExactlyTwentyNine() {
        // Off-by-one here would either crash on Android 9 (missing API) or
        // silently downgrade every modern device to app-private storage.
        assertEquals(
            SweepExportPlan.Strategy.APP_SPECIFIC,
            SweepExportPlan.planFor(28, "x.s1p").strategy
        )
        assertEquals(
            SweepExportPlan.Strategy.MEDIA_STORE,
            SweepExportPlan.planFor(29, "x.s1p").strategy
        )
        assertEquals(29, SweepExportPlan.MIN_SDK_FOR_MEDIA_STORE_DOWNLOADS)
    }

    @Test
    fun minSdk26_isCovered() {
        // The project's minSdk. If this ever returns MEDIA_STORE the app would
        // call a non-existent API on its oldest supported device.
        assertEquals(
            SweepExportPlan.Strategy.APP_SPECIFIC,
            SweepExportPlan.planFor(26, "x.s1p").strategy
        )
    }

    // ------------------------------------------------------------------
    // Tier-independent fields
    // ------------------------------------------------------------------

    @Test
    fun mimeTypeIsTextPlainOnEveryTier() {
        // Touchstone has no registered MIME type; application/octet-stream
        // makes many share targets refuse the file.
        assertEquals("text/plain", SweepExportPlan.planFor(26, "x.s1p").mimeType)
        assertEquals("text/plain", SweepExportPlan.planFor(36, "x.s1p").mimeType)
        assertEquals("text/plain", SweepExportPlan.MIME_TYPE)
    }

    @Test
    fun displayNameIsCarriedThroughUnchanged() {
        assertEquals(
            "given-name.s1p",
            SweepExportPlan.planFor(36, "given-name.s1p").displayName
        )
    }

    // ------------------------------------------------------------------
    // planS1pExport — naming delegated, not reimplemented
    // ------------------------------------------------------------------

    @Test
    fun planS1pExport_namesTheFileViaSweepExportNaming() {
        val result = sweep()
        val plan = SweepExportPlan.planS1pExport(
            sdkInt = 36,
            projectName = "Dipole20m",
            result = result,
            timestampLabel = "2026-07-30T1815"
        )
        assertEquals(
            SweepExportNaming.buildFileName(
                projectName = "Dipole20m",
                result = result,
                timestampLabel = "2026-07-30T1815"
            ),
            plan.displayName
        )
        // Centre of 14.0..14.4.
        assertEquals("Dipole20m_14.200MHz_2026-07-30T1815.s1p", plan.displayName)
    }

    @Test
    fun planS1pExport_stillPicksTheTierFromSdkInt() {
        val result = sweep()
        assertFalse(
            SweepExportPlan.planS1pExport(28, "p", result, "t").isPublicDownloads
        )
        assertTrue(
            SweepExportPlan.planS1pExport(29, "p", result, "t").isPublicDownloads
        )
    }
}
