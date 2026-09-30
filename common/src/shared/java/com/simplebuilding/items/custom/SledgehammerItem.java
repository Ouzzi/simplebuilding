package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.blocks.custom.PlacedTemplateBlock;
import com.simplebuilding.util.PlacedTemplates;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

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

    /** Haltbarkeit je abgebautem Block (eine Spitzhacke: 1), siehe {@link #mineBlock}. */
    public static final int WEAR_PER_BLOCK = 2;
    /** Haltbarkeit je Umformung (Block -> Treppe -> Stufe). */
    public static final int RESHAPE_DAMAGE = 1;
    /** Haltbarkeit je Rueckwaerts-Umformung mit Schleichen und Constructor's Touch. */
    public static final int RESHAPE_REVERSE_DAMAGE = 2;
    /** Diamantsplitter aus einem zerschlagenen Diamantblock. */
    public static final int DIAMOND_BLOCK_PEBBLES = 81;
    /** Haltbarkeit fuer das Zerschlagen eines Diamantblocks. */
    public static final int DIAMOND_CRUSH_DAMAGE = 1;

    /**
     * Einen Diamantblock zerschlagen erst Haemmer ab der Eisenstufe (Besitzer 2026-09-29): Eisen, Gold,
     * Diamant, Netherit und Enderit - in der Reihenfolge der Mod-Zeitalter liegt Gold ueber Eisen. Stein
     * und Kupfer prallen ab (dumpfer Klang, der Block bleibt).
     */
    public static boolean canCrushDiamondBlock(Item item) {
        if (!(item instanceof SledgehammerItem hammer)) {
            return false;
        }
        ToolMaterial m = hammer.getMaterial();
        return m != ToolMaterial.WOOD && m != ToolMaterial.STONE && m != ToolMaterial.COPPER;
    }
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

        // Kein Tempo-Bonus: das Item liefert das Tempo einer Spitzhacke seines Materials. Die
        // Verlangsamung (1x1 etwas langsamer, Flaeche je Block wie die Spitzhacke eine Stufe darunter)
        // rechnet SledgehammerUtils#miningSpeedDivisor, angewendet in BlockStateBaseMixin, weil erst
        // dort Spieler und Position bekannt sind.
        return baseSpeed;
    }

    /**
     * Der Hammer nutzt sich schneller ab als eine Spitzhacke (Besitzer 2026-09-28): jeder Block, den er
     * bricht - der angeschlagene wie jeder mitgenommene -, kostet {@value #WEAR_PER_BLOCK} statt 1
     * Haltbarkeit (Vanillas Werkzeug-Komponente zieht 1 ab, hier kommt der Rest dazu). Ein Block, fuer
     * den er nicht das richtige Werkzeug ist, kostet beim Flaechenabbau einen Punkt mehr
     * ({@code SledgehammerUsageEvent}).
     */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, level, state, pos, miner);
        if (result && !level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F && !stack.isEmpty()) {
            stack.hurtAndBreak(WEAR_PER_BLOCK - 1, miner, EquipmentSlot.MAINHAND);
        }
        return result;
    }

    /**
     * Abgelegte Schmiedevorlage mit Aufwertungs-Material in der Nebenhand: der Hammer bricht sie
     * nie. Im Kreativmodus ist das die Stelle, an der ein Linksklick ankommt (Vanilla zerstoert dort
     * sofort und ruft {@code attack} nicht), also zaehlt hier der Schlag ({@link PlacedTemplates#hit});
     * im Ueberlebensmodus zaehlt ihn {@code PlacedTemplateBlock#attack}.
     */
    @Override
    public boolean canDestroyBlock(ItemStack stack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
        if (user instanceof Player player && state.getBlock() instanceof PlacedTemplateBlock
                && PlacedTemplates.isHammerStance(player)) {
            if (player.getAbilities().instabuild) {
                PlacedTemplates.hit(level, pos, player);
            }
            return false;
        }
        return super.canDestroyBlock(stack, state, level, pos, user);
    }

    /**
     * The block a charge was started on, per player (audit 2026-09-26, P2 #7). Until then
     * {@code finishUsingItem} re-aimed at whatever the player looked at when the charge ended: start
     * on any stone of your own, turn to a protected diamond block, and the hammer crushed it into 81
     * pebbles. One map per side: {@code Entity#equals} compares entity ids, and in single player the
     * client and server player of one person share that id, so a single map let whichever side
     * finished first consume the other side's entry. Weak, so a player who logs out mid-charge is not
     * kept alive.
     */
    private static final Map<Player, ChargeTarget> SERVER_CHARGE_TARGETS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Player, ChargeTarget> CLIENT_CHARGE_TARGETS = Collections.synchronizedMap(new WeakHashMap<>());

    private static Map<Player, ChargeTarget> chargeTargets(Player player) {
        return player.level().isClientSide() ? CLIENT_CHARGE_TARGETS : SERVER_CHARGE_TARGETS;
    }

    private record ChargeTarget(BlockPos pos, Block block, BlockState initialState, BlockState cornerResult) {
    }

    /** Remembers the block a reshape or crush charge was started on. */
    private static void rememberTarget(Player player, BlockPos pos, BlockState state) {
        chargeTargets(player).put(player, new ChargeTarget(pos.immutable(), state.getBlock(), state, null));
    }

    /**
     * Whether the player may change the block at {@code pos} - the same question a block placement
     * asks (spawn protection, claim mods, adventure mode).
     */
    private static boolean mayChange(Level world, Player player, BlockPos pos, Direction side, ItemStack stack) {
        return player.mayBuild() && world.mayInteract(player, pos) && player.mayUseItemAt(pos, side, stack);
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

        if (!mayChange(world, player, pos, context.getClickedFace(), stack)) {
            return InteractionResult.PASS;
        }

        if (state.is(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK)) {
            if (!canCrushDiamondBlock(stack.getItem())) {
                // Zu schwach: dumpfer Klang, keine Ladung (keine Einblendung).
                if (!world.isClientSide()) {
                    world.playSound(null, pos, SoundEvents.METAL_HIT, SoundSource.BLOCKS, 0.8F, 0.5F);
                }
                return InteractionResult.FAIL;
            }
            rememberTarget(player, pos, state);
            player.startUsingItem(context.getHand());
            return InteractionResult.CONSUME;
        }

        // Relativer Hit Vector
        Vec3 relativeHit = context.getClickLocation().subtract(Vec3.atLowerCornerOf(pos));

        // FIX: pos übergeben
        BlockState transformState = getTransformationState(state, pos, context.getClickedFace(), relativeHit, player, stack);

        if (transformState != null) {
            rememberTarget(player, pos, state);
            if (isCornerMode(player, stack)) {
                chargeTargets(player).put(player, new ChargeTarget(pos.immutable(), state.getBlock(), state, transformState));
            }
            player.startUsingItem(context.getHand());
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
        if (user instanceof Player player) {
            chargeTargets(player).remove(player); // eine abgebrochene Ladung auch
        }
        return false; // Nichts tun, wenn vorzeitig abgebrochen
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (!(user instanceof Player player)) return stack;

        // Lief eine Aufwertung, ist das der fuenfte Schlag - und nie ein Umformen oder Zerschlagen
        // des Blocks, auf den der Spieler zufaellig gerade schaut.
        if (SledgehammerUpgrades.finish(world, player, stack)) {
            chargeTargets(player).remove(player);
            return stack;
        }

        // Only the block the charge was started on, and only while the player still aims at it
        // and may change it: a charge never re-aims at another block.
        ChargeTarget target = chargeTargets(player).remove(player);
        if (target == null) {
            return stack;
        }
        var hitResult = player.pick(5.0, 0.0f, false);
        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && ((net.minecraft.world.phys.BlockHitResult) hitResult).getBlockPos().equals(target.pos())) {
            BlockPos pos = target.pos();
            BlockState state = world.getBlockState(pos);
            Direction side = ((net.minecraft.world.phys.BlockHitResult)hitResult).getDirection();
            if (!state.is(target.block()) || !mayChange(world, player, pos, side, stack)) {
                return stack;
            }

            // Relativer Hit Vector berechnen
            Vec3 relativeHit = hitResult.getLocation().subtract(Vec3.atLowerCornerOf(pos));

            // Transformation abrufen (FIX: pos übergeben)
            boolean cornerCharge = target.cornerResult() != null;
            if (!state.is(Blocks.DIAMOND_BLOCK) && (cornerCharge != isCornerMode(player, stack)
                    || (cornerCharge && state != target.initialState()))) return stack;
            BlockState newState = cornerCharge ? target.cornerResult()
                    : getTransformationState(state, pos, side, relativeHit, player, stack);

            if (state.is(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK)) {
                if (!world.isClientSide()) {
                    crushDiamondBlock((ServerLevel) world, pos, player, stack);
                }
                return stack;
            }

            if (newState != null) {
                if (!world.isClientSide()) {
                    world.setBlockAndUpdate(pos, newState);
                    com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.HAMMER_RESHAPE);

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

        int ticks = reshapeTicks(this.getMaterial().speed(), efficiencyLevel);
        if (user instanceof Player player) {
            ChargeTarget target = chargeTargets(player).get(player);
            boolean corner = target != null ? target.cornerResult() != null : isCornerMode(player, stack);
            if (corner) return com.simplebuilding.util.HammerCorners.ticks(ticks);
        }
        return ticks;
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

    public static boolean isCornerMode(Player player, ItemStack stack) {
        return com.simplebuilding.version.McVersion.TRANSFORM_HINTS_AND_CORNERS
                && player.isShiftKeyDown() && !hasConstructorsTouch(stack, player.level());
    }

    public BlockState getTransformationState(BlockState state, BlockPos pos, Direction side, Vec3 hit, Player player, ItemStack stack) {
        Block block = state.getBlock();
        Level world = player.level();

        if (isCornerMode(player, stack)) {
            return com.simplebuilding.util.HammerCorners.subtract(state, state.isCollisionShapeFullBlock(world, pos), side, hit);
        }
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
     *   <li>vorwaerts: voller Block {@code x} -&gt; {@code x_stairs}, ersatzweise ohne {@code _planks},
     *       {@code _block} oder Plural-s ({@code bricks} -&gt; {@code brick_stairs}); Treppe {@code x_stairs} -&gt; {@code x_slab};</li>
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
            // Gegenstueck zu den Rueckwaerts-Ersatzregeln: oak_planks -> oak_stairs, bricks -> brick_stairs,
            // quartz_block -> quartz_stairs (Vanilla benennt die Treppe nach dem Material, nicht nach dem Block).
            if (stairs.isEmpty() && id.endsWith("_planks")) stairs = reshapeLookup(namespace, id.substring(0, id.length() - 7) + "_stairs");
            if (stairs.isEmpty() && id.endsWith("_block")) stairs = reshapeLookup(namespace, id.substring(0, id.length() - 6) + "_stairs");
            if (stairs.isEmpty() && id.endsWith("s")) stairs = reshapeLookup(namespace, id.substring(0, id.length() - 1) + "_stairs");
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

        // Oktant mit Auswahl in der Nebenhand und der Schlag trifft seine Figur: der Hammer bricht die
        // ganze Auswahl (Besitzer 2026-09-28). Tempo und Haltbarkeit folgen von selbst, weil Server-
        // Abbau, Tempo und Riss-Overlay alle diese Liste lesen.
        List<BlockPos> octant = SledgehammerUtils.octantSelection(player, initialPos);
        if (octant != null) {
            return octant;
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
        if (!world.getBlockState(pos).is(Blocks.DIAMOND_BLOCK) || !canCrushDiamondBlock(stack.getItem())) {
            return;
        }

        world.destroyBlock(pos, false, player);
        com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.DIAMOND_CRUSH);

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