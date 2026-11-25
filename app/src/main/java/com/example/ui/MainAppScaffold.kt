package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.repository.McuWorkspaceRepository
import com.example.ui.auth.signOutUser
import com.example.ui.screens.CodeAndPromptLabScreen
import com.example.ui.screens.HmiPythonStudioScreen
import com.example.ui.screens.HomeWorkbenchScreen
import com.example.ui.screens.SchematicVisionScreen
import com.example.ui.screens.StandardsAndLibraryScreen
import com.example.ui.viewmodel.EngineeringAiViewModel
import com.example.ui.viewmodel.WorkspaceViewModel
import com.google.firebase.firestore.FirebaseFirestore

sealed class StudioRoute(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
) {
    data object Workbench : StudioRoute("workbench", R.string.nav_workbench, Icons.Filled.DeveloperBoard, "nav_workbench")
    data object CodeLab : StudioRoute("code_lab", R.string.nav_code_lab, Icons.Filled.Code, "nav_code_lab")
    data object Schematic : StudioRoute("schematic", R.string.nav_schematic, Icons.Filled.ImageSearch, "nav_schematic")
    data object Hmi : StudioRoute("hmi", R.string.nav_hmi, Icons.Filled.Speed, "nav_hmi")
    data object Standards : StudioRoute("standards", R.string.nav_standards, Icons.Filled.MenuBook, "nav_standards")
}

private val StudioDestinations = listOf(
    StudioRoute.Workbench,
    StudioRoute.CodeLab,
    StudioRoute.Schematic,
    StudioRoute.Hmi,
    StudioRoute.Standards
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    currentUserId: String,
    userEmail: String?,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val workspaceViewModel: WorkspaceViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                WorkspaceViewModel(McuWorkspaceRepository(db), currentUserId)
            }
        }
    )

    val aiViewModel: EngineeringAiViewModel = viewModel(key = "ai_$currentUserId")
    val aiUiState by aiViewModel.uiState.collectAsStateWithLifecycle()
    val projectsState by workspaceViewModel.projectsState.collectAsStateWithLifecycle()
    val diagnosticsState by workspaceViewModel.diagnosticsState.collectAsStateWithLifecycle()
    val toastMessage by workspaceViewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            workspaceViewModel.clearToast()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: StudioRoute.Workbench.route

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "MCU Studio AI • ${aiUiState.selectedFamily.displayName}",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        if (!userEmail.isNullOrBlank()) {
                            Text(
                                text = userEmail.substringBefore("@"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onSignOutComplete = onSignedOut
                                )
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = stringResource(R.string.sign_out),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        StudioDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        StudioDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = StudioRoute.Workbench.route,
                    modifier = Modifier.weight(1f).fillMaxSize()
                ) {
                    composable(StudioRoute.Workbench.route) {
                        HomeWorkbenchScreen(
                            selectedFamily = aiUiState.selectedFamily,
                            projectsState = projectsState,
                            onSelectFamilyAndOpenCodeLab = { family ->
                                aiViewModel.selectFamily(family)
                                navigateTo(StudioRoute.CodeLab.route)
                            },
                            onNavigateToSchematic = { navigateTo(StudioRoute.Schematic.route) },
                            onNavigateToHmi = { navigateTo(StudioRoute.Hmi.route) },
                            onNavigateToStandards = { navigateTo(StudioRoute.Standards.route) }
                        )
                    }
                    composable(StudioRoute.CodeLab.route) {
                        CodeAndPromptLabScreen(
                            uiState = aiUiState,
                            aiViewModel = aiViewModel,
                            workspaceViewModel = workspaceViewModel
                        )
                    }
                    composable(StudioRoute.Schematic.route) {
                        SchematicVisionScreen(
                            uiState = aiUiState,
                            aiViewModel = aiViewModel,
                            workspaceViewModel = workspaceViewModel
                        )
                    }
                    composable(StudioRoute.Hmi.route) {
                        HmiPythonStudioScreen(
                            uiState = aiUiState,
                            aiViewModel = aiViewModel,
                            workspaceViewModel = workspaceViewModel
                        )
                    }
                    composable(StudioRoute.Standards.route) {
                        StandardsAndLibraryScreen(
                            uiState = aiUiState,
                            projectsState = projectsState,
                            diagnosticsState = diagnosticsState,
                            aiViewModel = aiViewModel,
                            workspaceViewModel = workspaceViewModel
                        )
                    }
                }
            }
        }
    }
}
