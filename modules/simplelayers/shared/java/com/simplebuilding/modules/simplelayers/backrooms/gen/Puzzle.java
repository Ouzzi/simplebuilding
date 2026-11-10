package com.simplebuilding.modules.simplelayers.backrooms.gen;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A level riddle as a pure state machine (BACK22 core K-R9.3 "PuzzleController"): states are ints, moves map a state to the next.
 * The level's block (switch panel, valve row, scarecrows) only stores the state and applies moves; {@link #solvable} proves for the
 * tests that every start state reaches the goal (owner: no dead ends, optional riddles never lock a way).
 */
public interface Puzzle {
    /** The start state for a position (pure, from the seed). */
    int start(long seed, int x, int z);

    /** All moves from {@code state} (resulting states). */
    List<Integer> moves(int state);

    boolean solved(int state);

    /** Fewest moves from {@code state} to a solved state, or -1 when none within {@code limit} states searched. */
    default int distance(int state, int limit) {
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        Set<Integer> seen = new HashSet<>();
        queue.add(new int[] {state, 0});
        seen.add(state);
        while (!queue.isEmpty() && seen.size() <= limit) {
            int[] s = queue.poll();
            if (solved(s[0])) return s[1];
            for (int n : moves(s[0])) if (seen.add(n)) queue.add(new int[] {n, s[1] + 1});
        }
        return -1;
    }

    /** Whether every state in {@code states} can be solved. */
    static boolean solvable(Puzzle p, Iterable<Integer> states, int limit) {
        for (int s : states) if (p.distance(s, limit) < 0) return false;
        return true;
    }

    /** The core's example: a 2×2 switch panel (Elektrostation draft) - pressing a switch flips it and its row neighbour. */
    Puzzle SWITCHES = new Puzzle() {
        @Override
        public int start(long seed, int x, int z) {
            return (int) (com.simplebuilding.modules.simplelayers.world.gen.GenHash.unit(seed, x, z, 2400) * 16);
        }

        @Override
        public List<Integer> moves(int state) {
            return List.of(state ^ 0b0011, state ^ 0b0001, state ^ 0b1100, state ^ 0b0100);
        }

        @Override
        public boolean solved(int state) {
            return state == 0b1111;
        }
    };
}
