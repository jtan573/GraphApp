# Analyser Folder

The Analyser computes similar and related events from a 5W1H graph, combining:
* Semantic similarity (WHO/WHY/HOW/TASK/INCIDENT/OUTCOME/IMPACT) via embeddings
* Computed similarity (WHERE/WHEN) via geo distance & time deltas
* Frequency weighting to prefer rarer/more informative matches
* Top-K ranking with optional explanations (matching tags)

It exposes a single high-level entry point you call from your ViewModel/use case layer.

<br>

## Public API (you’ll usually call just this)
``` kotlin
/**
 * Main function to compute similarity between events.
 *
 * @return Map of key event type → ranked list of similar events of that type.
 */
suspend fun computeSimilarAndRelatedEvents(
    newEventMap: Map<SchemaEventTypeNames, String>, // input 5W1H (text)
    eventRepository: EventRepository,               // graph/data access
    embeddingRepository: EmbeddingRepository,       // embeddings + cosine
    sourceEventType: SchemaKeyEventTypeNames? = null,
    targetEventType: SchemaKeyEventTypeNames? = null,
    numTopResults: Int = SimilarityConfig.NUM_TOP_RESULTS_REQUIRED,
    activeNodesOnly: Boolean
): Map<SchemaKeyEventTypeNames, List<EventDetails>>
```
<br>

## How it works (pipeline)
### 1. Normalize input
`convertStringInputToNodes(...)`
* For each (type → string) in newEventMap, find an existing node or create a temporary node with on-the-fly embeddings.

### 2. Scope the search set
`checkTargetNodeStatus(activeNodesOnly)` → ACTIVE vs ALL <br>
`prepareDatasetForSimilarityComputation(...)`

* For each input property:
  * Semantic types → getRelevantNodes(tags, type)
  * WHERE → getCloseNodesByLocation(...)
  * WHEN → getCloseNodesByDatetime(...)

* From each candidate, collect its key neighbors (INCIDENT/TASK/OUTCOME/IMPACT), or only the specified sourceEventType.

→ Output: allKeyNodeIdsByType : Map<type, List<nodeId>>

### 3. Score & rank
`generateSimilarityResults(...)`

For each candidate key node:
`computeOverallSimilarity(...)`
* Semantic properties → computeSimilarityForSemanticProperties(...) using embeddings & cosine
* Computed properties →
  * `computeSimilarityForWhereProperty` → inverse distance (thresholded by MAX_DISTANCE)
  * `computeSimilarityForWhenProperty` → inverse time delta (thresholded by MAX_TIME_DIFFERENCE)
  * Average per-property scores → base score
* `getWeightedSimilarityScore(...)` multiplies by ln(1 + frequency(node))

<br>

## Configuration & thresholds
All tunables live in backend/config/SimilarityConfig.kt:
* NUM_TOP_RESULTS_REQUIRED
* SIMILARITY_THRESHOLD_SEMANTIC_PROPS (tag-to-tag)
* MAX_DISTANCE (meters for WHERE)
* MAX_TIME_DIFFERENCE (ms for WHEN)

<br>

## Key helpers (when you need lower-level access)
* `generateSimilarityResults(...)` — run scoring/ranking over a prepared candidate set
* `computeOverallSimilarity(nodeId1, nodeId2, ...)` — average of property-level scores (+ optional explanations)
* `computeSimilarityForSemanticProperties(...)` — cosine on embeddings + tag explanations
* `computeSimilarityForWhereProperty(...)` — distance-based score (0..1)
* `computeSimilarityForWhenProperty(...)` — time-delta score (0..1)
* `getPropertyEmbeddings(...)` — gather semantic/computed features for a node (or for a temporary input)