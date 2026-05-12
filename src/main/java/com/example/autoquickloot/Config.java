package com.example.autoquickloot;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class Config {

    // -------------------------------------------------------------------------
    // Config spec builder
    // -------------------------------------------------------------------------
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // -------------------------------------------------------------------------
    // Toggle behaviour
    // -------------------------------------------------------------------------
    /** Whether Auto Quick Loot starts in the enabled (toggled-on) state each session. */
    public static final ForgeConfigSpec.BooleanValue TOGGLE_ENABLED_BY_DEFAULT;

    // -------------------------------------------------------------------------
    // HUD indicator
    // -------------------------------------------------------------------------
    /** Show the "Auto Quick Loot is enabled" HUD notice when toggled on. */
    public static final ForgeConfigSpec.BooleanValue HUD_ENABLED;

    /**
     * Screen anchor for the HUD notice.
     * Accepted values: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
     */
    public static final ForgeConfigSpec.ConfigValue<String> HUD_ANCHOR;

    /** Horizontal pixel offset from the chosen anchor edge. */
    public static final ForgeConfigSpec.IntValue HUD_OFFSET_X;

    /** Vertical pixel offset from the chosen anchor edge. */
    public static final ForgeConfigSpec.IntValue HUD_OFFSET_Y;

    // -------------------------------------------------------------------------
    // Static initialiser — build the spec
    // -------------------------------------------------------------------------
    static {
        BUILDER.comment("Auto Quick Loot — Client Configuration").push("toggle");

        TOGGLE_ENABLED_BY_DEFAULT = BUILDER
                .comment("Start each game session with Auto Quick Loot toggled on.")
                .define("enabledByDefault", false);

        BUILDER.pop().push("hud");

        HUD_ENABLED = BUILDER
                .comment("Show the status indicator on-screen when Auto Quick Loot is toggled on.")
                .define("enabled", true);

        HUD_ANCHOR = BUILDER
                .comment("Which corner of the screen to anchor the notice to.",
                         "Accepted values: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT")
                .define("anchor", "BOTTOM_RIGHT");

        HUD_OFFSET_X = BUILDER
                .comment("Horizontal pixel offset inward from the anchor corner.")
                .defineInRange("offsetX", 10, 0, 4096);

        HUD_OFFSET_Y = BUILDER
                .comment("Vertical pixel offset inward from the anchor corner.")
                .defineInRange("offsetY", 10, 0, 4096);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    // -------------------------------------------------------------------------
    // Registration helper — called from the main mod constructor
    // -------------------------------------------------------------------------
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "autoquickloot-client.toml");
    }

    // -------------------------------------------------------------------------
    // Runtime helpers
    // -------------------------------------------------------------------------

    /** Resolved anchor enum from the config string, falling back to BOTTOM_RIGHT. */
    public static HudAnchor getHudAnchor() {
        try {
            return HudAnchor.valueOf(HUD_ANCHOR.get().toUpperCase());
        } catch (IllegalArgumentException e) {
            return HudAnchor.BOTTOM_RIGHT;
        }
    }

    public enum HudAnchor {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
}
