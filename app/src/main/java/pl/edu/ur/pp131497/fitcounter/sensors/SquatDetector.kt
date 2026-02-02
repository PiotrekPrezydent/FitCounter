package pl.edu.ur.pp131497.fitcounter.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class SquatDetector(
    private val sensorManager: SensorManager,
    private val onRepDetected: () -> Unit
) : RepDetector, SensorEventListener {

    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    //filter const, the lower the better, but slower reaction
    private val ALPHA = 0.15f

    private var gravity = floatArrayOf(0f, 0f, 0f)

    //values fresh from my ass
    private val THRESHOLD_DOWN = 8.5f
    private val THRESHOLD_UP = 11.5f

    private enum class State {
        IDLE,
        DESCENDING,
        ASCENDING
    }
    //start state
    private var currentState = State.IDLE
    private var lastStateChangeTime: Long = 0

    private val MIN_DESCEND_DURATION = 300L // ms

    override fun start() {
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gravity = floatArrayOf(0f, 0f, 0f)
        currentState = State.IDLE
    }

    override fun stop() {
        sensorManager.unregisterListener(this)
    }

    //idk it works
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            gravity[0] = ALPHA * it.values[0] + (1 - ALPHA) * gravity[0]
            gravity[1] = ALPHA * it.values[1] + (1 - ALPHA) * gravity[1]
            gravity[2] = ALPHA * it.values[2] + (1 - ALPHA) * gravity[2]

            val magnitude = sqrt(
                (gravity[0] * gravity[0] +
                        gravity[1] * gravity[1] +
                        gravity[2] * gravity[2]).toDouble()
            ).toFloat()

            val now = System.currentTimeMillis()

            when (currentState) {
                State.IDLE -> {
                    if (magnitude < THRESHOLD_DOWN) {
                        currentState = State.DESCENDING
                        lastStateChangeTime = now
                    }
                }

                State.DESCENDING -> {
                    if (magnitude > THRESHOLD_UP) {
                        if (now - lastStateChangeTime > MIN_DESCEND_DURATION) {
                            currentState = State.ASCENDING
                            onRepDetected()
                            lastStateChangeTime = now
                        } else {
                            currentState = State.IDLE
                        }
                    }

                    if (now - lastStateChangeTime > 5000) {
                        currentState = State.IDLE
                    }
                }

                State.ASCENDING -> {
                    if (now - lastStateChangeTime > 1000 && magnitude < 10.5 && magnitude > 9.0) {
                        currentState = State.IDLE
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
}