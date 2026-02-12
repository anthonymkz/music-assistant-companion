package io.music_assistant.client.ui.compose.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.music_assistant.client.data.model.client.PlayerData

@Composable
fun SpeakerSelectorDialog(
    players: List<PlayerData>,
    currentPlayerIndex: Int,
    localPlayerId: String?,
    onPlayerSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Speaker") },
        text = {
            Column {
                players.forEachIndexed { index, data ->
                    val isSelected = index == currentPlayerIndex
                    val isLocal = data.playerId == localPlayerId
                    TextButton(
                        onClick = { onPlayerSelected(data.player.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Spacer(modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = data.player.displayName +
                                        (if (isLocal) " (local)" else ""),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
