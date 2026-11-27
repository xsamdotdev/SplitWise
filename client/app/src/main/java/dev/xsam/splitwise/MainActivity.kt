package dev.xsam.splitwise

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.xsam.splitwise.ui.AppViewModel
import dev.xsam.splitwise.ui.SplitWiseApp
import dev.xsam.splitwise.ui.theme.SplitWiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SplitWiseTheme {
                val viewModel: AppViewModel = viewModel(
                    factory = AppViewModel.Factory(applicationContext as Application),
                )
                SplitWiseApp(viewModel = viewModel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppPreview() {
    SplitWiseTheme {
        // Preview shows the app shell; live data comes from the API at runtime.
    }
}
