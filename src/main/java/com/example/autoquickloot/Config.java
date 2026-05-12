package com.example.autoquickloot;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Toggle behaviour
    public static final ForgeConfigSpec.BooleanValue TOGGLE_ENABLED_BY_DEFAULT;

    // HUD indicator
    public static final ForgeConfigSpec.BooleanValue HUD_ENABLED;

    /**
     * Raw X pixel position of the HUD notice. -1 = automatic (bottom-right corner).
     * Players can set this via the in-game "Edit HUD Position" screen instead.
     */
    public static final ForgeConfigSpec.IntValue HUD_X;

    /**
     * Raw Y pixel position of the HUD notice. -1 = automatic (bottom-right corner).
     * Players can set this via the in-game "Edit HUD Position" screen instead.
     */
    public static final ForgeConfigSpec.IntValue HUD_Y;

    static {
        BUILDER.comment("Auto Quick Loot — Client Configuration").push("toggle");

        TOGGLE_ENABLED_BY_DEFAULT = BUILDER
                .comment("Start each game session with Auto Quick Loot toggled on.")
                .define("enabledByDefault", false);

        BUILDER.pop().push("hud");

        HUD_ENABLED = BUILDER
                .comment("Show the status indicator on-screen when Auto Quick Loot is toggled on.")
                .define("enabled", true);

        HUD_X = BUILDER
                .comment("Horizontal pixel position of the HUD notice.",
                         "Set to -1 to use the default position (bottom-right).",
                         "Tip: use the in-game 'Edit HUD Position' keybinding to drag it instead.")
                .defineInRange("x", -1, -1, 4096);

        HUD_Y = BUILDER
                .comment("Vertical pixel position of the HUD notice.",
                         "Set to -1 to use the default position (bottom-right).",
                         "Tip: use the in-game 'Edit HUD Position' keybinding to drag it instead.")
                .defineInRange("y", -1, -1, 4096);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "autoquickloot-client.toml");
    }
}
