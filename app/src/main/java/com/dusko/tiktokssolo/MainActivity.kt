package com.dusko.tiktokssolo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*
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
    var topic by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("60") }
    var status by remember { mutableStateOf("Prêt") }
    val jobs = remember { mutableStateListOf<String>() }
    val selectedMedia = remember { mutableStateListOf<Uri>() }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        selectedMedia.clear(); selectedMedia.addAll(uris)
        status = if (uris.isEmpty()) "Aucun média sélectionné" else "${uris.size} média(s) sélectionné(s)"
    }

    MaterialTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("TikTok SOLO") }) }) { padding ->
            LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Générateur vidéo automatisé", style = MaterialTheme.typography.headlineSmall)
                    Text("Galerie Samsung → montage → MP4")
                }
                item {
                    Button(onClick = { galleryLauncher.launch(arrayOf("image/*", "video/*")) }, Modifier.fillMaxWidth()) {
                        Text("Choisir depuis la Galerie Samsung")
                    }
                }
                item { Text("Médias sélectionnés : ${selectedMedia.size}") }
                item { OutlinedTextField(topic, { topic = it }, label = { Text("Sujet") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(duration, { duration = it.filter(Char::isDigit) }, label = { Text("Durée cible (secondes)") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Button(
                        enabled = selectedMedia.isNotEmpty(),
                        onClick = {
                            status = "Montage en cours…"
                            MediaMontage(context).export(selectedMedia.toList(), duration.toIntOrNull() ?: 60) { result ->
                                result.onSuccess { file ->
                                    jobs.add("${topic.ifBlank { "Sans sujet" }} — ${file.name}")
                                    status = "Vidéo créée : ${file.name}"
                                }.onFailure { error -> status = "Erreur : ${error.message ?: "export impossible"}" }
                            }
                        },
                        Modifier.fillMaxWidth()
                    ) { Text("Créer la vidéo MP4") }
                }
                item {
                    OutlinedButton(onClick = { scheduleDaily(context); status = "Automatisation quotidienne activée" }, Modifier.fillMaxWidth()) {
                        Text("Automatiser chaque jour")
                    }
                }
                item { Text("État : $status") }
                item { Text("Vidéos créées", style = MaterialTheme.typography.titleMedium) }
                items(jobs) { Text("• $it") }
            }
        }
    }
}

private fun scheduleDaily(context: android.content.Context) {
    val request = PeriodicWorkRequestBuilder<DailyVideoWorker>(24, TimeUnit.HOURS).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork("daily_tiktok_solo", ExistingPeriodicWorkPolicy.UPDATE, request)
}
