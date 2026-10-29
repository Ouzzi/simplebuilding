package com.simplebuilding.modules.simplechallenges.preset;

import java.util.List;

/**
 * The runnable Unlock mode of a preset: cell size, start area (rectangles in cell units, inclusive), currency, price tier,
 * the dimensions it covers, the rescue timer and the credits per advancement frame.
 */
public record UnlockMode(int cell, List<int[]> start, Currency currency, Difficulty difficulty, List<String> dimensions,
                         int pityMinutes, int creditsTask, int creditsGoal, int creditsChallenge) {
    /** Most start cells one preset may declare (concept 2: start area at most 64 x 64 cells). */
    public static final int MAX_START_CELLS = 64 * 64;
    public static final int MAX_STACKS = 2;

    public int creditsFor(String frame) {
        return switch (frame) {
            case "challenge" -> creditsChallenge;
            case "goal" -> creditsGoal;
            default -> creditsTask;
        };
    }

    public boolean earnsCredits() {
        return creditsTask + creditsGoal + creditsChallenge > 0;
    }
}
