# The Tower — 逆向工程完整檔案庫

Unity IL2CPP 塔防遊戲 **The Tower** 的完整逆向工程產物。
遊戲版本：Unity 6000.3.15f1，metadata v39，arm64-v8a。

---

## 目錄結構

```
docs/                    遊戲邏輯與程式碼邏輯分析
  thetower-game-logic.md     全部系統、數值、表格
  thetower-code-logic.md     10 個核心系統的程式碼邏輯（三方交叉比對）
ghidra/
  thetower-ghidra-decompiled.txt        Ghidra 首次反編譯（無符號）
  thetower-ghidra-named-decompiled.txt  Ghidra 命名 17 個關鍵函數後的真正 C 偽代碼
dump/
  dump.cs                  29MB，836K 行完整類別/方法/欄位 + RVA（Il2CppDumper）
  il2cpp.h                 77MB，完整結構定義
  script.json              79MB，189,727 個方法地址 + 簽名
  stringliteral.json       所有字串常量
  dummy_dll/               127 個 DummyDll（含 Assembly-CSharp.dll）
metadata/
  global-metadata.dat      IL2CPP metadata v39（12MB）
  manifest.json            原始 APK manifest
scripts/
  RenameAndDecompile.java  Ghidra headless 腳本（批次命名 + 反編譯）
  DecompileKeyFuncs.java   Ghidra headless 腳本（6 個關鍵函數）
  ida_with_struct.py       IDA 載入 il2cpp.h 結構腳本
assets_index/             全部素材清單（檔名 + 大小）
  textures.txt  sprites.txt  audio.txt  textassets.txt
```

## 大型二進位檔（Release 附件）

GitHub 單檔上限 100MB，以下放在 **Releases**：

| 檔案 | 大小 | 說明 |
|---|---|---|
| `thetower-full.xapk` | 222MB | 完整 XAPK（含 split APK） |
| `thetower.apk` | 169MB | 原始 split APK base |
| `libil2cpp.so` | 73MB | arm64-v8a 主程式（未 strip） |
| `thetower-assets.zip` | 630MB | 全部 Unity 素材（1517 貼圖 / 1472 sprite / 20 音訊 / 93 TextAsset） |

## 關鍵位址

```
CodeRegistration      0x43E88F0
MetadataRegistration  0x455E560
```

| 函數 | RVA |
|---|---|
| Main.TowerFireFunction | 0x1E8A994 |
| Main.FixedUpdate | 0x1E8EC18 |
| Main.Update | 0x1E8EED8 |
| Main.CalculateEnemyDistances | 0x1E8E984 |
| Main.OrbFire | 0x1E8E080 |
| Main.GetUpgradeCostMultiplier | 0x1E78874 |
| Main.UpdateTimers | 0x1E87F00 |
| Main.GetEnemyTargetIndex | 0x1E8A974 |
| Enemy.CalculateDamageReductionResult | 0x21E9940 |
| Enemy.ApplyDamageReductionStats | 0x21E9D14 |
| Enemy.Attack | 0x21E67B0 |
| Enemy.ApplyProjectileDamage | 0x21EACC4 |
| Enemy.TotalLightshotDamage | 0x21EDEF0 |
| Enemy.OrbDamage | 0x21EF1E4 |
| Enemy.ThornDamage | 0x21E9EA4 |
| BattlePanelUI.StartNewRound | 0x1F0299C |

## 工具鏈與版本

- **Il2CppDumper**：AndnixSH fork（原版 v6.7.40 不支援 metadata v39），target net8.0
  - 直接餵 XAPK 才能 auto-detect 成功；餵 .so + metadata 會得到 CodeRegistration=0
- **Ghidra 12.2 PUBLIC**：需要 **JDK 25**（class file version 69，JDK 21/23/24 全被拒）
- **UnityPy**：素材提取
- 已驗證失敗：Rust il2cpp_dumper v8.5.0（對此 binary 必 stack overflow）

## 反編譯註記

Ghidra 反編譯品質說明：
- `Enemy.Attack` 完整還原（含 22 種敵人類型的傷害加成 switch）
- `CalculateDamageReductionResult`、`ThornDamage` 被誤判為 Unity List/Dictionary 操作（函數邊界偏移）
- `TowerFireFunction`、`FixedUpdate`、`GetUpgradeCostMultiplier` 被誤判為空 stub
- `DifficultyTierCalcs` 位址指向 pthread mutex 代碼

要拿到完整可讀 C#，需在 Ghidra 載入 `script.json` 做全量符號標註（189,727 個方法）。

## 免責

僅供個人研究與學習。遊戲素材與程式版權屬原作者。
