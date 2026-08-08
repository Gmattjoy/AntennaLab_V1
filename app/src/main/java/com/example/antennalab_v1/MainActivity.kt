package com.example.antennalab_v1

/*
########################################################################
FILE: MainActivity.kt
PURPOSE: Application entry point.
########################################################################
*/


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalContext
import com.example.antennalab_v1.features.app.AppRootScreen
import com.example.antennalab_v1.model.settings.resolveDarkTheme
import com.example.antennalab_v1.storage.SettingsRepository
import com.example.antennalab_v1.ui.theme.AntennaLab_V1Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            /*
            Read at the COMPOSITION ROOT on purpose. SettingsRepository is
            observable since slice 5d, so this read subscribes the root to the
            settings snapshot — flipping the theme recomposes everything below
            with no restart and no navigation.

            The tree recomposes IN PLACE rather than being replaced, so
            AppRootScreen's remembered navigation state survives a theme flip.

            resolveDarkTheme lives in the model so this stays a wiring point
            with no logic, and AntennaLab_V1Theme keeps its darkTheme: Boolean
            parameter — 18 call sites across 8 files pass it, and all but this
            one are @Previews that should not know settings exist.
            */
            val preference = SettingsRepository.current(LocalContext.current).themePreference

            AntennaLab_V1Theme(
                darkTheme = resolveDarkTheme(
                    preference = preference,
                    systemInDark = isSystemInDarkTheme()
                )
            ) {

                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRootScreen()
                }

            }

        }
    }
}