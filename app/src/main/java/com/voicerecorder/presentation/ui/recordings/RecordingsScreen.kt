package com.voicerecorder.presentation.ui.recordings

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.voicerecorder.R
import com.voicerecorder.domain.model.AudioRecording
import com.voicerecorder.domain.model.PlayerState
import com.voicerecorder.presentation.theme.FinalTalkTheme
import com.voicerecorder.presentation.ui.recordings.components.PlayerController
import com.voicerecorder.presentation.ui.util.FormatUtils
import com.voicerecorder.presentation.ui.util.glassmorphic
import com.voicerecorder.presentation.ui.util.magneticTilt
import com.voicerecorder.presentation.ui.util.neonAura
import java.io.File

@Composable
fun RecordingsScreen(
    viewModel: RecordingsViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val recordings by viewModel.recordings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    // Track selected recording for active bottom player controls
    var activeRecording by remember { mutableStateOf<AudioRecording?>(null) }

    // Tracks dialog visibility & metadata targets
    var recordingToDelete by remember { mutableStateOf<AudioRecording?>(null) }
    var recordingToRename by remember { mutableStateOf<AudioRecording?>(null) }
    var renameInputName by remember { mutableStateOf("") }

    // Sync active player details
    val activePlayingId =
        when (val ps = playerState) {
            is PlayerState.Playing -> ps.recordingId
            is PlayerState.Paused -> ps.recordingId
            else -> null
        }

    // Update active recording metadata if details are modified
    LaunchedEffect(recordings, activePlayingId) {
        activeRecording =
            if (activePlayingId != null) {
                recordings.firstOrNull { it.id == activePlayingId }
            } else {
                null
            }
    }

    val actionError by viewModel.actionError.collectAsState()

    LaunchedEffect(playerState) {
        val state = playerState
        if (state is PlayerState.Error) {
            val baseMsg = context.getString(state.errorResId)
            val fullMsg = if (state.details != null) "$baseMsg (${state.details})" else baseMsg
            snackbarHostState.showSnackbar(fullMsg)
            viewModel.stopPlayback()
        }
    }

    LaunchedEffect(actionError) {
        actionError?.let { errResId ->
            snackbarHostState.showSnackbar(context.getString(errResId))
            viewModel.clearActionError()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(FinalTalkTheme.gradients.backgroundGradient),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
        ) {
            // Modern Glassmorphic Search Bar Capsule
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_hint),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .glassmorphic(RoundedCornerShape(20.dp), borderWidth = 0.8.dp),
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
            )

            if (recordings.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.no_recordings),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = recordings,
                        key = { it.id },
                    ) { recording ->
                        val isPlayingThis = activePlayingId == recording.id
                        RecordingItem(
                            recording = recording,
                            isPlaying = isPlayingThis,
                            onClick = {
                                if (isPlayingThis) {
                                    if (playerState is PlayerState.Playing) {
                                        viewModel.pausePlayback()
                                    } else {
                                        viewModel.resumePlayback()
                                    }
                                } else {
                                    viewModel.playRecording(recording)
                                }
                            },
                            onActionRename = {
                                recordingToRename = recording
                                renameInputName = recording.title
                            },
                            onActionDelete = {
                                recordingToDelete = recording
                            },
                            onActionShare = {
                                shareRecordingFile(context, recording)
                            },
                        )
                    }
                    // Spacer at end to prevent overlapping with floating player
                    item {
                        Spacer(modifier = Modifier.height(130.dp))
                    }
                }
            }
        }

        // Bottom-docked player overlay controller
        PlayerController(
            playerState = playerState,
            activeRecording = activeRecording,
            onPlayPauseToggle = {
                if (playerState is PlayerState.Playing) {
                    viewModel.pausePlayback()
                } else {
                    viewModel.resumePlayback()
                }
            },
            onStop = { viewModel.stopPlayback() },
            onSeek = { viewModel.seekTo(it) },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
        )

        // Delete confirmation dialog
        recordingToDelete?.let { rec ->
            AlertDialog(
                onDismissRequest = { recordingToDelete = null },
                title = { Text(stringResource(R.string.dialog_delete_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.dialog_delete_msg,
                            rec.title,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteRecording(rec.id)
                            recordingToDelete = null
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recordingToDelete = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }

        // Rename input dialog
        recordingToRename?.let { rec ->
            AlertDialog(
                onDismissRequest = { recordingToRename = null },
                title = { Text(stringResource(R.string.dialog_rename_title)) },
                text = {
                    TextField(
                        value = renameInputName,
                        onValueChange = { renameInputName = it },
                        placeholder = { Text(stringResource(R.string.rename_input_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.renameRecording(rec.id, renameInputName)
                            recordingToRename = null
                        },
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recordingToRename = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordingItem(
    recording: AudioRecording,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onActionRename: () -> Unit,
    onActionDelete: () -> Unit,
    onActionShare: () -> Unit,
) {
    var expandedMenu by remember { mutableStateOf(false) }

    val activeBorderModifier =
        if (isPlaying) {
            Modifier.border(
                width = 1.2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp),
            )
        } else {
            Modifier
        }

    val glowAlpha = if (isPlaying) 0.15f else 0.0f

    // Glassmorphic item frame with scroll-safe magnetic 3D tilt and glowing aura
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .magneticTilt(maxRotationX = 6f, maxRotationY = 6f)
                .then(activeBorderModifier)
                .neonAura(color = MaterialTheme.colorScheme.primary, alpha = glowAlpha, radiusFraction = 0.85f)
                .glassmorphic(RoundedCornerShape(20.dp), borderWidth = 0.8.dp)
                .clickable(onClick = onClick)
                .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Icon layout
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .background(
                            brush = if (isPlaying) {
                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                            } else {
                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                            },
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isPlaying) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details metadata layout
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = recording.title,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = FormatUtils.formatDuration(recording.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = FormatUtils.formatFileSize(recording.fileSize),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = FormatUtils.formatDate(recording.timestamp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }

            // Actions menu toggle
            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_share)) },
                        onClick = {
                            expandedMenu = false
                            onActionShare()
                        },
                        leadingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp)) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = {
                            expandedMenu = false
                            onActionRename()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.action_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            expandedMenu = false
                            onActionDelete()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun shareRecordingFile(
    context: Context,
    recording: AudioRecording,
) {
    val uri =
        if (recording.filePath.startsWith("content://")) {
            android.net.Uri.parse(recording.filePath)
        } else {
            val file = java.io.File(recording.filePath)
            if (!file.exists()) return
            FileProvider.getUriForFile(
                context,
                "com.voicerecorder.fileprovider",
                file,
            )
        }

    val mimeType =
        when {
            recording.filePath.endsWith(".m4a") -> "audio/mp4"
            recording.filePath.endsWith(".3gp") -> "audio/3gpp"
            recording.filePath.endsWith(".wav") -> "audio/wav"
            recording.filePath.endsWith(".aac") -> "audio/aac"
            recording.filePath.endsWith(".ogg") -> "audio/ogg"
            else -> "audio/*"
        }

    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, recording.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
}
