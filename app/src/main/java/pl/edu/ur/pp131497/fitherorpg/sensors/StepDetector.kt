package pl.edu.ur.pp131497.fitherorpg.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast

//td why this is not working...
class StepDetector(
    private val context: Context,
    private val sensorManager: SensorManager,
    private val onStepDetected: () -> Unit
) : RepDetector, SensorEventListener {

    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    override fun start() {
        if (sensor == null) {
            Toast.makeText(context, "Error: no step sensor in this phone!", Toast.LENGTH_LONG).show()
            return
        }

        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun stop() {
        try {
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.values[0] == 1.0f) {
                onStepDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
}