package com.simplebuilding.items.custom;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class ChiselItem extends Item {

    public enum Direction {
        FORWARD,
        BACKWARD
    }

    private Direction chiselDirection = Direction.FORWARD;
    private SoundEvent chiselSound = SoundEvents.UI_STONECUTTER_TAKE_RESULT;
    private int cooldownTicks = 100;

    // Wir speichern das Material selbst, da "Item" kein Material hat.
    private final ToolMaterial material;

    private final Map<Block, Block> forwardMap;
    private final Map<Block, Block> backwardMap;
    private final Map<Block, Block> touchForwardMap;
    private final Map<Block, Block> touchBackwardMap;

    // =================================================================================
    // STATIC MAPS
    // =================================================================================

    // stone chisel/spatula transformations [smooth_sand_stone, cut_sand_stone, sand_stone, chiseled_sand_stone], [red_sand_stone, cut_red_sand_stone, red_sand_stone, chiseled_red_sand_stone], [stone, chiseled_stone];
    // stone constructor's touch transformations [mud_bricks, packed_mud, mud], [cobblestone, mossy_cobblestone], [all logs -> stripped logs];
    // iron/copper chisel/spatula transformations [chiseled_stone, stone_bricks, cracked_stone_bricks], [Andesite, polished_andesite], [Diorite, polished_diorite], [Granite, polished_granite], [tuff, polished_tuff];
    // iron/copper constructor's touch transformations [mud_bricks, bricks], [all wood -> strippesd wood];
    // gold chisel/spatula transformations [smooth_quartz, quartz_pillar, quartz_brick, chiseled_quartz, quartz_block], [tuff, chiseled_tuff, tuff_brick];
    // gold constructor's touch transformations [prismarine, prismarine_bricks], [smooth_stone, stone];
    // diamond chisel/spatula transformations [blackstone, chissled_blackstone, blackstone_bricks blackstone_cracked_bricks], [basalt, smooth_basalt, polished_basalt] , [polished_deepslate, chissled_deepslate, deepslate_bricks, cracked_deepslate_bricks, deepslate_titles, cracked_deepslate_titles, deepslate, cobbled_deepslate] , [cracked_stone_bricks, cobblestone];
    // diamond constructor's touch transformations [endstone, endstone_bricks] , [purpur_pillar, purpur_block] , [copper_block, cut_copper, chiseled_copper_block, copper_grate], [dead corals -> cycle trough (circle)], [corals -> cycle trough (circle)];
    // netherite chisel/spatula transformations [netherrack, netzer_bricks, cracke_bether_bricks, chiseled_netzer_bricks, netherrack (circle)] , [resin_bricks, chisled_resin_bricks], [chiseled_sand_stone, sand], [chiseled_red_sand_stone, red_sand];
    // netherite constructor's touch transformations [tuff_bricks, calcelite_block, dripstone_block] , [obsidian, crying_obsidian], [all stems -> stripped stems], [every concrete, concrete_powder];


    private static final Map<Block, Block> STONE_CHISEL_MAP = new HashMap<>();
    private static final Map<Block, Block> STONE_SPATULA_MAP = new HashMap<>();
    private static final Map<Block, Block> STONE_TOUCH_MAP = new HashMap<>();
    private static final Map<Block, Block> STONE_TOUCH_SPATULA_MAP = new HashMap<>();

    private static final Map<Block, Block> IRON_CHISEL_MAP = new HashMap<>();
    private static final Map<Block, Block> IRON_SPATULA_MAP = new HashMap<>();
    private static final Map<Block, Block> IRON_TOUCH_MAP = new HashMap<>();
    private static final Map<Block, Block> IRON_TOUCH_SPATULA_MAP = new HashMap<>();

    private static final Map<Block, Block> DIAMOND_CHISEL_MAP = new HashMap<>();
    private static final Map<Block, Block> DIAMOND_SPATULA_MAP = new HashMap<>();
    private static final Map<Block, Block> DIAMOND_TOUCH_MAP = new HashMap<>();
    private static final Map<Block, Block> DIAMOND_TOUCH_SPATULA_MAP = new HashMap<>();

    private static final Map<Block, Block> NETHERITE_CHISEL_MAP = new HashMap<>();
    private static final Map<Block, Block> NETHERITE_SPATULA_MAP = new HashMap<>();
    private static final Map<Block, Block> NETHERITE_TOUCH_MAP = new HashMap<>();
    private static final Map<Block, Block> NETHERITE_TOUCH_SPATULA_MAP = new HashMap<>();

    public static Map<Block, Block> FINAL_STONE_FWD, FINAL_STONE_BWD, FINAL_STONE_TOUCH_FWD, FINAL_STONE_TOUCH_BWD;
    public static Map<Block, Block> FINAL_IRON_FWD, FINAL_IRON_BWD, FINAL_IRON_TOUCH_FWD, FINAL_IRON_TOUCH_BWD;
    public static Map<Block, Block> FINAL_DIAMOND_FWD, FINAL_DIAMOND_BWD, FINAL_DIAMOND_TOUCH_FWD, FINAL_DIAMOND_TOUCH_BWD;
    public static Map<Block, Block> FINAL_NETHERITE_FWD, FINAL_NETHERITE_BWD, FINAL_NETHERITE_TOUCH_FWD, FINAL_NETHERITE_TOUCH_BWD;

    static {
        // =================================================================================
        // 1. STONE TIER
        // =================================================================================

        // [smooth -> cut -> sand_stone -> chiseled]
        registerLinear(STONE_CHISEL_MAP, STONE_SPATULA_MAP, Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE);
        registerLinear(STONE_CHISEL_MAP, STONE_SPATULA_MAP, Blocks.SANDSTONE_STAIRS, Blocks.SMOOTH_SANDSTONE_STAIRS); // Stairs
        registerLinear(STONE_CHISEL_MAP, STONE_SPATULA_MAP, Blocks.SANDSTONE_SLAB, Blocks.CUT_SANDSTONE_SLAB, Blocks.SMOOTH_SANDSTONE_SLAB); // Slabs

        // [smooth -> cut -> red_sand_stone -> chiseled]
        registerLinear(STONE_CHISEL_MAP, STONE_SPATULA_MAP, Blocks.SMOOTH_RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE, Blocks.RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE);
        registerLinear(STONE_CHISEL_MAP, STONE_SPATULA_MAP, Blocks.RED_SANDSTONE_STAIRS, Blocks.SMOOTH_RED_SANDSTONE_STAIRS); // Stairs
        registerLinear(STONE_CHISEL_MAP, STONE_SPATULA_MAP, Blocks.RED_SANDSTONE_SLAB, Blocks.CUT_RED_SANDSTONE_SLAB, Blocks.SMOOTH_RED_SANDSTONE_SLAB); // Slabs

        // [stone -> chiseled]
        registerLinear(STONE_CHISEL_MAP, STONE_SPATULA_MAP, Blocks.STONE, Blocks.CHISELED_STONE_BRICKS);
        registerLinear(STONE_CHISEL_MAP, STONE_SPATULA_MAP, Blocks.SMOOTH_STONE_SLAB, Blocks.STONE_SLAB); // Slabs
        registerLinear(STONE_CHISEL_MAP, STONE_SPATULA_MAP, Blocks.STONE_STAIRS, Blocks.COBBLESTONE_STAIRS);

        // [Mud Bricks -> Packed Mud -> Mud]
        registerLinear(STONE_TOUCH_MAP, STONE_TOUCH_SPATULA_MAP, Blocks.MUD_BRICKS, Blocks.PACKED_MUD, Blocks.MUD);
        registerLinear(STONE_TOUCH_MAP, STONE_TOUCH_SPATULA_MAP, Blocks.MUD_BRICK_STAIRS, Blocks.MUD_BRICK_STAIRS); // Fallback self or logic missing for packed mud stairs
        registerLinear(STONE_TOUCH_MAP, STONE_TOUCH_SPATULA_MAP, Blocks.MUD_BRICK_SLAB, Blocks.MUD_BRICK_SLAB);

        // [Cobblestone -> Mossy Cobblestone]
        registerLinear(STONE_TOUCH_MAP, STONE_TOUCH_SPATULA_MAP, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
        registerLinear(STONE_TOUCH_MAP, STONE_TOUCH_SPATULA_MAP, Blocks.COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE_STAIRS);
        registerLinear(STONE_TOUCH_MAP, STONE_TOUCH_SPATULA_MAP, Blocks.COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE_SLAB);

        // [All Logs -> Stripped Logs]
        registerLogs();

        // =================================================================================
        // 2. IRON / COPPER TIER (Alles Linear)
        // =================================================================================

        // [chiseled -> brick -> cracked]
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.CHISELED_STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS);
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.STONE_BRICK_STAIRS, Blocks.MOSSY_STONE_BRICK_STAIRS); // Mix touch logic?
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.STONE_BRICK_SLAB, Blocks.MOSSY_STONE_BRICK_SLAB);

        // Andesite, Diorite, Granite (Polished variants)
        registerAndesiteDioriteGranite();

        // Tuff
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_TUFF, Blocks.TUFF);
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_TUFF_STAIRS, Blocks.TUFF_STAIRS);
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_TUFF_SLAB, Blocks.TUFF_SLAB);

        // [Bricks -> Mud Bricks]
        registerLinear(IRON_TOUCH_MAP, IRON_TOUCH_SPATULA_MAP, Blocks.BRICKS, Blocks.MUD_BRICKS);
        registerLinear(IRON_TOUCH_MAP, IRON_TOUCH_SPATULA_MAP, Blocks.BRICK_STAIRS, Blocks.MUD_BRICK_STAIRS);
        registerLinear(IRON_TOUCH_MAP, IRON_TOUCH_SPATULA_MAP, Blocks.BRICK_SLAB, Blocks.MUD_BRICK_SLAB);

        // [All Woods -> Stripped Wood]
        registerWood();

        registerWoodVariants(IRON_CHISEL_MAP, IRON_SPATULA_MAP);
        registerNetherWoodVariants(IRON_TOUCH_MAP, IRON_TOUCH_SPATULA_MAP);

        // =================================================================================
        // 3. GOLD TIER (Linear)
        // =================================================================================

        // [smooth -> pillar -> brick -> chiseled -> block] (Updated Order)
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.SMOOTH_QUARTZ, Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_BRICKS, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK);
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.SMOOTH_QUARTZ_STAIRS, Blocks.QUARTZ_STAIRS);
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.SMOOTH_QUARTZ_SLAB, Blocks.QUARTZ_SLAB);

        // [tuff -> chiseled -> brick]
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.POLISHED_TUFF, Blocks.TUFF, Blocks.CHISELED_TUFF, Blocks.TUFF_BRICKS);
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.TUFF_STAIRS, Blocks.TUFF_BRICK_STAIRS, Blocks.POLISHED_TUFF_STAIRS);
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.TUFF_SLAB, Blocks.TUFF_BRICK_SLAB, Blocks.POLISHED_TUFF_SLAB);

        // [Prismarine -> Prismarine Bricks]
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS);
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.PRISMARINE_STAIRS, Blocks.PRISMARINE_BRICK_STAIRS);
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.PRISMARINE_SLAB, Blocks.PRISMARINE_BRICK_SLAB);

        // [Smooth Stone -> Stone]
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.SMOOTH_STONE, Blocks.STONE);
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.SMOOTH_STONE_SLAB, Blocks.STONE_SLAB);

        // =================================================================================
        // 4. DIAMOND TIER (Mix Linear & Cyclic)
        // =================================================================================

        // [Polished Blackstone -> Chiseled Blackstone -> Blackstone Bricks -> Cracked Blackstone Bricks]
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.POLISHED_BLACKSTONE, Blocks.BLACKSTONE, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.POLISHED_BLACKSTONE_STAIRS, Blocks.BLACKSTONE_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.POLISHED_BLACKSTONE_SLAB, Blocks.BLACKSTONE_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);

        // [Basalt -> Smooth Basalt -> Polished Basalt]
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.POLISHED_BASALT);
        // [Polished Deepslate -> Chiseled Deepslate -> Deepslate Bricks -> Cracked Deepslate Bricks -> Deepslate Tiles -> Cracked Deepslate Tiles -> Deepslate -> Cobbled Deepslate]
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.POLISHED_DEEPSLATE, Blocks.CHISELED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE);
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.POLISHED_DEEPSLATE_STAIRS, Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.COBBLED_DEEPSLATE_STAIRS);
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.POLISHED_DEEPSLATE_SLAB, Blocks.DEEPSLATE_BRICK_SLAB, Blocks.DEEPSLATE_TILE_SLAB, Blocks.COBBLED_DEEPSLATE_SLAB);

        // [Cracked Stone Bricks -> Cobblestone]
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.CRACKED_STONE_BRICKS, Blocks.COBBLESTONE);

        // [endstone -> endstone_bricks]
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.END_STONE, Blocks.END_STONE_BRICKS);
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.END_STONE_BRICK_STAIRS, Blocks.END_STONE_BRICK_STAIRS); // Only bricks have stairs
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.END_STONE_BRICK_SLAB, Blocks.END_STONE_BRICK_SLAB);

        // [purpur_pillar -> purpur_block]
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.PURPUR_PILLAR, Blocks.PURPUR_BLOCK);
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.PURPUR_STAIRS, Blocks.PURPUR_STAIRS); // Only block has stairs
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.PURPUR_SLAB, Blocks.PURPUR_SLAB);

        // [copper_block -> cut_copper -> chiseled_copper_block -> copper_grate]
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.COPPER_BLOCK, Blocks.CUT_COPPER, Blocks.CHISELED_COPPER, Blocks.COPPER_GRATE);
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.CUT_COPPER_STAIRS, Blocks.CUT_COPPER_STAIRS); // Only cut has stairs
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.CUT_COPPER_SLAB, Blocks.CUT_COPPER_SLAB);


        // [Dead Corals] -> CYCLIC (Circle)
        registerCyclic(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.DEAD_BRAIN_CORAL_BLOCK, Blocks.DEAD_BUBBLE_CORAL_BLOCK, Blocks.DEAD_FIRE_CORAL_BLOCK, Blocks.DEAD_HORN_CORAL_BLOCK, Blocks.DEAD_TUBE_CORAL_BLOCK);
        // [Alive Corals] -> CYCLIC (Circle)
        registerCyclic(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.BRAIN_CORAL_BLOCK, Blocks.BUBBLE_CORAL_BLOCK, Blocks.FIRE_CORAL_BLOCK, Blocks.HORN_CORAL_BLOCK, Blocks.TUBE_CORAL_BLOCK);

        // =================================================================================
        // 5. NETHERITE TIER (Mix Linear & Cyclic)
        // =================================================================================

        // [Netherrack -> Nether Bricks -> Cracked Nether Bricks -> Chiseled Nether Bricks] -> CYCLIC (Circle)
        registerCyclic(NETHERITE_CHISEL_MAP, NETHERITE_SPATULA_MAP, Blocks.NETHERRACK, Blocks.NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS);
        registerLinear(NETHERITE_CHISEL_MAP, NETHERITE_SPATULA_MAP, Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_BRICK_STAIRS); // Only bricks have stairs
        registerLinear(NETHERITE_CHISEL_MAP, NETHERITE_SPATULA_MAP, Blocks.NETHER_BRICK_SLAB, Blocks.NETHER_BRICK_SLAB);

        // [Resin Bricks -> Chiseled Resin Bricks]
        registerLinear(NETHERITE_CHISEL_MAP, NETHERITE_SPATULA_MAP, Blocks.RESIN_BRICKS, Blocks.CHISELED_RESIN_BRICKS);
        registerLinear(NETHERITE_CHISEL_MAP, NETHERITE_SPATULA_MAP, Blocks.RESIN_BRICK_STAIRS, Blocks.RESIN_BRICK_STAIRS);
        registerLinear(NETHERITE_CHISEL_MAP, NETHERITE_SPATULA_MAP, Blocks.RESIN_BRICK_SLAB, Blocks.RESIN_BRICK_SLAB);

        // [chiseled_sand_stone -> sand]
        registerLinear(NETHERITE_CHISEL_MAP, NETHERITE_SPATULA_MAP, Blocks.CHISELED_SANDSTONE, Blocks.SAND);
        // [chiseled_red_sand_stone -> red_sand]
        registerLinear(NETHERITE_CHISEL_MAP, NETHERITE_SPATULA_MAP, Blocks.CHISELED_RED_SANDSTONE, Blocks.RED_SAND);

        // [tuff -> calcite -> dripstone] (Updated Order)
        registerLinear(NETHERITE_TOUCH_MAP, NETHERITE_TOUCH_SPATULA_MAP, Blocks.POLISHED_DIORITE, Blocks.DIORITE, Blocks.CALCITE, Blocks.DRIPSTONE_BLOCK);
        // [obsidian -> crying_obsidian]
        registerLinear(NETHERITE_TOUCH_MAP, NETHERITE_TOUCH_SPATULA_MAP, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN);
        // [All Stems -> Stripped Stems]
        registerStems();
        // [Every Concrete & Concrete Powder]
        registerConcrete();

        // =================================================================================
        // MERGING
        // =================================================================================
        FINAL_STONE_FWD = Map.copyOf(STONE_CHISEL_MAP);
        FINAL_STONE_BWD = Map.copyOf(STONE_SPATULA_MAP);
        FINAL_STONE_TOUCH_FWD = merge(STONE_CHISEL_MAP, STONE_TOUCH_MAP);
        FINAL_STONE_TOUCH_BWD = merge(STONE_SPATULA_MAP, STONE_TOUCH_SPATULA_MAP);

        FINAL_IRON_FWD = merge(FINAL_STONE_FWD, IRON_CHISEL_MAP);
        FINAL_IRON_BWD = merge(FINAL_STONE_BWD, IRON_SPATULA_MAP);
        FINAL_IRON_TOUCH_FWD = merge(FINAL_STONE_TOUCH_FWD, merge(IRON_CHISEL_MAP, IRON_TOUCH_MAP));
        FINAL_IRON_TOUCH_BWD = merge(FINAL_STONE_TOUCH_BWD, merge(IRON_SPATULA_MAP, IRON_TOUCH_SPATULA_MAP));

        FINAL_DIAMOND_FWD = merge(FINAL_IRON_FWD, DIAMOND_CHISEL_MAP);
        FINAL_DIAMOND_BWD = merge(FINAL_IRON_BWD, DIAMOND_SPATULA_MAP);
        FINAL_DIAMOND_TOUCH_FWD = merge(FINAL_IRON_TOUCH_FWD, merge(DIAMOND_CHISEL_MAP, DIAMOND_TOUCH_MAP));
        FINAL_DIAMOND_TOUCH_BWD = merge(FINAL_IRON_TOUCH_BWD, merge(DIAMOND_SPATULA_MAP, DIAMOND_TOUCH_SPATULA_MAP));

        FINAL_NETHERITE_FWD = merge(FINAL_DIAMOND_FWD, NETHERITE_CHISEL_MAP);
        FINAL_NETHERITE_BWD = merge(FINAL_DIAMOND_BWD, NETHERITE_SPATULA_MAP);
        FINAL_NETHERITE_TOUCH_FWD = merge(FINAL_DIAMOND_TOUCH_FWD, merge(NETHERITE_CHISEL_MAP, NETHERITE_TOUCH_MAP));
        FINAL_NETHERITE_TOUCH_BWD = merge(FINAL_DIAMOND_TOUCH_BWD, merge(NETHERITE_SPATULA_MAP, NETHERITE_TOUCH_SPATULA_MAP));
    }

    // =================================================================================
    // KONSTRUKTOR & LOGIK
    // =================================================================================

    // ÄNDERUNG: Konstruktor angepasst für MiningToolItem
    public ChiselItem(ToolMaterial material, Settings settings) {
        super(settings); // 'Item' Konstruktor
        this.material = material;

        // Ersetze switch-case mit if-else, da ToolMaterial Objekte sind und kein konstantes Pattern.
        // Vergleiche Referenzen (== funktioniert für die statischen ToolMaterial Felder).

        if (material == ToolMaterial.STONE) {
            this.forwardMap = FINAL_STONE_FWD;
            this.backwardMap = FINAL_STONE_BWD;
            this.touchForwardMap = FINAL_STONE_TOUCH_FWD;
            this.touchBackwardMap = FINAL_STONE_TOUCH_BWD;
        } else if (material == ToolMaterial.COPPER || material == ToolMaterial.IRON) {
            this.forwardMap = FINAL_IRON_FWD;
            this.backwardMap = FINAL_IRON_BWD;
            this.touchForwardMap = FINAL_IRON_TOUCH_FWD;
            this.touchBackwardMap = FINAL_IRON_TOUCH_BWD;
        } else if (material == ToolMaterial.GOLD || material == ToolMaterial.DIAMOND) {
            this.forwardMap = FINAL_DIAMOND_FWD;
            this.backwardMap = FINAL_DIAMOND_BWD;
            this.touchForwardMap = FINAL_DIAMOND_TOUCH_FWD;
            this.touchBackwardMap = FINAL_DIAMOND_TOUCH_BWD;
        } else if (material == ToolMaterial.NETHERITE) {
            this.forwardMap = FINAL_NETHERITE_FWD;
            this.backwardMap = FINAL_NETHERITE_BWD;
            this.touchForwardMap = FINAL_NETHERITE_TOUCH_FWD;
            this.touchBackwardMap = FINAL_NETHERITE_TOUCH_BWD;
        } else {
            // Fallback
            this.forwardMap = Map.of();
            this.backwardMap = Map.of();
            this.touchForwardMap = Map.of();
            this.touchBackwardMap = Map.of();
        }
    }

    // Material Getter
    public ToolMaterial getMaterial() {
        return this.material;
    }

    // Bestimmt, ob Drops fallen (effektiv gegen Pickaxe, Axe, Shovel Blöcke)
    @Override
    public boolean isCorrectForDrops(ItemStack stack, BlockState state) {
        return state.isIn(BlockTags.PICKAXE_MINEABLE) ||
                state.isIn(BlockTags.AXE_MINEABLE) ||
                state.isIn(BlockTags.SHOVEL_MINEABLE);
    }

    // Berechnet die Abbaugeschwindigkeit
    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        // 1. Ist das Werkzeug effektiv?
        if (!isCorrectForDrops(stack, state)) return 1.0f;

        // FIX: benutze material.speed() statt getMiningSpeedMultiplier()
        // Da ToolMaterial ein Record ist, heißt die Methode so wie das Feld: speed()
        float materialSpeed = this.material.speed();

        // 3. Fast Chiseling Bonus
        int fastChiselingLevel = getFastChiselingLevel(stack);
        float efficiencyBonus = 0.0f;

        if (fastChiselingLevel == 1) {
            efficiencyBonus = 5.0f; // Effizienz 2 Äquivalent
        } else if (fastChiselingLevel >= 2) {
            efficiencyBonus = 17.0f; // Effizienz 4 Äquivalent
        }

        // 4. Halbe Geschwindigkeit
        return (materialSpeed + efficiencyBonus) * 0.5f;
    }

    // Sorgt dafür, dass Haltbarkeit beim normalen Abbauen abgezogen wird
    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient() && state.getHardness(world, pos) != 0.0F) {
            stack.damage(2, miner, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    private int getFastChiselingLevel(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) return 0;

        // Wir iterieren durch alle Enchants auf dem Item
        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            // Wir prüfen, ob der Key des Enchantments mit unserem ModEnchantments Key übereinstimmt
            if (entry.matchesKey(ModEnchantments.FAST_CHISELING)) {
                return enchantments.getLevel(entry);
            }
        }
        return 0;
    }

    public void setCooldownTicks(int ticks) { this.cooldownTicks = ticks; }
    public void setChiselSound(SoundEvent chiselSound) { this.chiselSound = chiselSound; }
    public void setChiselDirectionCycle(Direction direction) { this.chiselDirection = direction; }
    private boolean isDedicatedSpatula = false;
    public void setAsDedicatedSpatula(boolean value) { this.isDedicatedSpatula = value; }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        assert context.getPlayer() != null;
        // Vorab-Check ob Client oder Cooldown, spart Rechenleistung
        if (context.getWorld().isClient() || context.getPlayer().getItemCooldownManager().isCoolingDown(context.getStack())) {
            return ActionResult.PASS;
        }


        Vec3d relativeHit = context.getHitPos().subtract(Vec3d.of(context.getBlockPos()));

        return tryChiselBlock(context.getWorld(), context.getPlayer(), context.getHand(), context.getBlockPos(), context.getStack(), context.getSide(), relativeHit)
                ? ActionResult.SUCCESS : ActionResult.PASS;
    }

    private boolean tryChiselBlock(World world, PlayerEntity player, Hand hand, BlockPos pos, ItemStack stack, net.minecraft.util.math.Direction side, Vec3d relativeHit) {
        if (player.getItemCooldownManager().isCoolingDown(stack)) return false;

        BlockState oldState = world.getBlockState(pos);
        Block oldBlock = oldState.getBlock();

        RegistryWrapper.WrapperLookup registryManager = world.getRegistryManager();

        // Optimierung: Nur Enchantment abrufen, wenn nötig
        // Touch brauchen wir für die Map-Auswahl
        var touchEntry = getEnchantment(registryManager, ModEnchantments.CONSTRUCTORS_TOUCH);
        boolean hasConstructorsTouch = touchEntry != null && EnchantmentHelper.getLevel(touchEntry, stack) > 0;

        boolean isSneaking = player.isSneaking();
        boolean isReverseAction = false;

        Map<Block, Block> currentMap;

        if (this.isDedicatedSpatula) {
            // Spatel Logik: Standard ist Rückwärts
            if (isSneaking) {
                // Spatel + Sneak = Vorwärts? (Optional, aktuell nicht gefordert, aber logisch)
                currentMap = hasConstructorsTouch ? this.touchForwardMap : this.forwardMap;
            } else {
                currentMap = hasConstructorsTouch ? this.touchBackwardMap : this.backwardMap;
            }
        } else {
            // Meißel Logik: Standard ist Vorwärts
            if (isSneaking) {
                // Meißel + Sneak = Rückwärts ("Entchisseln") -> TEUER!
                currentMap = hasConstructorsTouch ? this.touchBackwardMap : this.backwardMap;
                isReverseAction = true;
            } else {
                currentMap = hasConstructorsTouch ? this.touchForwardMap : this.forwardMap;
            }
        }

        if (currentMap.containsKey(oldBlock)) {
            Block newBlock = currentMap.get(oldBlock);
            BlockState newState = newBlock.getDefaultState();

            // 1. Properties kopieren (Waterlogged, etc.)
            for (Property<?> prop : oldState.getProperties()) {
                if (newState.contains(prop)) {
                    newState = copyProperty(oldState, newState, prop);
                }
            }

            // Intuitive Ausrichtung anwenden
            newState = applyIntuitiveOrientation(newState, side, relativeHit, player);

            world.setBlockState(pos, newState);

            // Cooldown Berechnung mit Fast Chiseling
            var fastChiselEntry = getEnchantment(registryManager, ModEnchantments.FAST_CHISELING);
            int fastChiselingLevel = fastChiselEntry != null ? EnchantmentHelper.getLevel(fastChiselEntry, stack) : 0;

            int finalCooldown = this.cooldownTicks;
            if (fastChiselingLevel > 0) {
                finalCooldown = Math.max(1, (int)(finalCooldown * (1.0f - (fastChiselingLevel * 0.3f))));
            }

            if (!player.getAbilities().creativeMode) {
                player.getItemCooldownManager().set(stack, finalCooldown);
                int damageAmount = isReverseAction ? 2 : 1;
                // Unbreaking Logik ist in stack.damage enthalten
                stack.damage(damageAmount, (ServerWorld) world, (ServerPlayerEntity) player,
                        item -> player.sendEquipmentBreakStatus(item, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
            }

            // Sound mit leichter Variation (Pitch 0.8 - 1.2) klingt natürlicher
            float pitch = 1.0F + (world.random.nextFloat() * 0.4F - 0.2F);
            world.playSound(null, pos, chiselSound, SoundCategory.BLOCKS, 0.5f, pitch);

            spawnEffects((ServerWorld) world, pos, oldState);
            stack.set(ModDataComponentTypes.COORDINATES, pos);

            return true;
        }
        return false;
    }

    // Generischer Helper für Property Copying (Typensicherheit)
    private <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.with(property, from.get(property));
    }

    private void spawnEffects(ServerWorld world, BlockPos pos, BlockState oldState) {
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, oldState),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.1);
    }

    // =================================================================================
    // HELPER METHODEN
    // =================================================================================

    private static void registerLinear(Map<Block, Block> forward, Map<Block, Block> backward, Block... blocks) {
        if (blocks.length < 2) return;
        for (int i = 0; i < blocks.length - 1; i++) {
            Block current = blocks[i];
            Block next = blocks[i + 1];
            forward.put(current, next);
            backward.put(next, current);
        }
    }

    private static void registerCyclic(Map<Block, Block> forward, Map<Block, Block> backward, Block... blocks) {
        if (blocks.length < 2) return;
        for (int i = 0; i < blocks.length; i++) {
            Block current = blocks[i];
            Block next = blocks[(i + 1) % blocks.length];
            forward.put(current, next);
            backward.put(next, current);
        }
    }

    private static Map<Block, Block> merge(Map<Block, Block> base, Map<Block, Block> addition) {
        Map<Block, Block> result = new HashMap<>(base);
        result.putAll(addition);
        return Map.copyOf(result);
    }

    private static void registerLogs() {
        registerLinear(ChiselItem.STONE_TOUCH_MAP, ChiselItem.STONE_TOUCH_SPATULA_MAP, Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG);
        registerLinear(ChiselItem.STONE_TOUCH_MAP, ChiselItem.STONE_TOUCH_SPATULA_MAP, Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG);
        registerLinear(ChiselItem.STONE_TOUCH_MAP, ChiselItem.STONE_TOUCH_SPATULA_MAP, Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG);
        registerLinear(ChiselItem.STONE_TOUCH_MAP, ChiselItem.STONE_TOUCH_SPATULA_MAP, Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG);
        registerLinear(ChiselItem.STONE_TOUCH_MAP, ChiselItem.STONE_TOUCH_SPATULA_MAP, Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG);
        registerLinear(ChiselItem.STONE_TOUCH_MAP, ChiselItem.STONE_TOUCH_SPATULA_MAP, Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG);
        registerLinear(ChiselItem.STONE_TOUCH_MAP, ChiselItem.STONE_TOUCH_SPATULA_MAP, Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG);
        registerLinear(ChiselItem.STONE_TOUCH_MAP, ChiselItem.STONE_TOUCH_SPATULA_MAP, Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG);
        registerLinear(ChiselItem.STONE_TOUCH_MAP, ChiselItem.STONE_TOUCH_SPATULA_MAP, Blocks.PALE_OAK_LOG, Blocks.STRIPPED_PALE_OAK_LOG);
    }

    private static void registerWood() {
        registerLinear(ChiselItem.IRON_TOUCH_MAP, ChiselItem.IRON_TOUCH_SPATULA_MAP, Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_WOOD);
        registerLinear(ChiselItem.IRON_TOUCH_MAP, ChiselItem.IRON_TOUCH_SPATULA_MAP, Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD);
        registerLinear(ChiselItem.IRON_TOUCH_MAP, ChiselItem.IRON_TOUCH_SPATULA_MAP, Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD);
        registerLinear(ChiselItem.IRON_TOUCH_MAP, ChiselItem.IRON_TOUCH_SPATULA_MAP, Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_WOOD);
        registerLinear(ChiselItem.IRON_TOUCH_MAP, ChiselItem.IRON_TOUCH_SPATULA_MAP, Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_WOOD);
        registerLinear(ChiselItem.IRON_TOUCH_MAP, ChiselItem.IRON_TOUCH_SPATULA_MAP, Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD);
        registerLinear(ChiselItem.IRON_TOUCH_MAP, ChiselItem.IRON_TOUCH_SPATULA_MAP, Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_WOOD);
        registerLinear(ChiselItem.IRON_TOUCH_MAP, ChiselItem.IRON_TOUCH_SPATULA_MAP, Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_WOOD);
        registerLinear(ChiselItem.IRON_TOUCH_MAP, ChiselItem.IRON_TOUCH_SPATULA_MAP, Blocks.PALE_OAK_WOOD, Blocks.STRIPPED_PALE_OAK_WOOD);
    }
    private static void registerWoodVariants(Map<Block, Block> forward, Map<Block, Block> backward) {
        // Oak
        registerLinear(forward, backward, Blocks.OAK_PLANKS, Blocks.OAK_STAIRS);
        registerLinear(forward, backward, Blocks.OAK_STAIRS, Blocks.OAK_SLAB);
        // Spruce
        registerLinear(forward, backward, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS);
        registerLinear(forward, backward, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB);

        // Birch
        registerLinear(forward, backward, Blocks.BIRCH_PLANKS, Blocks.BIRCH_STAIRS);
        registerLinear(forward, backward, Blocks.BIRCH_STAIRS, Blocks.BIRCH_SLAB);

        // Jungle
        registerLinear(forward, backward, Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_STAIRS);
        registerLinear(forward, backward, Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_SLAB);

        // Acacia
        registerLinear(forward, backward, Blocks.ACACIA_PLANKS, Blocks.ACACIA_STAIRS);
        registerLinear(forward, backward, Blocks.ACACIA_STAIRS, Blocks.ACACIA_SLAB);

        // Dark Oak
        registerLinear(forward, backward, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_STAIRS);
        registerLinear(forward, backward, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB);

        // Mangrove
        registerLinear(forward, backward, Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_STAIRS);
        registerLinear(forward, backward, Blocks.MANGROVE_STAIRS, Blocks.MANGROVE_SLAB);

        // Cherry
        registerLinear(forward, backward, Blocks.CHERRY_PLANKS, Blocks.CHERRY_STAIRS);
        registerLinear(forward, backward, Blocks.CHERRY_STAIRS, Blocks.CHERRY_SLAB);
        // Bamboo
        registerLinear(forward, backward, Blocks.BAMBOO_PLANKS, Blocks.BAMBOO_STAIRS);
        registerLinear(forward, backward, Blocks.BAMBOO_STAIRS, Blocks.BAMBOO_SLAB);
        // Pale Oak
        registerLinear(forward, backward, Blocks.PALE_OAK_PLANKS, Blocks.PALE_OAK_STAIRS);
        registerLinear(forward, backward, Blocks.PALE_OAK_STAIRS, Blocks.PALE_OAK_SLAB);
    }

    // Registriert Planks -> Stairs -> Slab für Nether Hölzer (Crimson/Warped)
    private static void registerNetherWoodVariants(Map<Block, Block> forward, Map<Block, Block> backward) {
        // Crimson
        registerLinear(forward, backward, Blocks.CRIMSON_PLANKS, Blocks.CRIMSON_STAIRS);
        registerLinear(forward, backward, Blocks.CRIMSON_STAIRS, Blocks.CRIMSON_SLAB);

        // Warped
        registerLinear(forward, backward, Blocks.WARPED_PLANKS, Blocks.WARPED_STAIRS);
        registerLinear(forward, backward, Blocks.WARPED_STAIRS, Blocks.WARPED_SLAB);
    }

    private static void registerAndesiteDioriteGranite() {
        // Andesite
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_ANDESITE, Blocks.ANDESITE);
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_ANDESITE_STAIRS, Blocks.ANDESITE_STAIRS);
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_ANDESITE_SLAB, Blocks.ANDESITE_SLAB);

        // Diorite
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_DIORITE, Blocks.DIORITE);
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_DIORITE_STAIRS, Blocks.DIORITE_STAIRS);
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_DIORITE_SLAB, Blocks.DIORITE_SLAB);

        // Granite
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_GRANITE, Blocks.GRANITE);
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_GRANITE_STAIRS, Blocks.GRANITE_STAIRS);
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_GRANITE_SLAB, Blocks.GRANITE_SLAB);
    }

    private static void registerStems() {
        registerLinear(ChiselItem.NETHERITE_TOUCH_MAP, ChiselItem.NETHERITE_TOUCH_SPATULA_MAP, Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM);
        registerLinear(ChiselItem.NETHERITE_TOUCH_MAP, ChiselItem.NETHERITE_TOUCH_SPATULA_MAP, Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM);
    }

    private static void registerConcrete() {
        Block[] blocks = {
            Blocks.WHITE_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.MAGENTA_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE,
            Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.PINK_CONCRETE, Blocks.GRAY_CONCRETE,
            Blocks.LIGHT_GRAY_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.BLUE_CONCRETE,
            Blocks.BROWN_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.RED_CONCRETE, Blocks.BLACK_CONCRETE
        };
        Block[] powders = {
            Blocks.WHITE_CONCRETE_POWDER, Blocks.ORANGE_CONCRETE_POWDER, Blocks.MAGENTA_CONCRETE_POWDER, Blocks.LIGHT_BLUE_CONCRETE_POWDER,
            Blocks.YELLOW_CONCRETE_POWDER, Blocks.LIME_CONCRETE_POWDER, Blocks.PINK_CONCRETE_POWDER, Blocks.GRAY_CONCRETE_POWDER,
            Blocks.LIGHT_GRAY_CONCRETE_POWDER, Blocks.CYAN_CONCRETE_POWDER, Blocks.PURPLE_CONCRETE_POWDER, Blocks.BLUE_CONCRETE_POWDER,
            Blocks.BROWN_CONCRETE_POWDER, Blocks.GREEN_CONCRETE_POWDER, Blocks.RED_CONCRETE_POWDER, Blocks.BLACK_CONCRETE_POWDER
        };
        for(int i=0; i < blocks.length; i++) {
            registerLinear(ChiselItem.NETHERITE_TOUCH_MAP, ChiselItem.NETHERITE_TOUCH_SPATULA_MAP, blocks[i], powders[i]);
        }
    }

    public boolean canChisel(World world, BlockPos pos, ItemStack stack, PlayerEntity player) {
        // 1. Cooldown Check
        if (player.getItemCooldownManager().isCoolingDown(stack)) return false;

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // 2. Enchantment Check
        RegistryWrapper.WrapperLookup registryManager = world.getRegistryManager();
        var touchEntry = getEnchantment(registryManager, ModEnchantments.CONSTRUCTORS_TOUCH);
        boolean hasConstructorsTouch = touchEntry != null && EnchantmentHelper.getLevel(touchEntry, stack) > 0;

        boolean isSneaking = player.isSneaking();
        Map<Block, Block> currentMap;

        // 3. Map Auswahl (Exakt dieselbe Logik wie in tryChiselBlock)
        if (this.isDedicatedSpatula) {
            // Spatel Logik
            if (isSneaking) {
                currentMap = hasConstructorsTouch ? this.touchForwardMap : this.forwardMap;
            } else {
                currentMap = hasConstructorsTouch ? this.touchBackwardMap : this.backwardMap;
            }
        } else {
            // Meißel Logik
            if (isSneaking) {
                // Sneaken beim Meißel = Rückwärts.
                // Das erfordert zwingend eine Backward-Map.
                currentMap = hasConstructorsTouch ? this.touchBackwardMap : this.backwardMap;
            } else {
                // Nicht Sneaken = Vorwärts
                currentMap = hasConstructorsTouch ? this.touchForwardMap : this.forwardMap;
            }
        }

        // 4. Prüfung: Ist der Block in der AKTUELLEN Map?
        // Wenn ich sneake, aber der Block ist nur in der Forward-Map, gibt das hier false zurück -> Keine Animation.
        return currentMap.containsKey(block);
    }

    private RegistryEntry<Enchantment> getEnchantment(RegistryWrapper.WrapperLookup registry, net.minecraft.registry.RegistryKey<Enchantment> key) {
        Optional<RegistryEntry.Reference<Enchantment>> optional = registry.getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(key);
        return optional.orElse(null);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        if(stack.get(ModDataComponentTypes.COORDINATES) != null) {
            BlockPos p = stack.get(ModDataComponentTypes.COORDINATES);
            assert p != null;
            textConsumer.accept(Text.literal("Last Target: " + p.getX() + ", " + p.getY() + ", " + p.getZ())
                    .formatted(Formatting.GRAY));
        }
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }

    public static BlockState applyIntuitiveOrientation(BlockState state, net.minecraft.util.math.Direction side, Vec3d hit, PlayerEntity player) {
        // Toleranz für "Mitte" (z.B. 0.2 bedeutet 20% Randbereich auf jeder Seite)
        double margin = 0.25;

        // Lokale Koordinaten (0.0 bis 1.0)
        double x = hit.x;
        double y = hit.y;
        double z = hit.z;
        net.minecraft.util.math.Direction orientation = side;
        boolean isEdge = false;

        if (side.getAxis() == net.minecraft.util.math.Direction.Axis.Y) { // Oben oder Unten geklickt
            if (x < margin) { orientation = net.minecraft.util.math.Direction.WEST; isEdge = true; }
            else if (x > 1 - margin) { orientation = net.minecraft.util.math.Direction.EAST; isEdge = true; }
            else if (z < margin) { orientation = net.minecraft.util.math.Direction.NORTH; isEdge = true; }
            else if (z > 1 - margin) { orientation = net.minecraft.util.math.Direction.SOUTH; isEdge = true; }
        }
        else if (side.getAxis() == net.minecraft.util.math.Direction.Axis.X) { // Ost oder West geklickt
            if (y < margin) { orientation = net.minecraft.util.math.Direction.DOWN; isEdge = true; }
            else if (y > 1 - margin) { orientation = net.minecraft.util.math.Direction.UP; isEdge = true; }
            else if (z < margin) { orientation = net.minecraft.util.math.Direction.NORTH; isEdge = true; }
            else if (z > 1 - margin) { orientation = net.minecraft.util.math.Direction.SOUTH; isEdge = true; }
        }
        else if (side.getAxis() == net.minecraft.util.math.Direction.Axis.Z) { // Nord oder Süd geklickt
            if (y < margin) { orientation = net.minecraft.util.math.Direction.DOWN; isEdge = true; }
            else if (y > 1 - margin) { orientation = net.minecraft.util.math.Direction.UP; isEdge = true; }
            else if (x < margin) { orientation = net.minecraft.util.math.Direction.WEST; isEdge = true; }
            else if (x > 1 - margin) { orientation = net.minecraft.util.math.Direction.EAST; isEdge = true; }
        }

        // --- ANWENDUNG AUF BLÖCKE ---

        // 1. Pillars (Logs, Quartz Pillar, etc.)
        if (state.contains(PillarBlock.AXIS)) {
            net.minecraft.util.math.Direction.Axis axis;
            if (isEdge) {
                axis = orientation.getAxis();
            } else {
                axis = side.getAxis();
            }
            return state.with(PillarBlock.AXIS, axis);
        }

        // 2. Stairs (Treppen)
        if (state.contains(StairsBlock.FACING)) {
            net.minecraft.util.math.Direction facing;
            if (isEdge && orientation.getAxis().isHorizontal()) {
                facing = orientation.getOpposite();
            } else {
                facing = player.getHorizontalFacing();
            }
            state = state.with(StairsBlock.FACING, facing);

            // Half (Oben/Unten)
            BlockHalf half;
            if ((side == net.minecraft.util.math.Direction.UP && !isEdge) || (y < 0.5 && !isEdge)) {
                half = BlockHalf.BOTTOM;
            } else if ((side == net.minecraft.util.math.Direction.DOWN && !isEdge) || (y > 0.5 && !isEdge)) {
                half = BlockHalf.TOP;
            } else {
                if (y > 0.5) half = BlockHalf.TOP; else half = BlockHalf.BOTTOM;
            }
            if (orientation == net.minecraft.util.math.Direction.UP) half = BlockHalf.BOTTOM;
            if (orientation == net.minecraft.util.math.Direction.DOWN) half = BlockHalf.TOP;

            state = state.with(StairsBlock.HALF, half);
            return state;
        }

        // 3. Rods (End Rods, Lightning Rods, etc.)
        if (state.contains(Properties.FACING)) {
            return state.with(Properties.FACING, isEdge ? orientation : side);
        }

        return state;
    }
}