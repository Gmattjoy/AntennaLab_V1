package com.example.antennalab_v1.features.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.model.ProjectListItem
import com.example.antennalab_v1.storage.ProjectIndexManager
import com.example.antennalab_v1.storage.ProjectStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadProjectScreen(
    savedProjects: List<ProjectListItem>,
    onLoadProject: (String) -> Unit,
    onBackHome: () -> Unit
) {
    val context = LocalContext.current
    val displayedProjects = remember {
        mutableStateOf(
            LoadProjectController.resolveInitialProjects(
                passedInProjects = savedProjects,
                indexProjects = ProjectIndexManager.getAllProjects(context)
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Project Manager") },
                actions = { AppTopRightMenu() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (displayedProjects.value.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("No Saved Projects")
                        Text("There are currently no saved projects stored on this device.")
                    }
                }

            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedProjects.value, key = { it.projectId }) { savedProject ->
                        SavedProjectCard(
                            savedProject = savedProject,
                            onLoadProject = {
                                onLoadProject(savedProject.projectId)
                            },
                            onDuplicateProject = {
                                ProjectStorage.duplicateProject(context, savedProject.projectId)
                                displayedProjects.value = ProjectIndexManager.getAllProjects(context)
                            },
                            onDeleteProject = {
                                ProjectStorage.deleteProject(context, savedProject.projectId)
                                displayedProjects.value = ProjectIndexManager.getAllProjects(context)
                            }
                        )
                    }
                }
            }

            Button(
                onClick = onBackHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back Home")
            }
        }
    }
}

@Composable
private fun SavedProjectCard(
    savedProject: ProjectListItem,
    onLoadProject: () -> Unit,
    onDuplicateProject: () -> Unit,
    onDeleteProject: () -> Unit
) {
    val context = LocalContext.current
    val frequencyText = LoadProjectController.formatTargetFrequencyMHz(savedProject.targetFrequencyHz)
    val lastEditedText = LoadProjectController.formatLastEdited(savedProject.lastEditedEpochMillis)

    val loadedProject = remember(savedProject.projectId) {
        ProjectStorage.loadProjectById(context, savedProject.projectId)
    }

    val hasStoredCalibration = LoadProjectController.hasStoredCalibration(loadedProject)
    val calibrationCompletion = LoadProjectController.storedCalibrationCompletion(loadedProject)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text("Saved Project")
            Text("Name: ${savedProject.name}")
            Text("Type: ${savedProject.antennaType}")
            Text("Frequency: $frequencyText MHz")
            Text("Last Edited: $lastEditedText")
            Text("Stored Calibration: ${if (hasStoredCalibration) "Yes" else "No"}")
            if (hasStoredCalibration) {
                Text("Calibration Completion: $calibrationCompletion")
            }

            Button(
                onClick = onLoadProject,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Load Project")
            }

            Button(
                onClick = onDuplicateProject,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Duplicate Project")
            }

            Button(
                onClick = onDeleteProject,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Project")
            }
        }
    }
}