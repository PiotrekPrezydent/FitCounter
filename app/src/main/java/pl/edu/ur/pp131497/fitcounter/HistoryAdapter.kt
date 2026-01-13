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
        val tvPushups: TextView = view.findViewById(R.id.tvHistoryPushups)
        val tvSquats: TextView = view.findViewById(R.id.tvHistorySquats)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_day, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val dayData = daysList[position]

        holder.tvDate.text = dayData.dateString
        holder.tvPushups.text = "${dayData.totalPushups} Reps"
        holder.tvSquats.text = "${dayData.totalSquats} Reps"

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

            val typeName = if (session.type == TrainingType.PUSH_UP) "Push-ups" else "Squats"
            val color = if (session.type == TrainingType.PUSH_UP) "#E53935" else "#1E88E5" // red or blue

            sessionRow.text = "• $time   $typeName: ${session.reps} reps"
            sessionRow.textSize = 16f
            sessionRow.setTextColor(Color.parseColor("#455A64"))
            sessionRow.setPadding(0, 8, 0, 8)

            sessionsContainer.addView(sessionRow)
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}