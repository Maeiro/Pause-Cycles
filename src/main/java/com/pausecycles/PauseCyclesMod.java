package com.pausecycles;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Mod(PauseCyclesMod.MOD_ID)
public class PauseCyclesMod {
    public static final String MOD_ID = "pausecycles";
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Set<String> unavailableRulesLogged = new HashSet<>();
    private final List<RuleToggle> configuredRules = new ArrayList<>();
    private Boolean lastPlayersOnline = null;
    private int tickCounter = 0;

    public PauseCyclesMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PauseCyclesConfig.SPEC, "pausecycles-server.toml");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfigLoading);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfigReloading);
        MinecraftForge.EVENT_BUS.register(this);
        refreshRulesFromConfig();
        LOGGER.info("[Pause Cycles] Loaded.");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        resetState();
        updateRulesForCurrentPlayerState(event.getServer(), "server_started");
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        resetState();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tickCounter++;
        if (tickCounter < PauseCyclesConfig.CHECK_INTERVAL_TICKS.get()) {
            return;
        }
        tickCounter = 0;
        updateRulesForCurrentPlayerState(event.getServer(), "periodic_check");
    }

    private void updateRulesForCurrentPlayerState(MinecraftServer server, String reason) {
        if (configuredRules.isEmpty()) {
            return;
        }
        boolean hasPlayersOnline = server.getPlayerCount() > 0;
        if (lastPlayersOnline != null && lastPlayersOnline == hasPlayersOnline) {
            return;
        }
        lastPlayersOnline = hasPlayersOnline;

        String targetStateLabel = hasPlayersOnline ? "online values" : "offline values";
        LOGGER.info(
                "[Pause Cycles] State change ({}): playersOnline={} -> applying {} for {} gamerules.",
                reason,
                hasPlayersOnline,
                targetStateLabel,
                configuredRules.size()
        );

        for (RuleToggle rule : configuredRules) {
            applyRule(server, rule.ruleName(), hasPlayersOnline ? rule.onlineValue() : rule.offlineValue());
        }
    }

    private void applyRule(MinecraftServer server, String ruleName, String value) {
        String command = "gamerule " + ruleName + " " + value;
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4);

        try {
            server.getCommands().getDispatcher().execute(command, source);
            LOGGER.info("[Pause Cycles] Applied {}={}.", ruleName, value);
        } catch (CommandSyntaxException ex) {
            // Typical case: rule does not exist (e.g., doSeasonCycle without a seasons mod).
            if (unavailableRulesLogged.add(ruleName)) {
                LOGGER.warn("[Pause Cycles] Gamerule '{}' is not available. Skipping it.", ruleName);
            }
        } catch (Exception ex) {
            LOGGER.error("[Pause Cycles] Failed to apply {}={}.", ruleName, value, ex);
        }
    }

    private void resetState() {
        lastPlayersOnline = null;
        tickCounter = 0;
    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == PauseCyclesConfig.SPEC) {
            refreshRulesFromConfig();
        }
    }

    private void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == PauseCyclesConfig.SPEC) {
            refreshRulesFromConfig();
        }
    }

    private void refreshRulesFromConfig() {
        configuredRules.clear();
        unavailableRulesLogged.clear();

        for (String raw : PauseCyclesConfig.RULES.get()) {
            RuleToggle parsed = parseRule(raw);
            if (parsed == null) {
                LOGGER.warn("[Pause Cycles] Ignoring invalid rule entry: '{}'. Expected format gamerule|online|offline", raw);
                continue;
            }
            configuredRules.add(parsed);
        }

        LOGGER.info(
                "[Pause Cycles] Config loaded: checkIntervalTicks={}, configuredRules={}.",
                PauseCyclesConfig.CHECK_INTERVAL_TICKS.get(),
                configuredRules.size()
        );
    }

    private RuleToggle parseRule(String input) {
        if (input == null) {
            return null;
        }
        String[] parts = input.split("\\|", -1);
        if (parts.length != 3) {
            return null;
        }

        String ruleName = parts[0].trim();
        String onlineValue = normalizeBooleanString(parts[1]);
        String offlineValue = normalizeBooleanString(parts[2]);

        if (ruleName.isEmpty() || onlineValue == null || offlineValue == null) {
            return null;
        }
        return new RuleToggle(ruleName, onlineValue, offlineValue);
    }

    private String normalizeBooleanString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "false".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private record RuleToggle(String ruleName, String onlineValue, String offlineValue) {
    }
}
