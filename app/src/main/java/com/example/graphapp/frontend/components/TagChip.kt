package com.example.graphapp.frontend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TagChip(
    text: String,
    backgroundColor: Color = Color(0xFFe0a987)
) {
    Box(
        modifier = Modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(20))
            .padding(12.dp).wrapContentHeight()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black
        )
    }
}

@Composable
fun TagChipRow(
    tags: List<Pair<String, Float>>,
    label: String
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        FlowRow(
            modifier = Modifier.padding(start = 3.dp),
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Center
        ) {
            tags.forEach { (tag, sim) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TagChip(tag.lowercase())
                    Spacer(modifier = Modifier.width(3.dp))
                    if (label == "Distance") {
                        if (sim > 0.75f) {
                            Text("Very Near", style = MaterialTheme.typography.labelMedium,
                                color = Color.Blue)
                        } else if (sim > 0.5f) {
                            Text("Quite Near", style = MaterialTheme.typography.labelMedium,
                                color = Color.Blue)
                        } else {
                            Text("Near", style = MaterialTheme.typography.labelMedium,
                                color = Color.Blue)
                        }
                    } else if (label == "Time Difference") {
                        Text("Recent",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Blue)
                    }
                }
            }
        }
    }
}


