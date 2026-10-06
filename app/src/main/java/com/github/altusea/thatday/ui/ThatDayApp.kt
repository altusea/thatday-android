package com.github.altusea.thatday.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.altusea.thatday.data.EventRepository
import com.github.altusea.thatday.ui.detail.EventDetailScreen
import com.github.altusea.thatday.ui.edit.EventEditScreen
import com.github.altusea.thatday.ui.list.EventListScreen

private object Routes {
    const val ARG_EVENT_ID = "eventId"
    const val LIST = "list"
    const val EDIT = "edit?$ARG_EVENT_ID={$ARG_EVENT_ID}"
    const val DETAIL = "detail/{$ARG_EVENT_ID}"

    const val NEW_ID = -1L

    fun editNew() = "edit?$ARG_EVENT_ID=$NEW_ID"
    fun editExisting(id: Long) = "edit?$ARG_EVENT_ID=$id"
    fun detail(id: Long) = "detail/$id"
}

@Composable
fun ThatDayApp(repository: EventRepository) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            EventListScreen(
                repository = repository,
                onAddEvent = { navController.navigate(Routes.editNew()) },
                onOpenEvent = { navController.navigate(Routes.detail(it)) },
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_EVENT_ID) {
                    type = NavType.LongType
                    defaultValue = Routes.NEW_ID
                },
            ),
        ) { entry ->
            val id = entry.arguments?.getLong(Routes.ARG_EVENT_ID) ?: Routes.NEW_ID
            EventEditScreen(
                repository = repository,
                eventId = id.takeIf { it != Routes.NEW_ID },
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(Routes.ARG_EVENT_ID) { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong(Routes.ARG_EVENT_ID) ?: return@composable
            EventDetailScreen(
                repository = repository,
                eventId = id,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.editExisting(id)) },
            )
        }
    }
}
