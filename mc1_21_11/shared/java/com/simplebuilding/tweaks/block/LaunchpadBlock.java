package com.simplebuilding.tweaks.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.tweaks.block.entity.LaunchpadBlockEntity;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import com.simplebuilding.tweaks.easter.EasterEggs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Launchpad (Simple Tweaks): mit Windkugeln laden (Rechtsklick eine, schleichend alle aus der Hand),
 * 3 s stehen, Start. Stufen I-III (Diamant/Netherit/Enderit) fassen 4/8/16 Ladungen; jede Ladung
 * zaehlt doppelt so viel wie frueher, 16 Ladungen starten also so hoch wie die alten 32. Die
 * Enderit-Stufe schuetzt zusaetzlich bis zur naechsten Landung vor Fallschaden.
 */
public class LaunchpadBlock extends WaterloggedPadBlock {
    public static final MapCodec<LaunchpadBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            Codec.INT.fieldOf("tier").forGetter(LaunchpadBlock::getTier)
    ).apply(i, LaunchpadBlock::new));

    /** Hoechste Stufe (Enderit): Fallschutz nach dem Start. */
    public static final int ENDERITE_TIER = 3;

    /**
     * Sichtbarer Fuellstand (Immersion 2026-09-28): 0 leer, 1-3 = bis ein Drittel / zwei Drittel /
     * mehr geladen; die Spirale der Textur leuchtet mit jeder Stufe ein Stueck weiter. Die
     * Block-Entity haelt ihn nach jeder Aenderung der Ladungen nach ({@link #chargeLevel}).
     */
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 3);

    private final int tier;

    public LaunchpadBlock(BlockBehaviour.Properties properties, int tier) {
        super(properties, Block.box(1, 0, 1, 15, 1, 15), PadOwnership.OWNER_PAD, PadOwnership.STRANGER_PAD);
        this.tier = Math.max(1, Math.min(ENDERITE_TIER, tier));
        registerDefaultState(defaultBlockState().setValue(CHARGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CHARGE);
    }

    /**
     * Sichtbarer Fuellstand zu {@code charges} von {@code max}: 0 nur, wenn leer; sonst 1-3 nach
     * Dritteln (aufgerundet), voll ist immer 3.
     */
    public static int chargeLevel(int charges, int max) {
        if (charges <= 0 || max <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(3, (int) Math.ceil(3.0 * charges / max)));
    }

    public int getTier() {
        return tier;
    }

    @Override
    protected boolean isRedstoneControlled() {
        return true;
    }

    public boolean isEnderite() {
        return tier >= ENDERITE_TIER;
    }

    public int maxCharges() {
        return maxCharges(tier);
    }

    /** Fassungsvermoegen dieses gesetzten Pads: die letzte Easter-Stufe (EasterEggs) fasst doppelt so viel. */
    public int capacityAt(BlockGetter level, BlockPos pos) {
        return EasterEggs.isBoosted(level, pos) ? 2 * maxCharges() : maxCharges();
    }

    /** Fassungsvermoegen je Stufe: I = 4, II = 8, III = 16. */
    public static int maxCharges(int tier) {
        return 4 << (Math.max(1, Math.min(ENDERITE_TIER, tier)) - 1);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaunchpadBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TweaksBlockEntities.LAUNCHPAD, LaunchpadBlockEntity::tick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.WIND_CHARGE) || hand != InteractionHand.MAIN_HAND) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        return deposit(stack, level, pos, player, false);
    }

    /**
     * Schleichen + Rechtsklick: Vanilla fragt den Block dann gar nicht (ServerPlayerGameMode#useItemOn
     * unterdrueckt {@code useItemOn} bei gehaltener Umschalttaste), sondern das Item -
     * {@code LaunchpadWindChargeMixin} leitet {@code WindChargeItem#useOn} hierher. Liefert null, wenn
     * es kein schleichender Klick mit der Haupthand auf ein Launchpad ist.
     */
    public static @Nullable InteractionResult bulkDeposit(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive() || context.getHand() != InteractionHand.MAIN_HAND
                || !context.getItemInHand().is(Items.WIND_CHARGE)) {
            return null;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof LaunchpadBlock pad)) {
            return null;
        }
        return pad.deposit(context.getItemInHand(), level, pos, player, true);
    }

    /** Laedt eine Windkugel (all = false) oder alle aus dem Stapel, hoechstens bis zum Fassungsvermoegen. */
    public InteractionResult deposit(ItemStack stack, Level level, BlockPos pos, Player player, boolean all) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LaunchpadBlockEntity launchpad) {
            int max = capacityAt(level, pos);
            int current = launchpad.getCharges();
            if (current >= max) {
                // Voll: das Buendel-Geraeusch "passt nicht mehr" statt einer Meldung (keine Bildschirmtexte).
                level.playSound(null, pos, SoundEvents.BUNDLE_INSERT_FAIL, SoundSource.BLOCKS, 1.0f, 1.0f);
                return InteractionResult.FAIL;
            }
            int added = launchpad.addCharges(all ? stack.getCount() : 1, max);
            if (!player.getAbilities().instabuild) {
                stack.shrink(added);
            }
            // Sichtbar: ein Windstoss faehrt in die Spirale, je mehr Kugeln, desto mehr Woelkchen.
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMALL_GUST, pos.getX() + 0.5, pos.getY() + 0.15,
                        pos.getZ() + 0.5, Math.min(12, 2 + added), 0.25, 0.03, 0.25, 0.0);
            }
            // Der Fuellstand ist hoerbar: je voller, desto hoeher das Einlegen (statt "x/y geladen" im Bild).
            level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 1.0f, fillPitch(current + added, max));
        }
        return InteractionResult.SUCCESS;
    }

    /** Tonhoehe des Einlegens: 0,8 fast leer bis 1,8 voll. */
    public static float fillPitch(int charges, int max) {
        return 0.8f + (max <= 0 ? 1.0f : Math.min(1.0f, charges / (float) max));
    }
}
