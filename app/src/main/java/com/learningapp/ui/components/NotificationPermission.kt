package com.learningapp.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.learningapp.reminder.ReminderScheduler

/**
 * Returns a function that makes sure the app may post notifications, then calls [onResult]
 * with true/false. Before Android 13 (or when already allowed) it answers true at once.
 */
@Composable
fun rememberNotificationPermission(onResult: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { callback(it) }
    return {
        if (ReminderScheduler.canNotify(context) || Build.VERSION.SDK_INT < 33) callback(true)
        else launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
