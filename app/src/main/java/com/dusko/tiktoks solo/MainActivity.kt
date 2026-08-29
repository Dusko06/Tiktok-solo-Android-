package com.dusko.tiktokssolo

import android.os.Bundle
import androidx.activity.ComponentActivity
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

    MaterialTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("TikTok SOLO") }) }) { padding ->
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Générateur vidéo automatisé", style = MaterialTheme.typography.headlineSmall)
                    Text("Crée une vidéo verticale 9:16 à partir d'un sujet.")
                }
                item { OutlinedTextField(topic, { topic = it }, label = { Text("Sujet") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(duration, { duration = it.filter(Char::isDigit) }, label = { Text("Durée cible (secondes)") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Button(enabled = topic.isNotBlank(), onClick = {
                        jobs.add("${topic.trim()} — ${duration.ifBlank { "60" }} s")
                        status = "Projet ajouté"
                    }, modifier = Modifier.fillMaxWidth()) { Text("Créer la vidéo") }
                }
                item {
                    OutlinedButton(onClick = {
                        scheduleDaily(context)
                        status = "Automatisation quotidienne activée"
                    }, modifier = Modifier.fillMaxWidth()) { Text("Automatiser chaque jour") }
                }
                item { Text("État : $status") }
                item { Text("Projets", style = MaterialTheme.typography.titleMedium) }
                items(jobs) { Text("• $it") }
            }
        }
    }
}

private fun scheduleDaily(context: android.content.Context) {
    val request = PeriodicWorkRequestBuilder<DailyVideoWorker>(24, TimeUnit.HOURS).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork("daily_tiktok_solo", ExistingPeriodicWorkPolicy.UPDATE, request)
}
