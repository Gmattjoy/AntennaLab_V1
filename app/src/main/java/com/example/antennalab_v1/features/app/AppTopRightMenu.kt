package com.example.antennalab_v1.features.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

enum class AppMenuDestination(
    val label: String
) {
    HOME("Home"),
    LAB("Lab"),
    SWEEP("Sweep"),
    CONNECTIONS("Connections"),
    INSTRUMENT_DETAILS("Instrument details"),
    WIZARD("Wizard"),
    TEST_ANTENNA("Test Antenna"),
    PROJECTS("Projects"),
    SETTINGS("Settings")
}

object AppNavigationMenuBridge {
    var navigateTo: (AppMenuDestination) -> Unit = {}
}

@Composable
fun AppTopRightMenu() {
    var expanded by remember { mutableStateOf(false) }

    IconButton(
        onClick = { expanded = true }
    ) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "App navigation menu"
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        AppMenuDestination.entries.forEach { destination ->
            DropdownMenuItem(
                text = { Text(destination.label) },
                onClick = {
                    expanded = false
                    AppNavigationMenuBridge.navigateTo(destination)
                }
            )
        }
    }
}