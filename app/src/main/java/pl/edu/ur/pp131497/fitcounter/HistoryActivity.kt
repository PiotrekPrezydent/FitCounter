package pl.edu.ur.pp131497.fitcounter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pl.edu.ur.pp131497.fitcounter.database.DailySummary
import pl.edu.ur.pp131497.fitcounter.database.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val recyclerView = findViewById<RecyclerView>(R.id.rvHistory)
        recyclerView.layoutManager = LinearLayoutManager(this)

        //get db
        val db = DatabaseHelper(this)
        val allSessions = db.getAllSessions()

        if (allSessions.isNotEmpty()) {
            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)

            val groupedMap = allSessions.groupBy { session ->
                dateFormat.format(Date(session.date))
            }

            //td add steps
            val summaryList = groupedMap.map { (dateStr, sessions) ->
                DailySummary(
                    dateString = dateStr,
                    totalPushups = sessions.filter { it.type == TrainingType.PUSH_UP }
                        .sumOf { it.reps },
                    totalSquats = sessions.filter { it.type == TrainingType.SQUAT }
                        .sumOf { it.reps },
                    sessions = sessions // Przekazujemy listę do popupu
                )
            }

            recyclerView.adapter = HistoryAdapter(summaryList)
        }
    }
}