// Ghidra headless script: decompile 6 key game functions
// Usage:
//   analyzeHeadless <proj> <name> -import libil2cpp.so \
//     -postScript DecompileKeyFuncs.java -scriptPath .
//@category IL2CPP

import ghidra.app.script.GhidraScript;
import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileResults;
import ghidra.program.model.listing.Function;
import ghidra.program.model.address.Address;
import java.io.PrintWriter;
import java.io.FileWriter;

public class DecompileKeyFuncs extends GhidraScript {
    @Override
    public void run() throws Exception {
        DecompInterface decomp = new DecompInterface();
        decomp.openProgram(currentProgram);

        long[] addresses = {
            0x1E8A994L,  // Main.TowerFireFunction
            0x1E840C8L,  // Main.DifficultyTierCalcs
            0x1E78874L,  // Main.GetUpgradeCostMultiplier
            0x21E9940L,  // Enemy.CalculateDamageReductionResult
            0x1EA08D0L,  // Enemy.SpawnEnemy
            0x1F0299CL,  // BattlePanelUI.StartNewRound
        };
        String[] names = {
            "TowerFireFunction",
            "DifficultyTierCalcs",
            "GetUpgradeCostMultiplier",
            "CalculateDamageReduction",
            "SpawnEnemy",
            "StartNewRound",
        };

        PrintWriter out = new PrintWriter(new FileWriter("/tmp/ghidra_decompiled.txt"));

        for (int i = 0; i < addresses.length; i++) {
            Address addr = toAddr(addresses[i]);
            Function func = getFunctionAt(addr);
            if (func == null) {
                createFunction(addr, names[i]);
                func = getFunctionAt(addr);
            }
            if (func == null) {
                out.println("=== " + names[i] + " @ " + addr + " === FAILED TO CREATE FUNCTION");
                continue;
            }
            out.println("=".repeat(60));
            out.println("=== " + names[i] + " @ " + addr + " ===");
            out.println("=".repeat(60));
            DecompileResults results = decomp.decompileFunction(func, 60, monitor);
            if (results.decompileCompleted()) {
                out.println(results.getDecompiledFunction().getC());
            } else {
                out.println("DECOMPILE FAILED: " + results.getErrorMessage());
            }
            out.println();
        }

        out.close();
        decomp.dispose();
        println("Done! Output at /tmp/ghidra_decompiled.txt");
    }
}
