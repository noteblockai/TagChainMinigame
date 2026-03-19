package com.example.tagchain;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class GlowManager {
    private JavaPlugin plugin;
    private Map<Player, Boolean> glowStates = new HashMap<>();
    private int glowCycleTaskId = -1;

    public GlowManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startGlowCycle(HashMap<Player, Player> targetMap, JavaPlugin plugin) {
        int glowShowDuration = plugin.getConfig().getInt("game-settings.glow-show-duration", 3);
        int glowInterval = plugin.getConfig().getInt("game-settings.glow-interval", 60);

        glowCycleTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            // Show glow
            showGlow(targetMap);

            // Remove glow after 3 seconds
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, 
                () -> removeGlow(targetMap), 
                glowShowDuration * 20L);
        }, 0, glowInterval * 20L);
    }

    public void showGlow(HashMap<Player, Player> targetMap) {
        for (Map.Entry<Player, Player> entry : targetMap.entrySet()) {
            Player viewer = entry.getKey();
            Player target = entry.getValue();

            if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline()) {
                continue;
            }

            setGlowing(viewer, target, true);
        }
    }

    public void removeGlow(HashMap<Player, Player> targetMap) {
        for (Map.Entry<Player, Player> entry : targetMap.entrySet()) {
            Player viewer = entry.getKey();
            Player target = entry.getValue();

            if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline()) {
                continue;
            }

            setGlowing(viewer, target, false);
        }
    }

    public void removeGlow(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        // Remove glow effect using PotionEffect (fallback without ProtocolLib)
        // This is a simplified version - ProtocolLib would be needed for true per-player glow
        try {
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.GLOWING);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not remove glow for " + player.getName());
        }
    }

    public void removeAllGlows() {
        if (glowCycleTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(glowCycleTaskId);
            glowCycleTaskId = -1;
        }
    }

    private void setGlowing(Player viewer, Player target, boolean glowing) {
        // Note: This is simplified without ProtocolLib
        // Full implementation requires ProtocolLib for per-player packet manipulation
        // For now, this applies global glow to target player
        try {
            if (glowing) {
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false
                ));
            } else {
                target.removePotionEffect(org.bukkit.potion.PotionEffectType.GLOWING);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not set glow for player");
        }
    }
}

