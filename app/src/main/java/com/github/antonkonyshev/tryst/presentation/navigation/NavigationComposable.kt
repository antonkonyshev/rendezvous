package com.github.antonkonyshev.tryst.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.antonkonyshev.tryst.R
import com.github.antonkonyshev.tryst.presentation.collectAsEffect
import com.github.antonkonyshev.tryst.presentation.getActivity
import com.github.antonkonyshev.tryst.presentation.map.MapScreen
import com.github.antonkonyshev.tryst.presentation.settings.SettingsScreen

sealed class TrystNavRouting(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector
) {
    companion object {
        val screens = listOf(
            Map, Settings,
        )

        const val route_map = "map"
        const val route_settings = "settings"
    }

    private object Map : TrystNavRouting(
        route_map, R.string.map, Icons.Outlined.Map,
    )

    private object Settings : TrystNavRouting(
        route_settings, R.string.settings, Icons.Outlined.Settings,
    )
}

@Composable
fun TrystNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = TrystNavRouting.route_map,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        composable(TrystNavRouting.route_map) {
            MapScreen(modifier = modifier)
        }

        composable(TrystNavRouting.route_settings) {
            SettingsScreen(modifier = modifier)
        }
    }

    LocalContext.current.getActivity()?.eventBus?.collectAsEffect {
        if (it.id == "NavigateTo") {
            navController.navigate(it.extra)
        }
    }
}