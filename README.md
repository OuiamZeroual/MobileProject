# PairPlay 🎮

Application Android de **mini-jeux Pair-à-Pair via Bluetooth**, construite avec Kotlin, Jetpack Compose, Material 3, Clean Architecture + MVVM.

---

## ✨ Fonctionnalités

- **Mode solo** et **multijoueur Bluetooth** (2 joueurs, RFCOMM)
- Découverte automatique des appareils environnants
- Synchronisation de partie via protocole JSON (`INVITE / ACCEPT / START_GAME / SCORE / FINAL_RESULT`)
- **6 mini-jeux**, 3 catégories :
  - 📡 **Capteurs** — *Main Ferme* (accéléromètre), *Coup de Vent* (micro)
  - ✋ **Gestes** — *Swipe Express* (swipe directionnel), *Trace Parfaite* (drag)
  - ❓ **Q/R** — *Compte-Points* (estimation), *Quiz Éclair* (QCM)
- Sélection aléatoire de **3 jeux par partie** avec **garantie d'1 jeu par catégorie**
- Échange des scores en fin de manche, détermination du gagnant, sons victoire/défaite
- Historique des scores (Room)

---

## 🏗️ Architecture

**Clean Architecture + MVVM** strictement séparés :

```
presentation/  (Compose, ViewModels)
    ↓ UI state
domain/        (modèles purs, interfaces — AUCUNE dépendance Android)
    ↑ implémenté par
data/ core/    (Bluetooth RFCOMM, Room, GameEngine, MiniGameRegistry)
games/         (plugins — 1 package par jeu)
```

### Point d'extension unique : la classe `MiniGame`

Ajouter un 7ᵉ jeu est trivial :

1. Créer `games/monjeu/MonJeuGame.kt` qui implémente `MiniGame` (`id`, `displayName`, `category`, `durationMs`, `@Composable Content`).
2. Enregistrer la factory dans `di/AppModule.kt` :

```kotlin
register { MonJeuGame() }
```

Zéro modification du `GameEngine`, des écrans, ou du protocole Bluetooth. **Open/Closed Principle respecté**.

### Flux d'une partie multijoueur

```
Host                                          Client
 │    startServer() ──────RFCOMM listen──────►
 │    ◄───── connect() (UUID PairPlay) ─────── startDiscovery()
 │    INVITE(hostId) ───────────────────────►
 │    ◄─────────────────────── ACCEPT(clientId)
 │    [pickThree(seed) → 3 games/3 catégories]
 │    START_GAME(seed, gameIds) ─────────────►
 │                                               [lance même jeu, même seed]
 │    [joue + finit] onLocalGameFinished()
 │    SCORE(gameId, score) ──────────────────►
 │    ◄────────────────── SCORE(gameId, score)
 │    waitAllScoresFor(gameId) → jeu suivant
 │    ...
 │    (fin) FINAL_RESULT(totals, winnerId) ──►
 │    playVictory/Defeat                         playVictory/Defeat
```

---

## ⚙️ Stack technique

| Couche | Techno |
|---|---|
| UI | Jetpack Compose, Material 3, Navigation Compose |
| DI | Hilt |
| Async | Coroutines + StateFlow |
| Sérialisation | kotlinx.serialization (polymorphe, sealed interface) |
| Persistance | Room (historique) + DataStore |
| Bluetooth | RFCOMM (Bluetooth Classic, UUID fixe) |
| Permissions | Accompanist |

---

## 📦 Structure des packages

```
com.pairplay
├── core/
│   ├── engine/         GameEngine, state machine
│   ├── bluetooth/      BluetoothGatewayImpl (RFCOMM)
│   ├── protocol/       Msg (sealed), ProtocolCodec
│   └── sound/          SoundManager (victoire/défaite)
├── data/
│   ├── game/           MiniGameRegistryImpl (plugin registry)
│   ├── local/          Room (ScoreDao, ScoreDatabase)
│   └── repository/     ScoreRepositoryImpl
├── domain/
│   ├── bluetooth/      BluetoothGateway interface
│   ├── game/           MiniGame contract, MiniGameRegistry
│   ├── models/         Player, GamePhase, Category, SessionResult…
│   └── repository/     ScoreRepository
├── games/              <-- 1 package par mini-jeu (plugin)
│   ├── stability/
│   ├── blow/
│   ├── swipe/
│   ├── drag/
│   ├── estimation/
│   └── quiz/
├── presentation/
│   ├── ui/
│   │   ├── theme/      PairPlayTheme (Material 3)
│   │   ├── components/ GradientButton…
│   │   ├── home/       HomeScreen
│   │   ├── lobby/      LobbyScreen
│   │   ├── game/       GameHostScreen
│   │   └── result/     ResultScreen (victoire/défaite/égalité)
│   └── viewmodel/      PairPlayViewModel
├── di/                 AppModule (Hilt)
├── MainActivity.kt
└── PairPlayApp.kt      @HiltAndroidApp
```

---

## 🚀 Build & run

Pré-requis : **Android Studio Ladybug+** (AGP 8.7), **JDK 17**, **2 smartphones Android 8+**.

```bash
./gradlew assembleDebug
./gradlew installDebug      # installe sur le device connecté
```

⚠️ **Le multijoueur Bluetooth exige 2 appareils physiques** (les émulateurs n'émulent pas le Bluetooth Classic). En émulateur, utilisez le mode Solo.

### Permissions demandées au runtime

- `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` (API 31+)
- `ACCESS_FINE_LOCATION` (API ≤ 30, requise pour la découverte Classic)
- `RECORD_AUDIO` (pour le mini-jeu *Coup de Vent*)

### Appairage

Les deux téléphones doivent être **appairés via les paramètres Android** au moins une fois (ou le système propose l'appairage à la première connexion RFCOMM). L'UUID de service utilisé est :

```
b7c9e9b2-4e4e-4c6a-9f2d-7a1b2c3d4e5f
```

---

## 🎯 Extensibilité — Ajouter un mini-jeu en 3 étapes

```kotlin
// 1) games/monjeu/MonJeuGame.kt
class MonJeuGame : MiniGame {
    override val id = "monjeu"
    override val displayName = "Mon Jeu"
    override val description = "Appuie vite !"
    override val category = Category.GESTURE
    override val durationMs = 10_000L
    @Composable
    override fun Content(seed: Long, onFinish: (Int) -> Unit) { /* ... */ }
}

// 2) di/AppModule.kt — provideRegistry()
register { MonJeuGame() }

// 3) Le GameEngine le prendra automatiquement dans sa sélection aléatoire.
```

---

## 🗺️ Roadmap (bonus implémentés / à faire)

- [x] Mode solo
- [x] Mode multijoueur 2 joueurs (RFCOMM)
- [x] Sauvegarde des scores (Room)
- [x] Sons victoire/défaite (ToneGenerator, pas d'asset externe)
- [x] UI Material 3 moderne avec dégradés et animations
- [ ] Mode tournoi (bracket multi-parties)
- [ ] Multijoueur > 2 joueurs (passerait sur une topologie star via l'hôte)
- [ ] Mode entraînement par catégorie
- [ ] Reconnexion automatique après coupure

---

## 🧪 Tests

La couche **domain** est pure Kotlin, aucune dépendance Android : idéale pour du test unitaire. Le `MiniGameRegistry` et le `GameEngine` peuvent être testés avec un `FakeBluetoothGateway` qui implémente la même interface.

---

## 📜 Licence

MIT — libre d'usage pédagogique.
