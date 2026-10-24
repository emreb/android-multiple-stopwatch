package com.swetw.multilaps.timing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swetw.multilaps.R

/**
 * A small page with two lines written on it: there is a note here. It only tells; the note is read
 * and written elsewhere.
 */
@Composable
fun NoteMark(size: Dp, modifier: Modifier = Modifier) {
    val colour = LocalContentColor.current
    val spoken = stringResource(R.string.note_mark_description)
    Canvas(modifier = modifier.size(size).semantics { contentDescription = spoken }) {
        val stroke = 1.75.dp.toPx()
        val page = Size(this.size.width * 0.75f, this.size.height - stroke)
        val left = (this.size.width - page.width) / 2
        drawRoundRect(
            colour,
            topLeft = Offset(left, stroke / 2),
            size = page,
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(stroke),
        )
        for (line in 0..1) {
            val y = this.size.height * (0.4f + 0.25f * line)
            val inset = page.width * 0.25f
            drawLine(colour, Offset(left + inset, y), Offset(left + page.width - inset, y), stroke, StrokeCap.Round)
        }
    }
}
