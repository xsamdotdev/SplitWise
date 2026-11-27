package dev.xsam.splitwise.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.xsam.splitwise.data.GroupSummaryDto
import dev.xsam.splitwise.util.formatMoney

@Composable
fun GroupsScreen(groups: List<GroupSummaryDto>, viewModel: AppViewModel) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (groups.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No groups yet", style = MaterialTheme.typography.titleMedium)
                Text("Create a group, add friends by username, and start tracking expenses.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
            ) {
                items(groups, key = { it.id }) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clickable { viewModel.openGroup(group.id) },
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(group.name, style = MaterialTheme.typography.titleMedium)
                            Text("${group.memberCount} member${if (group.memberCount == 1) "" else "s"}")
                            Text(
                                "Your balance: ${formatMoney(group.myBalanceCents)}",
                                color = if (group.myBalanceCents >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
        ) {
            Text("New group")
        }
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, usernames ->
                viewModel.createGroup(name, usernames)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String, List<String>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var members by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New group") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = members,
                    onValueChange = { members = it },
                    label = { Text("Member usernames (comma separated)") },
                    placeholder = { Text("alice, bob") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        name.trim(),
                        members.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() },
                    )
                },
                enabled = name.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
