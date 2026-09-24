package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.SledgehammerUpgrades;
import com.simplebuilding.util.SledgehammerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.simplebuilding.util.EnchantmentHelper.getOverrideLevel;
import static com.simplebuilding.util.EnchantmentHelper.hasConstructorsTouch;

public class SledgehammerItem extends Item {

    public static final int STONE_ATTACK_DAMAGE = 4;
    public static final int COPPER_ATTACK_DAMAGE = 4;
    public static final int IRON_ATTACK_DAMAGE = 6;
    public static final int GOLD_ATTACK_DAMAGE = 5;
    public static final int DIAMOND_ATTACK_DAMAGE = 7;
    public static final int NETHERITE_ATTACK_DAMAGE = 8;
    public static final int ENDERITE_ATTACK_DAMAGE = 9;

    public static final float ATTACK_SPEED_OFFSET = 0.4f;

    public static final float STONE_ATTACK_SPEED = -3.0f - ATTACK_SPEED_OFFSET;
    public static final float COPPER_ATTACK_SPEED = -2.8f - ATTACK_SPEED_OFFSET;
    public static final float IRON_ATTACK_SPEED = -3.0f - ATTACK_SPEED_OFFSET;
    public static final float GOLD_ATTACK_SPEED = -2.8f - ATTACK_SPEED_OFFSET;
    public static final float DIAMOND_ATTACK_SPEED = -2.8f - ATTACK_SPEED_OFFSET;
    public static final float NETHERITE_ATTACK_SPEED = -2.6f - ATTACK_SPEED_OFFSET;
    public static final float ENDERITE_ATTACK_SPEED = -2.4f - ATTACK_SPEED_OFFSET;

    public static final int BASE_DURABILITY_MULTIPLIER = 4;
    public static final int DURABILITY_STONE_SLEDGEHAMMER = 190 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_COPPER_SLEDGEHAMMER = 190 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_IRON_SLEDGEHAMMER = 250 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_GOLD_SLEDGEHAMMER = 32 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_DIAMOND_SLEDGEHAMMER = 1561 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_NETHERITE_SLEDGEHAMMER = 2031 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_ENDERITE_SLEDGEHAMMER = 2500 * BASE_DURABILITY_MULTIPLIER;

    /** Haltbarkeit je Umformung (Block -> Treppe -> Stufe). */
    public static final int RESHAPE_DAMAGE = 1;
    /** Haltbarkeit je Rueckwaerts-Umformung mit Schleichen und Constructor's Touch. */
    public static final int RESHAPE_REVERSE_DAMAGE = 2;
    /** Diamantsplitter aus einem zerschlagenen Diamantblock. */
    public static final int DIAMOND_BLOCK_PEBBLES = 81;
    /** Haltbarkeit fuer das Zerschlagen eines Diamantblocks. */
    public static final int DIAMOND_CRUSH_DAMAGE = 1;
    /** Grenzen der Umform-Ladezeit in Ticks. */
    public static final int RESHAPE_MIN_TICKS = 4;
    public static final int RESHAPE_MAX_TICKS = 40;

    private final ToolMaterial material;

    public SledgehammerItem(ToolMaterial material, float attackDamage, float attackSpeed, int durability, Properties settings) {
        super(settings.pickaxe(material, attackDamage, attackSpeed).durability(durability));
        this.material = material;
    }

    public ToolMaterial getMaterial() {
        return this.material;
    }

    /**
     * Die Blockliste, die Override II ueber die Spitzhacke hinaus freischaltet.
     *
     * <p>{@link #isCorrectToolForDrops} (darf der Hammer das ernten?) und
     * {@link #getDestroySpeed} (wie schnell?) muessen ueber genau dieselben Bloecke reden. Als
     * zwei Listen konnten sie eine Stufe unabhaengig voneinander verlieren - dann bricht der
     * Hammer Heu mit Diamantgeschwindigkeit, das Heu droppt aber nichts, oder umgekehrt.
     */
    private static boolean isOverrideMineable(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_AXE) ||
                state.is(BlockTags.MINEABLE_WITH_SHOVEL) ||
                state.is(BlockTags.MINEABLE_WITH_HOE);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        // Standard-Verhalten (Pickaxe)
        if (super.isCorrectToolForDrops(stack, state)) {
            return true;
        }

        // Wenn Override Level >= 2, erlaube auch Axt, Schaufel und Hacke
        if (getOverrideLevel(stack) >= 2) {
            return isOverrideMineable(state);
        }
        return false;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float baseSpeed = super.getDestroySpeed(stack, state);

        // 2. NEU: Wenn Speed langsam ist (1.0f), aber Override II aktiv ist -> Setze vollen Speed
        if (baseSpeed <= 1.0F && getOverrideLevel(stack) >= 2) {
            if (isOverrideMineable(state)) {
                // Setze die Geschwindigkeit auf die des Materials (z.B. Diamant-Speed)
                baseSpeed = this.material.speed();
            }
        }

        // Kein Tempo-Bonus mehr: das Item liefert das Tempo einer Spitzhacke seines Materials.
        // Die Verlangsamung je mitabgebautem Block rechnet SledgehammerUtils#miningSpeedDivisor,
        // angewendet in BlockStateBaseMixin, weil erst dort Spieler und Position bekannt sind.
        return baseSpeed;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        BlockState state = world.getBlockState(pos);

        // Nugget in der Nebenhand und eine aufwertbare Maschine: schmieden statt umformen. Null
        // heisst "nicht aufwertbar" - dann bleibt es beim gewohnten Verhalten darunter.
        InteractionResult smithing = SledgehammerUpgrades.tryBegin(context);
        if (smithing != null) {
            return smithing;
        }

        if (state.is(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK)) {
            player.startUsingItem(context.getHand());
            return InteractionResult.CONSUME;
        }

        // Relativer Hit Vector
        Vec3 relativeHit = context.getClickLocation().subtract(Vec3.atLowerCornerOf(pos));

        // FIX: pos übergeben
        BlockState transformState = getTransformationState(state, pos, context.getClickedFace(), relativeHit, player, stack);

        if (transformState != null) {
            if (player != null) {
                player.startUsingItem(context.getHand());
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        // Nur eine laufende Aufwertung tickt; eine Umform-Ladung hat keinen Auftrag.
        if (user instanceof Player player) {
            SledgehammerUpgrades.tick(world, player, stack, remainingUseTicks);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
        SledgehammerUpgrades.clear(user); // eine abgebrochene Aufwertung verfaellt
        return false; // Nichts tun, wenn vorzeitig abgebrochen
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (!(user instanceof Player player)) return stack;

        // Lief eine Aufwertung, ist das der fuenfte Schlag - und nie ein Umformen oder Zerschlagen
        // des Blocks, auf den der Spieler zufaellig gerade schaut.
        if (SledgehammerUpgrades.finish(world, player, stack)) {
            return stack;
        }

        var hitResult = player.pick(5.0, 0.0f, false);
        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            BlockPos pos = ((net.minecraft.world.phys.BlockHitResult)hitResult).getBlockPos();
            BlockState state = world.getBlockState(pos);
            Direction side = ((net.minecraft.world.phys.BlockHitResult)hitResult).getDirection();

            // Relativer Hit Vector berechnen
            Vec3 relativeHit = hitResult.getLocation().subtract(Vec3.atLowerCornerOf(pos));

            // Transformation abrufen (FIX: pos übergeben)
            BlockState newState = getTransformationState(state, pos, side, relativeHit, player, stack);

            if (state.is(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK)) {
                if (!world.isClientSide()) {
                    crushDiamondBlock((ServerLevel) world, pos, player, stack);
                }
                return stack;
            }

            if (newState != null) {
                if (!world.isClientSide()) {
                    world.setBlockAndUpdate(pos, newState);

                    // Sound: Verwende den Break-Sound des Blocks, klingt natürlicher
                    world.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0f, 0.8f);

                    ((ServerLevel) world).sendParticles(
                            new BlockParticleOption(ParticleTypes.BLOCK, state),
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            20, 0.25, 0.25, 0.25, 0.05
                    );

                    if (!player.isCreative()) {
                        // Prüfen ob Reverse Action (teurer)
                        boolean isReverse = player.isShiftKeyDown() && hasConstructorsTouch(stack, world);
                        int damage = isReverse ? RESHAPE_REVERSE_DAMAGE : RESHAPE_DAMAGE;
                        stack.hurtAndBreak(damage, player, player.getUsedItemHand().asEquipmentSlot());
                    }
                }
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        if (user instanceof Player player && SledgehammerUpgrades.hasJob(player)) {
            // Aufwertung: fuenf Sekunden, eine fortgesetzte nur die fehlenden Schlaege
            return SledgehammerUpgrades.useDuration(player);
        }
        int efficiencyLevel = 0;
        var registry = user.registryAccess().lookup(Registries.ENCHANTMENT);
        if (registry.isPresent()) {
            var efficiencyEntry = registry.get().get(Enchantments.EFFICIENCY);
             if (efficiencyEntry.isPresent()) {
                 efficiencyLevel = EnchantmentHelper.getItemEnchantmentLevel(efficiencyEntry.get(), stack);
             }
        }

        return reshapeTicks(this.getMaterial().speed(), efficiencyLevel);
    }

    /**
     * Ladezeit einer Umformung in Ticks: 200 / (Materialtempo + 5 je Effizienzstufe), auf
     * {@value #RESHAPE_MIN_TICKS}..{@value #RESHAPE_MAX_TICKS} begrenzt. Eigene Methode, damit der
     * Wiki-Export dieselbe Rechnung benutzt wie das Spiel.
     */
    public static int reshapeTicks(float materialSpeed, int efficiencyLevel) {
        float baseTime = 20.0f;
        float factor = materialSpeed + (efficiencyLevel * 5.0f);
        int time = (int) (baseTime * 10.0f / factor);
        return Math.clamp(time, RESHAPE_MIN_TICKS, RESHAPE_MAX_TICKS);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    public BlockState getTransformationState(BlockState state, BlockPos pos, Direction side, Vec3 hit, Player player, ItemStack stack) {
        Block block = state.getBlock();
        Level world = player.level();

        // STRIKTE TRENNUNG:
        if (player.isShiftKeyDown()) {
            // === SNEAKING = REVERSE ===
            // Voraussetzung: Constructor's Touch
            if (!hasConstructorsTouch(stack, world)) {
                return null; // Keine Reparatur ohne Enchantment -> Keine Animation
            }
            Optional<Block> target = reshapeTarget(block, true, false);
            if (target.isEmpty()) {
                return null;
            }
            // Treppe -> voller Block ohne Ausrichtung; Stufe -> Treppe ausgerichtet wie beim Setzen.
            BlockState targetState = target.get().defaultBlockState();
            return block instanceof StairBlock ? targetState : ChiselItem.applyIntuitiveOrientation(targetState, side, hit, player);
        }

        // === NICHT SNEAKING = FORWARD ===
        // FIX: world und pos an isFullCube übergeben, statt null
        Optional<Block> target = reshapeTarget(block, false, state.isCollisionShapeFullBlock(world, pos));
        return target.map(b -> ChiselItem.applyIntuitiveOrientation(b.defaultBlockState(), side, hit, player)).orElse(null);
    }

    /**
     * Zielblock einer Umformung, ohne Ausrichtung: die Namensregel, nach der
     * {@link #getTransformationState} umformt. Eigene Methode, damit der JEI-Katalog
     * ({@code InWorldTransformations#reshapePairs}) dieselbe Regel benutzt wie das Spiel.
     *
     * <ul>
     *   <li>vorwaerts: voller Block {@code x} -&gt; {@code x_stairs}; Treppe {@code x_stairs} -&gt; {@code x_slab};</li>
     *   <li>rueckwaerts (Schleichen + Constructor's Touch): Stufe {@code x_slab} -&gt; {@code x_stairs};
     *       Treppe {@code x_stairs} -&gt; {@code x}, ersatzweise {@code xs} oder {@code x_planks}.</li>
     * </ul>
     *
     * @param fullBlock ob der Ausgangsblock ein voller Kollisionswuerfel ist (nur vorwaerts von Belang)
     */
    public static Optional<Block> reshapeTarget(Block block, boolean reverse, boolean fullBlock) {
        net.minecraft.resources.Identifier key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        String namespace = key.getNamespace();
        String id = key.getPath();
        if (reverse) {
            // 1. Slab -> Stairs
            if (block instanceof SlabBlock) {
                Optional<Block> stairs = reshapeLookup(namespace, id.replace("_slab", "") + "_stairs");
                if (stairs.isPresent()) {
                    return stairs;
                }
            }
            // 2. Stairs -> Block, Fallbacks: plural 's' oder '_planks'
            if (block instanceof StairBlock) {
                String baseName = id.replace("_stairs", "");
                Optional<Block> fullBlockTarget = reshapeLookup(namespace, baseName);
                if (fullBlockTarget.isEmpty()) fullBlockTarget = reshapeLookup(namespace, baseName + "s");
                if (fullBlockTarget.isEmpty()) fullBlockTarget = reshapeLookup(namespace, baseName + "_planks");
                return fullBlockTarget;
            }
            return Optional.empty();
        }
        // 1. Block -> Stairs
        if (fullBlock) {
            Optional<Block> stairs = reshapeLookup(namespace, id + "_stairs");
            if (stairs.isPresent()) {
                return stairs;
            }
        }
        // 2. Stairs -> Slab
        if (block instanceof StairBlock) {
            return reshapeLookup(namespace, id.replace("_stairs", "") + "_slab");
        }
        return Optional.empty();
    }

    private static Optional<Block> reshapeLookup(String namespace, String path) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(namespace, path));
    }

    public static List<BlockPos> getBlocksToBeDestroyed(int baseRange, BlockPos initialPos, Player player) {
        List<BlockPos> positions = new ArrayList<>();
        Level world = player.level();
        ItemStack stack = player.getMainHandItem();
        BlockState initialState = world.getBlockState(initialPos);

        if (!(stack.getItem() instanceof SledgehammerItem)) {
            return positions;
        }
        if (initialState.isAir() || initialState.getDestroySpeed(world, initialPos) < 0.0F) {
            return positions;
        }
        if (!SledgehammerUtils.canMineOrigin(world, initialPos, stack)) {
            return positions;
        }

        // Schleichen baut genau einen Block ab - wie eine Spitzhacke gleichen Materials. Server-
        // Abbau, Riss-Overlay und Highlight lesen alle diese Liste und folgen damit von selbst.
        if (player.isShiftKeyDown()) {
            positions.add(initialPos);
            return positions;
        }

        Direction sideHit = getHitSideFromPlayer(player);

        var registry = world.registryAccess();
        var enchantLookup = registry.lookupOrThrow(Registries.ENCHANTMENT);

        var radiusKey = enchantLookup.get(ModEnchantments.RADIUS);
        int range = baseRange + (radiusKey.isPresent() ? EnchantmentHelper.getItemEnchantmentLevel(radiusKey.get(), stack) : 0);

        var breakThroughKey = enchantLookup.get(ModEnchantments.BREAK_THROUGH);
        int depth = (breakThroughKey.isPresent() ? EnchantmentHelper.getItemEnchantmentLevel(breakThroughKey.get(), stack) : 0);

        // Positionen berechnen
        for(int x = -range; x <= range; x++) {
            for(int y = -range; y <= range; y++) {
                for(int z = 0; z <= depth; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        positions.add(initialPos);
                        continue;
                    }

                    BlockPos targetPos = null;

                    if (sideHit == Direction.DOWN || sideHit == Direction.UP) {
                        int depthOffset = (sideHit == Direction.UP) ? -z : z;
                        targetPos = initialPos.offset(x, depthOffset, y);
                    }
                    else if (sideHit == Direction.NORTH || sideHit == Direction.SOUTH) {
                        int depthOffset = (sideHit == Direction.NORTH) ? z : -z;
                        targetPos = initialPos.offset(x, y, depthOffset);
                    }
                    else if (sideHit == Direction.EAST || sideHit == Direction.WEST) {
                        int depthOffset = (sideHit == Direction.WEST) ? z : -z;
                        targetPos = initialPos.offset(depthOffset, y, x);
                    }

                    if (targetPos != null) {
                        positions.add(targetPos);
                    }
                }
            }
        }
        return positions;
    }

    private static void crushDiamondBlock(ServerLevel world, BlockPos pos, Player player, ItemStack stack) {
        if (!world.getBlockState(pos).is(Blocks.DIAMOND_BLOCK)) {
            return;
        }

        world.destroyBlock(pos, false, player);

        int totalPebbles = DIAMOND_BLOCK_PEBBLES;
        while (totalPebbles > 0) {
            int batch = Math.min(totalPebbles, 64);
            ItemEntity itemEntity = new ItemEntity(
                    world,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    new ItemStack(ModItems.DIAMOND_PEBBLE, batch)
            );
            world.addFreshEntity(itemEntity);
            totalPebbles -= batch;
        }

        world.playSound(null, pos, SoundEvents.METAL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (!player.isCreative()) {
            stack.hurtAndBreak(DIAMOND_CRUSH_DAMAGE, world, (ServerPlayer) player,
                    item -> player.onEquippedItemBroken(item, EquipmentSlot.MAINHAND));
        }
    }

    private static Direction getHitSideFromPlayer(Player player) {
        float pitch = player.getXRot();
        if (pitch < -60) return Direction.DOWN;
        if (pitch > 60) return Direction.UP;
        return player.getDirection().getOpposite();
    }


}