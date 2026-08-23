package com.weebtrazh.ancientcombat;

import net.minecraftforge.common.ForgeConfigSpec;

public class CompatConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_BC_MELEE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BC_CLOSER;
    public static final ForgeConfigSpec.BooleanValue HOLD_RANGE_FOR_THROWABLES;
    public static final ForgeConfigSpec.BooleanValue BC_MELEE_ON_THROWABLE_IN_RANGE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_POSE_TICK;
    public static final ForgeConfigSpec.IntValue POSE_TICK_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue POSE_SCAN_RADIUS;

    public static final ForgeConfigSpec.DoubleValue BODY_TURN_SPEED;
    public static final ForgeConfigSpec.DoubleValue MAX_YAW_ERROR;
    public static final ForgeConfigSpec.DoubleValue MAX_YAW_ERROR_CLOSE;
    public static final ForgeConfigSpec.IntValue FORCE_SWING_TICKS;

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

        b.comment("Master switches — Ancient Combat is independent of Ancient Tactics").push("general");
        ENABLE_BC_MELEE = b
                .comment("Replace AW melee doAttack with Better Combat upswing when weapon has BC attrs.")
                .define("enableBcMelee", true);
        ENABLE_BC_CLOSER = b
                .comment("Override shouldCloseOnTarget using BC attack range.")
                .define("enableBcCloser", true);
        HOLD_RANGE_FOR_THROWABLES = b
                .comment("Do not path-in while main hand is a Spartan throwing weapon with ammo. BC poke still allowed if already in range.")
                .define("holdRangeForThrowables", true);
        BC_MELEE_ON_THROWABLE_IN_RANGE = b
                .comment("If already in melee with a javelin/tomahawk/etc., use BC swings.")
                .define("bcMeleeOnThrowableInRange", true);
        ENABLE_POSE_TICK = b
                .comment("Client: tick BC pose updates on nearby AW NPCs.")
                .define("enablePoseTick", true);
        POSE_TICK_INTERVAL = b
                .comment("Client ticks between pose scans.")
                .defineInRange("poseTickInterval", 4, 1, 40);
        POSE_SCAN_RADIUS = b
                .comment("Only update poses for NPCs within this distance of the player.")
                .defineInRange("poseScanRadius", 48.0D, 16.0D, 128.0D);
        b.pop();

        b.comment("Body facing for BC hitboxes (prevents swinging into empty air)").push("facing");
        BODY_TURN_SPEED = b
                .comment("Max degrees of body yaw change per attack tick.")
                .defineInRange("bodyTurnSpeed", 24.0D, 4.0D, 90.0D);
        MAX_YAW_ERROR = b
                .comment("Max body yaw error (degrees) before allowing a swing at normal range.")
                .defineInRange("maxYawError", 35.0D, 5.0D, 90.0D);
        MAX_YAW_ERROR_CLOSE = b
                .comment("Max body yaw error when already very close.")
                .defineInRange("maxYawErrorClose", 55.0D, 10.0D, 120.0D);
        FORCE_SWING_TICKS = b
                .comment("If in range this many ticks without a valid facing, swing anyway (0 = never force).")
                .defineInRange("forceSwingTicks", 12, 0, 60);
        b.pop();

        b.comment("Size-based reach").push("reach");
        TINY_REACH_MULT = b.defineInRange("tinyReachMultiplier", 0.62D, 0.15D, 1.5D);
        SMALL_REACH_MULT = b.defineInRange("smallReachMultiplier", 0.78D, 0.15D, 1.5D);
        TINY_HEIGHT = b.defineInRange("tinyHeightThreshold", 1.15D, 0.4D, 2.0D);
        SMALL_HEIGHT = b.defineInRange("smallHeightThreshold", 1.55D, 0.5D, 2.5D);
        TALL_HEIGHT = b.defineInRange("tallHeightThreshold", 2.4D, 1.8D, 8.0D);
        WIDE_WIDTH = b.defineInRange("wideWidthThreshold", 1.2D, 0.6D, 4.0D);
        CLOSE_FACTOR_NORMAL = b.defineInRange("closeFactorNormal", 0.85D, 0.5D, 1.0D);
        CLOSE_FACTOR_SMALL = b.defineInRange("closeFactorSmall", 0.88D, 0.5D, 1.0D);
        CLOSE_FACTOR_TINY = b.defineInRange("closeFactorTiny", 0.92D, 0.5D, 1.0D);
        b.pop();

        SPEC = b.build();
    }
}