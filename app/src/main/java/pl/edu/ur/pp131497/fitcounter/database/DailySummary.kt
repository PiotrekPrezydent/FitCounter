package pl.edu.ur.pp131497.fitcounter.database

//td add steps
data class DailySummary(
    val dateString: String,           // format DD.MM.RRRR
    val totalPushups: Int,
    val totalSquats: Int,
    val sessions: List<TrainingSession> // needed for popup
)