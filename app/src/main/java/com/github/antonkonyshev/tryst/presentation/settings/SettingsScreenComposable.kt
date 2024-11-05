package com.github.antonkonyshev.tryst.presentation.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.antonkonyshev.tryst.presentation.getActivity
import com.github.antonkonyshev.tryst.presentation.map.MapViewModel
import com.github.antonkonyshev.tryst.presentation.navigation.TrystNavHost
import com.github.antonkonyshev.tryst.presentation.navigation.TrystNavRouting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MapViewModel = viewModel()) {
    val ctx = LocalContext.current

    TopAppBar(
        title = {
            Text(text = "Settings")
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    ctx.getActivity()?.emitUiEvent("NavigateTo", TrystNavRouting.route_map)
                }
            ) {
                Icon(imageVector = Icons.Outlined.ArrowBackIosNew, contentDescription = "Back")
            }
        }
    )
}