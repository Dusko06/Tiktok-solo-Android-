# TikTok SOLO Android

Application Android pour automatiser la création de vidéos TikTok verticales 9:16.

## Objectif
- Générer des scripts et hooks.
- Préparer voix off et sous-titres.
- Assembler les médias en vidéo MP4 1080x1920.
- Contrôler automatiquement la durée et le format.
- Préparer les métadonnées TikTok.
- Préparer ensuite l'intégration de publication via les API officielles TikTok.

## Architecture prévue
- Android / Kotlin
- Jetpack Compose
- WorkManager pour les tâches planifiées
- Media3/MediaCodec pour le traitement vidéo local
- Stockage local pour les projets et exports

## Sécurité
Aucune clé API ou secret ne doit être ajouté au dépôt. Les secrets seront configurés localement ou via un gestionnaire de secrets.
