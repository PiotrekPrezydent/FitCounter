package pl.edu.ur.pp131497.fitcounter.database

//td add steps
data class TrainingSession(
    val id: Long,
    val date: Long,
    val type: String,
    val reps: Int,
    val calories: Double
)