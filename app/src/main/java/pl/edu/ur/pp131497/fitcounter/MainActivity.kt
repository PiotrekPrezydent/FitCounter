package pl.edu.ur.pp131497.fitcounter

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //check if terms of use is accepted
        checkTermsStatus()

        val cardPushups = findViewById<CardView>(R.id.cardPushups)
        val cardSquats = findViewById<CardView>(R.id.cardSquats)
        val cardHistory = findViewById<CardView>(R.id.cardHistory)
        val cardSettings = findViewById<CardView>(R.id.cardSettings)

        cardPushups.setOnClickListener {
            val intent = Intent(this, TrainingActivity::class.java)
            intent.putExtra(TrainingType.KEY, TrainingType.PUSH_UP)
            startActivity(intent)
        }

        cardSquats.setOnClickListener {
            val intent = Intent(this, TrainingActivity::class.java)
            intent.putExtra(TrainingType.KEY, TrainingType.SQUAT)
            startActivity(intent)
        }

        cardHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        cardSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        val cardBmi = findViewById<CardView>(R.id.cardBmi) // Znajdź nowy kafelek

        cardBmi.setOnClickListener {
            val intent = Intent(this, BmiActivity::class.java)
            startActivity(intent)
        }

        val cardGuide = findViewById<CardView>(R.id.cardGuide)
        cardGuide.setOnClickListener {
            val intent = Intent(this, GuideActivity::class.java)
            startActivity(intent)
        }

        val cardSteps = findViewById<CardView>(R.id.cardSteps)

        //this works only for andoird 10+ API 29+
        cardSteps.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION), 100)
                    return@setOnClickListener
                }
            }

            val intent = Intent(this, TrainingActivity::class.java)
            intent.putExtra(TrainingType.KEY, TrainingType.STEP)
            startActivity(intent)
        }
    }

    //this should be in some prefs manager but im lazy
    private fun checkTermsStatus() {
        val prefs = getSharedPreferences("FitCounterPrefs", Context.MODE_PRIVATE)

        val isAccepted = prefs.getBoolean("TERMS_ACCEPTED", false)

        if (!isAccepted) {
            showStartupTermsDialog(prefs)
        }
    }

    //this text should be in const
    private fun showStartupTermsDialog(prefs: SharedPreferences) {
        val title = "Welcome to FitCounter"
        val message = """
            Before you start, please accept our Terms of Service:
            
            1. This app works offline. Data is stored locally.
            2. We use sensors (Proximity & Accelerometer) to count reps.
            3. Consult a doctor before exercise. Use at your own risk.
            
            Do you accept these terms?
        """.trimIndent()

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Accept") { _, _ ->
                prefs.edit().putBoolean("TERMS_ACCEPTED", true).apply()
            }
            .setNegativeButton("Decline") { _, _ ->
                finishAffinity()
            }
            .create()

        dialog.show()
    }
}