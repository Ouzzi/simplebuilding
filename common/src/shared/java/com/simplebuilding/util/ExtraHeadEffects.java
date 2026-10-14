package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The effects of a head in the extra head slot that Vanilla only gives to the helmet slot (owner N55: "effects work from
 * the head slot, too"): the head item's attribute modifiers for the head slot - the locator bar hiding of every mob head,
 * the silverfish head's half size, the slime head's extra safe fall - are applied as transient modifiers of the wearer
 * while the head sits in the extra slot. The id of each modifier is moved into {@code simplebuilding:extra_head/...}, so
 * the helmet slot's own modifier of the same head never clashes with it; a modifier the helmet slot already has (same
 * attribute, same original id: the same head in both slots) is not applied a second time - one effect per head kind.
 * {@code PlayerExtraHeadMixin} calls {@link #sync} every tick on the server.
 */
public final class ExtraHeadEffects {
    private ExtraHeadEffects() {
    }

    /** One attribute modifier this class has put on a player. */
    public record Applied(Holder<Attribute> attribute, AttributeModifier modifier) {
    }

    /** The modifier ids move below this prefix. */
    private static Identifier extraId(Identifier original) {
        return Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "extra_head/" + original.getNamespace() + "/" + original.getPath());
    }

    /** The modifiers the extra head of {@code player} contributes right now (empty without a head). */
    public static List<Applied> wanted(Player player) {
        ItemStack head = ExtraHead.get(player);
        if (head.isEmpty()) {
            return List.of();
        }
        Set<String> helmet = new HashSet<>();
        player.getItemBySlot(EquipmentSlot.HEAD).forEachModifier(EquipmentSlot.HEAD,
                (attribute, modifier) -> helmet.add(attribute.getRegisteredName() + "|" + modifier.id()));
        List<Applied> out = new ArrayList<>();
        head.forEachModifier(EquipmentSlot.HEAD, (attribute, modifier) -> {
            if (!helmet.contains(attribute.getRegisteredName() + "|" + modifier.id())) {
                out.add(new Applied(attribute, new AttributeModifier(extraId(modifier.id()), modifier.amount(), modifier.operation())));
            }
        });
        return List.copyOf(out);
    }

    /** Brings the modifiers on {@code player} in line with the extra head; {@code applied} = what {@link #sync} did last. */
    public static List<Applied> sync(Player player, List<Applied> applied) {
        List<Applied> wanted = wanted(player);
        if (wanted.equals(applied)) {
            return applied;
        }
        for (Applied old : applied) {
            AttributeInstance instance = player.getAttribute(old.attribute());
            if (instance != null) {
                instance.removeModifier(old.modifier().id());
            }
        }
        List<Applied> done = new ArrayList<>();
        for (Applied now : wanted) {
            AttributeInstance instance = player.getAttribute(now.attribute());
            if (instance != null) {
                instance.removeModifier(now.modifier().id());
                instance.addTransientModifier(now.modifier());
                done.add(now);
            }
        }
        return List.copyOf(done);
    }
}
