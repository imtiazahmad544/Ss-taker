package com.floatingscreen.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.floatingscreen.ui.theme.FloatingScreenTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Media projection permission launcher
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.onMediaProjectionResult(result.resultCode, result.data)
            Timber.d("Media projection permission granted")
        } else {
            Timber.w("Media projection permission denied")
        }
    }

    // Runtime permissions launcher
    private val runtimePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.forEach { (perm, granted) ->
            Timber.d("Permission $perm: $granted")
        }
        viewModel.refreshPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request runtime permissions on start
        runtimePermissionsLauncher.launch(
            viewModel.permissionManager.getRequiredRuntimePermissions()
        )

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val uiEvents = viewModel.uiEvent

            val navController = rememberNavController()

            // Handle UI events
            LaunchedEffect(Unit) {
                uiEvents.collect { event ->
                    when (event) {
                        is UiEvent.RequestMediaProjection -> requestMediaProjection()
                        is UiEvent.RequestOverlayPermission -> requestOverlayPermission()
                        is UiEvent.ShowMessage -> { /* Snackbar could be shown here */ }
                        is UiEvent.NavigateTo -> navController.navigate(event.route)
                    }
                }
            }

            FloatingScreenTheme(darkTheme = settings.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        navController = navController,
                        mainViewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }

    private fun requestMediaProjection() {
        viewModel.requestMediaProjection(this)
    }

    private fun requestOverlayPermission() {
        val intent = viewModel.permissionManager.getOverlayPermissionIntent()
        startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MainViewModel.REQUEST_MEDIA_PROJECTION) {
            viewModel.onMediaProjectionResult(resultCode, data)
        }
    }
}
