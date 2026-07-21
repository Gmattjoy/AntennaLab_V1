package com.example.antennalab_v1.features.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object HomeIcons {

    val Designer: ImageVector
        get() {
            if (_designer != null) return _designer!!
            _designer = ImageVector.Builder(
                name = "Designer",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.9f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(4f, 12f)
                    lineTo(9f, 12f)

                    moveTo(15f, 12f)
                    lineTo(20f, 12f)

                    moveTo(12f, 7f)
                    lineTo(12f, 17f)

                    moveTo(9f, 12f)
                    lineTo(12f, 9f)
                    lineTo(15f, 12f)
                    lineTo(12f, 15f)
                    close()
                }
            }.build()
            return _designer!!
        }

    val Analyzer: ImageVector
        get() {
            if (_analyzer != null) return _analyzer!!
            _analyzer = ImageVector.Builder(
                name = "Analyzer",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.9f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(5f, 5f)
                    lineTo(5f, 19f)
                    lineTo(19f, 19f)

                    moveTo(8f, 15f)
                    lineTo(11f, 12f)
                    lineTo(14f, 14f)
                    lineTo(18f, 9f)
                }
            }.build()
            return _analyzer!!
        }

    val Scanner: ImageVector
        get() {
            if (_scanner != null) return _scanner!!
            _scanner = ImageVector.Builder(
                name = "Scanner",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.9f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(4f, 16f)
                    lineTo(8f, 12f)
                    lineTo(12f, 10f)
                    lineTo(16f, 12f)
                    lineTo(20f, 16f)

                    moveTo(7f, 16f)
                    lineTo(10f, 13f)
                    lineTo(12f, 12f)
                    lineTo(14f, 13f)
                    lineTo(17f, 16f)

                    moveTo(12f, 16f)
                    lineTo(18f, 10f)

                    moveTo(12f, 16f)
                    lineTo(12.01f, 16.01f)
                }
            }.build()
            return _scanner!!
        }

    val LoadProject: ImageVector
        get() {
            if (_loadProject != null) return _loadProject!!
            _loadProject = ImageVector.Builder(
                name = "LoadProject",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.9f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(12f, 4f)
                    lineTo(12f, 14f)

                    moveTo(8.5f, 10.5f)
                    lineTo(12f, 14f)
                    lineTo(15.5f, 10.5f)

                    moveTo(5f, 18f)
                    lineTo(19f, 18f)
                }
            }.build()
            return _loadProject!!
        }

    val Settings: ImageVector
        get() {
            if (_settings != null) return _settings!!
            _settings = ImageVector.Builder(
                name = "Settings",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.9f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(5f, 7f)
                    lineTo(19f, 7f)

                    moveTo(5f, 12f)
                    lineTo(19f, 12f)

                    moveTo(5f, 17f)
                    lineTo(19f, 17f)

                    moveTo(9f, 5.5f)
                    lineTo(9f, 8.5f)

                    moveTo(15f, 10.5f)
                    lineTo(15f, 13.5f)

                    moveTo(11f, 15.5f)
                    lineTo(11f, 18.5f)
                }
            }.build()
            return _settings!!
        }

    val Help: ImageVector
        get() {
            if (_help != null) return _help!!
            _help = ImageVector.Builder(
                name = "Help",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.9f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(9f, 8f)
                    lineTo(10f, 7f)
                    lineTo(12f, 6.5f)
                    lineTo(14f, 7f)
                    lineTo(15f, 8f)
                    lineTo(15.5f, 9.5f)
                    lineTo(15f, 11f)
                    lineTo(14f, 12f)
                    lineTo(13f, 12.7f)
                    lineTo(12f, 13.5f)
                    lineTo(12f, 14.5f)

                    moveTo(12f, 18f)
                    lineTo(12.01f, 18.01f)
                }
            }.build()
            return _help!!
        }

    val ChevronRight: ImageVector
        get() {
            if (_chevronRight != null) return _chevronRight!!
            _chevronRight = ImageVector.Builder(
                name = "ChevronRight",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2.1f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(9f, 6f)
                    lineTo(15f, 12f)
                    lineTo(9f, 18f)
                }
            }.build()
            return _chevronRight!!
        }

    private var _designer: ImageVector? = null
    private var _analyzer: ImageVector? = null
    private var _scanner: ImageVector? = null
    private var _loadProject: ImageVector? = null
    private var _settings: ImageVector? = null
    private var _help: ImageVector? = null
    private var _chevronRight: ImageVector? = null
}