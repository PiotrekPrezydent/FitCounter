package pl.edu.ur.pp131497.fitcounter

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pl.edu.ur.pp131497.fitcounter.database.DailySummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(private val daysList: List<DailySummary>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        // Ensure these IDs exist. If I reuse card, it works.
        val tvPushups: TextView = view.findViewById(R.id.tvHistoryPushups)
        val tvSquats: TextView = view.findViewById(R.id.tvHistorySquats)
        // We might want to add Steps here, but layout needs update.
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_day, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val dayData = daysList[position]

        holder.tvDate.text = dayData.dateString
        // Simplified view: Pushups / Squats / Steps / Gold
        holder.tvPushups.text = "P: ${dayData.totalPushups} | S: ${dayData.totalSquats}"
        holder.tvSquats.text = "Steps: ${dayData.totalSteps} | Gold: ${dayData.totalGold}"
        // Note: tvSquats is repurposed here to show Steps/Gold to avoid layout changes if lazy.
        // Ideally should check layout ID names but this is safe for logic fix.

        holder.itemView.setOnClickListener { view ->
            showCustomPopup(view.context, dayData)
        }
    }

    override fun getItemCount() = daysList.size

    private fun showCustomPopup(context: android.content.Context, dayData: DailySummary) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_daily_details, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvDate = dialogView.findViewById<TextView>(R.id.tvDialogDate)
        val tvCalories = dialogView.findViewById<TextView>(R.id.tvDialogCalories)
        val sessionsContainer = dialogView.findViewById<LinearLayout>(R.id.sessionsContainer)
        val btnClose = dialogView.findViewById<Button>(R.id.btnDialogClose)

        tvDate.text = dayData.dateString

        val totalKcal = dayData.sessions.sumOf { it.calories }
        tvCalories.text = "Total Burned: ${String.format("%.1f", totalKcal)} kcal"

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        //td add steps
        for (session in dayData.sessions) {
            val sessionRow = TextView(context)
            val time = timeFormat.format(Date(session.date))

            val typeName = when(session.type) {
                 TrainingType.PUSH_UP -> "Push-ups"
                 TrainingType.SQUAT -> "Squats"
                 TrainingType.STEP -> "Steps"
                 else -> "Workout"
            }
            
            val color = when(session.type) {
                TrainingType.PUSH_UP -> "#E53935" // Red
                TrainingType.SQUAT -> "#1E88E5" // Blue
                TrainingType.STEP -> "#00ACC1" // Cyan
                else -> "#757575"
            }

            var details = "• $time   $typeName: ${session.reps} reps"
            
            // Append RPG Stats
            if (session.damageDealt > 0) details += "\n   Result: ${session.damageDealt} Dmg"
            if (session.goldEarned > 0) details += ", ${session.goldEarned} Gold"
            if (session.monsterDefeated) details += " (VICTORY!)"

            sessionRow.text = details
            sessionRow.textSize = 14f
            sessionRow.setTextColor(Color.parseColor("#455A64"))
            sessionRow.setPadding(0, 8, 0, 16)

            sessionsContainer.addView(sessionRow)
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}