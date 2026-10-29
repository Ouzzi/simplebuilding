package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Strong Gunpowder, Strong TNT and the strong rocket (BS25-21 with BS25-4, owner 25.10./29.10.). Prinzip A12: the use in
 * Vanilla play is blasting and flying farther; the change to Vanilla is none - Vanilla gunpowder, TNT, rockets and Elytra stay
 * as they are, the strong ones are own items you craft on purpose.
 *
 * <ul>
 *   <li><b>Strong Gunpowder</b>: 3 gunpowder + 1 blaze powder -&gt; 2 (shapeless).</li>
 *   <li><b>Strong Rocket</b>: a Vanilla firework rocket of flight duration {@value WitherWings#STRONG_FLIGHT}: paper + 1 Strong
 *       Gunpowder -&gt; 3 (shapeless recipe with the Vanilla fireworks component, no own item). Only a rocket of flight duration
 *       {@value WitherWings#STRONG_FLIGHT} or more lifts the wings above the Vanilla Elytra top speed ({@link WitherWings}).</li>
 *   <li><b>Strong TNT</b>: like TNT (5 Strong Gunpowder + 4 sand) with the explosion power {@link MobsConfig#strongTntPower}
 *       (default {@value #DEFAULT_POWER}, Vanilla TNT 4); lit by flint and steel, fire charge, redstone, burning arrows and
 *       other explosions; not flammable.</li>
 * </ul>
 * Looks are placeholders: Vanilla textures with a tint, until the owner picks one (preview a-mech29).
 */
public final class StrongExplosives {
    public static final String ID = MobsRegistry.ID;
    public static final float DEFAULT_POWER = 6.0f;

    public static final ResourceKey<Item> GUNPOWDER_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "strong_gunpowder"));
    public static final ResourceKey<Block> TNT_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ID, "strong_tnt"));
    public static final ResourceKey<Item> TNT_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "strong_tnt"));

    public static final Item GUNPOWDER = new Item(new Item.Properties().rarity(Rarity.UNCOMMON).setId(GUNPOWDER_KEY));
    public static final Block TNT_BLOCK = new StrongTntBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instabreak()
            .sound(SoundType.GRASS).setId(TNT_BLOCK_KEY));
    public static final Item TNT = new BlockItem(TNT_BLOCK, new Item.Properties().rarity(Rarity.UNCOMMON).useBlockDescriptionPrefix().setId(TNT_KEY));

    private StrongExplosives() {}

    /** The strong rocket as the recipe makes it: a Vanilla firework rocket of flight duration {@value WitherWings#STRONG_FLIGHT} with an own name. */
    public static net.minecraft.world.item.ItemStack rocket(int count) {
        var stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET, count);
        stack.set(net.minecraft.core.component.DataComponents.FIREWORKS,
                new net.minecraft.world.item.component.Fireworks(WitherWings.STRONG_FLIGHT, java.util.List.of()));
        stack.set(net.minecraft.core.component.DataComponents.ITEM_NAME,
                net.minecraft.network.chat.Component.translatable("simplemobs.item.strong_firework_rocket"));
        return stack;
    }

    public static void blocks(BiConsumer<ResourceKey<Block>, Block> sink) {
        sink.accept(TNT_BLOCK_KEY, TNT_BLOCK);
    }

    public static void items(BiConsumer<ResourceKey<Item>, Item> sink) {
        sink.accept(GUNPOWDER_KEY, GUNPOWDER);
        sink.accept(TNT_KEY, TNT);
    }
}
