package com.simplehammers.registry;

import com.simplehammers.HammerTiers;
import com.simplehammers.SimpleHammers;
import com.simplehammers.item.SledgehammerItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;

/**
 * Everything Simple Hammers registers: the seven sledgehammers (ids and numbers as in SimpleBuilding before the move),
 * the guide book and the creative tab. Loaders call the methods inside their matching registry event (Fabric: directly in
 * the initializer), items before the tab.
 */
public final class HammerRegistry {
    public static final Map<HammerTiers, SledgehammerItem> HAMMERS = new EnumMap<>(HammerTiers.class);
    /** Vanilla hoe of each tier: the hammer sits behind it in the tools tab (SimpleBuilding's order without SimpleBuilding). */
    public static final Map<HammerTiers, Item> VANILLA_ANCHORS = new EnumMap<>(Map.of(
            HammerTiers.STONE, Items.STONE_HOE, HammerTiers.COPPER, Items.COPPER_HOE, HammerTiers.IRON, Items.IRON_HOE,
            HammerTiers.GOLD, Items.GOLDEN_HOE, HammerTiers.DIAMOND, Items.DIAMOND_HOE, HammerTiers.NETHERITE, Items.NETHERITE_HOE,
            HammerTiers.ENDERITE, Items.NETHERITE_HOE));
    public static CreativeModeTab TAB;

    private HammerRegistry() {}

    public static void registerItems() {
        if (!HAMMERS.isEmpty()) return;
        for (HammerTiers tier : HammerTiers.values()) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleHammers.id(tier.id()));
            int durability = tier.baseDurability * SledgehammerItem.BASE_DURABILITY_MULTIPLIER;
            Item.Properties properties = new Item.Properties().setId(key).durability(durability).enchantable(tier.enchantability);
            if (tier.fireResistant()) properties = properties.fireResistant();
            SledgehammerItem hammer = new SledgehammerItem(tier.material, tier.attackDamage,
                    tier.attackSpeed - SledgehammerItem.ATTACK_SPEED_OFFSET, durability, properties);
            HAMMERS.put(tier, Registry.register(BuiltInRegistries.ITEM, key, hammer));
        }
        com.simplehammers.guide.HammersGuide.register();
    }

    public static Item hammer(HammerTiers tier) {
        return HAMMERS.get(tier);
    }

    public static void registerTab() {
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleHammers.id("hammers"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.simplehammers"))
                .icon(() -> new ItemStack(HAMMERS.get(HammerTiers.DIAMOND)))
                .displayItems((parameters, output) -> {
                    tabStacks().forEach(output::accept);
                    if (!SimpleHammers.on()) return;
                    // The hammer enchantments at their highest level, like SimpleBuilding's tab had them.
                    var enchantments = parameters.holders().lookupOrThrow(Registries.ENCHANTMENT);
                    for (String path : SimpleHammers.ENCHANTMENTS) {
                        enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, SimpleHammers.id(path))).ifPresent(holder -> {
                            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                            ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                            builder.upgrade(holder, holder.value().getMaxLevel());
                            book.set(DataComponents.STORED_ENCHANTMENTS, builder.toImmutable());
                            output.accept(book);
                        });
                    }
                })
                .build());
    }

    /** The hammers, then the guide (empty while the module is switched off). */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (!SimpleHammers.on()) return out;
        HAMMERS.values().forEach(item -> out.add(new ItemStack(item)));
        out.add(com.simplehammers.guide.HammersGuide.stack());
        return out;
    }

    /**
     * Vanilla tools tab: each hammer behind the hoe of its tier, the Enderite one behind the Netherite one - only without
     * SimpleBuilding, whose search-tab chains place them between its chisels and wands.
     */
    public static Map<Item, List<ItemStack>> vanillaPlacements() {
        Map<Item, List<ItemStack>> out = new java.util.LinkedHashMap<>();
        if (!SimpleHammers.on() || SimpleHammers.isLoaded("simplebuilding")) return out;
        for (HammerTiers tier : HammerTiers.values()) {
            out.computeIfAbsent(VANILLA_ANCHORS.get(tier), k -> new ArrayList<>()).add(new ItemStack(HAMMERS.get(tier)));
        }
        return out;
    }
}
