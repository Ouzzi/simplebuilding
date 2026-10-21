package com.simplebuilding.modules.simpletools;

import com.simplebuilding.modules.simpletools.brush.BiomeBrushItem;
import com.simplebuilding.modules.simpletools.vault.DeathVaultBlock;
import com.simplebuilding.modules.simpletools.vault.DeathVaultMenu;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/** The Death Vault block, its item and menu, the Biome Brush; each loader calls the parts inside its matching registration window. */
public final class ToolsRegistry {
    public static final Identifier DEATH_VAULT_ID = Identifier.fromNamespaceAndPath(SimpleTools.MOD_ID, "death_vault");

    public static DeathVaultBlock DEATH_VAULT;
    public static Item DEATH_VAULT_ITEM;
    public static final Identifier BIOME_BRUSH_ID = Identifier.fromNamespaceAndPath(SimpleTools.MOD_ID, "biome_brush");
    /** Biome Brush (moved from Simple Mobs on 21.10., old id {@code simplemobs:biome_brush} via IdMoves). */
    public static Item BIOME_BRUSH;
    public static MenuType<DeathVaultMenu> DEATH_VAULT_MENU;

    private ToolsRegistry() {}

    public static void registerBlocks() {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, DEATH_VAULT_ID);
        DEATH_VAULT = Registry.register(BuiltInRegistries.BLOCK, key, new DeathVaultBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                .strength(25.0F, 1200.0F).sound(SoundType.DEEPSLATE_BRICKS).lightLevel(state -> 7).setId(key)));
    }

    public static void registerItems() {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, DEATH_VAULT_ID);
        DEATH_VAULT_ITEM = Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(DEATH_VAULT, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
        ResourceKey<Item> brush = ResourceKey.create(Registries.ITEM, BIOME_BRUSH_ID);
        BIOME_BRUSH = Registry.register(BuiltInRegistries.ITEM, brush, new BiomeBrushItem(com.simplelib.api.SimpleRarity.CURSED.apply(new Item.Properties().durability(BiomeBrushItem.DURABILITY)
                .repairable(Items.NETHERITE_INGOT).enchantable(15)).fireResistant().setId(brush)));
    }

    public static void registerMenus() {
        DEATH_VAULT_MENU = Registry.register(BuiltInRegistries.MENU, DEATH_VAULT_ID, new MenuType<>(DeathVaultMenu::new, FeatureFlags.VANILLA_SET));
    }

    /** Creative tab entry after the Vanilla brush (tools tab). */
    public static List<ItemStack> brushStacks() {
        return BIOME_BRUSH == null || !SimpleTools.active() ? List.of() : List.of(new ItemStack(BIOME_BRUSH));
    }

    /** Creative tab entries (functional blocks). */
    public static List<ItemStack> tabStacks() {
        return DEATH_VAULT_ITEM == null ? List.of() : List.of(new ItemStack(DEATH_VAULT_ITEM));
    }
}
