// Ghidra headless: rename key IL2CPP methods and decompile
//@category IL2CPP

import ghidra.app.script.GhidraScript;
import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileResults;
import ghidra.program.model.listing.Function;
import ghidra.program.model.address.Address;
import ghidra.program.model.symbol.SourceType;
import java.io.PrintWriter;
import java.io.FileWriter;

public class RenameAndDecompile extends GhidraScript {
    @Override
    public void run() throws Exception {
        PrintWriter out = new PrintWriter(new FileWriter("/tmp/ghidra_named_decompiled.txt"));
        
        long[][] addrs = {
            {0x1E8A994L, 0}, {0x1E8EC18L, 1}, {0x1E8EED8L, 2},
            {0x1E8E984L, 3}, {0x1E8E080L, 4}, {0x1E78874L, 5},
            {0x1E87F00L, 6}, {0x1E8A974L, 7}, {0x21E9940L, 8},
            {0x21E9D14L, 9}, {0x21E67B0L, 10}, {0x21EACC4L, 11},
            {0x21EDEF0L, 12}, {0x21EF1E4L, 13}, {0x21E9EA4L, 14},
            {0x1F0299CL, 15}, {0x1E840C8L, 16}
        };
        String[] names = {
            "Main_TowerFireFunction", "Main_FixedUpdate", "Main_Update",
            "Main_CalculateEnemyDistances", "Main_OrbFire", "Main_GetUpgradeCostMultiplier",
            "Main_UpdateTimers", "Main_GetEnemyTargetIndex", "Enemy_CalculateDamageReductionResult",
            "Enemy_ApplyDamageReductionStats", "Enemy_Attack", "Enemy_ApplyProjectileDamage",
            "Enemy_TotalLightshotDamage", "Enemy_OrbDamage", "Enemy_ThornDamage",
            "BattlePanelUI_StartNewRound", "Main_DifficultyTierCalcs"
        };
        
        for (int i = 0; i < addrs.length; i++) {
            try {
                Address addr = toAddr(addrs[i][0]);
                Function func = getFunctionAt(addr);
                if (func == null) {
                    func = createFunction(addr, names[i]);
                }
                if (func != null) {
                    func.setName(names[i], SourceType.USER_DEFINED);
                    out.println("Renamed: " + names[i]);
                }
            } catch (Exception e) {
                out.println("Fail: " + names[i] + " " + e.getMessage());
            }
        }
        
        DecompInterface decomp = new DecompInterface();
        decomp.openProgram(currentProgram);
        
        for (int i = 0; i < addrs.length; i++) {
            try {
                Address addr = toAddr(addrs[i][0]);
                Function func = getFunctionAt(addr);
                if (func != null) {
                    out.println("");
                    out.println("=".repeat(60));
                    out.println("=== " + names[i] + " @ " + addr + " ===");
                    out.println("=".repeat(60));
                    DecompileResults res = decomp.decompileFunction(func, 120, monitor);
                    if (res.decompileCompleted()) {
                        out.println(res.getDecompiledFunction().getC());
                    } else {
                        out.println("FAIL: " + res.getErrorMessage());
                    }
                }
            } catch (Exception e) {
                out.println("ERROR: " + names[i] + " " + e.getMessage());
            }
        }
        
        out.close();
        decomp.dispose();
        println("Done!");
    }
}
