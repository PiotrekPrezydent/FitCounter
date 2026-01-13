package pl.edu.ur.pp131497.fitcounter

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import pl.edu.ur.pp131497.fitcounter.database.DatabaseHelper


//td add prefs manager
class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "FitCounterPrefs"
        const val KEY_WEIGHT = "USER_WEIGHT"
        const val KEY_HEIGHT = "USER_HEIGHT"
        const val DEFAULT_WEIGHT = 80.0f
        const val DEFAULT_HEIGHT = 180
    }

    private lateinit var etWeight: TextInputEditText
    private lateinit var etHeight: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnReset: TextView
    private lateinit var btnTerms: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        loadUserData()
        setupListeners()
    }

    private fun initViews() {
        etWeight = findViewById(R.id.etWeight)
        etHeight = findViewById(R.id.etHeight)
        btnSave = findViewById(R.id.btnSaveSettings)
        btnReset = findViewById(R.id.btnResetHistory)
        btnTerms = findViewById(R.id.btnTerms)
    }

    private fun loadUserData() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val savedWeight = prefs.getFloat(KEY_WEIGHT, DEFAULT_WEIGHT)
        val savedHeight = prefs.getInt(KEY_HEIGHT, DEFAULT_HEIGHT)

        etWeight.setText(savedWeight.toString())
        etHeight.setText(savedHeight.toString())
    }

    private fun setupListeners() {
        btnSave.setOnClickListener {
            saveUserData()
        }

        btnReset.setOnClickListener {
            showResetConfirmationDialog()
        }

        btnTerms.setOnClickListener {
            showTermsDialog()
        }
    }

    private fun saveUserData() {
        val weightStr = etWeight.text.toString()
        val heightStr = etHeight.text.toString()

        if (weightStr.isEmpty() || heightStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        try {
            editor.putFloat(KEY_WEIGHT, weightStr.toFloat())
            editor.putInt(KEY_HEIGHT, heightStr.toInt())
            editor.apply() // Zapisz w tle

            Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset History")
            .setMessage("Are you sure you want to delete all training history? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val db = DatabaseHelper(this)
                db.clearAllData()
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //this text should be in constants
    private fun showTermsDialog() {
        val title = "Terms & Privacy Info"

        val message = """
            Welcome to FitCounter!
            
            1. DATA PRIVACY (OFFLINE)
            This app operates 100% offline. Your workout history, weight, and height are stored LOCALLY on this device. We do not send your data to any cloud servers.
            
            2. SENSORS USAGE
            We use your device's Proximity Sensor and Accelerometer strictly to count your repetitions in real-time. This sensor data is not recorded permanently.
            
            3. MEDICAL DISCLAIMER
            This app is for informational purposes only. The calorie count is an estimation. Consult a doctor before starting any intense training program. Use this app at your own risk.
            
            4. DATA DELETION
            You can delete all your stored data at any time using the "Reset History" button below or by uninstalling the app.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("I Understand", null)
            .show()
    }
}