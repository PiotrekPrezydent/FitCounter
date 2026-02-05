package pl.edu.ur.pp131497.fitherorpg

import android.graphics.Color
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import pl.edu.ur.pp131497.fitherorpg.database.DatabaseHelper
import pl.edu.ur.pp131497.fitherorpg.viewmodels.TrainingViewModel

class TrainingActivity : AppCompatActivity() {
    private val viewModel: TrainingViewModel by viewModels()

    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvCounter: TextView
    private lateinit var btnStartStop: MaterialButton
    private lateinit var btnFinish: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        
        viewModel.initSensor(this, sensorManager)

        initViews()
        setupUIBasedOnType(viewModel.trainingType)
        observeViewModel()
        setupListeners()
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvExerciseTitle)
        tvStatus = findViewById(R.id.tvStatus)
        tvCounter = findViewById(R.id.tvCounter)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnFinish = findViewById(R.id.btnFinish)
    }


    //td add text and colors to constants
    private fun setupUIBasedOnType(type: String) {
        if (type == TrainingType.PUSH_UP) {
            tvTitle.text = "PUSH-UPS"
            tvTitle.setTextColor(Color.parseColor("#E53935"))
            tvStatus.text = "Place phone on floor under your chest"
        } else if (type == TrainingType.SQUAT) {
            tvTitle.text = "SQUATS"
            tvTitle.setTextColor(Color.parseColor("#1E88E5"))
            tvStatus.text = "Hold phone in hand or pocket"
        } else {
            tvTitle.text = "WALKING"
            tvTitle.setTextColor(Color.parseColor("#00ACC1")) // Cyan
            tvStatus.text = "Phone in pocket or hand..."
        }
    }

    private fun observeViewModel() {
        viewModel.reps.observe(this) { count ->
            tvCounter.text = count.toString()
        }

        viewModel.isTraining.observe(this) { isTraining ->
            if (isTraining) {
                btnStartStop.text = "PAUSE"
                btnStartStop.setBackgroundColor(Color.parseColor("#FFC107")) // Żółty

                if (viewModel.trainingType == TrainingType.PUSH_UP) {
                    tvStatus.text = "Go down..."
                } else if (viewModel.trainingType == TrainingType.SQUAT){
                    tvStatus.text = "Squat now..."
                } else {
                    tvStatus.text = "Walk now..."
                }

            } else {
                btnStartStop.text = "START"
                btnStartStop.setBackgroundColor(Color.parseColor("#4CAF50"))
                tvStatus.text = "Press Start to begin"
            }
        }
    }

    private fun setupListeners() {
        btnStartStop.setOnClickListener {
            viewModel.toggleTraining()
        }

        btnFinish.setOnClickListener {
            val reps = viewModel.reps.value ?: 0

            if (reps > 0) {
                val prefs = getSharedPreferences("FitCounterPrefs", android.content.Context.MODE_PRIVATE)
                val userWeight = prefs.getFloat("USER_WEIGHT", 80.0f).toDouble()

                val timePerRepSeconds = when (viewModel.trainingType) {
                    TrainingType.PUSH_UP -> 3.0
                    TrainingType.SQUAT -> 4.0
                    TrainingType.STEP -> 0.6
                    else -> 1.0
                }
                val met = when (viewModel.trainingType) {
                    TrainingType.PUSH_UP -> 8.0
                    TrainingType.SQUAT -> 5.0
                    TrainingType.STEP -> 3.5
                    else -> 1.0
                }
                val totalTimeHours = (reps * timePerRepSeconds) / 3600.0

                val burnedKcal = met * userWeight * totalTimeHours

                val db = DatabaseHelper(this)
                db.addSession(
                    type = viewModel.trainingType,
                    reps = reps,
                    date = System.currentTimeMillis(),
                    calories = burnedKcal,
                    monsterDefeated = false,
                    damageDealt = 0,
                    goldEarned = 0
                )
            }
            finish()
        }
    }
}