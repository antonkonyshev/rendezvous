package com.github.antonkonyshev.tryst.presentation.settings

import android.net.Uri
import android.widget.EditText
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.antonkonyshev.tryst.R
import com.github.antonkonyshev.tryst.domain.User
import com.github.antonkonyshev.tryst.presentation.getActivity
import com.github.antonkonyshev.tryst.presentation.map.MapViewModel
import com.github.antonkonyshev.tryst.presentation.navigation.TrystNavRouting
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MapViewModel = viewModel(),
    users: Set<User> = viewModel.users.collectAsStateWithLifecycle().value
) {
    val ctx = LocalContext.current

    Column {
        TopAppBar(
            title = {
                Text(text = stringResource(id = R.string.settings))
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        ctx.getActivity()?.emitUiEvent("NavigateTo", TrystNavRouting.route_map)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        )

        val userName = remember {
            mutableStateOf<String>(ctx.getSharedPreferences("avatars", 0).getString("name", "")!!)
        }
        val nameDialog = remember { mutableStateOf(false) }
        ListItem(
            leadingContent = {
                Text("", modifier = Modifier.size(60.dp, 60.dp))
            },
            headlineContent = {
                Text(stringResource(R.string.your_name))
            },
            supportingContent = {
                Text(userName.value)
            },
            modifier = Modifier.clickable {
                nameDialog.value = true
            }
        )
        AnimatedVisibility(visible = nameDialog.value) {
            UserNameDialog(
                userName
            ) {
                ctx.getSharedPreferences("avatars", 0).edit().putString("name", userName.value)
                    .commit()
                nameDialog.value = false
            }
        }

        for (user in listOf(null).plus(users)) {
            val avatarPath = remember {
                mutableStateOf<String?>(
                    ctx.getSharedPreferences("avatars", 0)
                        .getString(user?.uid ?: "own", null)
                )
            }

            AvatarListItem(
                user = user, avatarPath = avatarPath.value,
            ) { uri ->
                if (uri != null) {
                    val filePath = changeAvatar(ctx, user?.uid ?: "own", uri)
                    if (filePath != null) {
                        avatarPath.value = filePath
                        viewModel.viewModelScope.launch {
                            viewModel._changedPlacemark.emit(user?.uid ?: "own")
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun AvatarListItem(user: User?, avatarPath: String?, onChange: (Uri?) -> Unit) {
    val ctx = LocalContext.current
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { onChange(it) }

    ListItem(
        leadingContent = {
            Image(
                bitmap = when (avatarPath) {
                    null -> ImageProvider.fromResource(
                        ctx,
                        com.yandex.maps.mobile.R.drawable.search_layer_pin_selected_default
                    ).image.asImageBitmap()

                    else -> ImageProvider.fromFile(avatarPath).image.asImageBitmap()
                },
                contentDescription = stringResource(R.string.avatar),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(60.dp, 60.dp)
            )
        },
        headlineContent = {
            Text(user?.name ?: stringResource(R.string.your_placemark))
        },
        modifier = Modifier.clickable {
            avatarPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    )
}

@Composable
fun UserNameDialog(
    userName: MutableState<String>,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onConfirm() },
        title = { Text(stringResource(id = R.string.your_name)) },
        text = { TextField(value = userName.value, onValueChange = { userName.value = it }) },
        confirmButton = {
            Button(onClick = { onConfirm() }) {
                Text("OK")
            }
        }
    )
}