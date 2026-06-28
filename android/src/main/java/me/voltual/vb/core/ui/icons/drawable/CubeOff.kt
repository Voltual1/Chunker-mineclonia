package me.voltual.vb.core.ui.icons.drawable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom CubeOff vector icon translated from MDI path:
 * M20.84 22.73L17.28 19.17L12.57 21.82C12.41 21.94 12.21 22 12 22S11.59 21.94 11.43 21.82L3.53 17.38C3.21 17.21 3 16.88 3 16.5V7.5C3 7.12 3.21 6.79 3.53 6.62L4.3 6.19L1.11 3L2.39 1.73L22.11 21.46L20.84 22.73M12 4.15L17.96 7.5L13.31 10.11L20.53 17.33C20.82 17.16 21 16.85 21 16.5V7.5C21 7.12 20.79 6.79 20.47 6.62L12.57 2.18C12.41 2.06 12.21 2 12 2S11.59 2.06 11.43 2.18L7.56 4.36L9 5.82L12 4.15Z
 */
val CubeOff: ImageVector
    get() {
        if (_cubeOff != null) return _cubeOff!!
        
        _cubeOff = ImageVector.Builder(
            name = "CubeOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000))
            ) {
                // 第一部分：带裁剪反划线的几何路径
                moveTo(20.84f, 22.73f)
                lineTo(17.28f, 19.17f)
                lineTo(12.57f, 21.82f)
                curveTo(12.41f, 21.94f, 12.21f, 22f, 12f, 22f)
                reflectiveCurveTo(11.59f, 21.94f, 11.43f, 21.82f)
                lineTo(3.53f, 17.38f)
                curveTo(3.21f, 17.21f, 3f, 16.88f, 3f, 16.5f)
                verticalLineTo(7.5f)
                curveTo(3f, 7.12f, 3.21f, 6.79f, 3.53f, 6.62f)
                lineTo(4.3f, 6.19f)
                lineTo(1.11f, 3f)
                lineTo(2.39f, 1.73f)
                lineTo(22.11f, 21.46f)
                lineTo(20.84f, 22.73f)

                // 第二部分：上半侧切面的几何路径
                moveTo(12f, 4.15f)
                lineTo(17.96f, 7.5f)
                lineTo(13.31f, 10.11f)
                lineTo(20.53f, 17.33f)
                curveTo(20.82f, 17.16f, 21f, 16.85f, 21f, 16.5f)
                verticalLineTo(7.5f)
                curveTo(21f, 7.12f, 20.79f, 6.79f, 20.47f, 6.62f)
                lineTo(12.57f, 2.18f)
                curveTo(12.41f, 2.06f, 12.21f, 2f, 12f, 2f)
                reflectiveCurveTo(11.59f, 2.06f, 11.43f, 2.18f)
                lineTo(7.56f, 4.36f)
                lineTo(9f, 5.82f)
                lineTo(12f, 4.15f)
                close()
            }
        }.build()
        
        return _cubeOff!!
    }

private var _cubeOff: ImageVector? = null