package com.gabriel.mylibrary.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.gabriel.mylibrary.R
import com.gabriel.mylibrary.data.repository.LibraryRepository
import com.gabriel.mylibrary.data.repository.PreferencesRepository
import com.gabriel.mylibrary.data.covers.CoverStore
import com.gabriel.mylibrary.ui.screens.*

@Composable
fun MyLibraryApp(repository: LibraryRepository, preferencesRepository: PreferencesRepository, coverStore: CoverStore) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "library"
    val library: LibraryViewModel = viewModel(factory = viewModelFactory {
        initializer { LibraryViewModel(repository, preferencesRepository) }
    })
    val state by library.state.collectAsStateWithLifecycle()
    val completed by library.completed.collectAsStateWithLifecycle()
    val completionText = stringResource(R.string.progress_completed, completed.orEmpty())
    val actionFailed by library.actionFailed.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val error = stringResource(R.string.action_error)
    val dismiss = stringResource(R.string.dismiss)
    LaunchedEffect(actionFailed) {
        if (actionFailed) { snackbar.showSnackbar(error, dismiss); library.clearError() }
    }
    LaunchedEffect(completed) {
        if (completed != null) { snackbar.showSnackbar(completionText); library.clearCompletion() }
    }
    val largeText = LocalDensity.current.fontScale > 1.3f
    val showNavigation = route in listOf("library", "categories", "favorites", "category/{category}")
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showNavigation) {
                Surface(modifier = Modifier.navigationBarsPadding().padding(horizontal = if (largeText) 8.dp else 24.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(30.dp), shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0)) {
                        val tabs = listOf(Triple("library", R.string.nav_library, Icons.Default.LocalLibrary),
                            Triple("categories", R.string.nav_categories, Icons.Default.GridView),
                            Triple("favorites", R.string.nav_favorites, Icons.Default.Star))
                        tabs.forEach { (destination, label, icon) ->
                            NavigationBarItem(selected = route == destination || (destination == "categories" && route.startsWith("category/")),
                                onClick = { nav.navigate(destination) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                } },
                                icon = { Icon(icon, stringResource(label)) },
                                label = { Text(stringResource(label),
                                    style = if (largeText) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.testTag("nav_$destination"))
                        }
                    }
                }
            }
        }) { padding ->
        NavHost(navController = nav, startDestination = "library", modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable("library") {
                CollectionScreen(state, false, library, { nav.navigate("details/$it") }, { nav.navigate("form/0") })
            }
            composable("favorites") {
                CollectionScreen(state, true, library, { nav.navigate("details/$it") }, { nav.navigate("form/0") })
            }
            composable("categories") {
                CategoriesScreen(state, { nav.navigate("category/${it.id}") }, library)
            }
            composable("category/{category}", arguments = listOf(navArgument("category") { type = NavType.LongType })) { backStack ->
                val category = backStack.arguments?.getLong("category")
                CollectionScreen(state, false, library, { nav.navigate("details/$it") }, { nav.navigate("form/0") },
                    fixedCategoryId = category, onBack = { nav.popBackStack() })
            }
            composable("form/{itemId}", arguments = listOf(navArgument("itemId") { type = NavType.LongType })) {
                val form: ItemFormViewModel = viewModel(factory = viewModelFactory {
                    initializer { ItemFormViewModel(repository, createSavedStateHandle(), coverStore) }
                })
                ItemFormScreen(form) { nav.popBackStack() }
            }
            composable("details/{itemId}", arguments = listOf(navArgument("itemId") { type = NavType.LongType })) {
                val details: ItemDetailsViewModel = viewModel(factory = viewModelFactory {
                    initializer { ItemDetailsViewModel(repository, createSavedStateHandle()) }
                })
                ItemDetailsScreen(details, { nav.popBackStack() }, { nav.navigate("form/$it") })
            }
        }
    }
}
