package com.github.antonkonyshev.tryst.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MapViewModel = viewModel(),
    users: Set<User> = viewModel.users.collectAsStateWithLifecycle().value,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current

    Column {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(ScrollState(0))
        ) {
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

            Divider(thickness = 1.dp)

            val userName = remember {
                mutableStateOf<String>(
                    ctx.getSharedPreferences("avatars", 0).getString("name", "")!!
                )
            }
            val nameDialog = remember { mutableStateOf(false) }
            UserNameListItem(userName, nameDialog)

            Divider(thickness = 1.dp)

            val groupName = remember {
                mutableStateOf<String>(
                    ctx.getSharedPreferences("avatars", 0).getString("group", "Guest")!!
                )
            }
            val groupDialog = remember { mutableStateOf(false) }
            GroupNameListItem(groupName, groupDialog)

            Divider(thickness = 1.dp)

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

                Divider(thickness = 1.dp)
            }
        }

        val uriHandler = LocalUriHandler.current
        ListItem(
            headlineContent = {
                Text(
                    "Yandex MapKit",
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            modifier = Modifier.clickable {
                uriHandler.openUri(ctx.getString(R.string.yandex_mapkit_conditions_link))
            }
        )
    }
}

@Composable
fun UserNameListItem(userName: MutableState<String>, nameDialog: MutableState<Boolean>) {
    val ctx = LocalContext.current
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
}

@Composable
fun GroupNameListItem(groupName: MutableState<String>, groupDialog: MutableState<Boolean>) {
    val ctx = LocalContext.current
    ListItem(
        leadingContent = {
            Text("", modifier = Modifier.size(60.dp, 60.dp))
        },
        headlineContent = {
            Text(stringResource(R.string.your_group))
        },
        supportingContent = {
            Text(groupName.value)
        },
        modifier = Modifier.clickable { groupDialog.value = true }
    )
    
    AnimatedVisibility(visible = groupDialog.value) {
        GroupNameDialog(groupName) {
            ctx.getSharedPreferences("avatars", 0).edit().putString("group", groupName.value)
                .commit()
            groupDialog.value = false
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

@Composable
fun GroupNameDialog(
    groupName: MutableState<String>,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onConfirm() },
        title = { Text(stringResource(R.string.your_group)) },
        text = { TextField(value = groupName.value, onValueChange = { groupName.value = it }) },
        confirmButton = {
            Button(onClick = { onConfirm() }) {
                Text("OK")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val users = remember {
        setOf<User>(
            User("123", "First", 50.0, 50.0, Date().time),
            User("234", "Second", 51.0, 51.0, Date().time),
            User("345", "Third", 52.0, 52.0, Date().time),
        )
    }
    SettingsScreen(users = users)
}

@Preview(showBackground = true)
@Composable
fun UserNameListItemPreview() {
    val userName = remember { mutableStateOf("Test") }
    val nameDialog = remember { mutableStateOf(false) }
    UserNameListItem(userName = userName, nameDialog = nameDialog)
}

@Preview(showBackground = true)
@Composable
fun UserNameDialogPreview() {
    val userName = remember { mutableStateOf("Test") }
    val nameDialog = remember { mutableStateOf(true) }
    UserNameListItem(userName = userName, nameDialog = nameDialog)
}

@Preview(showBackground = true)
@Composable
fun GroupNameListItemPreview() {
    val groupName = remember { mutableStateOf("Test") }
    val groupDialog = remember { mutableStateOf(false) }
    GroupNameListItem(groupName, groupDialog)
}

@Preview(showBackground = true)
@Composable
fun GroupNameDialogPreview() {
    val groupName = remember { mutableStateOf("Test") }
    val groupDialog = remember { mutableStateOf(true) }
    GroupNameListItem(groupName, groupDialog)
}