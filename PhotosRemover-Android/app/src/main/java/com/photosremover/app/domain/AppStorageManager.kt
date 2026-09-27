package com.photosremover.app.domain

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val sizeBytes: Long,
    val isSystemApp: Boolean,
    val isHasFullStats: Boolean = false
)

object AppStorageManager {

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps?.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps?.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getInstalledApps(context: Context): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val hasStatsPermission = hasUsageStatsPermission(context)
        val storageStatsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        } else null

        val installedApps = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) {
            emptyList()
        }

        val result = mutableListOf<InstalledAppItem>()

        for (appInfo in installedApps) {
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            val appName = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                appInfo.packageName
            }

            var sizeBytes = 0L
            var isFullStats = false

            if (hasStatsPermission && storageStatsManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val stats = storageStatsManager.queryStatsForPackage(
                        appInfo.storageUuid,
                        appInfo.packageName,
                        Process.myUserHandle()
                    )
                    sizeBytes = stats.appBytes + stats.dataBytes + stats.cacheBytes
                    isFullStats = true
                } catch (_: Exception) {
                    // Fallback to APK file size
                }
            }

            if (sizeBytes <= 0L) {
                sizeBytes = getApkCodeSize(appInfo)
            }

            val icon = try {
                pm.getApplicationIcon(appInfo)
            } catch (_: Exception) {
                null
            }

            result.add(
                InstalledAppItem(
                    packageName = appInfo.packageName,
                    appName = appName,
                    icon = icon,
                    sizeBytes = sizeBytes,
                    isSystemApp = isSystem && !isUpdatedSystem,
                    isHasFullStats = isFullStats
                )
            )
        }

        // Sort descending by size (largest first, matching user photo)
        result.sortedByDescending { it.sizeBytes }
    }

    private fun getApkCodeSize(appInfo: ApplicationInfo): Long {
        return try {
            var size = 0L
            val sourceDir = appInfo.publicSourceDir ?: appInfo.sourceDir
            if (!sourceDir.isNullOrEmpty()) {
                val f = File(sourceDir)
                if (f.exists()) size += f.length()
            }
            appInfo.splitPublicSourceDirs?.forEach { split ->
                val f = File(split)
                if (f.exists()) size += f.length()
            }
            size
        } catch (_: Exception) {
            0L
        }
    }

    fun openAppDetailsSettings(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openSystemAppsSettings(context)
        }
    }

    fun openSystemAppsSettings(context: Context) {
        val intents = listOf(
            Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_SETTINGS),
            Intent().setClassName("com.samsung.android.sm", "com.samsung.android.sm.storage.ui.StorageActivity"),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Try next
            }
        }
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openSystemAppsSettings(context)
        }
    }
}
