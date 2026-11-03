package com.simplebuilding.gametest;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Damage of a breaker piston for the core tests on every line (MACH32-MOD stage 2): where SimpleBuilding owns the pistons
 * ({@code McVersion.PISTONS_IN_SB}, 26.2) through its own {@code NetheriteBreakerPistonBlock}; on 26.3 the pistons are Simple
 * Machines' (no class import, principle 3), so the two state properties {@code wear} (crack stage) and {@code wear_step} are found
 * by name. Only "damaged" and "undamaged" are compared there; the exact numbers are Simple Machines' own tests.
 */
final class BreakerDamage {
    private BreakerDamage() {
    }

    /** {@code state} with some damage: {@code damage} on 26.2, crack stage 1 on 26.3. */
    static BlockState damaged(BlockState state, int damage) {
        if (com.simplebuilding.version.McVersion.PISTONS_IN_SB) {
            return com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock.withDamage(state, damage);
        }
        IntegerProperty wear = property(state, "wear");
        return wear == null ? state : state.setValue(wear, 1);
    }

    /** Whether the breaker carries any damage. */
    static boolean isDamaged(BlockState state) {
        if (com.simplebuilding.version.McVersion.PISTONS_IN_SB) {
            return com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock.damageOf(state) > 0;
        }
        IntegerProperty wear = property(state, "wear");
        IntegerProperty step = property(state, "wear_step");
        return wear != null && state.getValue(wear) > 0 || step != null && state.getValue(step) > 0;
    }

    private static IntegerProperty property(BlockState state, String name) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return property instanceof IntegerProperty integer ? integer : null;
    }
}
