# Overview
An offline-first Android app that builds and queries a 5W1H event graph on-device to retrieve, rank, and explain similar events.

<br>

# Core Directory
This folder contains the **domain + data logic** for the app. It implements the 5W1H graph schema, similarity/search pipelines, configuration, and service wiring.

### Directory Structure
```
backend/
├─ analyser/
│  ├─ GraphAnalyser.kt           # high-level similarity/orchestration
│  ├─ GraphAnalyserHelper.kt     # scoring/aggregation helpers
│  └─ HelperClasses.kt           # small domain utilities
│
├─ config/
│  └─ SimilarityConfig.kt        # thresholds
│
├─ model/
│  └─ dto/                       # request/response DTOs used by use cases
│
├─ schema/
│  └─ GraphSchema.kt             # 5W1H schema, enums, labels, partitions
│
├─ services/
│  ├─ di/                        # Hilt bindings for repos/services
│  └─ kgraph/                    # graph-facing services (repos, queries, mappers)
│
└─ usecases/                     # entry points: invoke() methods the UI calls
```

### Similarity Computation
Refer to the [README.md](core/analyser/README.md) in the folder for more details about the similarity computation engine used.

<br>

# Data Layer
This module contains all **infrastructure adapters** for the app:
- database access & vector queries
- on-device tokenizer + sentence embeddings model
- repositories that expose a clean Kotlin API to the rest of the backend

### Directory
```
data/
├─ db/
│ └─ queries/               # files to interact with individual objectbox stores
│ └─ VectorDatabase.kt      # initialise objectbox database and define entities
│
├─ embedding/
│ ├─ HFTokenizer            # JNI bridge to HuggingFace tokenizers (native .so)
│ └─ SentenceEmbedding      # on-device sentence embedding service (TFLite/ONNX)
│
└─ repository/
  ├─ DictionaryRepository   # domain dictionary / keywords / tag lists
  ├─ EmbeddingRepository    # abstraction over embedding + cosine ops
  ├─ EventRepository        # read/write/query graph events
  ├─ PosTaggerRepository    # POS tagging
  └─ UserActionRepository   # record & query user actions
```
<br>

# Frontend (Jetpack Compose)
This module contains the **UI layer** of the app. 

### Directory
```
frontend/
├─ components/      # Reusable composables (buttons, cards, list items, dialogs)
├─ navigation/      # NavHost, routes, NavGraph builders
├─ screens/         # Feature/atomic screens (detail, list, settings, etc.)
├─ theme/           # Material3 theme (colors, typography, shapes)
├─ useCaseScreens/  # Scenario-driven flows (e.g., Threat Alert, Similar Events)
├─ viewmodels/      # UI state + business events (MVVM)
├─ MainActivity.kt  # Single activity host
└─ MainApp.kt       # App entry Composable: sets theme + NavHost
```
