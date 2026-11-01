package com.coworker.jjikmuk.feature.auth.presentation.permission

import android.Manifest
import android.content.Context
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

/** The introduction is shown once. Feature-specific runtime permission checks remain authoritative. */
object PermissionOnboarding {
    private fun prefs(context: Context) = context.getSharedPreferences("permission-onboarding", Context.MODE_PRIVATE)
    fun shouldShow(context: Context): Boolean = !prefs(context).getBoolean("completed", false) &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
    fun complete(context: Context) { prefs(context).edit().putBoolean("completed", true).apply() }
}
