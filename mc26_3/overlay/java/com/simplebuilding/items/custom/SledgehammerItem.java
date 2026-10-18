package com.simplebuilding.items.custom;

import net.minecraft.world.item.ToolMaterial;

/**
 * 26.3 (split stage 7): the sledgehammer is Simple Hammers' ({@link com.simplehammers.item.SledgehammerItem}, required
 * sub-mod); SimpleBuilding's code calls its statics ({@code isHammer}, {@code reshapeTarget}, {@code getBlocksToBeDestroyed},
 * the tier constants) through this subclass. SimpleBuilding registers no hammer on 26.3 ({@code McVersion.HAMMERS_IN_SB}),
 * so {@code instanceof} this class is never true - use {@link #isHammer}. The 26.2 twin (common/src/mc26_2/java) keeps
 * SimpleBuilding's own hammer.
 */
public class SledgehammerItem extends com.simplehammers.item.SledgehammerItem {
    public SledgehammerItem(ToolMaterial material, float attackDamage, float attackSpeed, int durability, Properties settings) {
        super(material, attackDamage, attackSpeed, durability, settings);
    }
}
