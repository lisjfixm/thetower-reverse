# The Tower 程式碼邏輯（從 IL2CPP 還原）

> 基於 dump.cs（105K 方法定義）+ ARM64 反組譯 + 常數提取
> 以下為核心系統的 pseudocode

---

## 1. 主遊戲循環

```csharp
// Main.FixedUpdate() — 每物理幀呼叫
void FixedUpdate() {
    if (!roundActiveBool || gameOverBool) return;
    
    CalculateEnemyDistances();      // 計算所有敵人距離
    TowerFireFunction(deltaTime);   // 塔開火
    UpdateTimers();                  // 更新冷卻計時器
    // 更新所有投射物
}

// Main.Update() — 每幀呼叫
void Update() {
    gameSpeed = gameMaxSpeed;       // 套用遊戲速度倍率
    // UI 更新、特效、動畫
}
```

---

## 2. 塔開火邏輯 (TowerFireFunction)

```csharp
void TowerFireFunction(float deltaTime) {
    // 攻擊冷卻 = 1 / attackSpeed
    attackCooldown -= deltaTime;
    
    if (attackCooldown <= 0 && numberOfTargetsAvailable > 0) {
        attackCooldown = 1f / attackSpeed;
        
        // 選擇目標（依目標優先級排序）
        int targetIndex = GetTargetPriorityRank(enemy);
        
        // 觸發隨機效果判定
        bool isCrit = Random.value < criticalChance;
        bool isSuperCrit = Random.value < superCritChance;
        bool isMultishot = Random.value < multishotChance;
        bool isRapidFire = Random.value < rapidFireChance;
        bool isBounce = Random.value < bounceChance;
        
        double damage = this.damage;
        if (isCrit) damage *= criticalMult;
        if (isSuperCrit) damage *= superCritMult;
        
        // 發射投射物
        Projectile.Activate(towerPosition, targetEnemy);
        
        if (isMultishot) {
            // 同時攻擊 multishotTargets 個目標
            for (int i = 1; i < multishotTargets; i++) {
                OrbFire(orbPosition, targetIndex + i);
            }
        }
        
        if (isRapidFire) {
            rapidFireStartTime = Time.time;
            rapidFireEndTime = Time.time + rapidFireDuration;
        }
        
        if (isBounce) {
            // 投射物命中後彈跳 bounceTargets 次
            HitAllBounceTargets(originEnemy, bounceTargets, damage, ...);
        }
    }
    
    // 軌道彈邏輯
    if (orbCount > 0) {
        for (int i = 0; i < orbCount; i++) {
            OrbFire(orbPositions[i], selectedTarget);
        }
    }
}
```

---

## 3. 敵人生成與波次 (DifficultyTierCalcs + SpawnEnemy)

```csharp
void DifficultyTierCalcs() {
    // 從 currentTier 往上數，找到最高的已解鎖 Tier
    int tier = 23; // MAX_POSSIBLE_TIER - 1
    while (tier > 0) {
        if (upgradeTierUnlocked[tier]) break;
        tier--;
    }
    
    // Tier 上限 24
    currentTier = Math.Min(tier, 24);
    
    // 依 Tier 查表取得數值倍率（jump table at 0xbb1234）
    // Tier 1: 1.0x 基礎值
    // Tier 2-24: 逐步提升
    // 每個 Tier 對應不同的敵人血量/傷害/金幣倍率
}

void SpawnEnemy(EnemyType type, Vector3? pos, bool isSummon = false, ...) {
    Enemy enemy = Instantiate(enemyPrefabs[type], pos).GetComponent<Enemy>();
    
    // 依波數設定敵人數值
    enemy.enemyHealth = currentWaveBaseHealth * waveHealthLevel;
    enemy.enemyDamage = currentWaveBaseDamage * waveDamageLevel;
    enemy.enemySpeed = baseSpeed * enemySpeedWaveMult;
    enemy.enemyKillCash = currentWaveBaseKillCash;
    
    // 特殊類型初始化
    switch (type) {
        case EnemyType.Commander:
            // 光環：周圍敵人血/速/質量 ×1.15
            break;
        case EnemyType.Saboteur:
            // 攻擊間隔從 5s 線性降到 1s（4000 波達底）
            enemy.attackInterval = Mathf.Lerp(5f, 1f, currentWave / 4000f);
            break;
        case EnemyType.Overcharge:
            enemy.chargeDuration = 3f;
            enemy.attackInterval = Mathf.Lerp(15f, 1f, currentWave / 4000f);
            break;
        case EnemyType.ScatterMain:
            enemy.splitsOnDeath = 4; // Ultimate 模式 5
            break;
    }
}

// 每波更新
void UpdateWave() {
    currentWave++;
    waveTimer = waveLengthSeconds;
    
    // 計算本波敵人基礎數值
    currentWaveBaseHealth = baseHealth * Mathf.Pow(healthGrowth, currentWave);
    currentWaveBaseDamage = baseDamage * Mathf.Pow(damageGrowth, currentWave);
    currentWaveBaseKillCash = baseCash * Mathf.Pow(cashGrowth, currentWave);
    
    // 迷你 Boss / Boss 觸發
    if (IsMiniBossWave(currentWave)) { /* spawn miniboss */ }
    if (IsBossWave(currentWave)) { /* spawn boss */ }
    if (IsFleetWave(currentWave)) { /* spawn fleet */ }
}
```

---

## 4. 傷害計算 (Enemy.CalculateDamageReductionResult)

```csharp
DamageReductionResult CalculateDamageReductionResult(double incomingDamage) {
    double damage = incomingDamage;
    
    // 絕對防禦：先扣固定值
    damage -= Main.Instance.defenseAbs;
    
    // 百分比防禦：再扣百分比
    damage *= (1 - Main.Instance.defenseRel);
    
    // 敵人護甲
    if (enemyArmor > 0) {
        // 護甲減免公式（armor / (armor + K)）
        damage *= 100.0 / (100.0 + enemyArmor);
    }
    
    // 破甲判定
    if (Random.value < renderArmorChance) {
        damage *= renderArmorMult; // 破甲後額外傷害
    }
    
    // impetus（穿甲）減少護甲效果
    // ...
    
    return new DamageReductionResult { finalDamage = damage };
}

void Enemy.TakeDamage(double damage, DamageSource source) {
    var result = CalculateDamageReductionResult(damage);
    enemyHealth -= result.finalDamage;
    
    // 觸發被動效果
    if (lifesteal > 0) {
        Main.Instance.towerHealth += result.finalDamage * lifesteal;
    }
    
    if (enemyHealth <= 0) {
        Die();
    }
}

void Enemy.Die() {
    Main.Instance.cash += enemyKillCash;
    Main.Instance.coins += enemyKillCoins;
    
    // Scatter 分裂
    if (enemyType == EnemyType.ScatterMain) {
        for (int i = 0; i < SCATTER_SPLITS; i++) {
            SpawnEnemy(EnemyType.ScatterChild, deathLocation);
        }
    }
    
    // DeathWave 每殺一隻 +0.0005 血量倍率
    Main.Instance.towerMaxHealthFromDeathWave += 
        Main.Instance.towerMaxHealth * DEATH_WAVE_HEALTH_MULTIPLIER_PER_KILL;
}
```

---

## 5. 升級成本 (GetUpgradeCostMultiplier)

```csharp
// upgradeType: 0=Attack, 1=Defense, 2=Utility, 3=Workshop, 4=Enhancement
// upgradeID: 升編號, level: 目前等級
double GetUpgradeCostMultiplier(UpgradeType type, int upgradeID, int level) {
    double multiplier = 1.0;
    
    // 依類型取得對應的倍率表
    UpgradeMultiplier[] table = type switch {
        UpgradeType.Attack => attackMultipliers,       // offset 0xd98
        UpgradeType.Defense => defenseMultipliers,      // offset 0xe08
        UpgradeType.Utility => utilityMultipliers,     // offset 0xe78
        UpgradeType.Workshop => workshopMultipliers,   // offset 0xed0
        UpgradeType.Enhancement => enhancementMultipliers, // offset 0xf10
        _ => null
    };
    
    if (table == null) return 1.0;
    
    // 遍歷倍率表，找到 <= level 的條目
    foreach (var entry in table) {
        if (entry.requiredLevel >= 1 && entry.requiredLevel <= level) {
            multiplier *= entry.multiplier;
        }
    }
    
    return multiplier;
}
// 最終成本 = baseCost * GetUpgradeCostMultiplier()
```

---

## 6. 塔防禦邏輯

```csharp
// 敵人到達塔牆時
void Enemy.ReachWall() {
    inWallMeleeAttackDistance = true;
    
    // 每秒對塔造成傷害
    float damagePerSecond = enemyDamage;
    
    // 荊棘反傷
    Main.Instance.thornDamage 對敵人造成傷害
    
    // 擊退判定
    if (Random.value < knockbackChance) {
        // 擊退敵人
    }
    
    // 牆存在時先打牆
    if (!isWallDestroyed) {
        wallHealth -= damagePerSecond;
        if (wallHealth <= 0) {
            isWallDestroyed = true;
            // 牆重建倒數
        }
    } else {
        towerHealth -= damagePerSecond;
        if (towerHealth <= 0) {
            // 免死判定
            if (Random.value < deathDefyChance) {
                towerHealth = towerMaxHealth * 0.1; // 免死回血
            } else {
                GameOver();
            }
        }
    }
}

void GameOver() {
    gameOverBool = true;
    // 計算本局獲得的 coins
    // 上傳成績到 PlayFab
    // 顯示結算畫面
}
```

---

## 7. 9 個終極武器邏輯

```csharp
// 每個終極武器有冷卻時間，擊殺敵人充能
UltimateWeaponID[] weapons = {
    ChainLightning,   // 連鎖閃電：在敵人之間跳躍
    SmartMissile,     // 智能導彈：自動追蹤最強敵人
    DeathWave,        // 死亡波：全屏傷害，每殺一隻+0.05%最大生命
    ChronoField,      // 時間力場：減速範圍內敵人
    InnerLandMines,   // 內圈地雷：定時爆炸
    GoldenTower,      // 黃金塔：金幣獲得提升
    PoisonSwamp,      // 毒沼澤：持續毒傷
    BlackHole,        // 黑洞：吸取敵人
    Spotlight         // 聚光燈：集中火力
};

// 充能機制：擊殺敵人獲得 UltimateCharge
// 集滿後可手動啟動或自動啟動
```

---

## 8. 卡牌系統 (Cards)

```csharp
void CalculateBenefits() {
    foreach (var card in equippedCards) {
        switch (card.id) {
            // 每張卡提供不同的數值修正
            // 例：+multishotTargets, +bounceChance, +orbCount 等
        }
    }
    // 重新計算所有戰鬥數值
}

// 卡牌稀有度
// Common: 基礎效果
// Rare: 較強效果
// Epic: 最強效果 + 特殊機制
// Mastery: 額外強化
```

---

## 9. 存檔結構 (SaveLoad.PlayerData)

```csharp
class PlayerData {
    // 貨幣
    double coins;
    int gems;
    int stones;
    int medals;
    long cells;
    int keys;
    
    // 永久升級
    bool[] upgradeTierUnlocked;      // 攻擊線
    bool[] upgradeDefenseTierUnlocked; // 防禦線
    bool[] upgradeUtilityTierUnlocked; // 實用線
    int[] highestWaveThisTier;
    
    // 卡牌
    int[] ownedCards;
    int[] cardLevels;
    
    // 模組
    Module[] ownedModules;
    
    // 設定
    bool hitTextEnabled;
    bool[] ultimateWeaponVFXEnabled;
    
    // 同步
    int saveRevision;
    int dataVersion;
}
// 存到 Firebase Firestore + PlayFab，有 HMAC 防篡改
```

---

## 10. 後端

- **PlayFab**：玩家資料、統計、排行榜
- **Firebase Firestore**：公會、聊天、郵件
- **Firebase Auth**：登入（Google/Guest）
- **AppsFlyer**：廣告歸因
- **Remote Config**：動態數值調整（Server-side balance patches）
```
