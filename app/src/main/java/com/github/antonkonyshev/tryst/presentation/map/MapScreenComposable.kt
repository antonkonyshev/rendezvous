package com.github.antonkonyshev.tryst.presentation.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.antonkonyshev.tryst.R
import com.github.antonkonyshev.tryst.domain.User
import com.github.antonkonyshev.tryst.presentation.getActivity
import com.github.antonkonyshev.tryst.presentation.navigation.TrystNavRouting
import com.yandex.mapkit.geometry.Point
import com.yandex.runtime.image.ImageProvider
import java.util.Date

@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(), modifier: Modifier = Modifier
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Map()

        MapControls(viewModel = viewModel, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun MapControls(
    viewModel: MapViewModel = viewModel(),
    users: Set<User> = viewModel.users.collectAsStateWithLifecycle().value,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current

    Box(modifier = Modifier) {
        FloatingActionButton(
            onClick = {
                ctx.getActivity()?.emitUiEvent("NavigateTo", TrystNavRouting.route_settings)
            },
            containerColor = Color.White,
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
                containerColor = Color.White,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = stringResource(R.string.zoom_out)
                )
            }

            FloatingActionButton(
                onClick = { viewModel._zoom.value += 0.6f },
                containerColor = Color.White,
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
                users.forEach { user ->
                    FloatingActionButton(
                        onClick = {
                            viewModel._target.value = Point(user.latitude, user.longitude)
                        },
                        containerColor = Color.White,
                        modifier = Modifier.padding(12.dp, 0.dp, 0.dp, 0.dp)
                    ) {
                        var avatarPath: String? = null
                        if (user?.uid != null) {
                            avatarPath =
                                ctx.getSharedPreferences("avatars", 0).getString(user?.uid, null)
                        }
                        if (avatarPath != null) {
                            Image(
                                bitmap = ImageProvider.fromFile(avatarPath).image.asImageBitmap(),
                                contentDescription = user?.name ?: "Guest",
                                modifier = Modifier.size(50.dp, 50.dp)
                            )
                        } else {
                            Text(
                                text = if (user.name.length > 6) user.name.substring(0, 5)
                                    .uppercase()
                                else user.name.uppercase()
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { viewModel._target.value = viewModel.currentLocation.value },
                    containerColor = Color.White,
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

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun MapControlsPreview() {
    val users = remember {
        setOf<User>(
            User("123", "First", 50.0, 50.0, Date().time),
            User("234", "Second", 51.0, 51.0, Date().time),
            User("345", "Third", 52.0, 52.0, Date().time),
        )
    }
    MapControls(users = users)
}
