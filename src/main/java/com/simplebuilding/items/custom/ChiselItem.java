package com.simplebuilding.items.custom;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.particle.ModParticles;
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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

// TODO
// Entchantments:
// - Fast Chiseling I, II: Reduziert den Cooldown (done)
// - Constructor's Touch: Fügt weitere Block-Transformationen hinzu
// - Range I, II: Erlaubt das Chiseln von weiter entfernten Blöcken
// - Unbreaking I, II, III: Reduziert die Abnutzung
// - Mending: Repariert den Chisel mit gesammelten XP

public class ChiselItem extends Item {


    private static Map<Block, Block> mergeMaps(Map<Block, Block> destination, Map<Block, Block> source) {
        Map<Block, Block> result = new HashMap<>(destination);
        result.putAll(source);
        return result;
    }

    // =================================================================================
    // 1. STANDARD MAPS (Ohne Enchantment)
    // =================================================================================

    public static final Map<Block, Block> CHISEL_MAP_STONE = Map.of(
            Blocks.STONE, Blocks.STONE_BRICKS,
            Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS,
            Blocks.SMOOTH_STONE, Blocks.STONE,
            Blocks.POLISHED_ANDESITE, Blocks.ANDESITE,
            Blocks.POLISHED_DIORITE, Blocks.DIORITE,
            Blocks.POLISHED_GRANITE, Blocks.GRANITE
    );

    public static final Map<Block, Block> CHISEL_MAP_IRON_BASE = Map.of(
            Blocks.IRON_BLOCK, Blocks.IRON_BARS,
            Blocks.QUARTZ_BLOCK, Blocks.CHISELED_QUARTZ_BLOCK,
            Blocks.BRICKS, Blocks.CRACKED_STONE_BRICKS
    );

    public static final Map<Block, Block> CHISEL_MAP_DIAMOND_BASE = Map.of(
            Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN,
            Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS,
            Blocks.BASALT, Blocks.POLISHED_BASALT
    );

    public static final Map<Block, Block> CHISEL_MAP_NETHERITE_BASE = Map.of(
            Blocks.POLISHED_BLACKSTONE, Blocks.BLACKSTONE,
            Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.BLACKSTONE,
            Blocks.DEEPSLATE, Blocks.DEEPSLATE_BRICKS,
            Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS,
            Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE
    );

    // Kummulative Standard Maps
    public static final Map<Block, Block> CHISEL_MAP_IRON = mergeMaps(CHISEL_MAP_STONE, CHISEL_MAP_IRON_BASE);
    public static final Map<Block, Block> CHISEL_MAP_DIAMOND = mergeMaps(CHISEL_MAP_IRON, CHISEL_MAP_DIAMOND_BASE);
    public static final Map<Block, Block> CHISEL_MAP_NETHERITE = mergeMaps(CHISEL_MAP_DIAMOND, CHISEL_MAP_NETHERITE_BASE);


    // =================================================================================
    // 2. CONSTRUCTOR'S TOUCH MAPS (Mit Enchantment)
    // =================================================================================

    // --- STONE TIER TOUCH (Holz Transformationen) ---
    // Teil 1 der Holzarten
    public static final Map<Block, Block> STONE_TOUCH_PART1 = Map.of(
            Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG,
            Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG,
            Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG,
            Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG,
            Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG,
            Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG,
            Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG,
            Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG,
            Blocks.STONE, Blocks.COBBLESTONE // Extra Stone Feature
    );
    // Teil 2 der Holzarten
    public static final Map<Block, Block> STONE_TOUCH_PART2 = Map.of(
            Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM,
            Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM,
            Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_WOOD,
            Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD
    );

    // Merge: Stone Touch = Standard Stone + Holz1 + Holz2
    public static final Map<Block, Block> CHISEL_MAP_STONE_CONSTRUCTOR = mergeMaps(
            CHISEL_MAP_STONE,
            mergeMaps(STONE_TOUCH_PART1, STONE_TOUCH_PART2)
    );


    // --- IRON TIER TOUCH (Mossy variants & Sandstone) ---
    public static final Map<Block, Block> IRON_TOUCH_EXTRAS = Map.of(
            Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE,
            Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS,
            Blocks.SANDSTONE, Blocks.CUT_SANDSTONE,
            Blocks.RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE,
            Blocks.ANDESITE, Blocks.POLISHED_ANDESITE, // Reverse mapping enabled by touch
            Blocks.DIORITE, Blocks.POLISHED_DIORITE,
            Blocks.GRANITE, Blocks.POLISHED_GRANITE
    );

    // Merge: Iron Touch = Iron Standard + Stone Touch + Iron Extras
    public static final Map<Block, Block> CHISEL_MAP_IRON_CONSTRUCTOR = mergeMaps(
            CHISEL_MAP_IRON,
            mergeMaps(CHISEL_MAP_STONE_CONSTRUCTOR, IRON_TOUCH_EXTRAS)
    );


    // --- DIAMOND TIER TOUCH (Nether & End extras) ---
    public static final Map<Block, Block> DIAMOND_TOUCH_EXTRAS = Map.of(
            Blocks.NETHERRACK, Blocks.NETHER_BRICKS,
            Blocks.RED_NETHER_BRICKS, Blocks.NETHER_BRICKS,
            Blocks.END_STONE, Blocks.END_STONE_BRICKS,
            Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR,
            Blocks.PRISMARINE, Blocks.DARK_PRISMARINE,
            Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE
    );

    // Merge: Diamond Touch = Diamond Standard + Iron Touch + Diamond Extras
    public static final Map<Block, Block> CHISEL_MAP_DIAMOND_CONSTRUCTOR = mergeMaps(
            CHISEL_MAP_DIAMOND,
            mergeMaps(CHISEL_MAP_IRON_CONSTRUCTOR, DIAMOND_TOUCH_EXTRAS)
    );


    // --- NETHERITE TIER TOUCH (Deepslate & Glass extras) ---
    public static final Map<Block, Block> NETHERITE_TOUCH_EXTRAS = Map.of(
            Blocks.COBBLED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE,
            Blocks.GLASS, Blocks.TINTED_GLASS,
            Blocks.MUD, Blocks.PACKED_MUD,
            Blocks.PACKED_MUD, Blocks.MUD_BRICKS,
            Blocks.NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS,
            Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR
    );

    // Merge: Netherite Touch = Netherite Standard + Diamond Touch + Netherite Extras
    public static final Map<Block, Block> CHISEL_MAP_NETHERITE_CONSTRUCTOR = mergeMaps(
            CHISEL_MAP_NETHERITE,
            mergeMaps(CHISEL_MAP_DIAMOND_CONSTRUCTOR, NETHERITE_TOUCH_EXTRAS)
    );


    // =================================================================================
    // 3. SPATULA MAPS (Umkehr-Funktion)
    // =================================================================================
    public static final Map<Block, Block> SPATULA_MAP_STONE = Map.of(
            Blocks.STONE_BRICKS, Blocks.STONE,
            Blocks.CHISELED_STONE_BRICKS, Blocks.STONE_BRICKS,
            Blocks.CRACKED_STONE_BRICKS, Blocks.STONE_BRICKS
    );
    public static final Map<Block, Block> SPATULA_MAP_IRON_BASE = Map.of(
            Blocks.IRON_BARS, Blocks.IRON_BLOCK,
            Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK,
            Blocks.POLISHED_ANDESITE, Blocks.ANDESITE
    );
    public static final Map<Block, Block> SPATULA_MAP_DIAMOND_BASE = Map.of(
            Blocks.CRYING_OBSIDIAN, Blocks.OBSIDIAN,
            Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE
    );
    public static final Map<Block, Block> SPATULA_MAP_NETHERITE_BASE = Map.of(
            Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.POLISHED_BLACKSTONE,
            Blocks.POLISHED_BASALT, Blocks.BASALT
    );

    public static final Map<Block, Block> SPATULA_MAP_IRON = mergeMaps(SPATULA_MAP_STONE, SPATULA_MAP_IRON_BASE);
    public static final Map<Block, Block> SPATULA_MAP_DIAMOND = mergeMaps(SPATULA_MAP_IRON, SPATULA_MAP_DIAMOND_BASE);
    public static final Map<Block, Block> SPATULA_MAP_NETHERITE = mergeMaps(SPATULA_MAP_DIAMOND, SPATULA_MAP_NETHERITE_BASE);


    private Map<Block, Block> transformationMap;
    private Map<Block, Block> constructorsTouchMap;

    private SoundEvent chiselSound = SoundEvents.UI_STONECUTTER_TAKE_RESULT;
    private int cooldownTicks = 100;

    private static final Map<Block, Block> DEFAULT_CHISEL_MAP = Map.of(
        Blocks.STONE, Blocks.STONE_BRICKS,
        Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS,
        Blocks.CHISELED_STONE_BRICKS, Blocks.COBBLESTONE
    );

    public ChiselItem(Settings settings) {
        super(settings);
        this.transformationMap = DEFAULT_CHISEL_MAP;
        this.constructorsTouchMap = new HashMap<>(DEFAULT_CHISEL_MAP); // Fallback
    }

    public void setTransformationMap(Map<Block, Block> map) {
        this.transformationMap = map;
        // Standardmäßig ist Touch Map gleich der normalen, falls nicht anders gesetzt
        if (this.constructorsTouchMap.equals(DEFAULT_CHISEL_MAP)) {
            this.constructorsTouchMap = map;
        }
    }

    public void setConstructorsTouchMap(Map<Block, Block> map) {
        this.constructorsTouchMap = map;
    }

    public void setCooldownTicks(int ticks) {
        this.cooldownTicks = ticks;
    }

    // --- STANDARD INTERAKTION (Nahkampf) ---
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        // Wir leiten einfach an unsere Logik-Methode weiter
        return tryChiselBlock(context.getWorld(), context.getPlayer(), context.getHand(), context.getBlockPos(), context.getStack())
                ? ActionResult.SUCCESS : ActionResult.PASS;
    }

    // --- RANGE ENCHANTMENT INTERAKTION (Fernkampf) ---
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // Prüfen ob Range Enchantment drauf ist
        RegistryWrapper.WrapperLookup registryManager = world.getRegistryManager();
        var rangeEntry = getEnchantment(registryManager, ModEnchantments.RANGE);

        if (rangeEntry != null) {
            int rangeLevel = EnchantmentHelper.getLevel(rangeEntry, stack);

            if (rangeLevel > 0) {
                // Basis Reichweite (ca 4.5) + Bonus pro Level (z.B. +2 Blöcke pro Level)
                double distance = 4.5d + (rangeLevel * 2.5d);

                // Raycast durchführen
                BlockHitResult hitResult = raycast(world, user, RaycastContext.FluidHandling.NONE, distance);

                if (hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockPos hitPos = hitResult.getBlockPos();
                    // Versuche dort zu chiseln
                    if (tryChiselBlock(world, user, hand, hitPos, stack)) {
                        return ActionResult.SUCCESS;
                    }
                }
            }
        }

        return super.use(world, user, hand);
    }

    // --- KERNLOGIK ---
    private boolean tryChiselBlock(World world, PlayerEntity player, Hand hand, BlockPos pos, ItemStack stack) {
        // 1. Cooldown Check (this bezieht sich auf das Item)
        if (player.getItemCooldownManager().isCoolingDown(stack)) {
            return false;
        }

        BlockState oldState = world.getBlockState(pos);
        Block oldBlock = oldState.getBlock();

        // Enchantment Level holen
        RegistryWrapper.WrapperLookup registryManager = world.getRegistryManager();
        var fastChiselEntry = getEnchantment(registryManager, ModEnchantments.FAST_CHISELING);
        var touchEntry = getEnchantment(registryManager, ModEnchantments.CONSTRUCTORS_TOUCH);

        int fastChiselingLevel = fastChiselEntry != null ? EnchantmentHelper.getLevel(fastChiselEntry, stack) : 0;
        boolean hasConstructorsTouch = touchEntry != null && EnchantmentHelper.getLevel(touchEntry, stack) > 0;

        // Map auswählen: Wenn Enchantment da ist, nimm die TouchMap, sonst die normale
        Map<Block, Block> currentMap = hasConstructorsTouch ? constructorsTouchMap : transformationMap;

        // Prüfen ob Transformation möglich ist
        if (currentMap.containsKey(oldBlock)) {
            if (!world.isClient()) {
                Block newBlock = currentMap.get(oldBlock);
                BlockState newState = newBlock.getDefaultState();

                // 1. Block tatsächlich ändern! (Das fehlte vorher)
                world.setBlockState(pos, newState);

                // 2. Cooldown berechnen (Fast Chiseling)
                int finalCooldown = this.cooldownTicks;
                if (fastChiselingLevel > 0) {
                    // 20% Reduktion pro Level
                    finalCooldown = Math.max(1, (int)(finalCooldown * (1.0f - (fastChiselingLevel * 0.2f))));
                }

                if (!player.getAbilities().creativeMode) {
                    player.getItemCooldownManager().set(stack, finalCooldown);
                    // 3. Durability abziehen (Unbreaking/Mending wird von Vanilla 'damage' handled)
                    stack.damage(1, (ServerWorld) world, (ServerPlayerEntity) player,
                            item -> player.sendEquipmentBreakStatus(item, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
                }

                // 4. Sound & Partikel
                world.playSound(null, pos, chiselSound, SoundCategory.BLOCKS, 0.5f, 1.5f);
                spawnEffects((ServerWorld) world, pos, oldState);

                // 5. Koordinaten speichern (für Tooltip)
                stack.set(ModDataComponentTypes.COORDINATES, pos);
            }
            return true;
        }
        return false;
    }

    private void spawnEffects(ServerWorld world, BlockPos pos, BlockState oldState) {
        // Partikel des alten Blocks
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, oldState),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                8, 0.2, 0.2, 0.2, 0.1);

        // Magische Partikel
        world.spawnParticles(ModParticles.PINK_GARNET_PARTICLE,
                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                5, 0.1, 0.1, 0.1, 0.05);
    }

    // Raycast Helper für Range Enchantment
    protected static BlockHitResult raycast(World world, PlayerEntity player, RaycastContext.FluidHandling fluidHandling, double maxDistance) {
        float pitch = player.getPitch();
        float yaw = player.getYaw();
        Vec3d eyePos = player.getEyePos();
        float f = -net.minecraft.util.math.MathHelper.cos(-yaw * 0.017453292F - 3.1415927F);
        float g = net.minecraft.util.math.MathHelper.sin(-yaw * 0.017453292F - 3.1415927F);
        float h = -net.minecraft.util.math.MathHelper.cos(-pitch * 0.017453292F);
        float i = net.minecraft.util.math.MathHelper.sin(-pitch * 0.017453292F);
        float j = g * h;
        float k = i;
        float l = f * h;
        Vec3d vec3d2 = eyePos.add((double)j * maxDistance, (double)k * maxDistance, (double)l * maxDistance);
        return world.raycast(new RaycastContext(eyePos, vec3d2, RaycastContext.ShapeType.OUTLINE, fluidHandling, player));
    }

    // Sicherer Enchantment Lookup
    private RegistryEntry<Enchantment> getEnchantment(RegistryWrapper.WrapperLookup registry, net.minecraft.registry.RegistryKey<Enchantment> key) {
        Optional<RegistryEntry.Reference<Enchantment>> optional = registry.getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(key);
        return optional.orElse(null);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        if(stack.get(ModDataComponentTypes.COORDINATES) != null) {
            BlockPos p = stack.get(ModDataComponentTypes.COORDINATES);
            // Schöner formatierter Tooltip in Grau
            textConsumer.accept(Text.literal("Last Target: " + p.getX() + ", " + p.getY() + ", " + p.getZ())
                    .formatted(Formatting.GRAY));
        }
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }

    public void setChiselSound(SoundEvent chiselSound) {
        this.chiselSound = chiselSound;
    }
}