# Fine Volume Tuner — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refaire l'app Android « Volume Control Helper / Fine-Tune Volume » : régler le volume plus finement que les paliers Android — plus doux que le minimum, plus fort que le maximum, avec un réglage par palier individuel et par type de stream.

**Architecture:** App Android native Kotlin (minSdk 24 / targetSdk 35), zéro permission runtime bloquante, zéro réseau. Un `LoudnessEnhancer` attaché à l'audio session 0 (output mix global) applique un gain en millibels sur TOUT l'audio du device. Un foreground service specialUse maintient l'effet et observe `VOLUME_CHANGED_ACTION` : à chaque changement de volume, l'app mappe (stream actif, palier courant) → gain configuré et l'applique. UI Compose Material 3 claire : slider fin par stream, mode « Par palier », réglages (auto-start au boot, reset). Logique métier en classes JVM pures testées par JUnit.

**Tech Stack:** Kotlin 2.0.21, AGP 8.7.3, Gradle 8.9, Jetpack Compose BOM 2024.12.01 + Material3, kotlinx-serialization-json 1.7.3, JUnit 4.

**Spec:** Fiche Play Store com.xupstudio.volumefinetuner + description Uptodown v5.1.3 (fine-tune par palier, par catégorie, plus doux/plus fort que les paliers stock).

## Global Constraints

- applicationId: `app.rilcy.finevolumetuner`. minSdk 24, targetSdk 35, compileSdk 35, JDK 17.
- Permissions manifest EXACTES (rien d'autre) : `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS` (runtime, refusable sans casser), `RECEIVE_BOOT_COMPLETED`.
- Pas d'ads, pas de tracking, AUCUNE permission internet. Dépendances autorisées : androidx + kotlin + kotlinx-serialization + junit uniquement.
- Thème CLAIR Material3 (dynamic light sur Android 12+). Strings FR (default) + values-en.
- L'effet global passe par `LoudnessEnhancer(0)` : gain ∈ [-3000, +1500] mB, appliqué à tout l'audio.
- Toute logique métier = classes JVM pures (`gainmath/`) testées via `./gradlew :app:testDebugUnitTest`. Livrable : `./gradlew assembleDebug assembleRelease` verts.
- Build env : `JAVA_HOME=~/.hermes/opt/jdk17`, `ANDROID_HOME=~/.hermes/android-sdk`, wrapper `./gradlew`.
- Git identity : user.name `Tourniercy`. Ne JAMAIS committer keystore/keystore.properties/local.properties.
- Le scaffold (Task 0) est déjà posé et compile : MainActivity minimal + thème + manifest + buildfiles. Ne pas re-générer, compléter.

---

### Task 0: Scaffold (DÉJÀ FAIT par le contrôleur — ne pas refaire)

`settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `AndroidManifest.xml` (activity only), `MainActivity.kt` (placeholder Compose), `ui/theme/Theme.kt`, `res/values/{strings,themes}.xml`, `res/values-en/strings.xml`, `res/drawable/ic_launcher.xml`, `.gitignore`, `local.properties`. Vérif : `./gradlew assembleDebug` vert.

---

### Task 1: GainMath — logique pure de mapping (TDD)

**Files:**
- Create: `app/src/main/java/app/rilcy/finevolumetuner/gainmath/GainMath.kt`
- Create: `app/src/main/java/app/rilcy/finevolumetuner/gainmath/Model.kt`
- Test: `app/src/test/java/app/rilcy/finevolumetuner/gainmath/GainMathTest.kt`

**Interfaces (exactes — les tâches 2/3/4 s'y raccrochent):**
- `enum class AudioStreamLabel(val streamType: Int, val labelFr: String, val labelEn: String)` — valeurs : `MUSIC(3, "Musique", "Music")`, `RING(2, "Sonnerie", "Ringtone")`, `ALARM(4, "Alarme", "Alarm")`, `NOTIFICATION(5, "Notifications", "Notifications")`, `VOICE_CALL(0, "Appel", "Call")`, `SYSTEM(1, "Système", "System")`
- `enum class TunerMode { NORMAL, PER_STEP }`
- `@Serializable data class StreamConfig(val offsetMb: Int = 0, val perStepMb: List<Int> = emptyList())`
- `@Serializable data class TunerConfig(val mode: TunerMode = TunerMode.NORMAL, val autostart: Boolean = false, val wasRunning: Boolean = false, val streams: Map<AudioStreamLabel, StreamConfig> = AudioStreamLabel.entries.associateWith { StreamConfig() })`
- `object GainMath` : `MIN_MB = -3000`, `MAX_MB = 1500`, `fun clampMb(v: Int): Int`, `fun percentToMb(p: Int): Int` (linéaire : `-3000 + p*45`, clampé), `fun mbToPercent(mb: Int): Int` (`(mb + 3000) / 45`, clampé [0,100]), `fun gainForStep(cfg: StreamConfig, step: Int): Int` (PER_STEP : `perStepMb.getOrNull(step) ?: offsetMb`, clampé)
- `object GainProfiles` : `val DEFAULT: TunerConfig`, `fun toJson(cfg: TunerConfig): String`, `fun fromJson(s: String): TunerConfig` (toute erreur → DEFAULT)

- [ ] **Step 1: tests failing** — écrire `GainMathTest.kt` :

```kotlin
package app.rilcy.finevolumetuner.gainmath

import org.junit.Assert.assertEquals
import org.junit.Test

class GainMathTest {
    @Test fun clamp() {
        assertEquals(-3000, GainMath.clampMb(-99999)); assertEquals(1500, GainMath.clampMb(99999)); assertEquals(750, GainMath.clampMb(750))
    }
    @Test fun percentRoundTrip() {
        assertEquals(-3000, GainMath.percentToMb(0)); assertEquals(1500, GainMath.percentToMb(100)); assertEquals(0, GainMath.percentToMb(66)) // -3000+66*45=-30→ wait: use exact: percentToMb(66) = -3000+2970 = -30
    }
}
```
(Le test exact attend `percentToMb(66) == -30` et `mbToPercent(0) == 66` — copier ces assertions telles quelles.)
Couvrir aussi : `gainForStep` hors liste → offsetMb ; `gainForStep` clampé ; `GainProfiles.toJson/fromJson` round-trip égal ; `fromJson("{garbage")` → DEFAULT ; `fromJson("{}")` → DEFAULT.

- [ ] **Step 2:** `./gradlew :app:testDebugUnitTest` → FAIL (unresolved reference)
- [ ] **Step 3:** implémenter `Model.kt` (types + @Serializable) et `GainMath.kt`. Pour la sérialization de la map clé-enum : transformer en `Map<String, StreamConfig>` via `associateBy { it.name }` dans toJson, reverse dans fromJson.
- [ ] **Step 4:** tests PASS → commit `feat: gainmath pure logic + tests`

### Task 2: AudioEngine + TunerService (foreground specialUse) + VolumeObserver

**Files:**
- Create: `app/src/main/java/app/rilcy/finevolumetuner/audio/EffectHandle.kt` — `interface EffectHandle { @Throws(Exception::class) fun setGainMb(mb: Int); fun release() }` + `class LoudnessEnhancerHandle : EffectHandle` (wrap `android.media.audiofx.LoudnessEnhancer(0)`, `setTargetGain`, `enabled = true`)
- Create: `app/src/main/java/app/rilcy/finevolumetuner/audio/AudioEngine.kt` — `class AudioEngine(private val handleFactory: () -> EffectHandle)` : `fun start(): Boolean`, `fun stop()`, `fun applyGainMb(mb: Int)`, `val running: Boolean`, `val lastError: String?` (jamais de crash : toute Exception → lastError + stop interne)
- Create: `app/src/main/java/app/rilcy/finevolumetuner/audio/VolumeObserver.kt` — `class VolumeObserver(context, onVolumeChanged: (streamType: Int, newVolume: Int) -> Unit)` ; BroadcastReceiver dynamique sur `android.media.VOLUME_CHANGED_ACTION`, extras `android.media.EXTRA_VOLUME_STREAM_TYPE` (Int, défaut 3) et `android.media.EXTRA_VOLUME_STREAM_VALUE` (Int, défaut -1) ; `fun register()`, `fun unregister()` ; static `fun parseExtras(extras: Bundle?): Pair<Int,Int>` — TESTABLE JVM (retourne (3,-1) si extras null).
- Create: `app/src/main/java/app/rilcy/finevolumetuner/service/TunerService.kt` — foreground `specialUse` ; static `fun start(context)`, `fun stop(context)` ; onStartCommand → startForeground (channel "fine_volume_tuner", importance LOW ; type `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` si SDK≥33) ; possède AudioEngine + VolumeObserver ; à chaque volumeChanged : résout le stream via `AudioStreamLabel.entries.firstOrNull { it.streamType == streamType }` (inconnu → ignorer), calcule le gain (mode NORMAL → offsetMb du stream ; PER_STEP → `GainMath.gainForStep(cfg, newVolume)`) et l'applique. `PROPERTY_SPECIAL_USE_FGS_SUBTYPE = "fine_audio_gain_adjustment"` via `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="fine_audio_gain_adjustment"/>` dans le manifest.
- Modify: `AndroidManifest.xml` — ajouter `<service android:name=".service.TunerService" android:exported="false" android:foregroundServiceType="specialUse">` + le `<property>` ci-dessus.

**Interfaces produites (consommées par Task 3/4) :** `TunerService.start(context)` / `stop(context)`, `AudioEngine.applyGainMb`, `VolumeObserver.parseExtras`.

- [ ] **Step 1: tests JVM failing** — `AudioEngineTest` (fake EffectHandle : start→running, double start ok idempotent, applyGain clampé [-3000,1500], exception du handle → running=false + lastError non null, stop→released) ; `VolumeObserverTest` (parseExtras null → (3,-1), extras valides → valeurs lues).
- [ ] **Step 2:** run → FAIL
- [ ] **Step 3:** implémenter les 4 fichiers + manifest.
- [ ] **Step 4:** `./gradlew :app:testDebugUnitTest :app:assembleDebug` → PASS/BUILD OK
- [ ] **Step 5:** commit `feat: audio engine + foreground special-use service + volume observer`

### Task 3: ViewModel + UI Compose

**Files:**
- Create: `app/src/main/java/app/rilcy/finevolumetuner/data/PrefsRepository.kt` — `interface ConfigRepository { fun load(): TunerConfig; fun save(cfg: TunerConfig) ; val autostart get/set ; val wasRunning get/set }` + `class PrefsRepository(context)` (SharedPreferences "fine_volume_tuner", JSON via GainProfiles) + `class InMemoryConfigRepository` (pour tests/UI preview).
- Create: `app/src/main/java/app/rilcy/finevolumetuner/vm/TunerViewModel.kt` — `class TunerViewModel(private val repo: ConfigRepository, private val audio: AudioManagerWrapper)` : `val uiState: StateFlow<TunerUiState>` ; `data class TunerUiState(val config: TunerConfig, val engineOn: Boolean, val activeStream: AudioStreamLabel = MUSIC, val maxSteps: Map<AudioStreamLabel, Int> = emptyMap())` ; actions : `toggleEngine()`, `setMode(TunerMode)`, `setStreamOffset(stream, percent)`, `setStepGain(stream, step, percent)`, `reset()`, `selectStream(stream)`. Persist à chaque mutation ; toggleEngine démarre/arrête `TunerService` via `interface ServiceControl { fun start(); fun stop() }` injecté + `wasRunning`.
- Create: `app/src/main/java/app/rilcy/finevolumetuner/audio/AudioManagerWrapper.kt` — `class AudioManagerWrapper(context)` : `fun maxVolume(streamType: Int): Int` (AudioManager.getStreamMaxVolume, fallback 15 si ≤0).
- Create: `app/src/main/java/app/rilcy/finevolumetuner/ui/MainScreen.kt` — Scaffold Material3 clair. Header : titre + Switch moteur. Segment mode : NORMAL (« Ajustement global ») / PER_STEP (« Par palier »). Liste des 6 streams : Card par stream (icône Material, nom localisé via labelFr/labelEn selon Locale.getDefault().language), Slider valueRange 0f..100f pour l'offset (affiche % + mB). En PER_STEP sur le stream sélectionné : LazyColumn des paliers 0..maxSteps-1, chacun un mini-slider 0..100 + valeur. BottomBar : bouton Réinitialiser. Les textes UI depuis strings.xml (FR default, EN).
- Modify: `MainActivity.kt` — `setContent { FineVolumeTunerTheme { MainScreen(vm) } }` + ViewModel wiring (`viewModel(factory=…)`) ; demande `POST_NOTIFICATIONS` au premier toggle moteur (SDK≥33) sans bloquer.

- [ ] **Step 1: tests failing** — `TunerViewModelTest` (fake repo + fake ServiceControl + fake AudioManagerWrapper via interface) : setStreamOffset persiste + UiState à jour ; setStepGain en PER_STEP modifie perStepMb[step] ; toggleEngine ON → serviceControl.start() + wasRunning=true ; reset → DEFAULT.
- [ ] **Step 2:** FAIL → **Step 3:** impl (UI sans test instrumenté) → **Step 4:** tests + assembleDebug verts → **Step 5:** commit `feat: compose ui + viewmodel`

### Task 4: BootReceiver + persistance wasRunning

**Files:**
- Create: `app/src/main/java/app/rilcy/finevolumetuner/service/BootReceiver.kt` — `onReceive` : si `intent.action == Intent.ACTION_BOOT_COMPLETED` (ou QUICKBOOT_POWERON) && `PrefsRepository.load().autostart && wasRunning` → `TunerService.start(context)`. Guard `goAsync()` inutile, simple startForegroundService.
- Modify: `AndroidManifest.xml` — `<receiver android:name=".service.BootReceiver" android:exported="true" android:permission="android.permission.RECEIVE_BOOT_COMPLETED"><intent-filter><action android:name="android.intent.action.BOOT_COMPLETED"/><action android:name="android.intent.action.QUICKBOOT_POWERON"/></intent-filter></receiver>` ; ajouter toggle « Démarrage auto » dans SettingsSheet/MainScreen (branche sur `config.autostart`).
- Modify: `TunerViewModel` — action `setAutostart(Boolean)`.

- [ ] **Step 1: test failing** — `BootReceiverTest` (Robolectric interdit → logique extraite : `object BootLogic { fun shouldStart(cfg: TunerConfig, action: String?): Boolean }` testé pur ; receiver = 3 lignes qui délèguent).
- [ ] **Step 2:** FAIL → **Step 3:** impl → **Step 4:** tests + assembleDebug → **Step 5:** commit `feat: boot receiver + autostart setting`

### Task 5: Signature release + badging

- [ ] Créer keystore release : `mkdir -p ~/.hermes/opt/keystores && keytool -genkeypair -v -keystore ~/.hermes/opt/keystores/fvt-release.keystore -alias fvt -keyalg RSA -keysize 2048 -validity 10000 -storepass rilcy-fvt-2026 -dname "CN=Fine Volume Tuner, OU=Rilcy, O=Tourniercy, C=FR"` (mot de passe stocké aussi dans `~/.hermes/opt/keystores/fvt-release.txt` chmod 600).
- [ ] `app/keystore.properties.example` (référence) + loader dans `app/build.gradle.kts` : si `keystore.properties` existe → signingConfig release (storeFile, keyAlias=fvt, passwords). `.gitignore` l'exclut déjà.
- [ ] Copier le keystore → `/workspace/fine-volume-tuner/keystore.properties` + `keystores/fvt-release.keystore` gitignored (chemin absolu ~/.hermes — OK pour build local ; le CI GitHub construira debug uniquement).
- [ ] `./gradlew assembleDebug assembleRelease` → APK signés ; `aapt2 dump badging app/build/outputs/apk/release/app-release.apk | head -30` : vérifier package/label/permissions/minSdk=24/targetSdk=35/launchable.
- [ ] Commit `build: release signing + keystore properties loader` (SANS le keystore ni keystore.properties).

### Task 6: README + CI GitHub Actions

- [ ] `README.md` bilingue court : description, fonctionnalités, permissions expliquées, build (`./gradlew assembleDebug`), lien APK.
- [ ] `.github/workflows/ci.yml` : on push/PR → `actions/setup-java@v4` (temurin 17) + `android-actions/setup-android@v3` + `gradle/actions/setup-gradle@v4` → `./gradlew testDebugUnitTest assembleDebug` → upload artifact `app-debug.apk` (actions/upload-artifact@v4).
- [ ] Commit `ci: unit tests + debug apk artifact`.

### Task 7: Landing page + déploiement (contrôleur)

- [ ] `site/index.html` statique clair FR/EN + icône SVG, lien APK (GitHub Release latest) + lien repo.
- [ ] Coolify (MCP) : static app, domaine `finevolumetuner.rilcy.app`, DNS CF (MCP cloudflare) ; APK servi depuis le site.
- [ ] Vérif live : `curl -sI https://finevolumetuner.rilcy.app` → 200.
