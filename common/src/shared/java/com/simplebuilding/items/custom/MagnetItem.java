package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.simplebuilding.util.EnchantmentHelper.*;

/**
 * Attractor (Registry-Id {@code magnet}): in der Hand zieht er lose Items zum Spieler, abgelegt
 * ({@link com.simplebuilding.util.PlacedAttractors}) zur Platte.
 *
 * <p>Seit 2026-09-29 (Besitzer) wie der Erzdetektor: ohne Beruehrung des Konstrukteurs ein schlichter
 * Magnet ohne Filter. Mit ihr stellt man den Filter ein - Schleichen + Rechtsklick auf einen Block
 * (dessen Item) oder ein liegendes Item in der Welt, im Inventar Rechtsklick mit dem Attractor auf
 * ein Item (oder mit einem Item auf den Attractor). Schleichen + Rechtsklick ins Leere loescht den
 * Filter. Ein Filter ohne die Verzauberung (aeltere Welten) wirkt nicht ({@link #effectiveFilter}).
 * Abgelegt wird ein Attractor mit Beruehrung deshalb mit einfachem Rechtsklick auf einen Block,
 * einer ohne wie bisher mit Schleichen + Rechtsklick.
 *
 * <p>Reichweite: klein ({@link #BASE_RANGE}), die Verzauberung Reichweite vergroessert den Zugradius
 * ({@link #RANGE_PER_LEVEL} je Stufe, nie ueber {@link #MAX_RANGE}); die Blockreichweite des Spielers
 * bleibt dabei unveraendert ({@code RangeReach}). Die Beruehrung vergroessert nichts mehr (bis
 * 2026-09-29: 4 Bloecke, mit Beruehrung 8, je Stufe +2).
 */
public class MagnetItem extends Item {

    private static final String FILTER_KEY = "MagnetFilter";
    /** Zugradius ohne Verzauberung (Bloecke um die Spielerhuelle). */
    public static final double BASE_RANGE = 4.0;
    /** Zusaetzlicher Zugradius je Stufe Reichweite. */
    public static final double RANGE_PER_LEVEL = 2.0;
    /** Obergrenze des Zugradius vor dem Config-Faktor (Reichweite III). */
    public static final double MAX_RANGE = 9.0;
    /** Harte Obergrenze nach dem Config-Faktor {@code tools.magnetRangeMultiplier}. */
    public static final double HARD_MAX_RANGE = 15.0;
    private static final double SYNC_DISTANCE_SQ = 64 * 64;

    public MagnetItem(Properties settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof Player player)) return;

        if (!isHeldInHand(player, stack, slot)) return;

        // Shift deaktiviert den Magneten, der Serverschalter server.features.attractor ganz.
        if (player.isShiftKeyDown() || !com.simplebuilding.config.ServerTuning.get().features.attractor) return;

        double currentRange = getCurrentRange(stack, world);

        String filterId = effectiveFilter(stack, world);

        AABB box = player.getBoundingBox().inflate(currentRange);
        List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class, box, itemEntity -> true);
        Vec3 targetPos = player.getEyePosition().subtract(0, 0.5, 0);

        for (ItemEntity itemEntity : items) {
            if (itemEntity.isRemoved() || itemEntity.getItem().isEmpty()) continue;

            if (!passesFilter(itemEntity, filterId)) continue;
            // Display items of other mods, items reserved for someone else, other players' death
            // drops and the item tag simplebuilding:attractor_ignore stay where they are.
            if (!com.simplebuilding.util.AttractorFilter.mayAttract(itemEntity, player)) continue;

            applyMagnetForce(itemEntity, targetPos);

            // Pickup Delay resetten
            itemEntity.setPickUpDelay(0);

            syncVelocityToNearbyPlayers(world, itemEntity);
        }
    }

    private static boolean isHeldInHand(Player player, ItemStack stack, @Nullable EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
            return true;
        }

        return player.getMainHandItem() == stack || player.getOffhandItem() == stack;
    }

    private static double getCurrentRange(ItemStack stack, ServerLevel world) {
        return pullRange(getMagnetRangeLevel(stack, world), rangeMultiplier());
    }

    /**
     * Zugradius zu einer Stufe Reichweite und einem Config-Faktor: Grundwert plus Stufen, auf
     * {@link #MAX_RANGE} gedeckelt, dann mal Faktor, hoechstens {@link #HARD_MAX_RANGE}.
     */
    public static double pullRange(int rangeLevel, double multiplier) {
        double range = Math.min(MAX_RANGE, BASE_RANGE + Math.max(0, rangeLevel) * RANGE_PER_LEVEL);
        return Math.min(HARD_MAX_RANGE, range * Math.max(0.0, multiplier));
    }

    /** Ob dieser Attractor filtern kann: nur mit Beruehrung des Konstrukteurs. */
    public static boolean canFilter(ItemStack stack, @Nullable Level level) {
        return hasConstructorsTouch(stack, level);
    }

    /** Der wirksame Filter: der gespeicherte, aber nur mit Beruehrung des Konstrukteurs; sonst null. */
    public static @Nullable String effectiveFilter(ItemStack stack, @Nullable Level level) {
        return canFilter(stack, level) ? filterOf(stack) : null;
    }

    /**
     * Config tools.magnetRangeMultiplier (Standard 1): Faktor auf die ganze Reichweite, fuer den
     * gehaltenen und den abgelegten Attractor ({@code PlacedAttractors}).
     */
    public static double rangeMultiplier() {
        com.simplebuilding.config.SimplebuildingConfig config = com.simplebuilding.Simplebuilding.getConfig();
        return config == null ? 1.0
                : com.simplebuilding.config.SimplebuildingConfig.bounded(config.tools.magnetRangeMultiplier, 0, 4, 1.0);
    }

    public static boolean passesFilter(ItemEntity itemEntity, @Nullable String filterId) {
        if (filterId == null || filterId.isEmpty()) {
            return true;
        }

        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem()).toString();
        return filterId.equals(itemId);
    }

    public static double minimumDistance() {
        return com.simplebuilding.config.ServerTuningConfig.clamp(
                com.simplebuilding.config.ServerTuning.get().tools.attractorMinimumDistance, 0.5, 2.0, 1.25);
    }

    private static void applyMagnetForce(ItemEntity itemEntity, Vec3 targetPos) {
        Vec3 itemPos = new Vec3(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
        Vec3 vec = targetPos.subtract(itemPos);
        double distanceSq = vec.lengthSqr();

        if (distanceSq > minimumDistance() * minimumDistance()) {
            Vec3 pull = vec.normalize().scale(0.10);
            Vec3 newVel = itemEntity.getDeltaMovement().scale(0.80).add(pull);

            if (itemEntity.onGround()) {
                newVel = newVel.add(0, 0.15, 0);
            }

            itemEntity.setDeltaMovement(newVel);
            return;
        }

        itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().scale(0.2));
    }

    private static void syncVelocityToNearbyPlayers(ServerLevel world, ItemEntity itemEntity) {
        ClientboundSetEntityMotionPacket packet = new ClientboundSetEntityMotionPacket(itemEntity);
        for (ServerPlayer serverPlayer : world.players()) {
            if (serverPlayer.distanceToSqr(itemEntity) < SYNC_DISTANCE_SQ) {
                serverPlayer.connection.send(packet);
            }
        }
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (canFilter(stack, world)) {
            ItemEntity seen = itemEntityInSight(player, player.blockInteractionRange());
            if (seen != null) {
                return pickFilter(world, player, stack, seen.getItem());
            }
        }
        if (getFilterId(stack) != null) {
            if (!world.isClientSide()) {
                setFilter(stack, null);
                // Keine Einblendung (Besitzer 2026-09-28): Rueckmeldung sind Klang und Tooltip.
                world.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Rechtsklick auf einen Block. Mit Beruehrung und Schleichen: Filter auf ein liegendes Item vor
     * dem Block oder auf den Block selbst. Sonst ablegen ({@link com.simplebuilding.util.PlacedTemplates#tryPlace}),
     * mit Beruehrung ohne Schleichen, ohne sie mit Schleichen.
     */
    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        if (player != null && player.isSecondaryUseActive() && canFilter(stack, level)) {
            double reach = context.getClickLocation().distanceTo(player.getEyePosition());
            ItemEntity seen = itemEntityInSight(player, reach);
            if (seen != null) {
                return pickFilter(level, player, stack, seen.getItem());
            }
            ItemStack block = new ItemStack(level.getBlockState(context.getClickedPos()).getBlock().asItem());
            if (!block.isEmpty()) {
                return pickFilter(level, player, stack, block);
            }
        }
        InteractionResult placed = com.simplebuilding.util.PlacedTemplates.tryPlace(context);
        return placed != null ? placed : InteractionResult.PASS;
    }

    /** Das erste liegende Item auf dem Sehstrahl innerhalb {@code reach}, sonst null. */
    public static @Nullable ItemEntity itemEntityInSight(Player player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0f);
        Vec3 end = eye.add(view.scale(reach));
        net.minecraft.world.phys.EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player.level(), player, eye, end, player.getBoundingBox().expandTowards(view.scale(reach)).inflate(1.0),
                entity -> entity instanceof ItemEntity item && item.isAlive() && !item.getItem().isEmpty(), 0.3f);
        return hit != null && hit.getEntity() instanceof ItemEntity item ? item : null;
    }

    /** Stellt den Filter auf das Item von {@code source} (beide Seiten gleich, Klang nur vom Server). */
    public static InteractionResult pickFilter(Level level, Player player, ItemStack attractor, ItemStack source) {
        if (source.isEmpty() || source.is(com.simplebuilding.util.PlacedAttractors.IGNORE)) {
            return InteractionResult.FAIL;
        }
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(source.getItem()).toString();
        setFilter(attractor, id);
        if (!level.isClientSide()) {
            // Keine Einblendung (Besitzer 2026-09-28): Rueckmeldung sind Klang und Tooltip.
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN,
                    SoundSource.PLAYERS, 0.5f, 1.5f);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Im Inventar: der Attractor am Mauszeiger, Rechtsklick auf ein Item in einem Feld stellt den
     * Filter darauf (nur mit Beruehrung; auf ein leeres Feld legt er sich wie gewohnt ab).
     */
    @Override
    public boolean overrideStackedOnOther(ItemStack attractor, net.minecraft.world.inventory.Slot slot,
                                          net.minecraft.world.inventory.ClickAction action, Player player) {
        if (action != net.minecraft.world.inventory.ClickAction.SECONDARY || !slot.hasItem() || !canFilter(attractor, player.level())) {
            return false;
        }
        return pickFilter(player.level(), player, attractor, slot.getItem()) == InteractionResult.SUCCESS;
    }

    /** Im Inventar: ein Item am Mauszeiger, Rechtsklick auf den Attractor stellt den Filter darauf. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack attractor, ItemStack carried, net.minecraft.world.inventory.Slot slot,
                                            net.minecraft.world.inventory.ClickAction action, Player player,
                                            net.minecraft.world.entity.SlotAccess carriedAccess) {
        if (action != net.minecraft.world.inventory.ClickAction.SECONDARY || carried.isEmpty() || !canFilter(attractor, player.level())) {
            return false;
        }
        return pickFilter(player.level(), player, attractor, carried) == InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay component, Consumer<Component> tooltip, TooltipFlag type) {
        if (!hasConstructorsTouch(stack, null)) {
            // Schlichter Magnet: kein Filter, nur der Hinweis auf die Verzauberung.
            tooltip.accept(Component.translatable("tooltip.simplebuilding.magnet.touch_hint").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        String filter = getFilterId(stack);
        if (filter != null && !filter.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.simplebuilding.magnet.filtering", filter).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.accept(Component.translatable("tooltip.simplebuilding.magnet.no_filter").withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("tooltip.simplebuilding.magnet.clear").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.simplebuilding.magnet.clear.2").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Setzt ({@code id}) oder loescht ({@code null}) den gespeicherten Filter. */
    public static void setFilter(ItemStack stack, @Nullable String id) {
        CustomData nbtComponent = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = nbtComponent.copyTag();
        if (id == null) nbt.remove(FILTER_KEY);
        else nbt.putString(FILTER_KEY, id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    /** Der Filter (Item-Id) dieses Attractors, oder null ohne Filter. */
    public static @Nullable String filterOf(ItemStack stack) {
        CustomData nbtComponent = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = nbtComponent.copyTag();
        return nbt.contains(FILTER_KEY) ? nbt.getStringOr(FILTER_KEY, "") : null;
    }

    private String getFilterId(ItemStack stack) {
        CustomData nbtComponent = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = nbtComponent.copyTag();
        // Hier den leeren String als Default, falls getString einen braucht
        if (nbt.contains(FILTER_KEY)) {
            return nbt.getStringOr(FILTER_KEY, "");
        }
        return null;
    }
}
