package com.example.tagchain;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
    private GameManager gameManager;

    public CommandHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        // Check if player is blacklisted
        if (gameManager.isBlacklisted(player.getUniqueId())) {
            player.sendMessage("§c[Tag Chain] 당신은 이 게임에 접근할 수 없습니다!");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§6Usage: /game <start|stop|status|setlobby|setgame|spectator>");
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "start":
                handleStart(player);
                return true;

            case "stop":
                handleStop(player);
                return true;

            case "status":
                handleStatus(player);
                return true;

            case "setlobby":
                handleSetLobby(player);
                return true;

            case "setgame":
                handleSetGame(player);
                return true;

            case "spectator":
                handleSpectator(player, args);
                return true;

            default:
                player.sendMessage("§6Usage: /game <start|stop|status|setlobby|setgame|spectator>");
                return true;
        }
    }

    private void handleStart(Player player) {
        if (gameManager.getGameState() != GameState.WAITING) {
            player.sendMessage("§cA game is already in progress!");
            return;
        }

        // Check if there are at least 4 players online
        int onlinePlayerCount = Bukkit.getOnlinePlayers().size();
        if (onlinePlayerCount < 4) {
            player.sendMessage("§c[Tag Chain] 게임을 시작하려면 최소 4명의 플레이어가 필요합니다!");
            player.sendMessage("§c현재 온라인: " + onlinePlayerCount + "/4");
            return;
        }

        gameManager.startGame();
        player.sendMessage("§a[Tag Chain] 게임이 시작되었습니다!");
    }

    private void handleStop(Player player) {
        if (gameManager.getGameState() == GameState.WAITING) {
            player.sendMessage("§cNo game is currently running!");
            return;
        }

        gameManager.stopGame();
        player.sendMessage("§aGame stopped!");
    }

    private void handleStatus(Player player) {
        player.sendMessage(gameManager.getStatus());
    }

    private void handleSetLobby(Player player) {
        gameManager.setLobbyLocation(player.getLocation());
        player.sendMessage("§aLobby location set!");
    }

    private void handleSetGame(Player player) {
        gameManager.setGameLocation(player.getLocation());
        player.sendMessage("§aGame location set!");
    }

    private void handleSpectator(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§6Usage: /game spectator <player>");
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            player.sendMessage("§c플레이어를 찾을 수 없습니다.");
            return;
        }

        // 이미 관전자인 경우
        if (gameManager.isSpectator(target.getUniqueId())) {
            player.sendMessage("§c" + target.getName() + "은(는) 이미 관전자입니다!");
            return;
        }

        // 게임에 참여하지 않은 플레이어인 경우
        if (!gameManager.isActivePlayer(target.getUniqueId())) {
            player.sendMessage("§c" + target.getName() + "은(는) 게임에 참여하지 않았습니다!");
            return;
        }

        gameManager.convertToSpectator(target);
        player.sendMessage("§a" + target.getName() + "이(가) 관전자로 변환되었습니다!");
    }
}
