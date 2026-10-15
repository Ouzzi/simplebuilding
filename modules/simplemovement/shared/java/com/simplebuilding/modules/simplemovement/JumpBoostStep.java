package com.simplebuilding.modules.simplemovement;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * While Jump Boost is active (potion, beacon, command), a player steps up at least one block without jumping: a transient
 * {@code step_height} modifier tops the attribute up to {@link #MIN_STEP}. Other sources (Step Height boots) are taken into account,
 * so the result is the maximum, never the sum.
 */
public final class JumpBoostStep {
    public static final double MIN_STEP = 1.0;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(SimpleMovement.MOD_ID, "jump_boost_step");

    private JumpBoostStep() {}

    /** Server tick of a player: add, adjust or remove the top-up modifier. */
    public static void tick(Player player) {
        if (player.level().isClientSide()) return;
        AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (step == null) return;
        AttributeModifier old = step.getModifier(ID);
        boolean wanted = player.hasEffect(MobEffects.JUMP_BOOST) && SimpleMovement.active() && SimpleMovement.config().jumpBoostStepUp;
        if (!wanted) {
            if (old != null) step.removeModifier(ID);
            return;
        }
        double need = missing(step);
        if (need <= 0) {
            if (old != null) step.removeModifier(ID);
        } else if (old == null || Math.abs(old.amount() - need) > 1.0E-9) {
            step.addOrUpdateTransientModifier(new AttributeModifier(ID, need, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** How much must be added (add_value) to reach {@link #MIN_STEP}, computed without this mod's own modifier; 0 if already enough. */
    static double missing(AttributeInstance step) {
        double add = step.getBaseValue(), mulBase = 0, mulTotal = 1;
        for (AttributeModifier m : step.getModifiers()) {
            if (m.id().equals(ID)) continue;
            switch (m.operation()) {
                case ADD_VALUE -> add += m.amount();
                case ADD_MULTIPLIED_BASE -> mulBase += m.amount();
                case ADD_MULTIPLIED_TOTAL -> mulTotal *= 1.0 + m.amount();
            }
        }
        double factor = (1.0 + mulBase) * mulTotal;
        if (factor <= 0 || add * factor >= MIN_STEP) return 0;
        // x joins the add stage: (add + x) * factor = MIN_STEP
        return MIN_STEP / factor - add;
    }
}
