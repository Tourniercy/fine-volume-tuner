# Fine Volume Tuner / Réglage Fin du Volume

Réglage fin du volume Android : **plus doux que le palier minimum, plus fort que le maximum, et palier par palier** — refaire librement l'idée de « Volume Control Helper / Fine-Tune Volume », sans pubs ni tracking.

Fine-grained Android volume control: **quieter than the minimum step, louder than the maximum, per-step tuning** — no ads, no tracking, no network permission.

## Comment ça marche / How it works

L'app attache un [`LoudnessEnhancer`](https://developer.android.com/reference/android/media/audiofx/LoudnessEnhancer) à la session audio 0 (mixage de sortie global). Le gain (−3000 à +1500 mB, soit −30 à +15 dB) s'applique à tout l'audio du device. Un service en avant-plan (type `specialUse`) maintient l'effet et observe les changements de volume système pour réappliquer le gain configuré — global ou par palier, par type de flux (musique, sonnerie, alarme, notifications, appel, système).

## Fonctionnalités

- 🎚️ Atténuation jusqu'à **−30 dB** sous le volume minimum (mode nuit, migraine)
- 🔊 Boost jusqu'à **+15 dB** au-delà du volume maximum
- 🪜 Mode **par palier** : un gain distinct pour chaque palier de volume
- 🎼 Réglage séparé par type de flux (6 streams)
- ♾️ Service persistant + redémarrage au boot (optionnel)
- 🔒 **Aucune permission internet**, aucune donnée collectée, 100% hors-ligne

## Build

```bash
./gradlew assembleDebug        # APK debug
./gradlew testDebugUnitTest    # tests unitaires
./gradlew assembleRelease      # APK release signé (voir ci-dessous)
```

Prérequis : JDK 17, Android SDK 35.

### Signature release

Copier `app/keystore.properties.example` vers `app/keystore.properties` (gitignoré) et renseigner le chemin du keystore + mots de passe. Sans ce fichier, la build release produit un APK non signé.

## Permissions

| Permission | Raison |
|---|---|
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | maintenir le réglage actif en arrière-plan |
| `POST_NOTIFICATIONS` | indicateur visuel du service (refusable) |
| `RECEIVE_BOOT_COMPLETED` | réactiver le réglage au démarrage (optionnel) |

## Licence

MIT © Tourniercy
