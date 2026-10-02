package app.prafullkumar.stats.sync

/**
 * The app's own Firebase project. A web API key only identifies the project —
 * access is decided by Auth and the Firestore rules in firebase/firestore.rules.
 */
object FirebaseProject {
    const val API_KEY = "AIzaSyD1z-Hhn0GE4jSe6o8wb8iutvV68cEcWr4"
    const val PROJECT_ID = "prafull-stats"

    val defaults = CloudConfig(apiKey = API_KEY, projectId = PROJECT_ID)
}
