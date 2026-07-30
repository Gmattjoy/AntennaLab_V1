package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.SweepResult

/*
########################################################################
FILE: SweepExportPlan.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Export

SYSTEM ROLE
The decision half of sweep file export, kept pure so it can be tested
without a device. `storage/SweepExportWriter` does nothing but EXECUTE a
plan produced here — it makes no strategy choice and formats no name.

WHY THIS SPLIT EXISTS
The interesting part of export is the API-tier branch, and that branch is
a pure function of Build.VERSION.SDK_INT. Passing sdkInt in as a
parameter turns the one piece of logic most likely to be wrong (and least
likely to be exercised, since most handsets are API 29+) into an
ordinary unit test.

THE TIERS
  API 29+  MediaStore public Downloads. insert() hands back a content://
           URI that ACTION_SEND can share directly — no FileProvider.
  API 26-28 MediaStore.Downloads does not exist. Writing to public
           storage there would need legacy WRITE_EXTERNAL_STORAGE, which
           this app deliberately does not request, so the file goes to
           the app-specific external directory (no permission required
           since API 19) and is shared via FileProvider.

The tier is carried on the plan as `isPublicDownloads` so the UI can say
what actually happened instead of implying a public save that did not
occur.
########################################################################
*/
object SweepExportPlan {

    /*
    MediaStore.Downloads arrived in Android 10 (API 29, Build.VERSION_CODES.Q).
    Named here rather than referencing Build so this file stays free of
    Android imports.
    */
    const val MIN_SDK_FOR_MEDIA_STORE_DOWNLOADS = 29

    /*
    Touchstone has no registered MIME type. text/plain is honest (the file
    IS text) and, unlike application/octet-stream, share targets and text
    editors will accept it.
    */
    const val MIME_TYPE = "text/plain"

    /*
    Subdirectory inside public Downloads, so exports group together
    instead of scattering through the user's Downloads root.
    */
    const val DOWNLOADS_SUBDIRECTORY = "AntennaLab"

    enum class Strategy {
        /** API 29+: insert into MediaStore public Downloads. */
        MEDIA_STORE,

        /** API 26-28: app-specific external dir + FileProvider share. */
        APP_SPECIFIC
    }

    /*
    --------------------------------------------------------------------
    The plan
    EDIT SECTION 1001
    --------------------------------------------------------------------
    */
    data class Plan(
        val strategy: Strategy,
        val displayName: String,
        val mimeType: String,
        /**
         * MediaStore RELATIVE_PATH for the MEDIA_STORE strategy; the
         * subdirectory name for APP_SPECIFIC. Never a absolute path —
         * the writer resolves the base directory.
         */
        val relativePath: String,
        /**
         * True only when the file lands in public Downloads. The UI MUST
         * reflect this rather than claiming a public save on every tier.
         */
        val isPublicDownloads: Boolean
    )

    /*
    --------------------------------------------------------------------
    Plan a .s1p export
    EDIT SECTION 1002
    --------------------------------------------------------------------
    */
    fun planS1pExport(
        sdkInt: Int,
        projectName: String,
        result: SweepResult,
        timestampLabel: String
    ): Plan {
        val displayName = SweepExportNaming.buildFileName(
            projectName = projectName,
            result = result,
            timestampLabel = timestampLabel,
            extension = TouchstoneExport.FILE_EXTENSION
        )
        return planFor(sdkInt = sdkInt, displayName = displayName)
    }

    /*
    --------------------------------------------------------------------
    Tier selection
    EDIT SECTION 1003
    --------------------------------------------------------------------
    */
    fun planFor(sdkInt: Int, displayName: String): Plan =
        if (sdkInt >= MIN_SDK_FOR_MEDIA_STORE_DOWNLOADS) {
            Plan(
                strategy = Strategy.MEDIA_STORE,
                displayName = displayName,
                mimeType = MIME_TYPE,
                // MediaStore expects "Download/<sub>" — the platform's
                // DIRECTORY_DOWNLOADS value is the singular "Download".
                relativePath = "Download/$DOWNLOADS_SUBDIRECTORY",
                isPublicDownloads = true
            )
        } else {
            Plan(
                strategy = Strategy.APP_SPECIFIC,
                displayName = displayName,
                mimeType = MIME_TYPE,
                relativePath = DOWNLOADS_SUBDIRECTORY,
                isPublicDownloads = false
            )
        }
}
