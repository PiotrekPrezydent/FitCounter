package pl.edu.ur.pp131497.fitherorpg

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import pl.edu.ur.pp131497.fitherorpg.viewmodels.TrainingViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: TrainingViewModel by viewModels()

    private lateinit var tvLevel: TextView
    private lateinit var tvGold: TextView
    private lateinit var tvMonsterName: TextView
    private lateinit var tvHpText: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvTotalDamage: TextView
    private lateinit var tvDailyChallenge: TextView
    
    private lateinit var pbExp: ProgressBar
    private lateinit var pbHp: ProgressBar
    
    private lateinit var ivMonster: ImageView
    
    private lateinit var btnFight: MaterialButton
    private lateinit var btnWeaponPush: MaterialButton
    private lateinit var btnWeaponSquat: MaterialButton
    private lateinit var btnWeaponStep: MaterialButton
    
    private lateinit var btnHistory: Button
    private lateinit var btnShop: Button
    private lateinit var btnSettings: Button
    private lateinit var btnInfo: android.widget.ImageButton
    private lateinit var btnHealthGuide: android.widget.ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init Sensors
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        viewModel.initSensor(this, sensorManager)

        initViews()
        setupListeners()
        observeViewModel()
        
        setupDailyChallenge()
        checkFirstRun()
    }
    
    private fun checkFirstRun() {
        val prefs = getSharedPreferences("FitCounterPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("TERMS_ACCEPTED", false)) {
            startActivity(Intent(this, GuideActivity::class.java))
            // Do not verify here. GuideActivity sets TERMS_ACCEPTED when user clicks button.
        }
    }

    private fun setupDailyChallenge() {
        val challenges = listOf(
            "Do 50 Squats today!",
            "Walk 2000 steps!",
            "Defeat 1 Monster!",
            "Earn 100 Gold!",
            "Do 30 Push-ups!"
        )
        val todayChallenge = challenges.random()
        tvDailyChallenge.text = todayChallenge
    }

    private fun initViews() {
        tvLevel = findViewById(R.id.tvLevel)
        tvGold = findViewById(R.id.tvGold)
        tvMonsterName = findViewById(R.id.tvMonsterName)
        tvHpText = findViewById(R.id.tvHpText)
        tvStatus = findViewById(R.id.tvStatus)
        tvTotalDamage = findViewById(R.id.tvTotalDamage)
        tvDailyChallenge = findViewById(R.id.tvDailyChallenge)
        
        pbExp = findViewById(R.id.pbExp)
        pbHp = findViewById(R.id.pbHp)
        
        ivMonster = findViewById(R.id.ivMonster)
        
        btnFight = findViewById(R.id.btnFight)
        btnWeaponPush = findViewById(R.id.btnWeaponPush)
        btnWeaponSquat = findViewById(R.id.btnWeaponSquat)
        btnWeaponStep = findViewById(R.id.btnWeaponStep)
        
        btnHistory = findViewById(R.id.btnHistory)
        btnShop = findViewById(R.id.btnShop)
        btnSettings = findViewById(R.id.btnSettings)
        btnInfo = findViewById(R.id.btnInfo)
        btnHealthGuide = findViewById(R.id.btnHealthGuide)
        
        updateWeaponUI(viewModel.trainingType)
    }

    private fun setupListeners() {
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        btnFight.setOnClickListener {
            if (viewModel.trainingType == TrainingType.STEP) {
                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION), 100)
                        return@setOnClickListener
                    }
                }
            }
            viewModel.toggleTraining()
        }

        btnWeaponPush.setOnClickListener { 
            viewModel.changeWeapon(TrainingType.PUSH_UP, this, sensorManager)
            updateWeaponUI(TrainingType.PUSH_UP)
            // Stop current fight if switching weapon? maybe safe default
            if (viewModel.isTraining.value == true) viewModel.toggleTraining() 
        }

        btnWeaponSquat.setOnClickListener { 
            viewModel.changeWeapon(TrainingType.SQUAT, this, sensorManager)
            updateWeaponUI(TrainingType.SQUAT)
            if (viewModel.isTraining.value == true) viewModel.toggleTraining()
        }

        btnWeaponStep.setOnClickListener { 
            viewModel.changeWeapon(TrainingType.STEP, this, sensorManager)
            updateWeaponUI(TrainingType.STEP)
            if (viewModel.isTraining.value == true) viewModel.toggleTraining()
        }
        
        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnShop.setOnClickListener {
            showShopDialog()
        }

        btnSettings.setOnClickListener {
             startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        btnInfo.setOnClickListener {
             startActivity(Intent(this, GuideActivity::class.java))
        }
        
        btnHealthGuide.setOnClickListener {
             startActivity(Intent(this, HealthGuideActivity::class.java))
        }
    }
    
    private fun updateWeaponUI(type: String) {
        val activeColor = Color.parseColor("#76FF03")
        val inactiveColor = Color.WHITE
        
        btnWeaponPush.strokeColor = android.content.res.ColorStateList.valueOf(if (type == TrainingType.PUSH_UP) activeColor else inactiveColor)
        btnWeaponSquat.strokeColor = android.content.res.ColorStateList.valueOf(if (type == TrainingType.SQUAT) activeColor else inactiveColor)
        btnWeaponStep.strokeColor = android.content.res.ColorStateList.valueOf(if (type == TrainingType.STEP) activeColor else inactiveColor)
        
        val statusText = when(type) {
            TrainingType.PUSH_UP -> "Place phone on floor under chest"
            TrainingType.SQUAT -> "Hold phone in hand"
            TrainingType.STEP -> "Phone in pocket"
            else -> ""
        }
        if (viewModel.isTraining.value != true) {
            tvStatus.text = statusText
        }
    }

    private fun observeViewModel() {
        viewModel.currentHp.observe(this) { hp ->
            pbHp.progress = hp
            tvHpText.text = "$hp / ${pbHp.max} HP"
        }
        
        viewModel.maxHp.observe(this) { max ->
            pbHp.max = max
        }
        
        viewModel.level.observe(this) { lvl ->
            tvLevel.text = "LVL $lvl"
        }
        
        viewModel.currentXp.observe(this) { xp ->
            pbExp.progress = xp
        }
        
        viewModel.targetXp.observe(this) { target ->
            pbExp.max = target
        }
        
        viewModel.monsterName.observe(this) { name ->
            tvMonsterName.text = name
        }
        
        viewModel.monsterImageResName.observe(this) { resName ->
             val resId = resources.getIdentifier(resName, "drawable", packageName)
             if (resId != 0) {
                 ivMonster.setImageResource(resId)
             }
        }
        
        viewModel.gold.observe(this) { gold ->
            tvGold.text = "$gold G"
        }
        
        viewModel.damageDealtTotal.observe(this) { dmg ->
            tvTotalDamage.text = "Damage: $dmg"
        }

        viewModel.isTraining.observe(this) { isTraining ->
            if (isTraining) {
                btnFight.text = "STOP FIGHTING"
                btnFight.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F")) // Red
                tvStatus.text = "FIGHTING! DO REPS!"
                tvStatus.setTextColor(Color.parseColor("#FF5252"))
            } else {
                btnFight.text = "FIGHT MONSTER"
                btnFight.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#D500F9")) // Purple
                tvStatus.text = "Prepare for battle..."
                tvStatus.setTextColor(Color.parseColor("#B0BEC5"))
            }
        }
    }
    
    private fun showShopDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_shop, null)
            val builder = AlertDialog.Builder(this)
                .setView(dialogView)
            
            val dialog = builder.create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            val btnBuyPotion = dialogView.findViewById<Button>(R.id.btnBuyPotion)
            val btnBuyCoin = dialogView.findViewById<Button>(R.id.btnBuyCoin)
            val btnBuyGrenade = dialogView.findViewById<Button>(R.id.btnBuyGrenade)
            val btnClose = dialogView.findViewById<Button>(R.id.btnCloseShop)
            
            // Update Buttons based on state
            val now = System.currentTimeMillis()
            val dmgExpiry = viewModel.damageBuffExpiry.value ?: 0L
            val goldExpiry = viewModel.goldBuffExpiry.value ?: 0L
            
            if (now < dmgExpiry) {
                btnBuyPotion.text = "Active"
                btnBuyPotion.isEnabled = false
                btnBuyPotion.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.GRAY)
            }
            
            if (now < goldExpiry) {
                btnBuyCoin.text = "Active"
                btnBuyCoin.isEnabled = false
                btnBuyCoin.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.GRAY)
            }
            
            btnBuyPotion.setOnClickListener {
                if (viewModel.buyItem("potion_strength")) {
                    btnBuyPotion.text = "Active"
                    btnBuyPotion.isEnabled = false
                    btnBuyPotion.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.GRAY)
                } else {
                    android.widget.Toast.makeText(this, "Not enough Gold!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            
            btnBuyCoin.setOnClickListener {
                if (viewModel.buyItem("coin_lucky")) {
                    btnBuyCoin.text = "Active"
                    btnBuyCoin.isEnabled = false
                    btnBuyCoin.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.GRAY)
                } else {
                     android.widget.Toast.makeText(this, "Not enough Gold!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            
            btnBuyGrenade.setOnClickListener {
                if (viewModel.buyItem("grenade")) {
                     android.widget.Toast.makeText(this, "BOOM! 100 Damage!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                     android.widget.Toast.makeText(this, "Not enough Gold!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            
            btnClose.setOnClickListener {
                dialog.dismiss()
            }
            
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Error opening shop: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}