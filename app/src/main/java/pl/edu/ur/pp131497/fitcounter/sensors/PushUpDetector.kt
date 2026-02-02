package pl.edu.ur.pp131497.fitcounter.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class PushUpDetector(
    private val sensorManager: SensorManager,
    private val onRepDetected: () -> Unit
) : RepDetector, SensorEventListener {
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private var isNear = false

    override fun start() {
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    //turn of sensor for better battery health
    override fun stop() {
        sensorManager.unregisterListener(this)
    }

    //count on getting close then getting far
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val distance = it.values[0]
            val maxRange = it.sensor.maximumRange

            if (distance < maxRange) {
                if (!isNear) {
                    isNear = true
                }
            }
            else {
                if (isNear) {
                    onRepDetected()
                    isNear = false
                }
            }
        }
    }

    //interface inplementation
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}