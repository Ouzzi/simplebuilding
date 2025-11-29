package com.simplebuilding.items.custom;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

// Entchantments:
// - Fast Chiseling I, II: Reduziert den Cooldown (done)
// - Constructor's Touch: Fügt weitere Block-Transformationen hinzu (done)
// - Range I, II: Erlaubt das Chiseln von weiter entfernten Blöcken (done)
// - Unbreaking I, II, III: Reduziert die Abnutzung (done, wird von Vanilla 'damage' gehandled)
// - Mending: Repariert den Chisel mit gesammelten XP (done, wird von Vanilla 'damage' gehandled)

public class ChiselItem extends Item {

    public enum Direction {
        FORWARD,
        BACKWARD
    }

    private Direction chiselDirection = Direction.FORWARD;
    private SoundEvent chiselSound = SoundEvents.UI_STONECUTTER_TAKE_RESULT;
    private int cooldownTicks = 100;

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

    // Chisel = (forewards) transformation from [1] to [2], ...
    // Spatula = (backwards) transformation from [2] to [1], ...

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
        // [smooth -> cut -> red_sand_stone -> chiseled]
        registerLinear(STONE_CHISEL_MAP, STONE_SPATULA_MAP, Blocks.SMOOTH_RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE, Blocks.RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE);
        // [stone -> chiseled]
        registerLinear(STONE_CHISEL_MAP, STONE_SPATULA_MAP, Blocks.STONE, Blocks.CHISELED_STONE_BRICKS);

        // [Mud Bricks -> Packed Mud -> Mud]
        registerLinear(STONE_TOUCH_MAP, STONE_TOUCH_SPATULA_MAP, Blocks.MUD_BRICKS, Blocks.PACKED_MUD, Blocks.MUD);
        // [Cobblestone -> Mossy Cobblestone]
        registerLinear(STONE_TOUCH_MAP, STONE_TOUCH_SPATULA_MAP, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
        // [All Logs -> Stripped Logs]
        registerLogs(STONE_TOUCH_MAP, STONE_TOUCH_SPATULA_MAP);

        // =================================================================================
        // 2. IRON / COPPER TIER (Alles Linear)
        // =================================================================================

        // [chiseled -> brick -> cracked]
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.CHISELED_STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS);

        // [andesite -> polished_andesite]
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_ANDESITE, Blocks.ANDESITE);
        // [diorite -> polished_diorite]
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_DIORITE, Blocks.DIORITE);
        // [granite -> polished_granite]
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_GRANITE, Blocks.GRANITE);
        // [tuff -> polished_tuff]
        registerLinear(IRON_CHISEL_MAP, IRON_SPATULA_MAP, Blocks.POLISHED_TUFF, Blocks.TUFF);

        // [Bricks -> Mud Bricks]
        registerLinear(IRON_TOUCH_MAP, IRON_TOUCH_SPATULA_MAP, Blocks.BRICKS, Blocks.MUD_BRICKS);
        // [All Woods -> Stripped Wood]
        registerWood(IRON_TOUCH_MAP, IRON_TOUCH_SPATULA_MAP);

        // =================================================================================
        // 3. GOLD TIER (Linear)
        // =================================================================================

        // [smooth -> pillar -> brick -> chiseled -> block] (Updated Order)
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.SMOOTH_QUARTZ, Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_BRICKS, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK);
        // [tuff -> chiseled -> brick]
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.POLISHED_TUFF, Blocks.TUFF, Blocks.CHISELED_TUFF, Blocks.TUFF_BRICKS);

        // [Prismarine -> Prismarine Bricks]
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS);
        // [Smooth Stone -> Stone]
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.SMOOTH_STONE, Blocks.STONE);

        // =================================================================================
        // 4. DIAMOND TIER (Mix Linear & Cyclic)
        // =================================================================================

        // [Polished Blackstone -> Chiseled Blackstone -> Blackstone Bricks -> Cracked Blackstone Bricks]
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.POLISHED_BLACKSTONE, Blocks.BLACKSTONE, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
        // [Basalt -> Smooth Basalt -> Polished Basalt]
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.POLISHED_BASALT);
        // [Polished Deepslate -> Chiseled Deepslate -> Deepslate Bricks -> Cracked Deepslate Bricks -> Deepslate Tiles -> Cracked Deepslate Tiles -> Deepslate -> Cobbled Deepslate]
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.POLISHED_DEEPSLATE, Blocks.CHISELED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE);

        // [Cracked Stone Bricks -> Cobblestone]
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.CRACKED_STONE_BRICKS, Blocks.COBBLESTONE);
        // [Tuff -> Chiseled Tuff Bricks]
        registerLinear(DIAMOND_CHISEL_MAP, DIAMOND_SPATULA_MAP, Blocks.TUFF_BRICKS, Blocks.CHISELED_TUFF_BRICKS);

        // [endstone -> endstone_bricks]
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.END_STONE, Blocks.END_STONE_BRICKS);
        // [purpur_pillar -> purpur_block]
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.PURPUR_PILLAR, Blocks.PURPUR_BLOCK);
        // [copper_block -> cut_copper -> chiseled_copper_block -> copper_grate]
        registerLinear(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.COPPER_BLOCK, Blocks.CUT_COPPER, Blocks.CHISELED_COPPER, Blocks.COPPER_GRATE);


        // [Dead Corals] -> CYCLIC (Circle)
        registerCyclic(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.DEAD_BRAIN_CORAL_BLOCK, Blocks.DEAD_BUBBLE_CORAL_BLOCK, Blocks.DEAD_FIRE_CORAL_BLOCK, Blocks.DEAD_HORN_CORAL_BLOCK, Blocks.DEAD_TUBE_CORAL_BLOCK);
        // [Alive Corals] -> CYCLIC (Circle)
        registerCyclic(DIAMOND_TOUCH_MAP, DIAMOND_TOUCH_SPATULA_MAP, Blocks.BRAIN_CORAL_BLOCK, Blocks.BUBBLE_CORAL_BLOCK, Blocks.FIRE_CORAL_BLOCK, Blocks.HORN_CORAL_BLOCK, Blocks.TUBE_CORAL_BLOCK);

        // =================================================================================
        // 5. NETHERITE TIER (Mix Linear & Cyclic)
        // =================================================================================

        // [Netherrack -> Nether Bricks -> Cracked Nether Bricks -> Chiseled Nether Bricks] -> CYCLIC (Circle)
        registerCyclic(NETHERITE_CHISEL_MAP, NETHERITE_SPATULA_MAP, Blocks.NETHERRACK, Blocks.NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS);
        // [Resin Bricks -> Chiseled Resin Bricks]
        registerLinear(NETHERITE_CHISEL_MAP, NETHERITE_SPATULA_MAP, Blocks.RESIN_BRICKS, Blocks.CHISELED_RESIN_BRICKS);
        // [chiseled_sand_stone -> sand]
        registerLinear(NETHERITE_CHISEL_MAP, NETHERITE_SPATULA_MAP, Blocks.CHISELED_SANDSTONE, Blocks.SAND);
        // [chiseled_red_sand_stone -> red_sand]
        registerLinear(NETHERITE_CHISEL_MAP, NETHERITE_SPATULA_MAP, Blocks.CHISELED_RED_SANDSTONE, Blocks.RED_SAND);

        // [tuff -> calcite -> dripstone] (Updated Order)
        registerLinear(NETHERITE_TOUCH_MAP, NETHERITE_TOUCH_SPATULA_MAP, Blocks.POLISHED_DIORITE, Blocks.DIORITE, Blocks.CALCITE, Blocks.DRIPSTONE_BLOCK);
        // [obsidian -> crying_obsidian]
        registerLinear(NETHERITE_TOUCH_MAP, NETHERITE_TOUCH_SPATULA_MAP, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN);
        // [All Stems -> Stripped Stems]
        registerStems(NETHERITE_TOUCH_MAP, NETHERITE_TOUCH_SPATULA_MAP);
        // [Every Concrete & Concrete Powder]
        registerConcrete(NETHERITE_TOUCH_MAP, NETHERITE_TOUCH_SPATULA_MAP);

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

    public ChiselItem(Settings settings, String tier) {
        super(settings);
        if (tier.equals("stone")) {
            this.forwardMap = FINAL_STONE_FWD;
            this.backwardMap = FINAL_STONE_BWD;
            this.touchForwardMap = FINAL_STONE_TOUCH_FWD;
            this.touchBackwardMap = FINAL_STONE_TOUCH_BWD;
        } else if (tier.equals("iron") || tier.equals("copper")) {
            this.forwardMap = FINAL_IRON_FWD;
            this.backwardMap = FINAL_IRON_BWD;
            this.touchForwardMap = FINAL_IRON_TOUCH_FWD;
            this.touchBackwardMap = FINAL_IRON_TOUCH_BWD;
        } else if (tier.equals("gold") || tier.equals("diamond")) {
            this.forwardMap = FINAL_DIAMOND_FWD;
            this.backwardMap = FINAL_DIAMOND_BWD;
            this.touchForwardMap = FINAL_DIAMOND_TOUCH_FWD;
            this.touchBackwardMap = FINAL_DIAMOND_TOUCH_BWD;
        } else if (tier.equals("netherite")) {
            this.forwardMap = FINAL_NETHERITE_FWD;
            this.backwardMap = FINAL_NETHERITE_BWD;
            this.touchForwardMap = FINAL_NETHERITE_TOUCH_FWD;
            this.touchBackwardMap = FINAL_NETHERITE_TOUCH_BWD;
        } else {
            this.forwardMap = Map.of();
            this.backwardMap = Map.of();
            this.touchForwardMap = Map.of();
            this.touchBackwardMap = Map.of();
        }
    }

    public void setCooldownTicks(int ticks) { this.cooldownTicks = ticks; }
    public void setChiselSound(SoundEvent chiselSound) { this.chiselSound = chiselSound; }
    public void setChiselDirectionCycle(Direction direction) { this.chiselDirection = direction; }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return tryChiselBlock(context.getWorld(), context.getPlayer(), context.getHand(), context.getBlockPos(), context.getStack())
                ? ActionResult.SUCCESS : ActionResult.PASS;
    }

    private boolean tryChiselBlock(World world, PlayerEntity player, Hand hand, BlockPos pos, ItemStack stack) {
        if (player.getItemCooldownManager().isCoolingDown(stack)) return false;

        BlockState oldState = world.getBlockState(pos);
        Block oldBlock = oldState.getBlock();

        RegistryWrapper.WrapperLookup registryManager = world.getRegistryManager();
        var fastChiselEntry = getEnchantment(registryManager, ModEnchantments.FAST_CHISELING);
        var touchEntry = getEnchantment(registryManager, ModEnchantments.CONSTRUCTORS_TOUCH);

        int fastChiselingLevel = fastChiselEntry != null ? EnchantmentHelper.getLevel(fastChiselEntry, stack) : 0;
        boolean hasConstructorsTouch = touchEntry != null && EnchantmentHelper.getLevel(touchEntry, stack) > 0;

        Map<Block, Block> currentMap;
        if (this.chiselDirection == Direction.FORWARD) {
            currentMap = hasConstructorsTouch ? this.touchForwardMap : this.forwardMap;
        } else {
            currentMap = hasConstructorsTouch ? this.touchBackwardMap : this.backwardMap;
        }

        if (currentMap.containsKey(oldBlock)) {
            if (!world.isClient()) {
                Block newBlock = currentMap.get(oldBlock);
                world.setBlockState(pos, newBlock.getDefaultState());

                int finalCooldown = this.cooldownTicks;
                if (fastChiselingLevel > 0) {
                    finalCooldown = Math.max(1, (int)(finalCooldown * (1.0f - (fastChiselingLevel * 0.2f))));
                }

                if (!player.getAbilities().creativeMode) {
                    player.getItemCooldownManager().set(stack, finalCooldown);
                    stack.damage(1, (ServerWorld) world, (ServerPlayerEntity) player,
                            item -> player.sendEquipmentBreakStatus(item, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
                }

                world.playSound(null, pos, chiselSound, SoundCategory.BLOCKS, 0.5f, 1.5f);
                spawnEffects((ServerWorld) world, pos, oldState);
                stack.set(ModDataComponentTypes.COORDINATES, pos);
            }
            return true;
        }
        return false;
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

    private static void registerLogs(Map<Block, Block> f, Map<Block, Block> b) {
        registerLinear(f, b, Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG);
        registerLinear(f, b, Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG);
        registerLinear(f, b, Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG);
        registerLinear(f, b, Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG);
        registerLinear(f, b, Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG);
        registerLinear(f, b, Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG);
        registerLinear(f, b, Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG);
        registerLinear(f, b, Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG);
        registerLinear(f, b, Blocks.PALE_OAK_LOG, Blocks.STRIPPED_PALE_OAK_LOG);
    }

    private static void registerWood(Map<Block, Block> f, Map<Block, Block> b) {
        registerLinear(f, b, Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_WOOD);
        registerLinear(f, b, Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD);
        registerLinear(f, b, Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD);
        registerLinear(f, b, Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_WOOD);
        registerLinear(f, b, Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_WOOD);
        registerLinear(f, b, Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD);
        registerLinear(f, b, Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_WOOD);
        registerLinear(f, b, Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_WOOD);
        registerLinear(f, b, Blocks.PALE_OAK_WOOD, Blocks.STRIPPED_PALE_OAK_WOOD);
    }

    private static void registerStems(Map<Block, Block> f, Map<Block, Block> b) {
        registerLinear(f, b, Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM);
        registerLinear(f, b, Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM);
    }

    private static void registerConcrete(Map<Block, Block> f, Map<Block, Block> b) {
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
            registerLinear(f, b, blocks[i], powders[i]);
        }
    }

    private RegistryEntry<Enchantment> getEnchantment(RegistryWrapper.WrapperLookup registry, net.minecraft.registry.RegistryKey<Enchantment> key) {
        Optional<RegistryEntry.Reference<Enchantment>> optional = registry.getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(key);
        return optional.orElse(null);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        if(stack.get(ModDataComponentTypes.COORDINATES) != null) {
            BlockPos p = stack.get(ModDataComponentTypes.COORDINATES);
            textConsumer.accept(Text.literal("Last Target: " + p.getX() + ", " + p.getY() + ", " + p.getZ())
                    .formatted(Formatting.GRAY));
        }
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}