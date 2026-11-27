package dev.xsam.splitwise.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitWiseApp(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Splitwise") },
                navigationIcon = {
                    if (state.screen is AppScreen.Detail) {
                        TextButton(onClick = viewModel::backToGroups) { Text("← Groups") }
                    }
                },
                actions = {
                    if (state.token != null) {
                        TextButton(onClick = viewModel::logout) { Text("Log out") }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            state.error?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f).padding(12.dp),
                        )
                        TextButton(onClick = viewModel::dismissError) { Text("OK") }
                    }
                }
            }

            when {
                state.token == null -> AuthScreen(viewModel)
                state.screen is AppScreen.Detail -> {
                    val detail = state.groupDetail
                    if (detail != null) {
                        GroupDetailScreen(state, detail, viewModel)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                else -> GroupsScreen(state.groups, viewModel)
            }
        }
    }
}
