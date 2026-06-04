package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.example.data.Category
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AuthState
import com.example.viewmodel.VotingViewModel
import kotlinx.coroutines.flow.collectLatest

enum class Screen {
    Home,
    Leaderboard,
    Suggestions,
    Profile
}

class MainActivity : ComponentActivity() {

    private val viewModel: VotingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val authState by viewModel.authState.collectAsState()
                
                // Track current active bottom navigation tab
                var currentTab by remember { mutableStateOf(Screen.Home) }
                
                // Track selected category details for nominees view
                val selectedCategory by viewModel.selectedCategory.collectAsState()

                // Listens and shows real-time toast alerts from the ViewModel
                LaunchedEffect(Unit) {
                    viewModel.toastMessage.collectLatest { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val state = authState) {
                        is AuthState.Unauthenticated, is AuthState.Error -> {
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = {
                                    currentTab = Screen.Home
                                }
                            )
                        }
                        is AuthState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is AuthState.Authenticated -> {
                            // High-fidelity Main shell layout with navigation rails config
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                bottomBar = {
                                    NavigationBar(
                                        modifier = Modifier
                                            .testTag("bottom_nav_bar")
                                            .windowInsetsPadding(WindowInsets.navigationBars),
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    ) {
                                        NavigationBarItem(
                                            selected = currentTab == Screen.Home,
                                            onClick = { 
                                                currentTab = Screen.Home 
                                            },
                                            icon = { Text(text = "🏠", fontSize = 20.sp) },
                                            label = { Text("Home") },
                                            modifier = Modifier.testTag("nav_home_tab")
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == Screen.Leaderboard,
                                            onClick = { currentTab = Screen.Leaderboard },
                                            icon = { Text(text = "📊", fontSize = 20.sp) },
                                            label = { Text("Leaderboard") },
                                            modifier = Modifier.testTag("nav_leaderboard_tab")
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == Screen.Suggestions,
                                            onClick = { currentTab = Screen.Suggestions },
                                            icon = { Text(text = "💡", fontSize = 20.sp) },
                                            label = { Text("Suggestions") },
                                            modifier = Modifier.testTag("nav_suggestions_tab")
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == Screen.Profile,
                                            onClick = { currentTab = Screen.Profile },
                                            icon = { Text(text = "👤", fontSize = 20.sp) },
                                            label = { Text("Profile") },
                                            modifier = Modifier.testTag("nav_profile_tab")
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                AnimatedContent(
                                    targetState = currentTab,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding),
                                    transitionSpec = {
                                        fadeIn() togetherWith fadeOut()
                                    },
                                    label = "ScreenTransition"
                                ) { tab ->
                                    when (tab) {
                                        Screen.Home -> {
                                            HomeScreen(
                                                viewModel = viewModel,
                                                onCategorySelected = { category ->
                                                    viewModel.selectCategory(category)
                                                    currentTab = Screen.Leaderboard
                                                },
                                                onNavigateToSuggestions = {
                                                    currentTab = Screen.Suggestions
                                                }
                                            )
                                        }
                                        Screen.Leaderboard -> {
                                            val categoryToDisplay = selectedCategory ?: Category(
                                                id = "best_barber",
                                                name = "Best Barber",
                                                iconName = "content_cut",
                                                nominationCount = 89
                                            )
                                            
                                            // Handle dynamically changing display category options
                                            LaunchedEffect(categoryToDisplay) {
                                                viewModel.selectCategory(categoryToDisplay)
                                            }

                                            NomineesScreen(
                                                viewModel = viewModel,
                                                category = categoryToDisplay,
                                                onBackClick = {
                                                    currentTab = Screen.Home
                                                },
                                                onNavigateToSuggestions = {
                                                    currentTab = Screen.Suggestions
                                                }
                                            )
                                        }
                                        Screen.Suggestions -> {
                                            SuggestionsScreen(
                                                viewModel = viewModel,
                                                onSuccess = {
                                                    currentTab = Screen.Home
                                                }
                                            )
                                        }
                                        Screen.Profile -> {
                                            ProfileScreen(
                                                viewModel = viewModel,
                                                onLogoutFinished = {
                                                    currentTab = Screen.Home
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
