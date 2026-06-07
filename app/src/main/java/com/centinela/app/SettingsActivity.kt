package com.centinela.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material3.Slider

fun getInstalledApps(context: Context): List<Pair<String, String>> {
    val pm = context.packageManager
    return pm.getInstalledApplications(PackageManager.GET_META_DATA)
        .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
        .filter { it.packageName != "com.centinela.app" }
        .map { Pair(it.packageName, pm.getApplicationLabel(it).toString()) }
        .sortedBy { it.second }
}

fun getBlockedApps(context: Context): Set<String> {
    val prefs = context.getSharedPreferences("centinela", Context.MODE_PRIVATE)
    return prefs.getStringSet("blocked_apps", setOf(
        "com.google.android.youtube",
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.twitter.android",
        "com.facebook.katana"
    )) ?: emptySet()
}

fun saveBlockedApps(context: Context, apps: Set<String>) {
    context.getSharedPreferences("centinela", Context.MODE_PRIVATE)
        .edit().putStringSet("blocked_apps", apps).apply()
}

fun getUsageThresholdMinutes(context: Context): Int {
    return context.getSharedPreferences("centinela", Context.MODE_PRIVATE)
        .getInt("usage_threshold_minutes", 20)
}

fun saveUsageThreshold(context: Context, minutes: Int) {
    context.getSharedPreferences("centinela", Context.MODE_PRIVATE)
        .edit().putInt("usage_threshold_minutes", minutes).apply()
}

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SettingsScreen(onDone = { finish() }) }
    }
}

@Composable
fun SettingsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val apps = remember { getInstalledApps(context) }
    var blockedApps by remember { mutableStateOf(getBlockedApps(context)) }
    var thresholdMinutes by remember { mutableStateOf(getUsageThresholdMinutes(context).toFloat()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080808))
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "CONFIGURACIÓN",
                color = Color(0xFF444444),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                "TIEMPO ANTES DEL BLOQUEO",
                color = Color(0xFF666666),
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${thresholdMinutes.toInt()} MINUTOS",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            Slider(
                value = thresholdMinutes,
                onValueChange = { thresholdMinutes = it },
                onValueChangeFinished = {
                    saveUsageThreshold(context, thresholdMinutes.toInt())
                },
                valueRange = 5f..60f,
                steps = 10,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = Color(0xFFCC0000),
                    activeTrackColor = Color(0xFFCC0000),
                    inactiveTrackColor = Color(0xFF222222)
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                "APPS VIGILADAS",
                color = Color(0xFF666666),
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(apps) { (packageName, appName) ->
            val isBlocked = packageName in blockedApps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isBlocked) Color(0xFF330000) else Color(0xFF1A1A1A))
                    .background(if (isBlocked) Color(0xFF110000) else Color(0xFF0D0D0D))
                    .clickable {
                        blockedApps = if (isBlocked) {
                            blockedApps - packageName
                        } else {
                            blockedApps + packageName
                        }
                        saveBlockedApps(context, blockedApps)
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    appName,
                    color = if (isBlocked) Color.White else Color(0xFF666666),
                    fontSize = 13.sp,
                    fontWeight = if (isBlocked) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    if (isBlocked) "✕" else "+",
                    color = if (isBlocked) Color(0xFFCC0000) else Color(0xFF333333),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = onDone,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFCC0000)
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    "GUARDAR Y SALIR",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                )
            }
        }
    }
}
