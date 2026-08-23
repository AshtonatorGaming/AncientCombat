package com.weebtrazh.ancientcombat;

import net.minecraftforge.common.ForgeConfigSpec;

public class CompatConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue TINY_REACH_MULT;
    public static final ForgeConfigSpec.DoubleValue SMALL_REACH_MULT;
    public static final ForgeConfigSpec.DoubleValue TINY_HEIGHT;
    public static final ForgeConfigSpec.DoubleValue SMALL_HEIGHT;
    public static final ForgeConfigSpec.DoubleValue TALL_HEIGHT;
    public static final ForgeConfigSpec.DoubleValue WIDE_WIDTH;
    public static final ForgeConfigSpec.DoubleValue CLOSE_FACTOR_NORMAL;
    public static final ForgeConfigSpec.DoubleValue CLOSE_FACTOR_SMALL;
    public static final ForgeConfigSpec.DoubleValue CLOSE_FACTOR_TINY;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Size-based reach (height in blocks ≈ hitbox height)").push("reach");

        TINY_REACH_MULT = b
                .comment("Reach multiplier for tiny NPCs (gnomes). Lower if swings look too long.")
                .defineInRange("tinyReachMultiplier", 0.62D, 0.15D, 1.5D);

        SMALL_REACH_MULT = b
                .comment("Reach multiplier for small NPCs (hobbits / short folk).")
                .defineInRange("smallReachMultiplier", 0.78D, 0.15D, 1.5D);

        TINY_HEIGHT = b
                .comment("Height at or below this uses tinyReachMultiplier.")
                .defineInRange("tinyHeightThreshold", 1.15D, 0.4D, 2.0D);

        SMALL_HEIGHT = b
                .comment("Height at or below this (but above tiny) uses smallReachMultiplier.")
                .defineInRange("smallHeightThreshold", 1.55D, 0.5D, 2.5D);

        TALL_HEIGHT = b
                .comment("Height at or above this gets giant reach padding.")
                .defineInRange("tallHeightThreshold", 2.4D, 1.8D, 8.0D);

        WIDE_WIDTH = b
                .comment("Width at or above this also counts as large.")
                .defineInRange("wideWidthThreshold", 1.2D, 0.6D, 4.0D);

        CLOSE_FACTOR_NORMAL = b
                .comment("How close normals stop pathing (fraction of effective reach).")
                .defineInRange("closeFactorNormal", 0.85D, 0.5D, 1.0D);

        CLOSE_FACTOR_SMALL = b
                .defineInRange("closeFactorSmall", 0.88D, 0.5D, 1.0D);

        CLOSE_FACTOR_TINY = b
                .defineInRange("closeFactorTiny", 0.92D, 0.5D, 1.0D);

        b.pop();
        SPEC = b.build();
    }
}