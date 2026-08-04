package com.example.antennalab_v1.storage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.antennalab_v1.domain.testing.SweepExportNaming
import com.example.antennalab_v1.domain.testing.SweepExportPlan
import com.example.antennalab_v1.domain.testing.TouchstoneExport
import com.example.antennalab_v1.model.testing.SweepResult
import java.io.File

/*
########################################################################
FILE: SweepExportWriter.kt
PACKAGE: com.example.antennalab_v1.storage
LAYER: Storage / File IO

SYSTEM ROLE
EXECUTES a SweepExportPlan. Every decision — filename, MIME type, which
API tier, which relative path — was already made purely in
domain/testing/SweepExportPlan; this file only performs IO and builds
the share Intent. Keep it that way: no formatting, no branching on data
values, nothing worth a unit test beyond the plan itself.

TWO TIERS (see SweepExportPlan for the reasoning)
  API 29+   MediaStore public Downloads. insert() returns a content://
            URI that ACTION_SEND shares directly, so FileProvider is NOT
            involved on this path.
  API 26-28 MediaStore.Downloads does not exist and public storage would
            need legacy WRITE_EXTERNAL_STORAGE, which this app does not
            request. The file goes to the app-specific external directory
            (no permission needed since API 19) and FileProvider makes it
            shareable.

The Saved result carries isPublicDownloads so the UI states what actually
happened. Do NOT report a public Downloads save on the fallback tier.
########################################################################
*/
object SweepExportWriter {

    /*
    Must match the authority declared for the <provider> in
    AndroidManifest.xml.
    */
    private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

    /*
    --------------------------------------------------------------------
    Outcome
    EDIT SECTION 1001
    --------------------------------------------------------------------
    */
    sealed interface Outcome {
        data class Saved(
            val uri: Uri,
            val displayName: String,
            /** True only when the file landed in public Downloads. */
            val isPublicDownloads: Boolean,
            /** Absolute path when known (fallback tier), else null. */
            val absolutePath: String?
        ) : Outcome

        data class Failed(val reason: String) : Outcome
    }

    /*
    --------------------------------------------------------------------
    Save a sweep as .s1p
    EDIT SECTION 1002
    --------------------------------------------------------------------
    Caller supplies the timestamp label so the pure naming layer stays
    clock-free and this function stays a straight executor.

    Never throws: IO failure is returned as Failed(reason) so the caller
    can surface it — flag, don't crash, consistent with the sweep
    pipeline's philosophy.
    --------------------------------------------------------------------
    */
    fun saveS1p(
        context: Context,
        result: SweepResult,
        projectName: String,
        timestampLabel: String
    ): Outcome {
        val plan = SweepExportPlan.planS1pExport(
            sdkInt = Build.VERSION.SDK_INT,
            projectName = projectName,
            result = result,
            timestampLabel = timestampLabel
        )
        val content = TouchstoneExport.buildS1p(result)

        return try {
            when (plan.strategy) {
                SweepExportPlan.Strategy.MEDIA_STORE ->
                    saveViaMediaStore(context, plan, content)

                SweepExportPlan.Strategy.APP_SPECIFIC ->
                    saveToAppSpecificDirectory(context, plan, content)
            }
        } catch (error: Exception) {
            Outcome.Failed(error.message ?: error.javaClass.simpleName)
        }
    }

    /*
    --------------------------------------------------------------------
    API 29+ — public Downloads
    EDIT SECTION 1003
    --------------------------------------------------------------------
    IS_PENDING is set during the write and cleared afterwards. Clearing it
    is what makes the file visible; skipping that step leaves a
    permanently invisible MediaStore entry.
    --------------------------------------------------------------------
    */
    private fun saveViaMediaStore(
        context: Context,
        plan: SweepExportPlan.Plan,
        content: String
    ): Outcome {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Unreachable: the plan only asks for MEDIA_STORE at 29+. Guarded
            // so the API-level lint contract is explicit rather than implied.
            return Outcome.Failed("MediaStore Downloads requires API 29")
        }

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, plan.displayName)
            /*
            MIME_TYPE is deliberately NOT set here.

            Verified on a device (API 35, 2026-07-30): declaring "text/plain"
            makes MediaStore enforce extension/MIME agreement and it silently
            renames the file to "<name>.s1p.txt", because .s1p is not a known
            extension for text/plain. That breaks the export outright —
            NanoVNA-Saver and the simulators filter on .s1p.

            Omitting it lets MediaStore infer from the display name and leaves
            the filename intact. plan.mimeType is still used for the SHARE
            intent, where advertising text/plain is what makes targets accept
            the file; the two concerns are separate and must stay separate.
            */
            put(MediaStore.Downloads.RELATIVE_PATH, plan.relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return Outcome.Failed("MediaStore rejected the insert")

        resolver.openOutputStream(uri)?.use { stream ->
            stream.write(content.toByteArray())
        } ?: run {
            resolver.delete(uri, null, null)
            return Outcome.Failed("Could not open the file for writing")
        }

        resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
            null,
            null
        )

        return Outcome.Saved(
            uri = uri,
            displayName = plan.displayName,
            isPublicDownloads = true,
            absolutePath = null
        )
    }

    /*
    --------------------------------------------------------------------
    API 26-28 — app-specific external directory
    EDIT SECTION 1004
    --------------------------------------------------------------------
    Collisions are ours to avoid here (unlike MediaStore, which
    uniquifies its own inserts), so the pure nextAvailableName helper
    picks a free name from what the directory already holds.
    --------------------------------------------------------------------
    */
    private fun saveToAppSpecificDirectory(
        context: Context,
        plan: SweepExportPlan.Plan,
        content: String
    ): Outcome {
        val baseDirectory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: return Outcome.Failed("External storage is unavailable")

        val directory = File(baseDirectory, plan.relativePath)
        if (!directory.exists() && !directory.mkdirs()) {
            return Outcome.Failed("Could not create ${directory.name}")
        }

        val existing = directory.list()?.toSet() ?: emptySet()
        val fileName = SweepExportNaming.nextAvailableName(plan.displayName, existing)
        val file = File(directory, fileName)
        file.writeText(content)

        return Outcome.Saved(
            uri = FileProvider.getUriForFile(context, fileProviderAuthority(context), file),
            displayName = fileName,
            isPublicDownloads = false,
            absolutePath = file.absolutePath
        )
    }

    /*
    --------------------------------------------------------------------
    Share sheet
    EDIT SECTION 1005
    --------------------------------------------------------------------
    Works for both tiers: a MediaStore content:// URI and a FileProvider
    content:// URI are both shareable, and the read-permission grant is
    required either way.
    --------------------------------------------------------------------
    */
    fun buildShareIntent(saved: Outcome.Saved, mimeType: String = SweepExportPlan.MIME_TYPE): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, saved.uri)
            putExtra(Intent.EXTRA_SUBJECT, saved.displayName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share ${saved.displayName}")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun fileProviderAuthority(context: Context): String =
        context.packageName + FILE_PROVIDER_SUFFIX
}
