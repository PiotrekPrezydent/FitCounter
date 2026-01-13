package pl.edu.ur.pp131497.fitcounter

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.util.Locale

class BmiActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bmi)

        calculateAndDisplayBMI()

        findViewById<Button>(R.id.btnUpdateProfile).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        calculateAndDisplayBMI()
    }

    private fun calculateAndDisplayBMI() {
        val prefs = getSharedPreferences("FitCounterPrefs", MODE_PRIVATE)
        val weight = prefs.getFloat("USER_WEIGHT", 80.0f)
        val heightCm = prefs.getInt("USER_HEIGHT", 180)

        findViewById<TextView>(R.id.tvUserInfo).text = "Calculated for: ${heightCm} cm, $weight kg"

        //BMI = kg / (m * m)
        val heightM = heightCm / 100.0f
        val bmi = if (heightM > 0) weight / (heightM * heightM) else 0.0f

        // view bmi
        val tvScore = findViewById<TextView>(R.id.tvBmiScore)
        tvScore.text = String.format(Locale.US, "%.1f", bmi)

        // set category and color
        val tvCategory = findViewById<TextView>(R.id.tvBmiCategory)
        val cardCategory = findViewById<CardView>(R.id.cardCategory)

        when {
            bmi < 18.5 -> {
                tvCategory.text = "Underweight"
                cardCategory.setCardBackgroundColor(Color.parseColor("#FFCA28")) // yellow
            }
            bmi < 25.0 -> {
                tvCategory.text = "Normal Weight"
                cardCategory.setCardBackgroundColor(Color.parseColor("#4CAF50")) // green
            }
            bmi < 30.0 -> {
                tvCategory.text = "Overweight"
                cardCategory.setCardBackgroundColor(Color.parseColor("#FF7043")) // orange
            }
            else -> {
                tvCategory.text = "Obesity"
                cardCategory.setCardBackgroundColor(Color.parseColor("#F44336")) // red
            }
        }
    }
}