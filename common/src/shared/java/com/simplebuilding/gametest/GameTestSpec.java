package com.simplebuilding.gametest;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * Loader-neutral description of one in-game test: the body to run plus the parameters the
 * test runner needs to set the room up.
 *
 * <p>The field set mirrors vanilla {@code TestData}, which is what every loader ends up
 * filling in - Fabric through its {@code @GameTest} annotation, NeoForge through
 * {@code RegisterGameTestsEvent}. Keeping the parameters here means a test that needs sky
 * access or a longer tick budget carries that requirement with it, instead of the knowledge
 * living in one loader's annotation only.
 *
 * @param name              test id path inside the {@code simplebuilding} namespace
 * @param body              the actual test; must end in {@code helper.succeed()} or throw
 * @param structure         structure to place, or {@code null} for the loader's empty 8x8 room
 * @param maxTicks          how long the test may run before it counts as timed out
 * @param setupTicks        ticks to wait after placing the structure before the body starts
 * @param required          whether a failure fails the whole suite
 * @param rotation          rotation the structure is placed with
 * @param manualOnly        whether the test is skipped in automated runs
 * @param maxAttempts       how often a failing test is retried
 * @param requiredSuccesses how many successful runs are needed
 * @param skyAccess         {@code false} encloses the room in barrier blocks
 */
public record GameTestSpec(
        String name,
        Consumer<GameTestHelper> body,
        String structure,
        int maxTicks,
        int setupTicks,
        boolean required,
        Rotation rotation,
        boolean manualOnly,
        int maxAttempts,
        int requiredSuccesses,
        boolean skyAccess) {

    /** Same default the loaders use when a test does not ask for more time. */
    public static final int DEFAULT_MAX_TICKS = 20;

    public GameTestSpec {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(rotation, "rotation");
        if (maxTicks <= 0) {
            throw new IllegalArgumentException("maxTicks must be positive for " + name);
        }
    }

    /** Starts a spec with the default parameters; override only what the test needs. */
    public static Builder named(String name, Consumer<GameTestHelper> body) {
        return new Builder(name, body);
    }

    /** Mutable collector for {@link GameTestSpec}, so call sites only spell out what differs. */
    public static final class Builder {
        private final String name;
        private final Consumer<GameTestHelper> body;
        private String structure;
        private int maxTicks = DEFAULT_MAX_TICKS;
        private int setupTicks;
        private boolean required = true;
        private Rotation rotation = Rotation.NONE;
        private boolean manualOnly;
        private int maxAttempts = 1;
        private int requiredSuccesses = 1;
        private boolean skyAccess;

        private Builder(String name, Consumer<GameTestHelper> body) {
            this.name = name;
            this.body = body;
        }

        public Builder structure(String value) {
            this.structure = value;
            return this;
        }

        public Builder maxTicks(int value) {
            this.maxTicks = value;
            return this;
        }

        public Builder setupTicks(int value) {
            this.setupTicks = value;
            return this;
        }

        public Builder required(boolean value) {
            this.required = value;
            return this;
        }

        public Builder rotation(Rotation value) {
            this.rotation = value;
            return this;
        }

        public Builder manualOnly(boolean value) {
            this.manualOnly = value;
            return this;
        }

        public Builder maxAttempts(int value) {
            this.maxAttempts = value;
            return this;
        }

        public Builder requiredSuccesses(int value) {
            this.requiredSuccesses = value;
            return this;
        }

        public Builder skyAccess(boolean value) {
            this.skyAccess = value;
            return this;
        }

        public GameTestSpec build() {
            return new GameTestSpec(name, body, structure, maxTicks, setupTicks, required,
                    rotation, manualOnly, maxAttempts, requiredSuccesses, skyAccess);
        }
    }
}
