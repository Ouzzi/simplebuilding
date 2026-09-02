package com.simplebuilding.items.custom;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.simplebuilding.items.ModToolMaterials;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.simplebuilding.util.EnchantmentHelper.*;

public class ChiselItem extends Item {

    public enum Direction {
        FORWARD,
        BACKWARD
    }

    @SuppressWarnings("unused")
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
        // MC 26.2: Kupferbloecke stehen in Blocks nur noch als WeatheringCopperCollection; plain(...)
        // liefert die unverwitterte, ungewachste Variante -- also exakt die frueheren Konstanten.
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, plain(Blocks.COPPER_BLOCK), plain(Blocks.CUT_COPPER), plain(Blocks.CHISELED_COPPER), plain(Blocks.COPPER_GRATE));
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, plain(Blocks.CUT_COPPER_STAIRS), plain(Blocks.CUT_COPPER_STAIRS)); // Only cut has stairs
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, plain(Blocks.CUT_COPPER_SLAB), plain(Blocks.CUT_COPPER_SLAB));


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
    public ChiselItem(ToolMaterial material, Properties settings) {
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
        } else if (material == ToolMaterial.NETHERITE || material == ModToolMaterials.ENDERITE) {
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
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) ||
                state.is(BlockTags.MINEABLE_WITH_AXE) ||
                state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    // Berechnet die Abbaugeschwindigkeit
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // 1. Ist das Werkzeug effektiv?
        if (!isCorrectToolForDrops(stack, state)) return 1.0f;

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
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClientSide() && state.getDestroySpeed(world, pos) != 0.0F) {
            stack.hurtAndBreak(2, miner, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    public void setCooldownTicks(int ticks) { this.cooldownTicks = ticks; }
    /** Fuer den Wiki-Export (WikiDataProvider): die Abklingzeit dieser Stufe. */
    public int getCooldownTicks() { return this.cooldownTicks; }
    public void setChiselSound(SoundEvent chiselSound) { this.chiselSound = chiselSound; }
    public void setChiselDirectionCycle(Direction direction) { this.chiselDirection = direction; }
    private boolean isDedicatedSpatula = false;
    public void setAsDedicatedSpatula(boolean value) { this.isDedicatedSpatula = value; }
    /** Fuer den Wiki-Export: unterscheidet Spachtel von Meissel. */
    public boolean isDedicatedSpatula() { return this.isDedicatedSpatula; }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        if (context.getLevel().isClientSide()) {
            if (player.getCooldowns().isOnCooldown(context.getItemInHand())) {
                return InteractionResult.PASS;
            }
            Block block = context.getLevel().getBlockState(context.getClickedPos()).getBlock();
            if (this.forwardMap.containsKey(block) || this.backwardMap.containsKey(block)
                    || this.touchForwardMap.containsKey(block) || this.touchBackwardMap.containsKey(block)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        if (player.getCooldowns().isOnCooldown(context.getItemInHand())) {
            return InteractionResult.PASS;
        }

        Vec3 relativeHit = context.getClickLocation().subtract(Vec3.atLowerCornerOf(context.getClickedPos()));

        return tryChiselBlock(context.getLevel(), player, context.getHand(), context.getClickedPos(), context.getItemInHand(), context.getClickedFace(), relativeHit)
                ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
    }

    private boolean tryChiselBlock(Level world, Player player, InteractionHand hand, BlockPos pos, ItemStack stack, net.minecraft.core.Direction side, Vec3 relativeHit) {
        if (player.getCooldowns().isOnCooldown(stack)) return false;

        BlockState oldState = world.getBlockState(pos);
        Block oldBlock = oldState.getBlock();

        var hasConstructorsTouch = hasEnchantment(stack, world, ModEnchantments.CONSTRUCTORS_TOUCH);

        boolean isSneaking = player.isShiftKeyDown();
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
            BlockState newState = newBlock.defaultBlockState();

            // 1. Properties kopieren (Waterlogged, etc.)
            for (Property<?> prop : oldState.getProperties()) {
                if (newState.hasProperty(prop)) {
                    newState = copyProperty(oldState, newState, prop);
                }
            }

            // Intuitive Ausrichtung anwenden
            newState = applyIntuitiveOrientation(newState, side, relativeHit, player);

            world.setBlockAndUpdate(pos, newState);

            // Cooldown Berechnung mit Fast Chiseling

            int fastChiselingLevel = getFastChiselingLevel(stack);

            int finalCooldown = this.cooldownTicks;
            if (fastChiselingLevel > 0) {
                finalCooldown = Math.max(1, (int)(finalCooldown * (1.0f - (fastChiselingLevel * 0.3f))));
            }

            if (!player.getAbilities().instabuild) {
                player.getCooldowns().addCooldown(stack, finalCooldown);
                int damageAmount = isReverseAction ? 2 : 1;
                // Unbreaking Logik ist in stack.damage enthalten
                stack.hurtAndBreak(damageAmount, (ServerLevel) world, (ServerPlayer) player,
                        item -> player.onEquippedItemBroken(item, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
            }

            // Sound mit leichter Variation (Pitch 0.8 - 1.2) klingt natürlicher
            float pitch = 1.0F + (world.getRandom().nextFloat() * 0.4F - 0.2F);
            world.playSound(null, pos, chiselSound, SoundSource.BLOCKS, 0.5f, pitch);

            spawnEffects((ServerLevel) world, pos, oldState);
            stack.set(ModDataComponentTypes.COORDINATES, pos);

            return true;
        }
        return false;
    }

    // Generischer Helper für Property Copying (Typensicherheit)
    private <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    private void spawnEffects(ServerLevel world, BlockPos pos, BlockState oldState) {
        world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, oldState),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.1);
    }

    // =================================================================================
    // HELPER METHODEN
    // =================================================================================

    /**
     * MC 26.2: Kupfer-Bloecke werden in {@link Blocks} als {@link WeatheringCopperCollection}
     * gefuehrt (weathering()/waxed() x unaffected/exposed/weathered/oxidized). Der Praefix der
     * weathering-unaffected-Variante ist leer, sie entspricht damit 1:1 der alten Konstante
     * (z.B. Blocks.COPPER_BLOCK == minecraft:copper_block).
     */
    private static Block plain(WeatheringCopperCollection<Block> copper) {
        return copper.weathering().unaffected();
    }

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
        // MC 26.2: Die 16 Farbvarianten liegen als ColorCollection<Block> vor (Blocks.CONCRETE /
        // Blocks.CONCRETE_POWDER); die Einzelkonstanten Blocks.WHITE_CONCRETE usw. entfielen.
        // asList() liefert white, orange, magenta, light_blue, yellow, lime, pink, gray, light_gray,
        // cyan, purple, blue, brown, green, red, black -- exakt die bisherige Array-Reihenfolge,
        // die Farbpaarung Beton <-> Betonpulver bleibt also unveraendert.
        List<Block> blocks = Blocks.CONCRETE.asList();
        List<Block> powders = Blocks.CONCRETE_POWDER.asList();
        for(int i=0; i < blocks.size(); i++) {
            registerLinear(ChiselItem.NETHERITE_TOUCH_MAP, ChiselItem.NETHERITE_TOUCH_SPATULA_MAP, blocks.get(i), powders.get(i));
        }
    }

    public boolean canChisel(Level world, BlockPos pos, ItemStack stack, Player player) {
        // 1. Cooldown Check
        if (player.getCooldowns().isOnCooldown(stack)) return false;

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        boolean hasConstructorsTouch = hasEnchantment(stack, world, ModEnchantments.CONSTRUCTORS_TOUCH);

        boolean isSneaking = player.isShiftKeyDown();
        Map<Block, Block> currentMap;

        if (this.isDedicatedSpatula) {
            if (isSneaking) {
                currentMap = hasConstructorsTouch ? this.touchForwardMap : this.forwardMap;
            } else {
                currentMap = hasConstructorsTouch ? this.touchBackwardMap : this.backwardMap;
            }
        } else {
            if (isSneaking) {
                currentMap = hasConstructorsTouch ? this.touchBackwardMap : this.backwardMap;
            } else {
                currentMap = hasConstructorsTouch ? this.touchForwardMap : this.forwardMap;
            }
        }
        return currentMap.containsKey(block);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        if(stack.get(ModDataComponentTypes.COORDINATES) != null) {
            BlockPos p = stack.get(ModDataComponentTypes.COORDINATES);
            assert p != null;
            textConsumer.accept(Component.literal("Last Target: " + p.getX() + ", " + p.getY() + ", " + p.getZ())
                    .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }

    public static BlockState applyIntuitiveOrientation(BlockState state, net.minecraft.core.Direction side, Vec3 hit, Player player) {
        // Toleranz für "Mitte" (z.B. 0.2 bedeutet 20% Randbereich auf jeder Seite)
        double margin = 0.25;

        // Lokale Koordinaten (0.0 bis 1.0)
        double x = hit.x;
        double y = hit.y;
        double z = hit.z;
        net.minecraft.core.Direction orientation = side;
        boolean isEdge = false;

        if (side.getAxis() == net.minecraft.core.Direction.Axis.Y) { // Oben oder Unten geklickt
            if (x < margin) { orientation = net.minecraft.core.Direction.WEST; isEdge = true; }
            else if (x > 1 - margin) { orientation = net.minecraft.core.Direction.EAST; isEdge = true; }
            else if (z < margin) { orientation = net.minecraft.core.Direction.NORTH; isEdge = true; }
            else if (z > 1 - margin) { orientation = net.minecraft.core.Direction.SOUTH; isEdge = true; }
        }
        else if (side.getAxis() == net.minecraft.core.Direction.Axis.X) { // Ost oder West geklickt
            if (y < margin) { orientation = net.minecraft.core.Direction.DOWN; isEdge = true; }
            else if (y > 1 - margin) { orientation = net.minecraft.core.Direction.UP; isEdge = true; }
            else if (z < margin) { orientation = net.minecraft.core.Direction.NORTH; isEdge = true; }
            else if (z > 1 - margin) { orientation = net.minecraft.core.Direction.SOUTH; isEdge = true; }
        }
        else if (side.getAxis() == net.minecraft.core.Direction.Axis.Z) { // Nord oder Süd geklickt
            if (y < margin) { orientation = net.minecraft.core.Direction.DOWN; isEdge = true; }
            else if (y > 1 - margin) { orientation = net.minecraft.core.Direction.UP; isEdge = true; }
            else if (x < margin) { orientation = net.minecraft.core.Direction.WEST; isEdge = true; }
            else if (x > 1 - margin) { orientation = net.minecraft.core.Direction.EAST; isEdge = true; }
        }

        // --- ANWENDUNG AUF BLÖCKE ---

        // 1. Pillars (Logs, Quartz Pillar, etc.)
        if (state.hasProperty(RotatedPillarBlock.AXIS)) {
            net.minecraft.core.Direction.Axis axis;
            if (isEdge) {
                axis = orientation.getAxis();
            } else {
                axis = side.getAxis();
            }
            return state.setValue(RotatedPillarBlock.AXIS, axis);
        }

        // 2. Stairs (Treppen)
        if (state.hasProperty(StairBlock.FACING)) {
            net.minecraft.core.Direction facing;
            if (isEdge && orientation.getAxis().isHorizontal()) {
                facing = orientation.getOpposite();
            } else {
                facing = player.getDirection();
            }
            state = state.setValue(StairBlock.FACING, facing);

            // Half (Oben/Unten)
            Half half;
            if ((side == net.minecraft.core.Direction.UP && !isEdge) || (y < 0.5 && !isEdge)) {
                half = Half.BOTTOM;
            } else if ((side == net.minecraft.core.Direction.DOWN && !isEdge) || (y > 0.5 && !isEdge)) {
                half = Half.TOP;
            } else {
                if (y > 0.5) half = Half.TOP; else half = Half.BOTTOM;
            }
            if (orientation == net.minecraft.core.Direction.UP) half = Half.BOTTOM;
            if (orientation == net.minecraft.core.Direction.DOWN) half = Half.TOP;

            state = state.setValue(StairBlock.HALF, half);
            return state;
        }

        // 3. Rods (End Rods, Lightning Rods, etc.)
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.setValue(BlockStateProperties.FACING, isEdge ? orientation : side);
        }

        return state;
    }
}