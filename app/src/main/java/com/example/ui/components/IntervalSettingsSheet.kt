package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.preferences.TimerSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalSettingsSheet(
    settings: TimerSettings,
    onUpdateSettings: ((TimerSettings) -> TimerSettings) -> Unit,
    onApplyPreset: (focusMin: Int, shortBreakMin: Int, longBreakMin: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Quick cadence presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PresetChip(
                    label = "25 / 5",
                    selected = settings.focusMinutes == 25 && settings.shortBreakMinutes == 5,
                    onClick = { onApplyPreset(25, 5, 15) },
                    modifier = Modifier.weight(1f).testTag("preset_classic"),
                )
                PresetChip(
                    label = "50 / 10",
                    selected = settings.focusMinutes == 50 && settings.shortBreakMinutes == 10,
                    onClick = { onApplyPreset(50, 10, 20) },
                    modifier = Modifier.weight(1f).testTag("preset_deep"),
                )
                PresetChip(
                    label = "15 / 3",
                    selected = settings.focusMinutes == 15 && settings.shortBreakMinutes == 3,
                    onClick = { onApplyPreset(15, 3, 10) },
                    modifier = Modifier.weight(1f).testTag("preset_sprint"),
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    DurationStepperRow(
                        title = stringResource(R.string.settings_focus_duration),
                        valueLabel = stringResource(R.string.settings_minutes_unit, settings.focusMinutes),
                        canDecrease = settings.focusMinutes > 1,
                        canIncrease = settings.focusMinutes < 90,
                        onDecrease = {
                            val step = if (settings.focusMinutes > 10) 5 else 1
                            onUpdateSettings { it.copy(focusMinutes = (it.focusMinutes - step).coerceAtLeast(1)) }
                        },
                        onIncrease = {
                            val step = if (settings.focusMinutes >= 10) 5 else 1
                            onUpdateSettings { it.copy(focusMinutes = (it.focusMinutes + step).coerceAtMost(90)) }
                        },
                        tagPrefix = "focus_duration",
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    DurationStepperRow(
                        title = stringResource(R.string.settings_short_break_duration),
                        valueLabel = stringResource(R.string.settings_minutes_unit, settings.shortBreakMinutes),
                        canDecrease = settings.shortBreakMinutes > 1,
                        canIncrease = settings.shortBreakMinutes < 30,
                        onDecrease = {
                            onUpdateSettings { it.copy(shortBreakMinutes = (it.shortBreakMinutes - 1).coerceAtLeast(1)) }
                        },
                        onIncrease = {
                            onUpdateSettings { it.copy(shortBreakMinutes = (it.shortBreakMinutes + 1).coerceAtMost(30)) }
                        },
                        tagPrefix = "short_break_duration",
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    DurationStepperRow(
                        title = stringResource(R.string.settings_long_break_duration),
                        valueLabel = stringResource(R.string.settings_minutes_unit, settings.longBreakMinutes),
                        canDecrease = settings.longBreakMinutes > 5,
                        canIncrease = settings.longBreakMinutes < 60,
                        onDecrease = {
                            onUpdateSettings { it.copy(longBreakMinutes = (it.longBreakMinutes - 5).coerceAtLeast(5)) }
                        },
                        onIncrease = {
                            onUpdateSettings { it.copy(longBreakMinutes = (it.longBreakMinutes + 5).coerceAtMost(60)) }
                        },
                        tagPrefix = "long_break_duration",
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    DurationStepperRow(
                        title = stringResource(R.string.settings_cycles_before_long),
                        valueLabel = stringResource(R.string.settings_cycles_unit, settings.sessionsBeforeLongBreak),
                        canDecrease = settings.sessionsBeforeLongBreak > 2,
                        canIncrease = settings.sessionsBeforeLongBreak < 8,
                        onDecrease = {
                            onUpdateSettings {
                                it.copy(sessionsBeforeLongBreak = (it.sessionsBeforeLongBreak - 1).coerceAtLeast(2))
                            }
                        },
                        onIncrease = {
                            onUpdateSettings {
                                it.copy(sessionsBeforeLongBreak = (it.sessionsBeforeLongBreak + 1).coerceAtMost(8))
                            }
                        },
                        tagPrefix = "cycles_before_long",
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    DurationStepperRow(
                        title = stringResource(R.string.settings_daily_goal),
                        valueLabel = stringResource(R.string.settings_minutes_unit, settings.dailyGoalMinutes),
                        canDecrease = settings.dailyGoalMinutes > 15,
                        canIncrease = settings.dailyGoalMinutes < 480,
                        onDecrease = {
                            onUpdateSettings {
                                it.copy(dailyGoalMinutes = (it.dailyGoalMinutes - 15).coerceAtLeast(15))
                            }
                        },
                        onIncrease = {
                            onUpdateSettings {
                                it.copy(dailyGoalMinutes = (it.dailyGoalMinutes + 15).coerceAtMost(480))
                            }
                        },
                        tagPrefix = "daily_goal",
                    )
                }
            }

            // Behavior toggles
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsToggleRow(
                        label = stringResource(R.string.settings_auto_start_break),
                        checked = settings.autoStartBreak,
                        onCheckedChange = { checked ->
                            onUpdateSettings { it.copy(autoStartBreak = checked) }
                        },
                        testTag = "toggle_auto_start_break",
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    SettingsToggleRow(
                        label = stringResource(R.string.settings_auto_start_focus),
                        checked = settings.autoStartFocus,
                        onCheckedChange = { checked ->
                            onUpdateSettings { it.copy(autoStartFocus = checked) }
                        },
                        testTag = "toggle_auto_start_focus",
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    SettingsToggleRow(
                        label = stringResource(R.string.settings_sound_chime),
                        checked = settings.soundEnabled,
                        onCheckedChange = { checked ->
                            onUpdateSettings { it.copy(soundEnabled = checked) }
                        },
                        testTag = "toggle_sound_chime",
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    SettingsToggleRow(
                        label = stringResource(R.string.settings_haptics),
                        checked = settings.hapticsEnabled,
                        onCheckedChange = { checked ->
                            onUpdateSettings { it.copy(hapticsEnabled = checked) }
                        },
                        testTag = "toggle_haptics",
                    )
                }
            }

            // About section
            AboutDeveloperCard()

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("settings_done_button"),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_done),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun DurationStepperRow(
    title: String,
    valueLabel: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    tagPrefix: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onDecrease,
                enabled = canDecrease,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("${tagPrefix}_decrease"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = stringResource(R.string.cd_decrease_value),
                )
            }

            IconButton(
                onClick = onIncrease,
                enabled = canIncrease,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("${tagPrefix}_increase"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.cd_increase_value),
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}

@Composable
private fun AboutDeveloperCard() {
    val uriHandler = LocalUriHandler.current
    val xUrl = stringResource(R.string.about_x_url)

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("about_section_card"),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.about_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${stringResource(R.string.app_name)} · ${
                        stringResource(R.string.about_version_format, BuildConfig.VERSION_NAME)
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            // Developer row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PersonOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.about_developer_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.about_developer_name),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            // X social link row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clickable {
                        try {
                            uriHandler.openUri(xUrl)
                        } catch (_: Exception) {
                            // Safely ignore if no browser handler is installed
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("about_x_link"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AlternateEmail,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.about_x_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.about_x_handle),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = stringResource(R.string.cd_open_x_profile),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
