package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.paymentbridge.AlMallahBridgeApp
import com.example.paymentbridge.security.DeviceKeyManager
import com.example.paymentbridge.ui.home.HomeScreen
import com.example.paymentbridge.ui.home.HomeViewModel
import com.example.paymentbridge.ui.parsertest.ParserTestScreen
import com.example.paymentbridge.ui.parsertest.ParserTestViewModel
import com.example.paymentbridge.ui.settings.SettingsScreen
import com.example.paymentbridge.ui.settings.SettingsViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AlMallahBridgeApp
        val repository = app.repository
        val keyManager = app.apiProvider.keyManager

        setContent {
            MyApplicationTheme {
                // Arabic-first RTL Layout Direction
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        val context = LocalContext.current

                        val homeViewModel = remember {
                            HomeViewModel(context.applicationContext, repository)
                        }
                        val parserTestViewModel = remember {
                            ParserTestViewModel(repository, keyManager)
                        }
                        val settingsViewModel = remember {
                            SettingsViewModel(keyManager, repository)
                        }

                        NavHost(
                            navController = navController,
                            startDestination = "home"
                        ) {
                            composable("home") {
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    onNavigateToParserTest = { navController.navigate("parser_test") },
                                    onNavigateToSettings = { navController.navigate("settings") }
                                )
                            }
                            composable("parser_test") {
                                ParserTestScreen(
                                    viewModel = parserTestViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
