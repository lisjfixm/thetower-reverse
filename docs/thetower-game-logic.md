# The Tower 完整遊戲邏輯分析

> 來源：IL2CPP dump (Unity 6000.3.15f1, metadata v39)
> 包名：com.TechTreeGames.TheTower
> 分析日期：2026-11-09

---

## 一、核心循環

```
開始新一輪 → 塔自動射擊 → 殺敵賺 cash → 升級塔（攻擊/防禦/實用）
→ 敵人隨波數變強 → 死亡 → 用 coins/gems 永久升級 → 再開新一輪
```

- **cash**：局內貨幣，死了歸零，用於局內升級
- **coins**：永久貨幣，用於永久升級
- **gems / stones / medals / keys / cells**：特殊/付費貨幣
- 最高 Tier：24 層
- 最少第 4 波才能 resume 上一局

---

## 二、戰鬥系統

### 塔的攻擊屬性（Main 類別）

| 屬性 | 欄位 | 說明 |
|------|------|------|
| 傷害 | `damage` (double) | 投射物基礎傷害 |
| 攻速 | `attackSpeed` (float) | 每秒攻擊次數 |
| 射程 | `maxDistance` (float) | 塔攻擊範圍 |
| 時域射程 | `chronoDistance` (float) | ChronoField 影響範圍 |
| 暴擊率 | `criticalChance` (float) | 0~1 |
| 暴擊倍率 | `criticalMult` (float) | 暴擊傷害倍率 |
| 穿甲 | `impetus` (float) | 護甲穿透 |
| 多重射擊率 | `multishotChance` (float) | 觸發多目標機率 |
| 多重射擊數 | `multishotTargets` (int) | 同時打幾個目標 |
| 速射率 | `rapidFireChance` (float) | 觸發速射機率 |
| 速射持續 | `rapidFireDuration` (float) | 速射持續秒數 |
| 連彈率 | `bounceChance` (float) | 彈跳機率 |
| 連彈數 | `bounceTargets` (int) | 彈跳次數 |
| 連彈範圍 | `bounceRange` (float) | 彈跳搜尋半徑 |
| 超暴擊率 | `superCritChance` (float) | 超級暴擊機率 |
| 超暴擊倍率 | `superCritMult` (float) | 超級暴擊傷害 |
| 破甲率 | `renderArmorChance` (float) | 破甲觸發機率 |
| 破甲倍率 | `renderArmorMult` (float) | 破甲後傷害倍率 |

### 防禦屬性

| 屬性 | 欄位 | 說明 |
|------|------|------|
| 塔血量 | `towerHealth` / `towerMaxHealth` (double) | |
| 生命回復 | `towerHealthRegen` (double) | 每秒回復 |
| 絕對防禦 | `defenseAbs` (double) | 固定減傷 |
| 百分比防禦 | `defenseRel` (double) | 百分比減傷 |
| 荊棘傷害 | `thornDamage` (float) | 近戰敵人反傷 |
| 吸血 | `lifesteal` (float) | 傷害回血比例 |
| 擊退率 | `knockbackChance` (float) | |
| 擊退力 | `knockbackForce` (float) | |
| 軌道彈速 | `orbSpeed` (float) | |
| 軌道彈數 | `orbCount` (int) | |
| 地雷率 | `mineChance` (float) | |
| 地雷傷害 | `mineDamage` (double) | |
| 地雷半徑 | `mineRadius` (float) | |
| 免死率 | `deathDefyChance` (float) | 血量歸零時免死 |
| 牆血量 | `wallHealth` / `wallMaxHealth` (double) | |
| 牆重建 | `wallRebuild` (double) | 每秒修復 |

### 實用屬性

| 屬性 | 欄位 | 說明 |
|------|------|------|
| 金幣加成 | `cashBonusUpgrade` (double) | 局內金幣加成 % |
| 每波金幣 | `cashPerWave` (double) | |
| 鑽石加成 | `coinsBonusUpgrade` (double) | |
| 每波鑽石 | `coinsPerWave` (double) | |
| 免費攻擊升級率 | `freeAttackUpgradeChance` (float) | |
| 免費防禦升級率 | `freeDefenseUpgradeChance` (float) | |
| 免費實用升級率 | `freeUtilityUpgradeChance` (float) | |
| 每波利息 | `interestPerWave` (float) | |
| 恢復包量 | `recoveryAmount` (double) | |
| 恢復包上限 | `recoveryMax` (double) | |
| 恢復觸發率 | `recoveryChance` (float) | |
| 敵人攻擊跳過率 | `enemyAttackLevelSkipChance` (float) | |
| 敵人血量跳過率 | `enemyHealthLevelSkipChance` (float) | |

---

## 三、9 個終極武器（Ultimate Weapons）

| ID | 名稱 | 說明 |
|----|------|------|
| 0 | ChainLightning | 連鎖閃電 |
| 1 | SmartMissile | 智能導彈 |
| 2 | DeathWave | 死亡波 |
| 3 | ChronoField | 時間減速力場 |
| 4 | InnerLandMines | 內圈地雷 |
| 5 | GoldenTower | 黃金塔（加傷） |
| 6 | PoisonSwamp | 毒沼澤 |
| 7 | BlackHole | 黑洞 |
| 8 | Spotlight | 聚光燈 |

每個終極武器有 Plus 版本（`UltimateWeaponPlusStats`）。

---

## 四、13 種敵人

| 類型 | Enum 值 | 尺寸 | 特色 |
|------|---------|------|------|
| Basic | 0 | 0.222 | 基本 |
| Fast | 1 | 0.222 | 快速 |
| Tank | 2 | 0.4 | 高血量 |
| Boss | 3 | 0.76 | Boss |
| Ranged | 4 | 0.222 | 遠程攻擊塔 |
| Protector | 5 | 0.222 | 護盾保護周圍 |
| Vampire | 6 | 0.55 | 吸血 |
| ScatterMain | 7 | 0.55 | 死亡分裂 |
| ScatterChild | 8 | - | 分裂小怪 |
| Ray | 9 | 0.55 | 雷射迷你 Boss |
| Saboteur | 10 | 0.55 | 干擾塔 |
| Commander | 11 | 0.55 | 光環增益 |
| Overcharge | 12 | 0.55 | 蓄力爆發 |

### 敵人關鍵數值

- **Scatter 分裂數**：4 隻（Ultimate 模式 5 隻）
- **Ray 射擊冷卻**：30 秒
- **近戰攻擊距離**：0.3
- **牆附近速度限制**：4.5
- **Saboteur**：攻擊間隔從 5 秒隨波數縮短至 1 秒（4000 波達到上限）
- **Commander**：周圍敵人血量/質量/速度 ×1.15，脈衝持續 0.5 秒，間隔 10 秒→2 秒
- **Overcharge**：蓄力 3 秒，攻擊間隔 15 秒→1 秒，攻擊指數 1.4
- **SPEED_ADJUSTER**：0.41（速度調整係數）
- **MISS_DECAY**：0.875（閃避衰減）
- **STUN_DECAY**：0.995（眩暈衰減）
- **DEATH_DEFY_DECAY**：0.9（免死衰減）
- **Pulsar Harvester**：100~400 遞減回報，常數 1500，最多 399 次

---

## 五、21 種傷害來源

```
Projectile, Cannon, Orb, DeathRay, BlackHole, FlameBot, Swamp,
LandMine, InnerLandMine, SmartMissile, ChainLightning, ChainLightningPlus,
DeathWave, Thorn, SecondWindBlast, Nuke, EnergyNet, GuardianAttack,
Impact, Ally, Electron
```

---

## 六、卡牌系統

- 稀有度：Common(0) / Rare(1) / Epic(2)
- 卡牌可裝備在 slots，有 Mastery（精通）系統
- 多個 Preset（預設配置）可切換
- 卡牌效果影響攻擊/防禦/實用各方面

---

## 七、模組系統（Modules）

- 稀有度：Common / Rare / Epic
- 有主效果、副屬性、獨特效果
- 可 Shatter（分解）成 shards
- 有 Assistant Module（輔助模組）系統
- 模組 shard 類型：Armor, Cannon, Core, Generator

---

## 八、永久升級樹（Vault）

### 攻擊線（1-110）
Damage, AttackSpeed, CritChance, CritFactor, DamagePerMeter, MultishotChance,
RapidFireChance, BounceShotChance, SuperCritChance, SuperCritMultiplier,
RendArmorChance, RendArmorMultiplier

### 防禦線（200-330）
Health, HealthRegen, DefensePercent, DefenseAbsolute, ThornDamage,
KnockbackChance, KnockbackForce, OrbSpeed, OrbsCount, ShockwaveSize,
ShockwaveFrequency, DeathDefyChance, WallHealth, WallRebuild

### 經濟線（400-510）
Cash, CashPerWave, CoinsPerKill, CoinsPerWave, FreeAttackUpgrade,
FreeDefenseUpgrade, FreeUtilityUpgrade, InterestPerWave, RecoveryAmount,
MaxRecovery, EnemyAttackSkip, EnemyHealthSkip

### 終極武器線（600-680）
UltimateDamage, ChainLightningDamage, SmartMissileDamage, DeathWaveDamage,
InnerLandMinesDamage, GoldenTowerBonus, PoisonSwampDamage, SpotlightBonus,
BlackHoleSize

### QoL（1000+）
AutoRestartRun, AdGemsStacking

---

## 九、難度曲線

- 每波有 `currentWaveBaseHealth`、`currentWaveBaseDamage`、`currentWaveBaseKillCash`
- `DifficultyTierCalcs()` 計算 Tier，TierUp() 提升層級
- 最高 24 Tier
- 每波有 `waveLengthSeconds`（波長）和 `waveCooldownSeconds`（波間冷卻）
- `enemySpeedWaveMult`、`enemyMassWaveMult` 隨波數調整
- 有 `enemyHealthAdjust`、`enemyAttackAdjust` 微調
- 迷你 Boss 在特定波數出現（Vampire/Scatter/Ray）
- Boss Wave、Fleet Wave、Elite Wave 交替出現
- DeathWave 每殺一隻敵人 +0.0005 血量倍率

---

## 十、遊戲加速與特殊模式

- `gameSpeed` / `gameMaxSpeed`：遊戲速度倍率
- Demon Mode：上限 10 波
- Second Wind：上限 2 波
- Viking Funeral：上限 10 波
- Energy Shield：需 2 波充能
- Wall：需 2 波充能
- Auto Retry Timer：100
- 最少第 4 波才能 Resume

---

## 十一、其他系統

| 系統 | 類別 | 說明 |
|------|------|------|
| Tech Tree | TechTreeManager | 永久解鎖樹 |
| Relics | RelicsManager | 被動遺物 |
| Perks | Perks | 局內詞條 |
| Lab | Lab / LabTimeManager | 閒置升級 |
| Guild | GuildManager | 公會、聊天、貢獻 |
| Mail | MailManager | 系統信件 |
| Tournament | TournamentManager | 週末競賽排行榜 |
| DailyMission | DailyMissionManager | 每日任務 |
| Bot | Bots | AI 對手 |
| Guardian | GuardianPresetData | 守護者系統 |
| Dissonance | DissonanceManager | 諧音/不諧音增益 |
| Chat | ChatUI | 公會聊天 |
| Milestone | MilestoneManager | 里程碑 |
| Shop | StoreOrganizer / SalesCatalog | 商店、特賣 |

---

## 十二、技術棧

- Unity 6000.3.15f1 (IL2CPP, ARM64)
- 後端：PlayFab + Firebase Firestore
- 廣告：LevelPlay (ironSource) + Tapjoy + AdMob
- 分析：AppsFlyer + Firebase Analytics
- 動畫：Spine（2D骨骼動畫）
- UI：LogicUI + SuperScrollView
- 本地化：I2 Localization（支援多國語言）
- 音訊：20 個 WAV 背景音樂（每關卡 Main + Menu 版）
