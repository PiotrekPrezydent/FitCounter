package pl.edu.ur.pp131497.fitcounter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


import android.content.Context
import android.widget.Button

class GuideActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)

        val btnAccept = findViewById<Button>(R.id.btnAccept)
        btnAccept.setOnClickListener {
            val prefs = getSharedPreferences("FitCounterPrefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("TERMS_ACCEPTED", true)
                .putBoolean("FIRST_RUN", false)
                .apply()
            
            finish()
        }
    }
}