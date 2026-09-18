package com.elmallah.paymentbridge.ui.components

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean
)

object InstalledAppHelper {

    fun getInstalledApps(
        context: Context,
        showAll: Boolean = false,
        searchQuery: String = ""
    ): List<InstalledAppItem> {
        val pm = context.packageManager
        val apps = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) {
            emptyList()
        }

        val selfPackage = context.packageName

        val filtered = apps.filter { info ->
            if (info.packageName == selfPackage) return@filter false

            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isMessaging = info.packageName.contains("messaging", ignoreCase = true) ||
                info.packageName.contains("mms", ignoreCase = true) ||
                info.packageName.contains("sms", ignoreCase = true)

            // If showAll is false, filter out pure system services that aren't messaging or financial
            if (!showAll && isSystem && !isMessaging) {
                false
            } else {
                true
            }
        }.map { info ->
            val label = try {
                pm.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                info.packageName
            }
            InstalledAppItem(
                packageName = info.packageName,
                appName = label.ifBlank { info.packageName },
                isSystemApp = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        }

        val query = searchQuery.trim().lowercase()
        val searched = if (query.isNotBlank()) {
            filtered.filter {
                it.appName.lowercase().contains(query) ||
                it.packageName.lowercase().contains(query)
            }
        } else {
            filtered
        }

        return searched.sortedWith(compareBy<InstalledAppItem> {
            // Prioritize banking and wallet and messaging apps to the top
            val pkg = it.packageName.lowercase()
            val name = it.appName.lowercase()
            val isPriority = pkg.contains("vodafone") || pkg.contains("wallet") ||
                pkg.contains("nbe") || pkg.contains("misr") || pkg.contains("cib") ||
                pkg.contains("instapay") || pkg.contains("messaging") ||
                name.contains("فودافون") || name.contains("بنك") || name.contains("أهلي") ||
                name.contains("مصر") || name.contains("إنستاباي")
            if (isPriority) 0 else 1
        }.thenBy { it.appName.lowercase() })
    }

    fun getAppIconBitmap(context: Context, packageName: String): Bitmap? {
        return try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            drawableToBitmap(drawable)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}

@Composable
fun AppIconView(
    packageName: String?,
    modifier: Modifier = Modifier.size(40.dp)
) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        if (!packageName.isNullOrBlank()) {
            InstalledAppHelper.getAppIconBitmap(context, packageName)
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        } else {
            val pkg = packageName?.lowercase() ?: ""
            val icon = when {
                pkg.contains("messaging") || pkg.contains("sms") -> Icons.Default.Message
                pkg.contains("bank") || pkg.contains("nbe") || pkg.contains("misr") || pkg.contains("cib") -> Icons.Default.AccountBalance
                pkg.contains("wallet") || pkg.contains("cash") || pkg.contains("vodafone") -> Icons.Default.Payment
                else -> Icons.Default.Android
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
