package com.github.altusea.thatday.ui.detail

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.altusea.thatday.R
import com.github.altusea.thatday.data.EventRepository
import com.github.altusea.thatday.data.TimeKind
import com.github.altusea.thatday.time.RelativeTime
import com.github.altusea.thatday.ui.relativeText
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    repository: EventRepository,
    eventId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: EventDetailViewModel = viewModel(
        key = "detail-$eventId",
        factory = viewModelFactory { initializer { EventDetailViewModel(repository, eventId) } },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val zone = ZoneId.systemDefault()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.event?.title.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val event = state.event
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                event == null && state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                event != null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val absolute = RelativeTime.absoluteLabel(
                        occurredAt = event.occurredAt,
                        allDay = event.timeKind == TimeKind.ALL_DAY,
                        zone = zone,
                        locale = locale,
                    )
                    val kindLabel = stringResource(
                        if (event.timeKind == TimeKind.ALL_DAY) R.string.kind_all_day else R.string.kind_timed,
                    )
                    Text(
                        text = "$absolute · $kindLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.relative?.let {
                        Text(
                            text = relativeText(it),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (!event.note.isNullOrBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.detail_note_header),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(text = event.note, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_edit))
                        }
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.action_delete))
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_dialog_title)) },
            text = { Text(stringResource(R.string.delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete(onDeleted = onBack)
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
