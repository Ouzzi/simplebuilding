package com.simplelib.item;

import com.simplelib.api.CoreHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A building core (IG31-13, formerly SimpleBuilding's {@code BuildingCoreItem}): copper to enderite, not stackable, an
 * ingredient of several Simple mods (wands, beacons, gadgets). On its own it does nothing when used; a partner that gives
 * the cores their show (SimpleBuilding: animation and the rare ore transmutation) installs {@link CoreHooks#setCore}.
 */
public class CoreItem extends Item {
    public CoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        CoreHooks.Core hook = CoreHooks.core();
        return hook == null ? super.use(level, player, hand) : hook.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        CoreHooks.Core hook = CoreHooks.core();
        return hook == null ? super.useOn(context) : hook.useOn(context);
    }
}
