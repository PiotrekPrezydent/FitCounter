package pl.edu.ur.pp131497.fitherorpg.database

//td add steps
data class DailySummary(
    val dateString: String,
    val totalPushups: Int,
    val totalSquats: Int,
    val totalSteps: Int, // Added
    val totalGold: Int, // Added
    val sessions: List<TrainingSession>
)