package com.example.tagchain;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class GameEventListener implements Listener {
    private GameManager gameManager;

    public GameEventListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        // Check if both are players
        if (!(damager instanceof Player) || !(victim instanceof Player)) {
            return;
        }

        Player attacker = (Player) damager;
        Player targetPlayer = (Player) victim;

        // Only allow during RUNNING phase
        if (gameManager.getGameState() != GameState.RUNNING) {
            event.setCancelled(true);
            return;
        }

        // Prevent spectators from attacking
        if (gameManager.isSpectator(attacker.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Handle catch/elimination
        gameManager.handlePlayerHit(attacker, targetPlayer);

        // Cancel the damage event (don't let them actually die)
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (gameManager.getGameState() == GameState.WAITING) {
            return;
        }

        // Eliminate player
        gameManager.eliminatePlayer(player);

        String msg = "§6[Tag Chain] §c" + player.getName() + " disconnected and was eliminated!";
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
        }
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();

        // Check if player is spectator
        if (gameManager.isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
            String msg = "§6[Tag Chain] §cSpectators cannot chat during the game.";
            player.sendMessage(msg);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check both hardcoded blacklist and config blacklist
        if (TagChainPlugin.HARDCODED_BLACKLIST.contains(player.getUniqueId().toString()) || 
            gameManager.isBlacklisted(player.getUniqueId())) {
            
            player.kickPlayer("§c[Tag Chain] 당신은 이 게임에 접근할 수 없습니다!");
        }
    }
}
