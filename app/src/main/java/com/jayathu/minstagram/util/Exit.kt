package com.jayathu.minstagram.util

import android.content.Context
import android.content.Intent
import com.jayathu.minstagram.data.Prefs

data class LaunchableApp(val packageName: String, val label: String)

// Where the "better things to do" exit sends you: the app the user chose in
// settings, or the home screen if they haven't picked one (or it's since gone).
// Kept in one place so the quiz overlay and the session sheet leave the same way.
fun leaveToChosenApp(context: Context) {
    val chosen = Prefs.get(context).getString(Prefs.EXIT_TARGET_PACKAGE, null)
    val appIntent = chosen
        ?.let { context.packageManager.getLaunchIntentForPackage(it) }
        ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    val intent = appIntent ?: homeIntent()
    context.startActivity(intent)
}

private fun homeIntent(): Intent =
    Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

// Human-readable name of the current exit target, for the settings row.
fun exitTargetLabel(context: Context, packageName: String?): String {
    if (packageName == null) return "Home screen"
    val pm = context.packageManager
    return runCatching {
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault("Home screen")
}

// Installed apps that show up in the launcher, minus our own, sorted by name.
// Needs the launcher <intent> in the manifest <queries> to be visible on
// Android 11 and up.
fun installedLaunchableApps(context: Context): List<LaunchableApp> {
    val pm = context.packageManager
    val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(query, 0)
        .mapNotNull { resolved ->
            val pkg = resolved.activityInfo.packageName
            if (pkg == context.packageName) null
            else LaunchableApp(pkg, resolved.loadLabel(pm).toString())
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}
