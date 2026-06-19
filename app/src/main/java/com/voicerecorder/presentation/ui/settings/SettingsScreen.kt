package com.voicerecorder.presentation.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicerecorder.R
import com.voicerecorder.domain.model.AudioFormat
import com.voicerecorder.domain.model.AudioQuality
import com.voicerecorder.domain.model.Language
import com.voicerecorder.domain.model.SaveLocation
import com.voicerecorder.domain.model.ThemeMode
import com.voicerecorder.presentation.theme.FinalTalkTheme
import com.voicerecorder.presentation.ui.util.glassmorphic
import com.voicerecorder.presentation.ui.util.magneticTilt
import com.voicerecorder.presentation.ui.util.neonAura

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAbout: () -> Unit,
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val audioFormat by viewModel.audioFormat.collectAsState()
    val audioQuality by viewModel.audioQuality.collectAsState()
    val saveLocation by viewModel.saveLocation.collectAsState()
    val publicFolderName by viewModel.publicFolderName.collectAsState()
    val publicFolderUri by viewModel.publicFolderUri.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val folderPickerLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree(),
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                var displayName = ""
                try {
                    val documentId = android.provider.DocumentsContract.getTreeDocumentId(uri)
                    val documentUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
                    context.contentResolver.query(
                        documentUri,
                        arrayOf(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                        null,
                        null,
                        null,
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            displayName = cursor.getString(0) ?: ""
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (displayName.isEmpty()) {
                    displayName = uri.path?.substringAfterLast(':') ?: context.getString(R.string.folder_custom_fallback)
                }
                viewModel.setPublicFolder(uri.toString(), displayName)
            }
        }

    val scrollState = rememberScrollState()

    var themeExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var formatExpanded by remember { mutableStateOf(false) }
    var qualityExpanded by remember { mutableStateOf(false) }

    val themeLabel =
        when (themeMode) {
            ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
            ThemeMode.DARK -> stringResource(R.string.theme_dark)
        }

    val languageLabel =
        when (language) {
            Language.SYSTEM -> stringResource(R.string.lang_system)
            Language.ENGLISH -> stringResource(R.string.lang_en)
            Language.SPANISH -> stringResource(R.string.lang_es)
            Language.FRENCH -> stringResource(R.string.lang_fr)
            Language.GERMAN -> stringResource(R.string.lang_de)
            Language.HEBREW -> stringResource(R.string.lang_he)
        }

    val formatLabel =
        when (audioFormat) {
            AudioFormat.M4A -> stringResource(R.string.format_m4a)
            AudioFormat.AMR -> stringResource(R.string.format_amr)
            AudioFormat.WAV -> stringResource(R.string.format_wav)
            AudioFormat.AAC -> stringResource(R.string.format_aac)
            AudioFormat.OGG -> stringResource(R.string.format_ogg)
        }

    val qualityLabel =
        when (audioQuality) {
            AudioQuality.HIGH -> stringResource(R.string.quality_high)
            AudioQuality.MEDIUM -> stringResource(R.string.quality_medium)
            AudioQuality.LOW -> stringResource(R.string.quality_low)
        }

    val saveLocationLabel =
        when (saveLocation) {
            SaveLocation.PRIVATE -> stringResource(R.string.location_private)
            SaveLocation.PUBLIC -> stringResource(R.string.location_public)
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(FinalTalkTheme.gradients.backgroundGradient)
                .padding(20.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Section: General Settings
            Text(
                text = stringResource(R.string.settings_header_general),
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp),
            )

            // Glassmorphic panel with interactive magnetic touch parallax
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .magneticTilt(maxRotationX = 5f, maxRotationY = 5f)
                    .glassmorphic(RoundedCornerShape(24.dp), borderWidth = 0.8.dp)
            ) {
                Column {
                    SettingsDropdownItem(
                        title = stringResource(R.string.setting_theme),
                        subtitle = themeLabel,
                        icon = Icons.Default.Settings,
                        expanded = themeExpanded,
                        onExpandedChange = { themeExpanded = it },
                        onDismissRequest = { themeExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_system)) },
                            onClick = {
                                viewModel.setThemeMode(ThemeMode.SYSTEM)
                                themeExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_light)) },
                            onClick = {
                                viewModel.setThemeMode(ThemeMode.LIGHT)
                                themeExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_dark)) },
                            onClick = {
                                viewModel.setThemeMode(ThemeMode.DARK)
                                themeExpanded = false
                            },
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    SettingsDropdownItem(
                        title = stringResource(R.string.setting_language),
                        subtitle = languageLabel,
                        icon = Icons.Default.Language,
                        expanded = languageExpanded,
                        onExpandedChange = { languageExpanded = it },
                        onDismissRequest = { languageExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lang_system)) },
                            onClick = {
                                viewModel.setLanguage(Language.SYSTEM)
                                languageExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lang_en)) },
                            onClick = {
                                viewModel.setLanguage(Language.ENGLISH)
                                languageExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lang_es)) },
                            onClick = {
                                viewModel.setLanguage(Language.SPANISH)
                                languageExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lang_fr)) },
                            onClick = {
                                viewModel.setLanguage(Language.FRENCH)
                                languageExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lang_de)) },
                            onClick = {
                                viewModel.setLanguage(Language.GERMAN)
                                languageExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lang_he)) },
                            onClick = {
                                viewModel.setLanguage(Language.HEBREW)
                                languageExpanded = false
                            },
                        )
                    }
                }
            }

            // Section: Recording Configs
            Text(
                text = stringResource(R.string.settings_header_recording),
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .magneticTilt(maxRotationX = 5f, maxRotationY = 5f)
                    .glassmorphic(RoundedCornerShape(24.dp), borderWidth = 0.8.dp)
            ) {
                Column {
                    SettingsDropdownItem(
                        title = stringResource(R.string.setting_format),
                        subtitle = formatLabel,
                        icon = Icons.Default.Info,
                        expanded = formatExpanded,
                        onExpandedChange = { formatExpanded = it },
                        onDismissRequest = { formatExpanded = false },
                    ) {
                        AudioFormat.entries.forEach { format ->
                            val label =
                                when (format) {
                                    AudioFormat.M4A -> stringResource(R.string.format_m4a)
                                    AudioFormat.AMR -> stringResource(R.string.format_amr)
                                    AudioFormat.WAV -> stringResource(R.string.format_wav)
                                    AudioFormat.AAC -> stringResource(R.string.format_aac)
                                    AudioFormat.OGG -> stringResource(R.string.format_ogg)
                                }
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.setAudioFormat(format)
                                    formatExpanded = false
                                },
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    SettingsDropdownItem(
                        title = stringResource(R.string.setting_quality),
                        subtitle = qualityLabel,
                        icon = Icons.Default.Tune,
                        expanded = qualityExpanded,
                        onExpandedChange = { qualityExpanded = it },
                        onDismissRequest = { qualityExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.quality_high)) },
                            onClick = {
                                viewModel.setAudioQuality(AudioQuality.HIGH)
                                qualityExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.quality_medium)) },
                            onClick = {
                                viewModel.setAudioQuality(AudioQuality.MEDIUM)
                                qualityExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.quality_low)) },
                            onClick = {
                                viewModel.setAudioQuality(AudioQuality.LOW)
                                qualityExpanded = false
                            },
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    SettingsSwitchItem(
                        title = stringResource(R.string.setting_save_location),
                        subtitle = saveLocationLabel,
                        icon = Icons.Default.Folder,
                        checked = saveLocation == SaveLocation.PUBLIC,
                        onCheckedChange = { isChecked ->
                            val targetLoc = if (isChecked) SaveLocation.PUBLIC else SaveLocation.PRIVATE
                            viewModel.setSaveLocation(targetLoc)
                        },
                    )

                    if (saveLocation == SaveLocation.PUBLIC) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        SettingsNavigationItem(
                            title = stringResource(R.string.setting_select_public_folder),
                            subtitle = if (publicFolderName.isNotEmpty()) publicFolderName else stringResource(R.string.setting_default_public_folder_path),
                            icon = Icons.Default.Folder,
                            onClick = {
                                folderPickerLauncher.launch(null)
                            },
                        )
                    }
                }
            }

            // Section: App Info
            Text(
                text = stringResource(R.string.settings_header_support),
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .magneticTilt(maxRotationX = 5f, maxRotationY = 5f)
                    .glassmorphic(RoundedCornerShape(24.dp), borderWidth = 0.8.dp)
            ) {
                SettingsNavigationItem(
                    title = stringResource(R.string.setting_about),
                    icon = Icons.Default.Info,
                    onClick = onNavigateToAbout,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsDropdownItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(true) }
                    .padding(vertical = 16.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 16.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
        )
    }
}

@Composable
private fun SettingsNavigationItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}
