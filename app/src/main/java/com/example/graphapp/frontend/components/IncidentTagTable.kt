package com.example.graphapp.frontend.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graphapp.core.schema.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.core.analyser.SimilarEventTags
import com.example.graphapp.core.model.dto.EventDetails

@Composable
fun IncidentTagTable(incident: EventDetails) {
    val props = incident.simProperties.orEmpty()
    val byType = remember (props) { props.associateBy { it.propertyType } }

    val wanted = listOf(
        "Incident" to SchemaEventTypeNames.INCIDENT.key,
        "Approach" to SchemaEventTypeNames.HOW.key,
        "Distance" to SchemaEventTypeNames.WHERE.key,
        "Time Difference" to SchemaEventTypeNames.WHEN.key
    )
    val rows: List<Pair<String, SimilarEventTags>> = remember(byType) {
        wanted.mapNotNull { (label, key) ->
            byType[key]
                ?.takeIf { sp ->
                    sp.relevantTagsA.isNotEmpty() || sp.relevantTagsB.isNotEmpty()
                }
                ?.let { sp -> label to sp }
        }
            .sortedByDescending { (_, sp) -> sp.simScore }   // ← sort by simScore (highest first)
    }


    Column (
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        // Header
        Row (
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Property", modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge)
            Text("Similarity", modifier = Modifier.weight(4f),
                style = MaterialTheme.typography.labelLarge)
        }
        HorizontalDivider()

        rows.forEach { (label, sp) ->
            if (label == "Distance" || label == "Time Difference") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(label, modifier = Modifier.weight(1f, fill = true), style = MaterialTheme.typography.labelMedium)
                    Box(modifier = Modifier.weight(4f, fill = true)) {
                        TagChipRow(sp.relevantTagsA, label)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(label, modifier = Modifier.weight(1f, fill = true), style = MaterialTheme.typography.labelMedium)

                    val ranked: List<Triple<String, String, Float>> =
                        (sp.relevantTagsA).zip(sp.relevantTagsB) { (s1, f1), (s2, _) -> Triple(s1, s2, f1) }
                            .sortedByDescending { it.third }

                    Column (
                        modifier = Modifier.weight(4f, fill = true)
                    ) {
                        ranked.forEach { (tagA, tagB, _) ->
                            Row (
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                TagChip(tagA.lowercase())
                                Text("↔", textAlign = TextAlign.Center, fontSize = 30.sp)
                                TagChip(tagB.lowercase())
                            }
                        }
                    }
                }
            }
            HorizontalDivider()
        }
    }
}
