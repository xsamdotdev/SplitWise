package dev.xsam.splitwise.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.xsam.splitwise.data.CreateExpenseRequest
import dev.xsam.splitwise.data.CustomSplitRequest
import dev.xsam.splitwise.data.ExpenseDto
import dev.xsam.splitwise.data.GroupDetailDto
import dev.xsam.splitwise.util.formatMoney
import kotlin.math.abs

@Composable
fun GroupDetailScreen(state: AppUiState, group: GroupDetailDto, viewModel: AppViewModel) {
    var showAddMember by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(group.name, style = MaterialTheme.typography.headlineSmall)
            Text("Members: ${group.members.joinToString { it.username }}")
        }

        Section("Balances") {
            val nonZero = group.balances.filter { it.netCents != 0 }
            if (nonZero.isEmpty()) {
                Text("Everyone is settled up.")
            } else {
                nonZero.forEach { balance ->
                    val isOwed = balance.netCents > 0
                    Text(
                        text = "${balance.username} ${if (isOwed) "gets back" else "owes"} ${formatMoney(abs(balance.netCents))}",
                        color = if (isOwed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Section("Suggested payments") {
            if (group.simplifiedDebts.isEmpty()) {
                Text("Nothing to settle.")
            } else {
                group.simplifiedDebts.forEach { debt ->
                    Text("${debt.fromUsername} pays ${debt.toUsername} ${formatMoney(debt.amountCents)}")
                }
            }
        }

        Section("Expenses") {
            if (group.expenses.isEmpty()) {
                Text("No expenses yet.")
            } else {
                group.expenses.forEach { expense ->
                    ExpenseRow(group, expense, state.username) {
                        viewModel.deleteExpense(group.id, expense.id)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showAddMember = true }, modifier = Modifier.weight(1f)) { Text("Add member") }
            Button(onClick = { showAddExpense = true }, modifier = Modifier.weight(1f)) { Text("Add expense") }
        }
    }

    if (showAddMember) {
        AddMemberDialog(
            onDismiss = { showAddMember = false },
            onAdd = { username ->
                viewModel.addMember(group.id, username)
                showAddMember = false
            },
        )
    }
    if (showAddExpense) {
        AddExpenseDialog(
            members = group.members.map { it.username },
            onDismiss = { showAddExpense = false },
            onSave = { request ->
                viewModel.addExpense(group.id, request)
                showAddExpense = false
            },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Column(content = content)
    }
}

@Composable
private fun ExpenseRow(group: GroupDetailDto, expense: ExpenseDto, currentUsername: String?, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${expense.description} — ${formatMoney(expense.amountCents)}", style = MaterialTheme.typography.titleMedium)
            Text("Paid by ${expense.paidBy.username}")
            Text(
                "Split: " + expense.splits.joinToString { "${it.username} ${formatMoney(it.amountCents)}" },
                style = MaterialTheme.typography.bodySmall,
            )
            if (currentUsername == expense.paidBy.username || currentUsername == group.createdByUsername) {
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun AddMemberDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add member") },
        text = {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onAdd(username.trim().lowercase()) }, enabled = username.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddExpenseDialog(
    members: List<String>,
    onDismiss: () -> Unit,
    onSave: (CreateExpenseRequest) -> Unit,
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var paidBy by remember { mutableStateOf(members.firstOrNull().orEmpty()) }
    var equalSplit by remember { mutableStateOf(true) }
    var participants by remember { mutableStateOf(members.toSet()) }
    var customAmounts by remember { mutableStateOf(emptyMap<String, String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add expense") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("What was it for?") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Total amount (e.g. 24.99)") },
                    singleLine = true,
                )

                Text("Paid by", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    members.forEach { member ->
                        FilterChip(
                            selected = paidBy == member,
                            onClick = { paidBy = member },
                            label = { Text(member) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }

                Row {
                    FilterChip(
                        selected = equalSplit,
                        onClick = { equalSplit = true },
                        label = { Text("Split equally") },
                    )
                    FilterChip(
                        selected = !equalSplit,
                        onClick = { equalSplit = false },
                        label = { Text("Custom amounts") },
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                Text("Participants", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    members.forEach { member ->
                        FilterChip(
                            selected = participants.contains(member),
                            onClick = {
                                participants = if (participants.contains(member)) participants - member else participants + member
                            },
                            label = { Text(member) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }

                if (!equalSplit) {
                    members.filter { participants.contains(it) }.forEach { member ->
                        OutlinedTextField(
                            value = customAmounts[member].orEmpty(),
                            onValueChange = { customAmounts = customAmounts + (member to it) },
                            label = { Text("$member's share") },
                            singleLine = true,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        CreateExpenseRequest(
                            description = description.trim(),
                            amount = amount.trim(),
                            paidByUsername = paidBy,
                            splitType = if (equalSplit) "equal" else "custom",
                            participantUsernames = if (equalSplit) participants.toList() else null,
                            splits = if (equalSplit) {
                                null
                            } else {
                                participants.toList().map { CustomSplitRequest(it, customAmounts[it].orEmpty()) }
                            },
                        ),
                    )
                },
                enabled = description.isNotBlank() && amount.isNotBlank() && participants.isNotEmpty(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
