package com.voicerecorder.presentation.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.voicerecorder.R
import com.voicerecorder.VoiceRecorderApplication
import com.voicerecorder.data.local.PreferencesManager
import com.voicerecorder.presentation.theme.FinalTalkTheme
import com.voicerecorder.presentation.ui.about.AboutScreen
import com.voicerecorder.presentation.ui.home.PermissionScreen
import com.voicerecorder.presentation.ui.recorder.RecorderScreen
import com.voicerecorder.presentation.ui.recorder.RecorderViewModel
import com.voicerecorder.presentation.ui.recordings.RecordingsScreen
import com.voicerecorder.presentation.ui.recordings.RecordingsViewModel
import com.voicerecorder.presentation.ui.settings.SettingsScreen
import com.voicerecorder.presentation.ui.settings.SettingsViewModel
import com.voicerecorder.presentation.ui.util.LocaleHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private var currentLangCode: String? = null

    override fun attachBaseContext(newBase: Context) {
        preferencesManager = PreferencesManager(newBase)
        val langCode = runBlocking { preferencesManager.languageFlow.first().code }
        currentLangCode = langCode
        val localeContext = LocaleHelper.applyLocale(newBase, langCode)
        super.attachBaseContext(localeContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as VoiceRecorderApplication
        val container = app.container

        setContent {
            val settingsViewModel: SettingsViewModel =
                viewModel(
                    factory = SettingsViewModel.provideFactory(container.preferencesRepository),
                )
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val language by settingsViewModel.language.collectAsState()

            // Recreate activity if language changes dynamically in datastore preferences
            LaunchedEffect(language) {
                if (currentLangCode != null && currentLangCode != language.code) {
                    recreate()
                }
            }

            FinalTalkTheme(themeMode = themeMode) {
                MainContent(
                    app = app,
                    settingsViewModel = settingsViewModel,
                    recorderViewModel =
                        viewModel(
                            factory =
                                RecorderViewModel.provideFactory(
                                    app,
                                    container.audioRecorderRepository,
                                    container.recordingRepository,
                                    container.preferencesRepository,
                                ),
                        ),
                    recordingsViewModel =
                        viewModel(
                            factory =
                                RecordingsViewModel.provideFactory(
                                    container.recordingRepository,
                                    container.audioPlayerRepository,
                                ),
                        ),
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainContent(
        app: Application,
        settingsViewModel: SettingsViewModel,
        recorderViewModel: RecorderViewModel,
        recordingsViewModel: RecordingsViewModel,
    ) {
        var hasPermissions by remember {
            mutableStateOf(hasRequiredPermissions())
        }

        val permissionLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
            ) { results ->
                hasPermissions = results.values.all { it }
            }

        if (!hasPermissions) {
            PermissionScreen(
                onRequestPermissions = {
                    val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                },
            )
        } else {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val snackbarHostState = remember { SnackbarHostState() }

            val tabs =
                listOf(
                    NavigationItem.Recorder,
                    NavigationItem.Recordings,
                    NavigationItem.Settings,
                )

            val currentTab = tabs.find { it.route == currentRoute }
            val showTopBar = currentRoute != "about"
            val showBottomBar = tabs.any { it.route == currentRoute }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    if (showTopBar) {
                        TopAppBar(
                            title = {
                                val titleRes = if (currentRoute == "about") R.string.title_about else (currentTab?.titleRes ?: R.string.app_name)
                                Text(
                                    text = stringResource(titleRes),
                                    style =
                                        MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = (-0.5).sp,
                                        ),
                                )
                            },
                            navigationIcon = {
                                if (currentRoute == "about") {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.action_cancel),
                                        )
                                    }
                                }
                            },
                            colors =
                                TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                    titleContentColor = MaterialTheme.colorScheme.primary,
                                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                                ),
                        )
                    }
                },
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != tab.route) {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = stringResource(tab.titleRes),
                                        )
                                    },
                                    label = { Text(stringResource(tab.titleRes)) },
                                    colors =
                                        NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor =
                                                MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.6f,
                                                ),
                                            unselectedTextColor =
                                                MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.6f,
                                                ),
                                            indicatorColor =
                                                MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.1f,
                                                ),
                                        ),
                                )
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = NavigationItem.Recorder.route,
                    modifier = Modifier.padding(paddingValues),
                ) {
                    composable(NavigationItem.Recorder.route) {
                        RecorderScreen(
                            viewModel = recorderViewModel,
                            snackbarHostState = snackbarHostState,
                        )
                    }
                    composable(NavigationItem.Recordings.route) {
                        RecordingsScreen(
                            viewModel = recordingsViewModel,
                            snackbarHostState = snackbarHostState,
                        )
                    }
                    composable(NavigationItem.Settings.route) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateToAbout = { navController.navigate("about") },
                        )
                    }
                    composable("about") {
                        AboutScreen(onBackClick = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val micPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val notificationPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        return micPermission && notificationPermission
    }
}

sealed class NavigationItem(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val titleRes: Int) {
    object Recorder : NavigationItem("recorder", Icons.Default.Mic, R.string.title_recorder)

    object Recordings : NavigationItem("recordings", Icons.AutoMirrored.Filled.List, R.string.title_recordings)

    object Settings : NavigationItem("settings", Icons.Default.Settings, R.string.title_settings)
}
