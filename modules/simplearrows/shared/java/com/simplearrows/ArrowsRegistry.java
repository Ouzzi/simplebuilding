package com.simplearrows;

import com.simplearrows.entity.LightArrow;
import com.simplearrows.entity.SilverArrow;
import com.simplearrows.item.LightArrowItem;
import com.simplearrows.item.SilverArrowItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;

/**
 * Everything Simple Arrows registers (Fabric: directly in the initializer): two arrow entity types like Vanilla's (0.5 x 0.5,
 * tracked from 4 chunks, updated every 20 ticks) and their items, which bows, crossbows and dispensers shoot; the Fletching
 * Table ({@link com.simplearrows.fletching.Fletchings}) and the guide.
 */
public final class ArrowsRegistry {
    public static final ResourceKey<EntityType<?>> SILVER_ARROW_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SimpleArrows.id("silver_arrow"));
    public static final ResourceKey<EntityType<?>> LIGHT_ARROW_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SimpleArrows.id("light_arrow"));
    public static final EntityType<SilverArrow> SILVER_ARROW_ENTITY = EntityType.Builder.<SilverArrow>of(SilverArrow::new, MobCategory.MISC)
            .noLootTable().sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20).build(SILVER_ARROW_KEY);
    public static final EntityType<LightArrow> LIGHT_ARROW_ENTITY = EntityType.Builder.<LightArrow>of(LightArrow::new, MobCategory.MISC)
            .noLootTable().sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20).build(LIGHT_ARROW_KEY);
    public static Item SILVER_ARROW;
    public static Item LIGHT_ARROW;
    public static net.minecraft.world.item.CreativeModeTab TAB;

    private ArrowsRegistry() {}

    public static void registerAll() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, SILVER_ARROW_KEY, SILVER_ARROW_ENTITY);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, LIGHT_ARROW_KEY, LIGHT_ARROW_ENTITY);
        SILVER_ARROW = item("silver_arrow", SilverArrowItem::new);
        LIGHT_ARROW = item("light_arrow", LightArrowItem::new);
        DispenserBlock.registerProjectileBehavior(SILVER_ARROW);
        DispenserBlock.registerProjectileBehavior(LIGHT_ARROW);
        com.simplearrows.fletching.Fletchings.registerAll();
        com.simplearrows.guide.ArrowsGuide.register();
    }

    public static void registerTab() {
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleArrows.id("arrows"),
                net.minecraft.world.item.CreativeModeTab.builder(net.minecraft.world.item.CreativeModeTab.Row.TOP, 0)
                        .title(net.minecraft.network.chat.Component.translatable("itemgroup.simplearrows.arrows"))
                        .icon(() -> new ItemStack(LIGHT_ARROW))
                        .displayItems((parameters, output) -> stacks().forEach(output::accept))
                        .build());
    }

    /** Whether Simple Ores is there: the Silver Arrow is only made from its silver (no recipe and no tab entry without it). */
    public static boolean silverAvailable() {
        return BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.Identifier.fromNamespaceAndPath(SimpleArrows.ORES_NAMESPACE, "silver_nugget"));
    }

    /**
     * The one "Simple Arrows" tab (DW04C-4, owner 04.11.: SimpleBuilding's own arrows tab is gone; empty while the module is
     * switched off): Light Arrow, the Silver Arrow when Simple Ores is there, every Fletching Table arrow of this installation
     * (tip by tip), the guide.
     */
    public static List<ItemStack> stacks() {
        List<ItemStack> out = vanillaTabStacks();
        if (out.isEmpty()) return out;
        out.addAll(com.simplearrows.fletching.Fletchings.arrowStacks(1));
        out.add(com.simplearrows.guide.ArrowsGuide.stack());
        return out;
    }

    /** What also goes into Vanilla's combat tab after the spectral arrow: Light and Silver Arrow (not the 90 table arrows). */
    public static List<ItemStack> vanillaTabStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (!SimpleArrows.on()) return out;
        out.add(new ItemStack(LIGHT_ARROW));
        if (silverAvailable()) out.add(new ItemStack(SILVER_ARROW));
        return out;
    }

    private static Item item(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleArrows.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }
}
