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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.antennalab_v1.features.app.AppRootScreen
import com.example.antennalab_v1.ui.theme.AntennaLab_V1Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            AntennaLab_V1Theme {

                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRootScreen()
                }

            }

        }
    }
}