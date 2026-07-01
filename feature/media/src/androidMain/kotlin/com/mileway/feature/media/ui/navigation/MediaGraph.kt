package com.mileway.feature.media.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mileway.feature.media.ui.camera.CameraCaptureScreen
import com.mileway.feature.media.ui.screens.AttachmentPreviewScreen
import com.mileway.feature.media.ui.screens.AttachmentSelectionScreen
import com.mileway.feature.media.ui.screens.CloudLibraryScreen
import com.mileway.feature.media.viewmodel.MediaAction
import com.mileway.feature.media.viewmodel.MediaViewModel
import org.koin.compose.viewmodel.koinViewModel

object MediaRoutes {
    const val SELECTION = "media_selection"

    /** Camera route with an `odometer` flag arg; defaults to plain capture. */
    const val CAMERA = "media_camera"
    const val CAMERA_ARG_ODOMETER = "odometer"

    /** Convenience builder for navigating to the camera in plain or odometer mode. */
    fun camera(odometer: Boolean): String = "$CAMERA?$CAMERA_ARG_ODOMETER=$odometer"

    const val PREVIEW = "media_preview"
    const val CLOUD_LIBRARY = "media_cloud_library"
}

/**
 * Wires the three media routes into a host [NavHostController].
 *
 * All screens share a single [MediaViewModel] instance scoped to the SELECTION
 * back-stack entry so the pending capture batch and attachment list survive navigation
 * between the selection, camera and preview destinations.
 */
fun NavGraphBuilder.mediaGraph(navController: NavHostController) {
    composable(MediaRoutes.SELECTION) { entry ->
        val parentEntry = remember(entry) { navController.getBackStackEntry(MediaRoutes.SELECTION) }
        val vm: MediaViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        AttachmentSelectionScreen(
            viewModel = vm,
            onNavigateToCamera = { odometer ->
                navController.navigate(MediaRoutes.camera(odometer))
            },
            onNavigateToPreview = { navController.navigate(MediaRoutes.PREVIEW) },
            onNavigateBack = { navController.popBackStack() },
            onNavigateToLibrary = { navController.navigate(MediaRoutes.CLOUD_LIBRARY) },
        )
    }

    composable(
        route = "${MediaRoutes.CAMERA}?${MediaRoutes.CAMERA_ARG_ODOMETER}={${MediaRoutes.CAMERA_ARG_ODOMETER}}",
        arguments =
            listOf(
                navArgument(MediaRoutes.CAMERA_ARG_ODOMETER) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) { entry ->
        val parentEntry = remember(entry) { navController.getBackStackEntry(MediaRoutes.SELECTION) }
        val vm: MediaViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        val state by vm.uiState.collectAsStateWithLifecycle()
        val odometer = entry.arguments?.getBoolean(MediaRoutes.CAMERA_ARG_ODOMETER) ?: false
        CameraCaptureScreen(
            onCaptured = { uri ->
                vm.onAction(MediaAction.Captured(uri))
                navController.navigate(MediaRoutes.PREVIEW)
            },
            isOdometerMode = odometer,
            flashMode = state.flashMode,
            onCycleFlash = { vm.onAction(MediaAction.CycleFlashMode) },
        )
    }

    composable(MediaRoutes.CLOUD_LIBRARY) { entry ->
        val selEntry = remember(entry) { navController.getBackStackEntry(MediaRoutes.SELECTION) }
        val selVm: MediaViewModel = koinViewModel(viewModelStoreOwner = selEntry)
        CloudLibraryScreen(
            onNavigateBack = { navController.popBackStack() },
            onSelectUri = { uri ->
                selVm.onAction(MediaAction.PickedFromGallery(uri))
                navController.popBackStack(MediaRoutes.SELECTION, inclusive = false)
            },
        )
    }

    composable(MediaRoutes.PREVIEW) { entry ->
        val parentEntry = remember(entry) { navController.getBackStackEntry(MediaRoutes.SELECTION) }
        val vm: MediaViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        AttachmentPreviewScreen(
            viewModel = vm,
            onRetake = { navController.popBackStack() },
            onUsePhoto = { navController.popBackStack(MediaRoutes.SELECTION, inclusive = false) },
            onAddMore = {
                // Return to the camera (plain mode) to add another photo to the batch.
                navController.navigate(MediaRoutes.camera(false))
            },
        )
    }
}
