package com.example.graphapp.frontend.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.graphapp.backend.core.GraphSchema
import com.example.graphapp.backend.model.dto.EventDetails

@Composable
fun IncidentCardCollapsible(incidents: List<EventDetails>, impacts: Map<Long, List<String>>?) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    incidents.forEach { incident ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFCCB6CC))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = incident.eventName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f),
                    softWrap = true,
                    maxLines = Int.MAX_VALUE // allow wrapping
                )
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .rotate(if (expanded) 180f else 0f)
                        .clickable(onClick = { expanded = !expanded })
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                ) {
                    val incidentDesc = incident.eventProperties[GraphSchema.SchemaEventTypeNames.HOW.key]
                    if (incidentDesc != null) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "SUMMARY",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                            )
                            Text(
                                text = incidentDesc,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    if (impacts != null && impacts[incident.eventId] != null) {
                        HorizontalDivider(modifier = Modifier
                            .padding(4.dp)
                            .padding(bottom = 4.dp),
                            thickness = 1.dp, color = Color.DarkGray)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "IMPACT",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                            )
                            impacts[incident.eventId]?.forEach { impact ->
                                Text(
                                    text = impact,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}