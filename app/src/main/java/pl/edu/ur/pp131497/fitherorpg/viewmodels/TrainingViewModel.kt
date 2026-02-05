
package pl.edu.ur.pp131497.fitherorpg.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import pl.edu.ur.pp131497.fitherorpg.MonsterManager
import pl.edu.ur.pp131497.fitherorpg.TrainingType
import pl.edu.ur.pp131497.fitherorpg.sensors.PushUpDetector
import pl.edu.ur.pp131497.fitherorpg.sensors.RepDetector
import pl.edu.ur.pp131497.fitherorpg.sensors.SquatDetector
import pl.edu.ur.pp131497.fitherorpg.sensors.StepDetector

class TrainingViewModel(
    application: Application,
    private val state: SavedStateHandle
) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("FitQuestState", Context.MODE_PRIVATE)

    var trainingType: String = state.get<String>(TrainingType.KEY) ?: TrainingType.PUSH_UP
        private set

    private val _reps = MutableLiveData(0)
    val reps: LiveData<Int> get() = _reps

    private val _isTraining = MutableLiveData(false)
    val isTraining: LiveData<Boolean> get() = _isTraining

    // RPG Stats
    private val _currentHp = MutableLiveData(0)
    val currentHp: LiveData<Int> get() = _currentHp

    private val _maxHp = MutableLiveData(100)
    val maxHp: LiveData<Int> get() = _maxHp

    private val _monsterName = MutableLiveData("")
    val monsterName: LiveData<String> get() = _monsterName
    
    private val _monsterImageResName = MutableLiveData("monster_slime")
    val monsterImageResName: LiveData<String> get() = _monsterImageResName

    private val _gold = MutableLiveData(0)
    val gold: LiveData<Int> get() = _gold

    private val _damageDealtTotal = MutableLiveData(0)
    val damageDealtTotal: LiveData<Int> get() = _damageDealtTotal
    
    // Potions & Buffs
    // Storing expiry timestamps (ms). If current time < expiry, buff is active.
    private val _damageBuffExpiry = MutableLiveData(0L)
    val damageBuffExpiry: LiveData<Long> get() = _damageBuffExpiry
    
    private val _goldBuffExpiry = MutableLiveData(0L)
    val goldBuffExpiry: LiveData<Long> get() = _goldBuffExpiry

    private var detector: RepDetector? = null

    private val _level = MutableLiveData(1)
    val level: LiveData<Int> get() = _level

    private val _currentXp = MutableLiveData(0)
    val currentXp: LiveData<Int> get() = _currentXp
    
    private val _targetXp = MutableLiveData(100)
    val targetXp: LiveData<Int> get() = _targetXp

    init {
        loadGameState()
    }
    
    private fun loadGameState() {
        val lvl = prefs.getInt("PLAYER_LEVEL", 1)
        _level.value = lvl
        
        val xp = prefs.getInt("PLAYER_XP", 0)
        _currentXp.value = xp
        
        _targetXp.value = lvl * 100
        
        // Load Saved Monster
        val monsterIndex = prefs.getInt("MONSTER_INDEX", -1)
        val monster = if (monsterIndex != -1) {
             MonsterManager.getMonsterByIndex(monsterIndex, lvl)
        } else {
             MonsterManager.getMonsterByLevel(lvl)
        }
        
        _monsterName.value = monster.name
        _monsterImageResName.value = monster.imageResName
        _maxHp.value = monster.maxHp
        
        val storedHp = prefs.getInt("MONSTER_HP", monster.maxHp)
        _currentHp.value = if (storedHp > 0) storedHp else monster.maxHp
        
        _gold.value = prefs.getInt("PLAYER_GOLD", 0)
        
        val loadedTotalDmg = prefs.getInt("TOTAL_DAMAGE", 0)
        _damageDealtTotal.value = loadedTotalDmg

        // Load Buffs
        _damageBuffExpiry.value = prefs.getLong("BUFF_DAMAGE_EXPIRY", 0L)
        _goldBuffExpiry.value = prefs.getLong("BUFF_GOLD_EXPIRY", 0L)
    }

    // ... (rest of initSensor, changeWeapon, handleStep)
    fun initSensor(context: android.content.Context, sensorManager: SensorManager) {
        // Stop existing if any
        detector?.stop()

        detector = when (trainingType) {
            TrainingType.PUSH_UP -> PushUpDetector(sensorManager) { attackMonster(5) }
            TrainingType.SQUAT -> SquatDetector(sensorManager) { attackMonster(7) }
            TrainingType.STEP -> StepDetector(context, sensorManager) { handleStep() }
            else -> null
        }
        
        if (_isTraining.value == true) {
            detector?.start()
        }
    }

    fun changeWeapon(newType: String, context: Context, sensorManager: SensorManager) {
        if (trainingType == newType) return
        trainingType = newType
        state[TrainingType.KEY] = newType // Persist state
        initSensor(context, sensorManager)
    }

    private fun handleStep() {
        incrementReps()
        // 10 steps = 1 Gold
        val currentReps = _reps.value ?: 0
        if (currentReps % 10 == 0) {
            addGold(1)
            sessionGold += 1
        }
        attackMonster(1)
    }
    
    private fun attackMonster(baseDamage: Int) {
        if (trainingType != TrainingType.STEP) {
             incrementReps()
        }
        
        var damage = baseDamage
        
        // Check Damage Buff
        val now = System.currentTimeMillis()
        val dmgExpiry = _damageBuffExpiry.value ?: 0L
        if (now < dmgExpiry) {
            damage *= 2
        }

        val current = _currentHp.value ?: 10
        val newHp = (current - damage).coerceAtLeast(0)
        _currentHp.value = newHp
        
        val totalDmg = _damageDealtTotal.value ?: 0
        _damageDealtTotal.value = totalDmg + damage
        
        sessionDamage += damage
        
        saveGameState(newHp)
        // Save Total Damage frequently or onPause? For now, we save here for safety.
        prefs.edit().putInt("TOTAL_DAMAGE", totalDmg + damage).apply()

        if (newHp == 0) {
            onMonsterDefeated()
        }
    }

    private fun onMonsterDefeated() {
        val lvl = _level.value ?: 1
        val monster = MonsterManager.getMonsterByLevel(lvl)
        
        addGold(monster.goldReward)
        sessionGold += monster.goldReward
        sessionMonsterDefeated = true
        
        // Add XP
        val currentXpVal = _currentXp.value ?: 0
        var newXp = currentXpVal + monster.expReward
        var newLevel = lvl
        var target = lvl * 100
        
        // Level Up Loop (in case big reward)
        while (newXp >= target) {
            newXp -= target
            newLevel++
            target = newLevel * 100
        }
        
        _currentXp.value = newXp
        _level.value = newLevel
        _targetXp.value = target
        
        prefs.edit()
            .putInt("PLAYER_LEVEL", newLevel)
            .putInt("PLAYER_XP", newXp)
            .apply()
            
        // Respawn next monster
        val nextMonster = MonsterManager.getMonsterByLevel(newLevel)
        _monsterName.value = nextMonster.name
        _monsterImageResName.value = nextMonster.imageResName
        _maxHp.value = nextMonster.maxHp
        _currentHp.value = nextMonster.maxHp // Full HP
        
        saveGameState(nextMonster.maxHp)
        
        // Save the new monster ID
        prefs.edit().putInt("MONSTER_INDEX", nextMonster.id).apply()
    }
    
    private fun addGold(baseAmount: Int) {
        var amount = baseAmount
        
        // Check Gold Buff
        if (amount > 0) {
            val now = System.currentTimeMillis()
            val goldExpiry = _goldBuffExpiry.value ?: 0L
            if (now < goldExpiry) {
                amount *= 2
            }
        }
        
        val currentGold = _gold.value ?: 0
        val newGold = currentGold + amount
        _gold.postValue(newGold)
        prefs.edit().putInt("PLAYER_GOLD", newGold).apply()
    }
    
    private fun saveGameState(hp: Int) {
        prefs.edit().putInt("MONSTER_HP", hp).apply()
    }

    // Session Stats
    private var sessionReps = 0
    private var sessionDamage = 0
    private var sessionGold = 0
    private var sessionMonsterDefeated = false

    fun toggleTraining() {
        val currentlyTraining = _isTraining.value ?: false
        if (currentlyTraining) {
            // Stop
            detector?.stop()
            _isTraining.value = false
            saveSession()
        } else {
            // Start
            resetSessionStats()
            detector?.start()
            _isTraining.value = true
        }
    }
    
    private fun resetSessionStats() {
        sessionReps = 0
        sessionDamage = 0
        sessionGold = 0
        sessionMonsterDefeated = false
        _reps.value = 0
        _damageDealtTotal.value = 0 // Or keep total? The UI view seems to imply session damage. Let's reset.
    }

    private fun saveSession() {
        if (sessionReps == 0) return
        
        val db = pl.edu.ur.pp131497.fitherorpg.database.DatabaseHelper(getApplication())
        // Calc calories (simplified)
        val mets = when(trainingType) {
             TrainingType.PUSH_UP -> 8.0
             TrainingType.SQUAT -> 5.0
             TrainingType.STEP -> 3.5
             else -> 1.0
        }
        val userWeight = prefs.getFloat("USER_WEIGHT", 80.0f).toDouble()
        // Approx time
        val durationHours = (sessionReps * 3.0) / 3600.0 // rough estimate
        val burnt = mets * userWeight * durationHours
        
        db.addSession(
            type = trainingType,
            reps = sessionReps,
            date = System.currentTimeMillis(),
            calories = burnt,
            monsterDefeated = sessionMonsterDefeated,
            damageDealt = sessionDamage,
            goldEarned = sessionGold
        )
    }

    private fun incrementReps() {
        val current = _reps.value ?: 0
        _reps.value = current + 1
        sessionReps++
    }

    override fun onCleared() {
        super.onCleared()
        detector?.stop()
        // Determine if potion should expire? 
        // For simplicity, let's say potion is one-time use per session, so we clear it here?
        // Or we prefer manual consumption. Let's leave it in prefs for now.
    }
    
    // Shop Logic
    fun buyItem(itemId: String): Boolean {
        val currentGold = _gold.value ?: 0
        val now = System.currentTimeMillis()
        
        when (itemId) {
            "potion_strength" -> {
                if (currentGold >= 50) {
                    addGold(-50)
                    // Add 5 minutes
                    val currentExpiry = _damageBuffExpiry.value ?: 0L
                    val start = if (currentExpiry > now) currentExpiry else now
                    val newExpiry = start + (5 * 60 * 1000)
                    _damageBuffExpiry.value = newExpiry
                    prefs.edit().putLong("BUFF_DAMAGE_EXPIRY", newExpiry).apply()
                    return true
                }
            }
            "coin_lucky" -> {
                if (currentGold >= 100) {
                    addGold(-100)
                    // Add 10 minutes
                    val currentExpiry = _goldBuffExpiry.value ?: 0L
                    val start = if (currentExpiry > now) currentExpiry else now
                    val newExpiry = start + (10 * 60 * 1000)
                    _goldBuffExpiry.value = newExpiry
                    prefs.edit().putLong("BUFF_GOLD_EXPIRY", newExpiry).apply()
                    return true
                }
            }
            "grenade" -> {
                if (currentGold >= 200) {
                    addGold(-200)
                    attackMonster(100) // Instant 100 damage
                    return true
                }
            }
        }
        return false
    }
}