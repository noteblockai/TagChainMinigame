package com.example.tagchain;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class GameManager {
    private JavaPlugin plugin;
    private GlowManager glowManager;
    private GameState gameState = GameState.WAITING;
    private HashMap<Player, Player> targetMap = new HashMap<>();
    private Set<UUID> initialParticipants = new HashSet<>();
    private Set<UUID> spectators = new HashSet<>();
    private int glowTaskId = -1;
    private int prepTaskId = -1;

    public GameManager(JavaPlugin plugin, GlowManager glowManager) {
        this.plugin = plugin;
        this.glowManager = glowManager;
    }

    public void startGame() {
        if (gameState != GameState.WAITING) {
            return;
        }

        // Phase 1: Initialization
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        // Remove blacklisted players
        players.removeIf(player -> isBlacklisted(player.getUniqueId()));
        
        if (players.isEmpty()) {
            return;
        }

        // Save initial participants
        for (Player player : players) {
            initialParticipants.add(player.getUniqueId());
        }

        // Teleport to game location
        Location gameLoc = loadGameLocation();
        for (Player player : players) {
            player.teleport(gameLoc);
        }

        // Assign targets in circular chain
        assignTargetsCircular(players);

        // Set state to PREPARING
        gameState = GameState.PREPARING;

        // Broadcast message
        String msg = plugin.getConfig().getString("messages.preparation-start", "§6[Tag Chain] §aPreparation phase started!");
        for (Player player : players) {
            player.sendMessage(msg);
        }

        // Start 2-minute preparation timer
        int prepDuration = plugin.getConfig().getInt("game-settings.preparation-phase-duration", 120);
        prepTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::startMainGame, prepDuration * 20L);
    }

    private void assignTargetsCircular(List<Player> players) {
        // Shuffle to randomize
        Collections.shuffle(players);

        // Assign targets in circular chain
        for (int i = 0; i < players.size(); i++) {
            Player hunter = players.get(i);
            Player target = players.get((i + 1) % players.size());
            targetMap.put(hunter, target);
        }
    }

    private void startMainGame() {
        // Check offline players
        List<Player> offlinePlayers = new ArrayList<>();
        for (UUID uuid : initialParticipants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                offlinePlayers.add(Bukkit.getOfflinePlayer(uuid).getPlayer());
            }
        }

        // Eliminate offline players
        for (Player player : offlinePlayers) {
            if (player != null) {
                eliminatePlayer(player);
            }
        }

        // Check win condition (only 2 players left)
        if (getActivePlayers().size() <= 2) {
            endGame();
            return;
        }

        // Set state to RUNNING
        gameState = GameState.RUNNING;

        // Broadcast game start
        String gameStartMsg = plugin.getConfig().getString("messages.game-start", "§6[Tag Chain] §aGame started!");
        String creatorMsg = "§2[에러는 디스코드로문의] §6제작자: mineharuhi";
        
        for (Player player : getActivePlayers()) {
            player.sendMessage(gameStartMsg);
            player.sendMessage(creatorMsg);
        }

        // Start glow cycle
        glowManager.startGlowCycle(targetMap, plugin);
    }

    public void eliminatePlayer(Player player) {
        if (player == null) return;

        UUID playerUUID = player.getUniqueId();

        // Find who was targeting this player
        Player hunter = null;
        for (Map.Entry<Player, Player> entry : targetMap.entrySet()) {
            if (entry.getValue().equals(player)) {
                hunter = entry.getKey();
                break;
            }
        }

        // Reassign target
        if (hunter != null) {
            Player nextTarget = targetMap.get(player);
            if (nextTarget != null && !nextTarget.equals(hunter)) {
                targetMap.put(hunter, nextTarget);
            }
        }

        // Remove from target map
        targetMap.remove(player);

        // Convert to spectator
        convertToSpectator(player);

        // Broadcast elimination
        String msg = plugin.getConfig().getString("messages.player-eliminated", "§6[Tag Chain] §c{player} was eliminated!")
                .replace("{player}", player.getName());
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
        }

        // Check win condition
        if (getActivePlayers().size() <= 2) {
            endGame();
        }
    }

    public void convertToSpectator(Player player) {
        if (player == null) return;

        UUID playerUUID = player.getUniqueId();

        // If already spectator, return
        if (spectators.contains(playerUUID)) {
            return;
        }

        spectators.add(playerUUID);

        // Set GameMode to SPECTATOR
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);

        // Teleport to lobby
        Location lobbyLoc = loadLobbyLocation();
        player.teleport(lobbyLoc);

        // Remove glow effect
        glowManager.removeGlow(player);
    }

    public void endGame() {
        gameState = GameState.END;

        // Cancel tasks
        if (glowTaskId != -1) {
            Bukkit.getScheduler().cancelTask(glowTaskId);
            glowTaskId = -1;
        }
        if (prepTaskId != -1) {
            Bukkit.getScheduler().cancelTask(prepTaskId);
            prepTaskId = -1;
        }

        // Remove all glowing effects
        glowManager.removeAllGlows();

        // Teleport all players to lobby
        Location lobbyLoc = loadLobbyLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(lobbyLoc);
        }

        // Cleanup
        targetMap.clear();
        initialParticipants.clear();
        spectators.clear();

        // Reset state
        gameState = GameState.WAITING;
    }

    public void stopGame() {
        if (gameState == GameState.WAITING) {
            return;
        }

        endGame();
    }

    public List<Player> getActivePlayers() {
        List<Player> active = new ArrayList<>();
        for (Player player : targetMap.keySet()) {
            if (!spectators.contains(player.getUniqueId())) {
                active.add(player);
            }
        }
        return active;
    }

    public Location loadGameLocation() {
        String world = plugin.getConfig().getString("game.world", "world");
        double x = plugin.getConfig().getDouble("game.x", 100);
        double y = plugin.getConfig().getDouble("game.y", 64);
        double z = plugin.getConfig().getDouble("game.z", 100);
        float yaw = (float) plugin.getConfig().getDouble("game.yaw", 0);
        float pitch = (float) plugin.getConfig().getDouble("game.pitch", 0);

        World w = Bukkit.getWorld(world);
        return new Location(w, x, y, z, yaw, pitch);
    }

    public Location loadLobbyLocation() {
        String world = plugin.getConfig().getString("lobby.world", "world");
        double x = plugin.getConfig().getDouble("lobby.x", 0);
        double y = plugin.getConfig().getDouble("lobby.y", 64);
        double z = plugin.getConfig().getDouble("lobby.z", 0);
        float yaw = (float) plugin.getConfig().getDouble("lobby.yaw", 0);
        float pitch = (float) plugin.getConfig().getDouble("lobby.pitch", 0);

        World w = Bukkit.getWorld(world);
        return new Location(w, x, y, z, yaw, pitch);
    }

    public void setLobbyLocation(Location loc) {
        plugin.getConfig().set("lobby.world", loc.getWorld().getName());
        plugin.getConfig().set("lobby.x", loc.getX());
        plugin.getConfig().set("lobby.y", loc.getY());
        plugin.getConfig().set("lobby.z", loc.getZ());
        plugin.getConfig().set("lobby.yaw", loc.getYaw());
        plugin.getConfig().set("lobby.pitch", loc.getPitch());
        plugin.saveConfig();
    }

    public void setGameLocation(Location loc) {
        plugin.getConfig().set("game.world", loc.getWorld().getName());
        plugin.getConfig().set("game.x", loc.getX());
        plugin.getConfig().set("game.y", loc.getY());
        plugin.getConfig().set("game.z", loc.getZ());
        plugin.getConfig().set("game.yaw", loc.getYaw());
        plugin.getConfig().set("game.pitch", loc.getPitch());
        plugin.saveConfig();
    }

    public boolean isSpectator(UUID uuid) {
        return spectators.contains(uuid);
    }

    public boolean isActivePlayer(UUID uuid) {
        // 게임 체인(targetMap)에 있는 플레이어인지 확인
        for (Player p : targetMap.keySet()) {
            if (p.getUniqueId().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public GameState getGameState() {
        return gameState;
    }

    public HashMap<Player, Player> getTargetMap() {
        return targetMap;
    }

    public Set<UUID> getInitialParticipants() {
        return initialParticipants;
    }

    public void handlePlayerHit(Player attacker, Player victim) {
        if (gameState != GameState.RUNNING) {
            return;
        }

        // Check if attacker's target is victim
        if (targetMap.get(attacker) != null && targetMap.get(attacker).equals(victim)) {
            eliminatePlayer(victim);
        }
    }

    public boolean isBlacklisted(UUID uuid) {
        // 하드코딩된 블랙리스트 확인
        if (TagChainPlugin.HARDCODED_BLACKLIST.contains(uuid.toString())) {
            return true;
        }
        
        // config.yml 블랙리스트 확인
        List<String> blacklist = plugin.getConfig().getStringList("blacklist");
        return blacklist.contains(uuid.toString());
    }

    public String getStatus() {
        return "§6[Tag Chain] Status: " + gameState + " | Active: " + getActivePlayers().size() + " | Spectators: " + spectators.size();
    }
}
