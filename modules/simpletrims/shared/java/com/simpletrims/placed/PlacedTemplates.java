package com.simpletrims.placed;

import com.simpletrims.SimpleTrims;
import com.simpletrims.TemplateTools;
import com.simpletrims.TrimsRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Placed trim templates (owner 2026-09-28, moved from SimpleBuilding): sneak + use with a trim template
 * ({@code #minecraft:trim_templates}) lays it flat on the floor, on a wall or under a ceiling
 * ({@link PlacedTrimTemplateBlock}); without sneaking nothing changes.
 *
 * <p>Upgrade in the world (tool rule {@link TemplateTools}: axe on its own, hammer with a hammer partner): tool in the
 * main hand, Glow Ink Sac or Glowstone Dust in the off hand, then {@link #PLACED_HITS} right clicks on a placed Vanilla
 * armor trim template - each with cracks ({@code simplelib.api.InWorldStrikes}), sparks and a rising tone. The last one
 * costs {@link #CATALYST_COST} of the off-hand material plus the {@link #extraMaterials} from the inventory and turns
 * the template into the Glowing (glow ink) or Emitting (glowstone) one. Whoever holds such a material near an upgradable
 * template sees sparks circle above it now and then ({@link #tryHint}).
 */
public final class PlacedTemplates {
    public static final int PLACED_HITS = 5;
    public static final int HIT_RESET_TICKS = 100;
    public static final double HINT_RANGE = 6.0;
    public static final int HINT_INTERVAL = 10;
    public static final int HINT_SOUND_INTERVAL = 60;
    /** Durability one strike costs the tool (outside creative). */
    public static final int TOOL_DAMAGE = 1;
    /** Off-hand material per finished upgrade (outside creative). */
    public static final int CATALYST_COST = 2;

    private PlacedTemplates() {}

    // ----- placing ---------------------------------------------------------------------------------

    /** Trim templates lie down here (Vanilla ones, this mod's and any other mod's in the Vanilla tag). */
    public static boolean isPlaceable(ItemStack stack) {
        return SimpleTrims.on() && !stack.isEmpty() && stack.is(ItemTags.TRIM_TEMPLATES);
    }

    /** Sneak + right click of a trim template on a block (from {@code Item#useOn}); null = nothing placed. */
    public static @Nullable InteractionResult tryPlace(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.isSecondaryUseActive() || !isPlaceable(stack) || !mayChange(context.getLevel(), player, context.getClickedPos())) {
            return null;
        }
        BlockPlaceContext place = new BlockPlaceContext(context);
        if (!place.canPlace()) return null;
        Level level = context.getLevel();
        BlockPos pos = place.getClickedPos();
        BlockState state = TrimsRegistry.PLACED_TEMPLATE.getStateForPlacement(place);
        if (state == null || !state.canSurvive(level, pos)) return null;
        if (!level.isClientSide()) {
            if (!level.setBlock(pos, state, 11)) return null;
            if (level.getBlockEntity(pos) instanceof PlacedTrimTemplateBlockEntity be) be.setTemplate(stack.copyWithCount(1));
            SoundType sound = state.getSoundType();
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    /** Building rights: adventure mode, spawn protection and every claim provider ({@code framework Protection}). */
    public static boolean mayChange(Level level, Player player, BlockPos pos) {
        if (!player.mayBuild()) return false;
        if (!(level instanceof ServerLevel server)) return true;
        if (!server.mayInteract(player, pos)) return false;
        var minecraft = server.getServer();
        // Mock/fake players (no ServerPlayer) have no command source; claim providers only judge real players.
        if (!(player instanceof net.minecraft.server.level.ServerPlayer)) return true;
        return !com.simplebuilding.framework.api.Protection.active(minecraft) || com.simplebuilding.framework.api.Protection.allows(minecraft,
                new com.simplebuilding.framework.api.Protection.Target(player.getUUID(), server.dimension().identifier().toString(),
                        pos.getX(), pos.getY(), pos.getZ(), net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_OWNERS)
                                .test(((net.minecraft.server.level.ServerPlayer) player).createCommandSourceStack())));
    }

    public static ItemStack templateAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PlacedTrimTemplateBlockEntity be ? be.getTemplate() : ItemStack.EMPTY;
    }

    // ----- upgrading -------------------------------------------------------------------------------

    /** Off-hand material -> result, in a fixed order (glow ink: Glowing, glowstone dust: Emitting). */
    public static Map<Item, Item> upgrades() {
        Map<Item, Item> upgrades = new LinkedHashMap<>();
        upgrades.put(Items.GLOW_INK_SAC, TrimsRegistry.GLOWING_TRIM_TEMPLATE);
        upgrades.put(Items.GLOWSTONE_DUST, TrimsRegistry.EMITTING_TRIM_TEMPLATE);
        return upgrades;
    }

    /** Inventory materials besides the catalyst (the catalyst stays in the off hand until the last strike). */
    public static Map<Item, Integer> extraMaterials(Item catalyst) {
        Map<Item, Integer> materials = new LinkedHashMap<>();
        if (upgrades().containsKey(catalyst)) {
            materials.put(Items.DIAMOND, 4);
            materials.put(catalyst == Items.GLOW_INK_SAC ? Items.GLOWSTONE : Items.BLAZE_POWDER, 2);
        }
        return materials;
    }

    /**
     * Vanilla armor trim templates: every item whose id contains {@code trim_smithing_template}. This mod's results are
     * called {@code *_trim_template} and do not fall under it, so an upgraded template cannot be upgraded again.
     */
    public static boolean isTrimTemplate(ItemStack stack) {
        return !stack.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains("trim_smithing_template");
    }

    public static boolean isUpgradable(BlockGetter level, BlockPos pos) {
        return isTrimTemplate(templateAt(level, pos));
    }

    /** Template tool in the main hand and an upgrade material in the off hand. */
    public static boolean isToolStance(Player player) {
        return TemplateTools.isTemplateTool(player.getMainHandItem()) && upgrades().containsKey(player.getOffhandItem().getItem());
    }

    public static boolean isToolTarget(BlockGetter level, BlockPos pos, Player player) {
        return isToolStance(player) && level.getBlockState(pos).getBlock() instanceof PlacedTrimTemplateBlock && isUpgradable(level, pos);
    }

    public static boolean hasMaterials(Player player) {
        if (player.isCreative()) return true;
        if (player.getOffhandItem().getCount() < CATALYST_COST) return false;
        for (var material : extraMaterials(player.getOffhandItem().getItem()).entrySet()) {
            if (player.getInventory().countItem(material.getKey()) < material.getValue()) return false;
        }
        return true;
    }

    /** Only after all materials were checked, on the last strike. */
    public static void consumeMaterials(Player player) {
        if (player.isCreative()) return;
        for (var material : extraMaterials(player.getOffhandItem().getItem()).entrySet()) {
            int remaining = material.getValue();
            for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.is(material.getKey())) {
                    int count = Math.min(remaining, stack.getCount());
                    stack.shrink(count);
                    remaining -= count;
                }
            }
        }
        player.getOffhandItem().shrink(CATALYST_COST);
        player.getInventory().setChanged();
    }

    /** A right click with the tool: a held click repeats every 4 ticks, a strike counts only {@code MIN_INTERVAL} after the last. */
    public static boolean strike(ServerLevel level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof PlacedTrimTemplateBlockEntity be && be.hits() > 0
                && level.getGameTime() - be.lastHitTime() < com.simplelib.api.InWorldStrikes.MIN_INTERVAL
                && level.getGameTime() >= be.lastHitTime()) {
            return false;
        }
        return hit(level, pos, player);
    }

    /** One strike; the {@link #PLACED_HITS}th upgrades. True if the strike counted. */
    public static boolean hit(Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !isToolTarget(level, pos, player) || !mayChange(level, player, pos)
                || !(level.getBlockEntity(pos) instanceof PlacedTrimTemplateBlockEntity be) || !hasMaterials(player)) {
            return false;
        }
        ItemStack tool = player.getMainHandItem();
        Item catalyst = player.getOffhandItem().getItem();
        Item result = upgrades().get(catalyst);
        int hits = be.registerHit(catalyst, level.getGameTime());
        if (!player.isCreative()) tool.hurtAndBreak(TOOL_DAMAGE, player, EquipmentSlot.MAINHAND);
        boolean glowing = result == TrimsRegistry.GLOWING_TRIM_TEMPLATE;
        Vec3 centre = be.surfaceCentre();
        ServerLevel server = (ServerLevel) level;
        com.simplelib.api.InWorldStrikes.crack(server, pos, hits, PLACED_HITS);
        if (hits < PLACED_HITS) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.8F, 0.9F + 0.25F * hits);
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, be.getTemplate().getItem()),
                    centre.x, centre.y, centre.z, 4 + 2 * hits, 0.2, 0.05, 0.2, 0.05);
            server.sendParticles(catalystParticle(glowing), centre.x, centre.y, centre.z, 2 * hits, 0.25, 0.05, 0.25, 0.01);
            return true;
        }
        be.setTemplate(new ItemStack(result));
        consumeMaterials(player);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1.0F, 1.5F);
        level.playSound(null, pos, glowing ? SoundEvents.GLOW_INK_SAC_USE : SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.0F, 1.0F);
        server.sendParticles(ParticleTypes.GLOW, centre.x, centre.y, centre.z, 8, 0.3, 0.1, 0.3, 0.02);
        server.sendParticles(glowing ? ParticleTypes.GLOW_SQUID_INK : ParticleTypes.LARGE_SMOKE, centre.x, centre.y, centre.z, 4, 0.2, 0.05, 0.2, 0.01);
        return true;
    }

    private static ParticleOptions catalystParticle(boolean glowInk) {
        return glowInk ? ParticleTypes.GLOW : ParticleTypes.WAX_ON;
    }

    // ----- hint ------------------------------------------------------------------------------------

    public static boolean holdsCatalyst(Player player) {
        return upgrades().containsKey(player.getMainHandItem().getItem()) || upgrades().containsKey(player.getOffhandItem().getItem());
    }

    public static @Nullable Player hintViewer(Level level, BlockPos pos) {
        if (!SimpleTrims.on() || !isUpgradable(level, pos)) return null;
        return level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HINT_RANGE,
                entity -> entity instanceof Player player && !player.isSpectator() && holdsCatalyst(player));
    }

    /** Three sparks circle over the template, at most every {@link #HINT_SOUND_INTERVAL} ticks a soft chime. */
    public static @Nullable Player tryHint(ServerLevel level, BlockPos pos, PlacedTrimTemplateBlockEntity be) {
        Player viewer = hintViewer(level, pos);
        if (viewer == null) return null;
        long now = level.getGameTime();
        boolean glowInk = upgrades().containsKey(viewer.getOffhandItem().getItem())
                ? viewer.getOffhandItem().is(Items.GLOW_INK_SAC) : viewer.getMainHandItem().is(Items.GLOW_INK_SAC);
        Vec3 centre = be.surfaceCentre();
        Direction normal = be.normal();
        for (int i = 0; i < 3; i++) {
            double angle = now * 0.35 + i * (Math.PI * 2.0 / 3.0);
            double a = Math.cos(angle) * 0.38;
            double b = Math.sin(angle) * 0.38;
            double x = centre.x + normal.getStepX() * 0.12;
            double y = centre.y + normal.getStepY() * 0.12;
            double z = centre.z + normal.getStepZ() * 0.12;
            if (normal.getAxis() == Direction.Axis.Y) {
                x += a;
                z += b;
            } else if (normal.getAxis() == Direction.Axis.X) {
                y += a;
                z += b;
            } else {
                x += a;
                y += b;
            }
            level.sendParticles(catalystParticle(glowInk), x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (be.takeHintSound(now, HINT_SOUND_INTERVAL)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.25F, 1.3F + level.getRandom().nextFloat() * 0.3F);
        }
        return viewer;
    }
}
