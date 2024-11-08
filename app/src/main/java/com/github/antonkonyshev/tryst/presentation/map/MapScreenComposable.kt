package com.github.antonkonyshev.tryst.presentation.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.antonkonyshev.tryst.R
import com.github.antonkonyshev.tryst.presentation.getActivity
import com.github.antonkonyshev.tryst.presentation.navigation.TrystNavRouting
import com.yandex.mapkit.geometry.Point

@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {
    Box {
        Map()

        val ctx = LocalContext.current

        FloatingActionButton(
            onClick = {
                ctx.getActivity()?.emitUiEvent("NavigateTo", TrystNavRouting.route_settings)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(start = 0.dp, top = 40.dp, end = 15.dp, bottom = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = stringResource(R.string.menu)
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = 0.dp, top = 0.dp, end = 15.dp, bottom = 30.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel._zoom.value -= 0.6f },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = stringResource(R.string.zoom_out)
                )
            }

            FloatingActionButton(
                onClick = { viewModel._zoom.value += 0.6f },
                modifier = Modifier.padding(0.dp, 12.dp, 0.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.zoom_in)
                )
            }

            Row(
                modifier = Modifier.padding(start = 0.dp, top = 24.dp, end = 0.dp, bottom = 0.dp)
            ) {
                viewModel.users.collectAsStateWithLifecycle().value.forEach { user ->
                    FloatingActionButton(
                        onClick = {
                            viewModel._target.value = Point(user.latitude, user.longitude)
                        },
                        modifier = Modifier.padding(12.dp, 0.dp, 0.dp, 0.dp)
                    ) {
                        Text(
                            text = if (user.name.length > 6) user.name.substring(0, 5).uppercase()
                            else user.name.uppercase()
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { viewModel._target.value = viewModel.currentLocation.value },
                    modifier = Modifier.padding(24.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Adjust,
                        contentDescription = stringResource(R.string.center)
                    )
                }
            }
        }
    }
}
