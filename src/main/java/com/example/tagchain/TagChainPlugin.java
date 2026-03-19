package com.example.tagchain;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class TagChainPlugin extends JavaPlugin {
    private GameManager gameManager;
    private GlowManager glowManager;
    private GameEventListener eventListener;
    private CommandHandler commandHandler;

    // 하드코딩된 블랙리스트 - config.yml이 삭제되어도 유지
    public static final Set<String> HARDCODED_BLACKLIST = new HashSet<>(Arrays.asList(
        "a4ca86f3-0489-4b67-91a3-7bd94cf31067"
    ));

    @Override
    public void onEnable() {
        // Load configuration
        saveDefaultConfig();

        // Initialize managers
        glowManager = new GlowManager(this);
        gameManager = new GameManager(this, glowManager);

        // Initialize event listener
        eventListener = new GameEventListener(gameManager);
        getServer().getPluginManager().registerEvents(eventListener, this);

        // Initialize command handler
        commandHandler = new CommandHandler(gameManager);
        getCommand("game").setExecutor(commandHandler);

        getLogger().info("Tag Chain Minigame plugin enabled!");
    }

    @Override
    public void onDisable() {
        // Stop game if running
        if (gameManager != null && gameManager.getGameState() != GameState.WAITING) {
            gameManager.stopGame();
        }

        getLogger().info("Tag Chain Minigame plugin disabled!");
    }
}
