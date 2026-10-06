package com.github.altusea.thatday.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.altusea.thatday.R
import com.github.altusea.thatday.data.Event
import com.github.altusea.thatday.data.EventRepository
import com.github.altusea.thatday.data.TimeKind
import com.github.altusea.thatday.time.Relative
import com.github.altusea.thatday.time.RelativeTime
import com.github.altusea.thatday.ui.relativeText
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    repository: EventRepository,
    onAddEvent: () -> Unit,
    onOpenEvent: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: EventListViewModel = viewModel(
        factory = viewModelFactory { initializer { EventListViewModel(repository) } },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val locale = LocalConfiguration.current.locales[0]
    val zone = ZoneId.systemDefault()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddEvent,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add)) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.isEmpty -> EmptyState(modifier = Modifier.align(Alignment.Center))

                else -> LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = MAX_CONTENT_WIDTH),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    state.groups.forEach { group ->
                        stickyHeader(key = "header-${group.relative}") {
                            GroupHeader(label = relativeText(group.relative))
                        }
                        items(items = group.events, key = { it.id }) { event ->
                            EventCard(
                                event = event,
                                relative = group.relative,
                                zone = zone,
                                locale = locale,
                                onClick = { onOpenEvent(event.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventCard(
    event: Event,
    relative: Relative,
    zone: ZoneId,
    locale: Locale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val absolute = RelativeTime.absoluteLabel(
        occurredAt = event.occurredAt,
        allDay = event.timeKind == TimeKind.ALL_DAY,
        zone = zone,
        locale = locale,
    )
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        // Read the whole card as one sentence for TalkBack (DESIGN.md §3.0).
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Column {
                    Text(
                        text = absolute,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!event.note.isNullOrBlank()) {
                        Text(
                            text = event.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            trailingContent = {
                Text(
                    text = relativeText(relative),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.list_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.list_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val MAX_CONTENT_WIDTH = 640.dp
