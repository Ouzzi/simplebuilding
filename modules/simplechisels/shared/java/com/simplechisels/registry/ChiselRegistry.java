package com.simplechisels.registry;

import com.simplechisels.ChiselTier;
import com.simplechisels.SimpleChisels;
import com.simplechisels.item.ChiselItem;
import com.simplelib.api.ChiselActions;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Everything Simple Chisels registers: the seven chisels (ids and numbers as in SimpleBuilding before the move), the
 * "last target" component, the guide book and the creative tab "Simple Chisels". Loaders call the methods inside their
 * matching registry event (Fabric: directly in the initializer), items before the tab.
 */
public final class ChiselRegistry {
    public static final Map<ChiselTier, ChiselItem> CHISELS = new EnumMap<>(ChiselTier.class);
    /** The block the chisel reshaped last (tooltip line). SimpleBuilding's {@code coordinates} stays its own. */
    public static DataComponentType<BlockPos> LAST_TARGET;
    public static CreativeModeTab TAB;

    private ChiselRegistry() {}

    public static void registerItems() {
        if (!CHISELS.isEmpty()) return;
        LAST_TARGET = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleChisels.id("last_target"),
                DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).build());
        for (ChiselTier tier : ChiselTier.values()) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleChisels.id(tier.id()));
            Item.Properties properties = new Item.Properties().setId(key).durability(tier.durability).enchantable(tier.enchantability);
            if (tier.fireResistant()) properties = properties.fireResistant();
            CHISELS.put(tier, Registry.register(BuiltInRegistries.ITEM, key, new ChiselItem(tier, properties)));
        }
        com.simplechisels.guide.ChiselsGuide.register();
    }

    public static ChiselItem chisel(ChiselTier tier) {
        return CHISELS.get(tier);
    }

    public static void registerTab() {
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleChisels.id("chisels"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemgroup.simplechisels.chisels"))
                .icon(() -> new ItemStack(CHISELS.get(ChiselTier.IRON)))
                .displayItems((parameters, output) -> {
                    tabStacks().forEach(output::accept);
                    if (!SimpleChisels.on()) return;
                    // Fast Chiseling at its highest level, like SimpleBuilding's tab had it.
                    parameters.holders().lookupOrThrow(Registries.ENCHANTMENT).get(ChiselItem.FAST_CHISELING).ifPresent(holder -> {
                        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                        builder.upgrade(holder, holder.value().getMaxLevel());
                        book.set(DataComponents.STORED_ENCHANTMENTS, builder.toImmutable());
                        output.accept(book);
                    });
                })
                .build());
    }

    /** The chisels, then the guide (empty while the module is switched off). */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (!SimpleChisels.on()) return out;
        CHISELS.values().forEach(item -> out.add(new ItemStack(item)));
        out.add(com.simplechisels.guide.ChiselsGuide.stack());
        return out;
    }

    /** The chisels for SimpleLib {@link ChiselActions#tools} (weakest first, the current tables). */
    public static List<ChiselActions.Tool> publishedTools() {
        List<ChiselActions.Tool> out = new ArrayList<>();
        for (ChiselItem chisel : CHISELS.values()) {
            out.add(new ChiselActions.Tool(chisel, chisel.getForwardMap(), chisel.getBackwardMap(), chisel.getTouchForwardMap(),
                    chisel.getTouchBackwardMap(), chisel.tier().cooldownTicks, false, ChiselItem.TRANSFORM_DAMAGE, ChiselItem.REVERSE_TRANSFORM_DAMAGE));
        }
        return out;
    }
}
