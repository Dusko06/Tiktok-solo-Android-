package com.dusko.tiktokssolo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.work.*
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TikTokSoloApp() }
    }
}

@Composable
private fun TikTokSoloApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tiktok_solo", Context.MODE_PRIVATE) }
    var topic by remember { mutableStateOf(prefs.getString("topic", "") ?: "") }
    var duration by remember { mutableStateOf(prefs.getString("duration", "60") ?: "60") }
    var status by remember { mutableStateOf("Prêt") }
    var lastFile by remember { mutableStateOf<File?>(null) }
    val selectedMedia = remember { mutableStateListOf<Uri>().apply { addAll(loadUris(prefs)) } }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) {
            status = "Aucun média sélectionné"
        } else {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) { }
            }
            selectedMedia.clear()
            selectedMedia.addAll(uris)
            prefs.edit().putString("media_uris", uris.joinToString("\n") { it.toString() }).apply()
            status = "${uris.size} média(s) enregistré(s)"
        }
    }

    MaterialTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("TikTok SOLO") }) }) { padding ->
            LazyColumn(
                Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Générateur vidéo automatisé", style = MaterialTheme.typography.headlineSmall)
                    Text("Galerie Samsung → montage → MP4 → partage TikTok")
                }
                item {
                    Button(
                        onClick = { galleryLauncher.launch(arrayOf("image/*", "video/*")) },
                        Modifier.fillMaxWidth()
                    ) { Text("Choisir depuis la Galerie Samsung") }
                }
                item { Text("Médias enregistrés : ${selectedMedia.size}") }
                item {
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it; prefs.edit().putString("topic", it).apply() },
                        label = { Text("Sujet") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = {
                            duration = it.filter(Char::isDigit)
                            prefs.edit().putString("duration", duration).apply()
                        },
                        label = { Text("Durée cible (secondes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Button(
                        enabled = selectedMedia.isNotEmpty(),
                        onClick = {
                            status = "Montage en cours…"
                            MediaMontage(context).export(
                                selectedMedia.toList(),
                                duration.toIntOrNull()?.coerceIn(15, 180) ?: 60
                            ) { result ->
                                result.onSuccess { file ->
                                    lastFile = file
                                    status = "Vidéo créée : ${file.name}"
                                }.onFailure { error ->
                                    status = "Erreur : ${error.message ?: "export impossible"}"
                                }
                            }
                        },
                        Modifier.fillMaxWidth()
                    ) { Text("Créer la vidéo MP4") }
                }
                item {
                    Button(
                        enabled = lastFile?.exists() == true,
                        onClick = { lastFile?.let { shareToTikTok(context, it) } },
                        Modifier.fillMaxWidth()
                    ) { Text("Partager vers TikTok") }
                }
                item {
                    OutlinedButton(
                        enabled = selectedMedia.isNotEmpty(),
                        onClick = {
                            scheduleDaily(context)
                            status = "3 créations automatiques/jour programmées"
                        },
                        Modifier.fillMaxWidth()
                    ) { Text("Activer l'automatisation 3×/jour") }
                }
                item { Text("État : $status") }
            }
        }
    }
}

private fun loadUris(prefs: android.content.SharedPreferences): List<Uri> =
    prefs.getString("media_uris", "")!!
        .split("\n")
        .filter { it.isNotBlank() }
        .map(Uri::parse)

private fun shareToTikTok(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val packages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
    val pm = context.packageManager
    val target = packages.firstOrNull { pkg ->
        try { pm.getPackageInfo(pkg, 0); true } catch (_: Exception) { false }
    }
    if (target != null) intent.setPackage(target)
    context.startActivity(Intent.createChooser(intent, "Partager la vidéo"))
}

private fun scheduleDaily(context: Context) {
    val workManager = WorkManager.getInstance(context)
    listOf(7L, 15L, 23L).forEachIndexed { index, hour ->
        val request = OneTimeWorkRequestBuilder<DailyVideoWorker>()
            .setInputData(workDataOf("slot" to index, "hour" to hour))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(DailyVideoWorker.delayUntilHour(hour), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            "daily_tiktok_solo_$index",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
