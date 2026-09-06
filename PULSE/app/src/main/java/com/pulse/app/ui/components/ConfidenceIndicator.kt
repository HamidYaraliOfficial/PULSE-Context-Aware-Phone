package com.pulse.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pulse.app.core.theme.PulseSemanticColors

/** Circular "Meeting — 91% confidence" style indicator, per the spec. */
@Composable
fun ConfidenceIndicator(
    confidencePercent: Int,
    modifier: Modifier = Modifier,
    diameter: androidx.compose.ui.unit.Dp = 96.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 8.dp,
) {
    val color = when {
        confidencePercent >= 75 -> PulseSemanticColors.ConfidenceHigh
        confidencePercent >= 50 -> PulseSemanticColors.ConfidenceMedium
        else -> PulseSemanticColors.ConfidenceLow
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                size = Size(size.width - stroke.width, size.height - stroke.width),
                topLeft = androidx.compose.ui.geometry.Offset(stroke.width / 2, stroke.width / 2),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * (confidencePercent / 100f),
                useCenter = false,
                style = stroke,
                size = Size(size.width - stroke.width, size.height - stroke.width),
                topLeft = androidx.compose.ui.geometry.Offset(stroke.width / 2, stroke.width / 2),
            )
        }
        Text("$confidencePercent%", style = MaterialTheme.typography.titleLarge)
    }
}
