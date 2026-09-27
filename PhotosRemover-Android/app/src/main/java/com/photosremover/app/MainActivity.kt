package com.photosremover.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.photosremover.app.ui.components.AppDrawer
import com.photosremover.app.ui.components.LargeMediaCleanerSheet
import com.photosremover.app.ui.screens.HomeScreen
import com.photosremover.app.ui.screens.PreviewScreen
import com.photosremover.app.ui.screens.ResultsScreen
import com.photosremover.app.ui.screens.ScanningScreen
import com.photosremover.app.ui.theme.PhotosRemoverTheme
import com.photosremover.app.ui.viewmodel.AppScreen
import com.photosremover.app.ui.viewmodel.MainUiEvent
import com.photosremover.app.ui.viewmodel.MainViewModel
import com.photosremover.app.ui.viewmodel.ResultsTab

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var hasPhotoPermission by mutableStateOf(false)
    private var isPartialAccess by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updatePermissionStates()

        setContent {
            PhotosRemoverTheme {
                MainContent(
                    viewModel = viewModel,
                    activity = this,
                    hasPermission = hasPhotoPermission,
                    isPartialAccess = isPartialAccess,
                    onPermissionUpdated = { updatePermissionStates() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStates()
    }

    private fun updatePermissionStates() {
        hasPhotoPermission = checkHasPhotoPermission(this)
        isPartialAccess = checkIsPartialAccess(this)
    }
}

@Composable
fun MainContent(
    viewModel: MainViewModel,
    activity: ComponentActivity,
    hasPermission: Boolean,
    isPartialAccess: Boolean,
    onPermissionUpdated: () -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val exactSets by viewModel.exactSets.collectAsState()
    val similarSets by viewModel.similarSets.collectAsState()
    val exactVideoSets by viewModel.exactVideoSets.collectAsState()
    val similarVideoSets by viewModel.similarVideoSets.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val previewPhoto by viewModel.previewPhoto.collectAsState()
    val previewSet by viewModel.previewSet.collectAsState()
    val cleanedCount by viewModel.cleanedCount.collectAsState()
    val cleanedSize by viewModel.cleanedSize.collectAsState()
    val sensitivity by viewModel.sensitivity.collectAsState()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
    val excludedIds by viewModel.excludedIds.collectAsState()
    val storageBreakdown by viewModel.storageBreakdown.collectAsState()
    val allPhotos by viewModel.allPhotos.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()
    var showLargeMediaCleaner by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDeviceStorage()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onPermissionUpdated()
        if (checkHasPhotoPermission(activity)) {
            // Permission just granted – start the scan automatically
            viewModel.startScan()
        } else {
            Toast.makeText(activity, "Photo permission is needed to scan duplicates", Toast.LENGTH_SHORT).show()
        }
    }

    // Android 10/11+ IntentSender Launcher for media deletion consent
    var pendingConsentCount by remember { mutableStateOf(0) }
    var pendingConsentBytes by remember { mutableStateOf(0L) }

    val deleteIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onDeletionConfirmed(pendingConsentCount, pendingConsentBytes)
        } else {
            Toast.makeText(activity, "Deletion cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Error dialog state
    var errorDialogTitle by remember { mutableStateOf("") }
    var errorDialogMessage by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }

    // Collect UI Events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is MainUiEvent.RequestDeleteConsent -> {
                    pendingConsentCount = event.count
                    pendingConsentBytes = event.bytes
                    deleteIntentLauncher.launch(
                        IntentSenderRequest.Builder(event.intentSender).build()
                    )
                }
                is MainUiEvent.ShowToast -> {
                    Toast.makeText(activity, event.message, Toast.LENGTH_SHORT).show()
                }
                is MainUiEvent.ShowError -> {
                    errorDialogTitle = event.title
                    errorDialogMessage = event.message
                    showErrorDialog = true
                }
            }
        }
    }

    // Handle Android System Back button
    BackHandler(enabled = currentScreen != AppScreen.HOME || isDrawerOpen || showLargeMediaCleaner) {
        if (showLargeMediaCleaner) {
            showLargeMediaCleaner = false
        } else if (isDrawerOpen) {
            viewModel.setDrawerOpen(false)
        } else {
            when (currentScreen) {
                AppScreen.PREVIEW -> viewModel.closePreview()
                AppScreen.SCANNING -> viewModel.cancelScan()
                AppScreen.RESULTS -> viewModel.navigateToHome()
                AppScreen.HOME -> activity.finish()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Screen Body
        when (currentScreen) {
            AppScreen.HOME -> {
                HomeScreen(
                    cleanedCount = cleanedCount,
                    cleanedSizeBytes = cleanedSize,
                    hasPermission = hasPermission,
                    storageBreakdown = storageBreakdown,
                    isPartialAccess = isPartialAccess,
                    onRequestPermission = {
                        val permsToRequest = getRequiredPermissions()
                        permissionLauncher.launch(permsToRequest)
                    },
                    onOpenSettings = {
                        openAppSettings(activity)
                    },
                    onStartScan = { viewModel.startScan() },
                    onOpenDrawer = { viewModel.setDrawerOpen(true) }
                )
            }
            AppScreen.SCANNING -> {
                ScanningScreen(
                    progress = scanProgress,
                    onCancelScan = { viewModel.cancelScan() }
                )
            }
            AppScreen.RESULTS -> {
                ResultsScreen(
                    exactSets = exactSets,
                    similarSets = similarSets,
                    exactVideoSets = exactVideoSets,
                    similarVideoSets = similarVideoSets,
                    selectedTab = selectedTab,
                    storageBreakdown = storageBreakdown,
                    onTabSelected = { viewModel.setTab(it) },
                    onPhotoClick = { photo, set -> viewModel.openPreview(photo, set) },
                    onTogglePhotoSelect = { setId, photoId -> viewModel.togglePhotoSelection(setId, photoId) },
                    onToggleSetSelect = { setId -> viewModel.toggleSetSelection(setId) },
                    onSelectAllExceptBest = { setId -> viewModel.selectAllExceptBestInSet(setId) },
                    onDeleteClick = { viewModel.deleteSelectedPhotos() },
                    onBackClick = { viewModel.navigateToHome() },
                    onOpenDrawer = { viewModel.setDrawerOpen(true) },
                    onTogglePhotoExclude = { photoId -> viewModel.toggleExclusion(photoId) },
                    onOpenLargeMediaCleaner = {
                        viewModel.loadAllMediaIfNeeded()
                        showLargeMediaCleaner = true
                    }
                )
            }
            AppScreen.PREVIEW -> {
                if (previewPhoto != null && previewSet != null) {
                    val isPhotoExcluded = viewModel.isExcluded(previewPhoto!!.id)
                    PreviewScreen(
                        photo = previewPhoto!!,
                        set = previewSet!!,
                        isExcluded = isPhotoExcluded,
                        onBackClick = { viewModel.closePreview() },
                        onToggleSelect = {
                            viewModel.togglePhotoSelection(previewSet!!.id, previewPhoto!!.id)
                        },
                        onToggleExclude = {
                            viewModel.toggleExclusion(previewPhoto!!.id)
                        }
                    )
                }
            }
        }

        // Slide-out Drawer Overlay
        if (isDrawerOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { viewModel.setDrawerOpen(false) }
            )
        }

        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            AppDrawer(
                cleanedCount = cleanedCount,
                cleanedSizeBytes = cleanedSize,
                sensitivity = sensitivity,
                excludedCount = excludedIds.size,
                onSensitivityChange = { viewModel.setSensitivity(it) },
                onCloseClick = { viewModel.setDrawerOpen(false) },
                onLargeMediaCleanerClick = {
                    viewModel.setDrawerOpen(false)
                    viewModel.loadAllMediaIfNeeded()
                    showLargeMediaCleaner = true
                },
                onFeedbackClick = {
                    Toast.makeText(activity, "Thank you for using Remo!", Toast.LENGTH_SHORT).show()
                },
                onRateClick = {
                    Toast.makeText(activity, "Rate us 5 stars on Google Play!", Toast.LENGTH_SHORT).show()
                },
                onAboutClick = {
                    Toast.makeText(activity, "Remo v1.0.0 - Fast & Private Duplicate Remover", Toast.LENGTH_LONG).show()
                },
                onClearExclusionsClick = {
                    if (excludedIds.isEmpty()) {
                        Toast.makeText(activity, "Chưa có ảnh nào trong danh sách loại trừ", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.clearAllExclusions()
                    }
                }
            )
        }

        // Large Media Cleaner Bottom Sheet
        if (showLargeMediaCleaner) {
            LargeMediaCleanerSheet(
                photos = allPhotos,
                videos = allVideos,
                onDeleteSelected = { photosToDelete, videosToDelete ->
                    viewModel.deleteLargeMediaItems(photosToDelete, videosToDelete)
                },
                onDismiss = { showLargeMediaCleaner = false }
            )
        }

        // Error dialog – shown when scan fails with a critical error
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = {
                    Text(text = errorDialogTitle, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                },
                text = {
                    Text(text = errorDialogMessage, color = Color(0xFF475569))
                },
                confirmButton = {
                    Button(
                        onClick = { showErrorDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC800))
                    ) {
                        Text("OK", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

private fun checkHasPhotoPermission(activity: Activity): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

private fun checkIsPartialAccess(activity: Activity): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
    } else {
        false
    }
}

private fun openAppSettings(activity: Activity) {
    try {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    } catch (_: Exception) {}
}

private fun getRequiredPermissions(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }
}
