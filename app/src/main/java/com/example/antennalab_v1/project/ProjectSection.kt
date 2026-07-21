package com.example.antennalab_v1.project

/*
########################################################################
FILE: ProjectSection.kt
PACKAGE: com.example.antennalab_v1.project
LAYER: UI / Project Workspace Navigation

SYSTEM ROLE
Defines the main workspace sections used by ProjectPageScreen.

These sections control which workspace panel is currently visible.

CURRENT DEVELOPMENT ROLE
Used by the workspace hub to switch between:

• Overview
• Design
• Materials
• Testing
• Notes

SAFE EDIT AREA
- add future sections here
- adjust ordering if workspace grows
########################################################################
*/

/*
########################################################################
EDIT SECTION 1001
PROJECT WORKSPACE SECTIONS
------------------------------------------------------------------------
PURPOSE
Enumerates the main workspace tabs available inside a project.
########################################################################
*/
enum class ProjectSection {

    OVERVIEW,

    DESIGN,

    MATERIALS,

    TESTING,

    NOTES
}