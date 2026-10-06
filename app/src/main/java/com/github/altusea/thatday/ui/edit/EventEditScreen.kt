package com.github.altusea.thatday.ui.edit

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.altusea.thatday.R
import com.github.altusea.thatday.data.EventRepository
import com.github.altusea.thatday.data.TimeKind
import com.github.altusea.thatday.time.EventTimes
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditScreen(
    repository: EventRepository,
    eventId: Long?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: EventEditViewModel = viewModel(
        key = "edit-${eventId ?: "new"}",
        factory = viewModelFactory { initializer { EventEditViewModel(repository, eventId) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    BackHandler(enabled = state.isDirty && !state.saved) { showDiscardDialog = true }

    val requestClose = {
        if (state.isDirty) showDiscardDialog = true else onDone()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (eventId == null) R.string.edit_new_title else R.string.edit_edit_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = requestClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .widthIn(max = 640.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text(stringResource(R.string.field_title_label)) },
                placeholder = { Text(stringResource(R.string.field_title_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            ReadOnlyField(
                value = state.date.format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale),
                ),
                label = stringResource(R.string.field_date_label),
                onClick = { showDatePicker = true },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = stringResource(R.string.cd_pick_date),
                    )
                },
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TimeKind.entries.forEachIndexed { index, kind ->
                    SegmentedButton(
                        selected = state.timeKind == kind,
                        onClick = { viewModel.onTimeKindChange(kind) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = TimeKind.entries.size),
                    ) {
                        Text(
                            stringResource(
                                if (kind == TimeKind.ALL_DAY) R.string.kind_all_day else R.string.kind_timed,
                            ),
                        )
                    }
                }
            }

            if (state.timeKind == TimeKind.TIMED) {
                ReadOnlyField(
                    value = LocalTime.of(state.hour, state.minute).format(
                        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale),
                    ),
                    label = stringResource(R.string.cd_pick_time),
                    onClick = { showTimePicker = true },
                )
            }

            if (state.reminderAvailable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.remind_label, state.aheadDays),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = state.remind, onCheckedChange = viewModel::onRemindChange)
                }
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.field_note_label)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = requestClose,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = viewModel::save,
                    enabled = state.canSave,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = EventTimes.localDateToPickerMillis(state.date),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.onDateChange(EventTimes.pickerMillisToLocalDate(it))
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.common_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.hour,
            initialMinute = state.minute,
            is24Hour = is24Hour,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onTimeChange(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    },
                ) {
                    Text(stringResource(R.string.common_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_dialog_title)) },
            text = { Text(stringResource(R.string.discard_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onDone()
                    },
                ) {
                    Text(stringResource(R.string.action_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.action_keep_editing))
                }
            },
        )
    }
}

@Composable
private fun ReadOnlyField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = trailingIcon,
            modifier = Modifier.fillMaxWidth(),
        )
        // A transparent layer makes the whole field a picker trigger, not just
        // the trailing icon.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick),
        )
    }
}
