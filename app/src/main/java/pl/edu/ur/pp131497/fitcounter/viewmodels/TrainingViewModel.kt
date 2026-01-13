package pl.edu.ur.pp131497.fitcounter.viewmodels

import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import pl.edu.ur.pp131497.fitcounter.TrainingType
import pl.edu.ur.pp131497.fitcounter.sensors.PushUpDetector
import pl.edu.ur.pp131497.fitcounter.sensors.RepDetector
import pl.edu.ur.pp131497.fitcounter.sensors.SquatDetector
import pl.edu.ur.pp131497.fitcounter.sensors.StepDetector

class TrainingViewModel(
    private val state: SavedStateHandle
) : ViewModel() {

    val trainingType: String = state.get<String>(TrainingType.KEY) ?: TrainingType.PUSH_UP

    private val _reps = MutableLiveData(0)
    val reps: LiveData<Int> get() = _reps

    private val _isTraining = MutableLiveData(false)
    val isTraining: LiveData<Boolean> get() = _isTraining

    private var detector: RepDetector? = null

    fun initSensor(context: android.content.Context, sensorManager: SensorManager) {
        if (detector != null) return

        detector = when (trainingType) {
            TrainingType.PUSH_UP -> PushUpDetector(sensorManager) { incrementReps() }
            TrainingType.SQUAT -> SquatDetector(sensorManager) { incrementReps() }
            TrainingType.STEP -> StepDetector(context, sensorManager) { incrementReps() }
            else -> null
        }
    }

    fun toggleTraining() {
        val currentlyTraining = _isTraining.value ?: false
        if (currentlyTraining) {
            detector?.stop()
            _isTraining.value = false
        } else {
            detector?.start()
            _isTraining.value = true
        }
    }

    private fun incrementReps() {
        val current = _reps.value ?: 0
        _reps.postValue(current + 1)
    }


    override fun onCleared() {
        super.onCleared()
        detector?.stop()
    }
}