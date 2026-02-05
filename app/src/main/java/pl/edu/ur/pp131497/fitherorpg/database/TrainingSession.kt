package pl.edu.ur.pp131497.fitherorpg.database

//td add steps
data class TrainingSession(
    val id: Long,
    val date: Long,
    val type: String,
    val reps: Int,
    val calories: Double,
    val monsterDefeated: Boolean = false,
    val damageDealt: Int = 0,
    val goldEarned: Int = 0
)