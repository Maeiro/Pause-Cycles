package com.pausecycles;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class PauseCyclesConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RULES;
    public static final ForgeConfigSpec.IntValue CHECK_INTERVAL_TICKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("pausecycles");

        CHECK_INTERVAL_TICKS = builder
                .comment("How often player state is checked. 20 ticks = 1 second.")
                .defineInRange("checkIntervalTicks", 20, 1, 20 * 60);

        RULES = builder
                .comment(
                        "Rules to toggle using format: gamerule|onlineValue|offlineValue",
                        "Values are usually true/false for boolean gamerules.",
                        "Examples:",
                        "doDaylightCycle|true|false",
                        "doSeasonCycle|true|false"
                )
                .defineListAllowEmpty(
                        List.of("rules"),
                        () -> List.of(
                                "doDaylightCycle|true|false",
                                "doSeasonCycle|true|false"
                        ),
                        value -> value instanceof String
                );

        builder.pop();
        SPEC = builder.build();
    }

    private PauseCyclesConfig() {
    }
}
