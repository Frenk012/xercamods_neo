package xerca.xercapaint;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side configuration for XercaPaint.
 * All options are authoritative on the server and synced to clients where relevant.
 */
public final class Config {
    public static final ModConfigSpec SPEC;

    // Feature 1: dye cost when painting
    public static final ModConfigSpec.BooleanValue DYE_COST_ENABLED;
    public static final ModConfigSpec.IntValue CHARGE_PER_DYE;
    public static final ModConfigSpec.IntValue MAX_CHARGE;

    // Feature 9: import permission
    public static final ModConfigSpec.BooleanValue IMPORT_ENABLED;
    public static final ModConfigSpec.BooleanValue IMPORT_REQUIRES_OP;
    public static final ModConfigSpec.IntValue IMPORT_OP_LEVEL;

    // Feature 3: villager trades
    public static final ModConfigSpec.BooleanValue VILLAGER_TRADES_ENABLED;

    private Config() {
    }

    // Safe accessors: config is not loaded yet during early world load, so fall back to defaults.
    public static boolean dyeCostEnabled() {
        return SPEC.isLoaded() && DYE_COST_ENABLED.get();
    }

    public static int chargePerDye() {
        return SPEC.isLoaded() ? CHARGE_PER_DYE.get() : 256;
    }

    public static int maxCharge() {
        return SPEC.isLoaded() ? MAX_CHARGE.get() : 4096;
    }

    public static boolean importEnabled() {
        return !SPEC.isLoaded() || IMPORT_ENABLED.get();
    }

    public static boolean importRequiresOp() {
        return SPEC.isLoaded() && IMPORT_REQUIRES_OP.get();
    }

    public static int importOpLevel() {
        return SPEC.isLoaded() ? IMPORT_OP_LEVEL.get() : 2;
    }

    public static boolean villagerTradesEnabled() {
        return !SPEC.isLoaded() || VILLAGER_TRADES_ENABLED.get();
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("painting");
        DYE_COST_ENABLED = builder
                .comment("If true, painting consumes paint charge stored in the palette. Refill the palette by crafting it with dyes.")
                .define("dyeCostEnabled", false);
        CHARGE_PER_DYE = builder
                .comment("Paint charge one dye adds to its OWN colour when crafted onto a palette (charge is tracked per basic colour).")
                .defineInRange("chargePerDye", 256, 1, 1_000_000);
        MAX_CHARGE = builder
                .comment("Maximum paint charge a palette can hold PER basic colour.")
                .defineInRange("maxCharge", 4096, 1, 1_000_000);
        builder.pop();

        builder.push("import");
        IMPORT_ENABLED = builder
                .comment("If false, the /paintimport command is disabled entirely.")
                .define("importEnabled", true);
        IMPORT_REQUIRES_OP = builder
                .comment("If true, only operators (see importOpLevel) may use /paintimport. Prevents arbitrary image injection on public servers.")
                .define("importRequiresOp", false);
        IMPORT_OP_LEVEL = builder
                .comment("Permission level required to import when importRequiresOp is true.")
                .defineInRange("importOpLevel", 2, 0, 4);
        builder.pop();

        builder.push("trades");
        VILLAGER_TRADES_ENABLED = builder
                .comment("If true, adds cartographer and wandering trader trades for canvases and paintings.")
                .define("villagerTradesEnabled", true);
        builder.pop();

        SPEC = builder.build();
    }
}
