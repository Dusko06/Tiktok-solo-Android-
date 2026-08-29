package com.dusko.tiktokssolo

data class VideoProject(
    val topic: String,
    val durationSeconds: Int = 60,
    val format: String = "1080x1920",
    val hook: String,
    val script: String,
    val description: String,
    val hashtags: List<String>
)

object VideoPlanner {
    fun create(topic: String, durationSeconds: Int): VideoProject {
        val clean = topic.trim()
        val hook = "Tu savais ça sur $clean ? Voici ce qu'il faut vraiment savoir."
        val script = "Aujourd'hui, on parle de $clean. $hook " +
            "On va aller à l'essentiel, avec les informations importantes et les points qui peuvent faire débat. " +
            "Regarde jusqu'à la fin et dis-moi en commentaire ce que tu en penses."
        return VideoProject(
            topic = clean,
            durationSeconds = durationSeconds.coerceAtLeast(60),
            hook = hook,
            script = script,
            description = "$clean : les points essentiels à connaître. Donne ton avis en commentaire.",
            hashtags = listOf("#actualite", "#france", "#information", "#tiktokfr", "#pourtoi")
        )
    }
}
