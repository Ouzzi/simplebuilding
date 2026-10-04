package com.simplebuilding.util;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.PlacedTemplateBlock;
import com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BlueprintItem;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.SledgehammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Abgelegte Schmiedevorlagen (Besitzer 2026-09-28): Schleichen + Rechtsklick mit einer Vorlage legt
 * sie flach auf den Boden, an die Wand oder unter die Decke ({@link PlacedTemplateBlock}); ohne
 * Schleichen bleibt alles beim Alten. Blaupausen legen sich genauso ab, als {@code placed_blueprint}
 * mit derselben Block-Entity und demselben Renderer; gesperrte Oktanten als Merkstein, dessen Auswahl
 * jeder Spieler per Rechtsklick fuer sich ein- und ausblendet ({@link #toggleOctantOutline}).
 *
 * <p>Die Besatz-Aufwertung ({@link SledgehammerEntityInteraction}) geht auch an der abgelegten Vorlage:
 * Vorschlaghammer in der Haupthand, Leuchttinte oder Glowstonestaub in der Nebenhand, dann
 * {@link #PLACED_HITS} Schlaege statt einem - jeder Schlag mit Funken und einem hoeher werdenden Ton,
 * der letzte kostet das Nebenhand-Material und auf 26.3 die Zusatzmaterialien aus dem Inventar.
 * Wer das Material in der Hand haelt, sieht an jeder
 * aufwertbaren abgelegten Vorlage in seiner Naehe ab und zu kreisende Funken ({@link #tryHint}).
 */
public final class PlacedTemplates {
    /** Schlaege, die eine abgelegte Vorlage bis zur Aufwertung braucht (im Rahmen: einer). */
    public static final int PLACED_HITS = 5;
    /** Wer so lange nicht nachschlaegt, faengt wieder bei null an. */
    public static final int HIT_RESET_TICKS = 100;
    /** So weit (Bloecke) sieht eine abgelegte Vorlage einen Spieler mit Material in der Hand. */
    public static final double HINT_RANGE = 6.0;
    /** Abstand der Hinweis-Funken in Ticks (je Vorlage, versetzt nach Position). */
    public static final int HINT_INTERVAL = 10;
    /** Hoechstens so oft (Ticks) ein leiser Ton zu den Funken. */
    public static final int HINT_SOUND_INTERVAL = 60;

    private PlacedTemplates() {
    }

    // =====================================================================================
    // Ablegen
    // =====================================================================================

    /**
     * Vorlagen, die sich ablegen lassen: jede {@link SmithingTemplateItem} (Netherit-Aufwertung, alle
     * Besatzvorlagen, die Besatz-Aufwertungen der Mod, Vorlagen anderer Mods), die beiden
     * Aufwertungsvorlagen der Mod, die schlichte Items sind, der Attractor ({@link PlacedAttractors}:
     * abgelegt zieht er lose Items an) und der kalibrierte Detector ({@link PlacedDetectors}).
     */
    public static boolean isPlaceableTemplate(ItemStack stack) {
        Item item = stack.getItem();
        return !stack.isEmpty() && (item instanceof SmithingTemplateItem
                || item == ModItems.ENDERITE_UPGRADE_TEMPLATE || item == ModItems.BASIC_UPGRADE_TEMPLATE
                || item == ModItems.MAGNET
                // Kalibrierter Detector (PlacedDetectors: abgelegt sucht er weiter nach seinem Zielblock).
                || com.simplebuilding.items.custom.OreDetectorItem.isArmed(stack));
    }

    /**
     * Kleinteile (Besitzer 2026-10-02): Steinkiesel, Feuersteinsplitter, Stoecke, Barren, Klumpen, Edelsteine, Ziegel -
     * Tag {@code simplebuilding:placeable_small}. Server-Optionen: {@code server.features.placeVanillaItems} schaltet
     * alle Vanilla-Teile ab, {@code server.features.placeDisabledItems} einzelne IDs (auch Mod-Teile).
     */
    public static boolean isPlaceableSmall(ItemStack stack) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES || stack.isEmpty() || !stack.is(com.simplebuilding.util.ModTags.Items.PLACEABLE_SMALL)) {
            return false;
        }
        var features = com.simplebuilding.config.ServerTuning.get().features;
        net.minecraft.resources.Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if ("minecraft".equals(id.getNamespace()) && !features.placeVanillaItems) {
            return false;
        }
        return !itemListed(features.placeDisabledItems, id);
    }

    /** Ob {@code id} in einer Liste von Item-IDs steht (Komma, Semikolon oder Leerzeichen; ohne Namensraum = minecraft). */
    public static boolean itemListed(@Nullable String list, net.minecraft.resources.Identifier id) {
        if (list == null || list.isBlank()) {
            return false;
        }
        for (String entry : list.split("[,;\\s]+")) {
            String value = entry.trim().toLowerCase(java.util.Locale.ROOT);
            if (!value.isEmpty() && (value.contains(":") ? value : "minecraft:" + value).equals(id.toString())) {
                return true;
            }
        }
        return false;
    }

    /** Blaupausen lassen sich genauso ablegen (Besitzer 2026-09-28), als eigener Block. */
    public static boolean isPlaceableBlueprint(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlueprintItem;
    }

    /**
     * Oktanten legen sich ab, wenn sie gesperrt sind (Besitzer 2026-09-29, "platzierter Oktant"):
     * ungesperrt setzt Schleichen + Rechtsklick die zweite Ecke, gesperrt ist die Auswahl fertig und der
     * Oktant wird zum Merkstein - mit Ecken, Form und Farbe. Abgelegt wie eine Vorlage
     * ({@code placed_smithing_template}); Rechtsklick darauf schaltet fuer diesen Spieler die Auswahl
     * ein und aus ({@link #toggleOctantOutline}).
     */
    public static boolean isPlaceableOctant(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof OctantItem
                && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr("Locked", false);
    }

    /** Der Block, als der dieser Stapel abgelegt wird, oder null, wenn er sich nicht ablegen laesst. */
    public static @Nullable Block placedBlockFor(ItemStack stack) {
        if (isPlaceableTemplate(stack) || isPlaceableOctant(stack) || isPlaceableSmall(stack)) {
            return ModBlocks.PLACED_SMITHING_TEMPLATE;
        }
        return isPlaceableBlueprint(stack) ? ModBlocks.PLACED_BLUEPRINT : null;
    }

    /**
     * Ob dieser Klick ablegt: Schleichen + Rechtsklick - ausser beim Attractor mit Beruehrung des
     * Konstrukteurs, dessen Schleich-Rechtsklick den Filter stellt ({@code MagnetItem#useOn}); er legt
     * sich mit einfachem Rechtsklick ab (Besitzer 2026-09-29).
     */
    public static boolean isPlaceGesture(Player player, ItemStack stack, Level level) {
        if (PlacedAttractors.isAttractor(stack) && com.simplebuilding.items.custom.MagnetItem.canFilter(stack, level)) {
            return !player.isSecondaryUseActive();
        }
        return player.isSecondaryUseActive();
    }

    /**
     * Rechtsklick einer Vorlage oder Blaupause auf einen Block (aus {@code Item#useOn}). Null, wenn
     * nichts abgelegt wird - dann laeuft das gewohnte Verhalten des Items weiter.
     */
    public static @Nullable InteractionResult tryPlace(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Block block = placedBlockFor(stack);
        if (player == null || !isPlaceGesture(player, stack, context.getLevel()) || block == null || !player.mayBuild()) {
            return null;
        }
        BlockPlaceContext place = new BlockPlaceContext(context);
        if (!place.canPlace()) {
            return null;
        }
        Level level = context.getLevel();
        BlockPos pos = place.getClickedPos();
        BlockState state = block.getStateForPlacement(place);
        if (state == null || !state.canSurvive(level, pos)) {
            return null;
        }
        if (!level.isClientSide()) {
            if (!level.setBlock(pos, state, 11)) {
                return null;
            }
            if (level.getBlockEntity(pos) instanceof PlacedTemplateBlockEntity be) {
                be.setTemplate(stack.copyWithCount(1));
            }
            SoundType sound = state.getSoundType();
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    // =====================================================================================
    // Abgelegter Oktant: Auswahl je Spieler ein- und ausblenden
    // =====================================================================================

    /** Liegt an {@code pos} ein abgelegter Oktant? */
    public static boolean isPlacedOctant(BlockGetter level, BlockPos pos) {
        return templateAt(level, pos).getItem() instanceof OctantItem;
    }

    /**
     * Rechtsklick auf einen abgelegten Oktanten: blendet dessen Auswahl fuer genau diesen Spieler ein
     * oder wieder aus. Wer sie eingeblendet hat, sieht den Oktanten ausserdem durch Waende leuchten, um
     * ihn wiederzufinden. Gespeichert und zum Client geschickt wird die Liste der Spieler in der
     * Block-Entity ({@link PlacedTemplateBlockEntity#outlineViewers()}). Liefert den neuen Zustand fuer
     * den Spieler (true = eingeblendet).
     */
    public static boolean toggleOctantOutline(Level level, BlockPos pos, PlacedTemplateBlockEntity be, Player player) {
        boolean shown = be.toggleOutlineViewer(player.getUUID());
        level.playSound(null, pos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 0.6F, shown ? 1.4F : 0.9F);
        return shown;
    }

    // =====================================================================================
    // Aufwerten mit dem Vorschlaghammer
    // =====================================================================================

    /** Vorschlaghammer in der Haupthand und ein Aufwertungs-Material in der Nebenhand. */
    public static boolean isHammerStance(Player player) {
        return player.getMainHandItem().getItem() instanceof SledgehammerItem
                && SledgehammerEntityInteraction.trimUpgrades().containsKey(player.getOffhandItem().getItem());
    }

    /** Die Vorlage an {@code pos}, oder leer, wenn dort keine abgelegte Vorlage liegt. */
    public static ItemStack templateAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PlacedTemplateBlockEntity be ? be.getTemplate() : ItemStack.EMPTY;
    }

    /** Liegt hier eine Besatzvorlage, die sich aufwerten laesst? */
    public static boolean isUpgradable(BlockGetter level, BlockPos pos) {
        ItemStack template = templateAt(level, pos);
        return !template.isEmpty() && SledgehammerEntityInteraction.isTrimTemplate(template.getItem());
    }

    /**
     * Zeigt der Spieler mit Hammer und Material auf eine aufwertbare abgelegte Vorlage? Auch fuer die
     * Hammer-Neigung in der ersten Person ({@code HeldItemRendererMixin}).
     */
    public static boolean isHammerTarget(BlockGetter level, BlockPos pos, Player player) {
        return isHammerStance(player) && level.getBlockState(pos).getBlock() instanceof PlacedTemplateBlock
                && isUpgradable(level, pos);
    }

    /**
     * Ein Schlag des Vorschlaghammers auf die abgelegte Vorlage (Linksklick: {@code attack} im
     * Ueberlebensmodus, {@code canDestroyBlock} im Kreativmodus). Zaehlt je Material und laesst die
     * Zaehlung nach {@link #HIT_RESET_TICKS} ohne Schlag verfallen; der {@link #PLACED_HITS}. Schlag
     * wertet auf. Liefert true, wenn der Schlag zaehlte.
     */
    public static boolean hit(Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !isHammerTarget(level, pos, player)
                || !com.simplebuilding.api.WorldPermissions.mayChange(level, player, pos)
                || !(level.getBlockEntity(pos) instanceof PlacedTemplateBlockEntity be)) {
            return false;
        }
        ItemStack hammer = player.getMainHandItem();
        ItemStack catalyst = player.getOffhandItem();
        if (!SledgehammerEntityInteraction.hasMaterials(player)) {
            return false;
        }
        Item result = SledgehammerEntityInteraction.trimUpgrades().get(catalyst.getItem());
        long now = level.getGameTime();
        int hits = be.registerHit(catalyst.getItem(), now);
        if (!player.isCreative()) {
            hammer.hurtAndBreak(SledgehammerEntityInteraction.HAMMER_DAMAGE, player, EquipmentSlot.MAINHAND);
        }
        boolean glowing = result == ModItems.GLOWING_TRIM_TEMPLATE;
        Vec3 centre = be.surfaceCentre();
        if (hits < PLACED_HITS) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.8F, 0.9F + 0.25F * hits);
            if (level instanceof ServerLevel server) {
                server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, be.getTemplate().getItem()),
                        centre.x, centre.y, centre.z, 4 + 2 * hits, 0.2, 0.05, 0.2, 0.05);
                server.sendParticles(catalystParticle(glowing), centre.x, centre.y, centre.z, 2 * hits, 0.25, 0.05, 0.25, 0.01);
            }
            return true;
        }
        be.setTemplate(new ItemStack(result));
        // Fortschritt hammer/glow_up (frueher nur ueber den entfallenen Rahmen-Weg ausgeloest).
        com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.TRIM_TEMPLATE_FORGED);
        SledgehammerEntityInteraction.consumeMaterials(player);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1.0F, 1.5F);
        level.playSound(null, pos, glowing ? SoundEvents.GLOW_INK_SAC_USE : SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.GLOW, centre.x, centre.y, centre.z, 8, 0.3, 0.1, 0.3, 0.02);
            server.sendParticles(glowing ? ParticleTypes.GLOW_SQUID_INK : ParticleTypes.LARGE_SMOKE,
                    centre.x, centre.y, centre.z, 4, 0.2, 0.05, 0.2, 0.01);
        }
        return true;
    }

    private static ParticleOptions catalystParticle(boolean glowInk) {
        return glowInk ? ParticleTypes.GLOW : ParticleTypes.WAX_ON;
    }

    // =====================================================================================
    // Hinweis
    // =====================================================================================

    /** Haelt der Spieler Leuchttinte oder Glowstonestaub in einer der beiden Haende? */
    public static boolean holdsCatalyst(Player player) {
        return SledgehammerEntityInteraction.trimUpgrades().containsKey(player.getMainHandItem().getItem())
                || SledgehammerEntityInteraction.trimUpgrades().containsKey(player.getOffhandItem().getItem());
    }

    /** Der naechste Spieler mit Material in der Hand, fuer den diese Vorlage einen Hinweis zeigt; sonst null. */
    public static @Nullable Player hintViewer(Level level, BlockPos pos) {
        if (!isUpgradable(level, pos)) {
            return null;
        }
        return level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HINT_RANGE,
                entity -> entity instanceof Player player && !player.isSpectator() && holdsCatalyst(player));
    }

    /**
     * Hinweis-Funken: drei Funken kreisen ueber der Vorlage (Leuchttinte: Leuchtpartikel, Glowstone:
     * Wachsfunken), dazu hoechstens alle {@link #HINT_SOUND_INTERVAL} Ticks ein leises Klingen. Nur,
     * wenn ein Spieler mit Material in der Hand in der Naehe ist. Liefert den Spieler, fuer den der
     * Hinweis kam, sonst null.
     */
    public static @Nullable Player tryHint(ServerLevel level, BlockPos pos, PlacedTemplateBlockEntity be) {
        Player viewer = hintViewer(level, pos);
        if (viewer == null) {
            return null;
        }
        long now = level.getGameTime();
        boolean glowInk = SledgehammerEntityInteraction.trimUpgrades().containsKey(viewer.getOffhandItem().getItem())
                ? viewer.getOffhandItem().getItem() == net.minecraft.world.item.Items.GLOW_INK_SAC
                : viewer.getMainHandItem().getItem() == net.minecraft.world.item.Items.GLOW_INK_SAC;
        Vec3 centre = be.surfaceCentre();
        Direction normal = be.normal();
        for (int i = 0; i < 3; i++) {
            double angle = now * 0.35 + i * (Math.PI * 2.0 / 3.0);
            double a = Math.cos(angle) * 0.38;
            double b = Math.sin(angle) * 0.38;
            // Kreis in der Ebene der Vorlage, leicht davor.
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
