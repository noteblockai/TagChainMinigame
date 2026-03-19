package com.example.tagchain;

public enum GameState {
    WAITING,      // No game in progress
    PREPARING,    // 2-minute preparation phase
    RUNNING,      // Main game active
    END           // Game finished
}
