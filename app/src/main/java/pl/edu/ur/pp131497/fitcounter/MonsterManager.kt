package pl.edu.ur.pp131497.fitcounter

data class Monster(
    val id: Int, // Template Index
    val name: String,
    val maxHp: Int,
    val expReward: Int,
    val goldReward: Int,
    val imageResName: String // e.g. "monster_slime"
)

object MonsterManager {
    
    // Base templates with relative power scalings
    data class Template(val name: String, val res: String, val hpScale: Double, val rewardScale: Double)
    
    private val templates = listOf(
        Template("Slime", "monster_slime", 1.0, 1.0),
        Template("Goblin", "monster_goblin", 1.2, 1.1),
        Template("Skeleton", "monster_skeleton", 1.4, 1.2),
        Template("Orc", "monster_orc", 1.8, 1.4),
        Template("Vampire", "monster_vampire", 2.2, 1.6),
        Template("Golem", "monster_golem", 3.0, 2.0),
        Template("Demon", "monster_demon", 3.5, 2.5),
        Template("Dragon", "monster_dragon", 4.0, 3.0)
    )

    fun getMonsterByLevel(level: Int): Monster {
        // Randomly pick ANY monster type, allowing for variety at all levels
        val index = templates.indices.random()
        return getMonsterByIndex(index, level)
    }

    fun getMonsterByIndex(index: Int, level: Int): Monster {
        // Safe fallback
        val safeIndex = if (index in templates.indices) index else 0
        val template = templates[safeIndex]
        
        // 2. Determine Stats
        // Exponential scaling for HP
        // Base HP 40. Growth 1.15 per level
        val growthFactor = Math.pow(1.15, (level - 1).toDouble())
        
        val hp = (40 * template.hpScale * growthFactor).toInt()
        val xp = (20 * template.rewardScale * growthFactor).toInt()
        val gold = (10 * template.rewardScale * growthFactor).toInt()
        
        // 3. Determine Prefix
        val prefix = when {
            level % 10 == 0 -> "Legendary" // Boss every 10 levels
            level % 5 == 0 -> "Elite"
            level > 50 -> "Ancient"
            level > 20 -> "Brutal"
            else -> ""
        }
        
        val fullName = if (prefix.isNotEmpty()) "$prefix ${template.name}" else template.name
        
        // Boss Buffs
        val finalHp = if (level % 10 == 0) hp * 2 else hp
        val finalXp = if (level % 10 == 0) xp * 4 else xp
        val finalGold = if (level % 10 == 0) gold * 5 else gold
        
        return Monster(
            id = safeIndex,
            name = fullName,
            maxHp = finalHp,
            expReward = finalXp,
            goldReward = finalGold,
            imageResName = template.res
        )
    }
}
