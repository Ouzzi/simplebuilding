package com.simplebuilding.items.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class OreDetectorItem extends Item {

    // Balancing Konstanten (Radien)
    private static final int RANGE_COMMON = 24;    // Kohle, Eisen, Kupfer
    private static final int RANGE_MEDIUM = 16;    // Gold, Lapis, Redstone
    private static final int RANGE_RARE = 10;      // Diamant, Smaragd
    private static final int RANGE_VERY_RARE = 6;  // Netherite (Muss sehr nah sein)

    private static final int SCAN_INTERVAL = 20;   // Ping alle 1 Sekunde (20 Ticks)

    private enum DetectMode {
        IRON(Formatting.GRAY, "Iron", BlockTags.IRON_ORES),
        GOLD(Formatting.GOLD, "Gold", BlockTags.GOLD_ORES),
        DIAMOND(Formatting.AQUA, "Diamond", BlockTags.DIAMOND_ORES),
        NETHERITE(Formatting.DARK_PURPLE, "Netherite", null),
        ALL(Formatting.WHITE, "All Ores", null),
        CUSTOM(Formatting.YELLOW, "Custom", null);

        final Formatting color;
        final String name;
        final TagKey<Block> tag;

        DetectMode(Formatting color, String name, TagKey<Block> tag) {
            this.color = color;
            this.name = name;
            this.tag = tag;
        }
    }

    public OreDetectorItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof PlayerEntity player)) return;

        // Nur aktiv, wenn in Main- oder Offhand gehalten
        boolean isHeld = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        if (!isHeld) return;

        // Zeit-Check (Scannt nur jede Sekunde)
        if (world.getTime() % SCAN_INTERVAL != 0) return;

        DetectMode mode = getMode(stack);
        BlockPos playerPos = player.getBlockPos();

        // 1. Suche starten
        BlockPos targetPos = findNearestOre(world, playerPos, mode, stack);

        if (targetPos != null) {
            BlockState targetState = world.getBlockState(targetPos);

            // 2. Distanz berechnen
            double distance = Math.sqrt(playerPos.getSquaredDistance(targetPos));

            // 3. Audio Logic
            // Pitch variiert leicht je nach Distanz (näher = höher)
            float pitch = (float) (1.8f - (distance / 32.0f));
            pitch = Math.max(0.6f, Math.min(2.0f, pitch));

            // Sound 1: "Ping" Sound (Amethyst) - Das Sonar
            world.playSound(null, targetPos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.8f, pitch);

            // Sound 2: "Tech" Sound (Sculk Sensor) - Die Mechanik
            world.playSound(null, targetPos, SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.BLOCKS, 0.5f, 2.0f);

            // Sound 3 (NEU): Der Block-Sound selbst (z.B. Stein-Knacken oder Glas-Klirren bei Diamant)
            // Wir nehmen eine leisere Lautstärke (0.35), damit es subtil im Hintergrund ist.
            SoundEvent blockSound = targetState.getSoundGroup().getBreakSound();
            world.playSound(null, targetPos, blockSound, SoundCategory.BLOCKS, 0.35f, pitch);

            // 4. Visueller "Sonar-Strahl"
            spawnSonarBeam(world, player.getEyePos(), targetPos, targetState);
        }
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient() && user.isSneaking()) {
            ItemStack stack = user.getStackInHand(hand);
            cycleMode(stack, user);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() != null && context.getPlayer().isSneaking()) {
            World world = context.getWorld();
            if (!world.isClient()) {
                ItemStack stack = context.getStack();
                BlockState state = world.getBlockState(context.getBlockPos());

                setMode(stack, DetectMode.CUSTOM);
                setCustomBlock(stack, state);

                context.getPlayer().sendMessage(Text.literal("Calibrated to: ").formatted(Formatting.GREEN)
                        .append(state.getBlock().getName().copy().formatted(Formatting.WHITE)), true);

                // Sound-Feedback für Kalibrierung
                world.playSound(null, context.getBlockPos(), SoundEvents.BLOCK_SCULK_CATALYST_BLOOM, SoundCategory.PLAYERS, 1.0f, 1.0f);

                return ActionResult.SUCCESS;
            }
        }
        return super.useOnBlock(context);
    }

    // --- LOGIK & BALANCING ---

    private BlockPos findNearestOre(World world, BlockPos start, DetectMode mode, ItemStack stack) {
        BlockPos nearest = null;
        double minSqDist = Double.MAX_VALUE;

        // Custom Block Lookup vorbereiten
        BlockState customTarget = null;
        if (mode == DetectMode.CUSTOM) {
            customTarget = getCustomBlock(stack, world.getRegistryManager());
        }

        // Maximale Reichweite basierend auf dem Modus bestimmen (Performance & Balance)
        int searchRadius = getSearchRadiusForMode(mode);

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {

                    BlockPos pos = start.add(x, y, z);
                    double sqDist = start.getSquaredDistance(pos);

                    // Globale Begrenzung und Optimierung
                    if (sqDist > (searchRadius * searchRadius)) continue;
                    if (sqDist >= minSqDist) continue;

                    BlockState state = world.getBlockState(pos);

                    // Hier prüfen wir, ob das Erz gültig ist UND ob es nah genug für seinen Typ ist
                    if (isValidTargetWithDistance(state, mode, customTarget, Math.sqrt(sqDist))) {
                        minSqDist = sqDist;
                        nearest = pos;
                    }
                }
            }
        }
        return nearest;
    }

    private boolean isValidTargetWithDistance(BlockState state, DetectMode mode, BlockState customTarget, double distance) {
        if (state.isAir()) return false;

        // 1. Ist es überhaupt das gesuchte Erz?
        boolean isMatch = checkMatch(state, mode, customTarget);
        if (!isMatch) return false;

        // 2. Balance-Check: Ist es nah genug für diesen speziellen Block?
        int maxDist = getBalancedDistance(state);
        return distance <= maxDist;
    }

    private boolean checkMatch(BlockState state, DetectMode mode, BlockState customTarget) {
        return switch (mode) {
            case IRON -> state.isIn(BlockTags.IRON_ORES);
            case GOLD -> state.isIn(BlockTags.GOLD_ORES);
            case DIAMOND -> state.isIn(BlockTags.DIAMOND_ORES);
            case NETHERITE -> state.isOf(Blocks.ANCIENT_DEBRIS);
            case ALL -> state.isIn(BlockTags.COAL_ORES) || state.isIn(BlockTags.IRON_ORES) ||
                    state.isIn(BlockTags.COPPER_ORES) || state.isIn(BlockTags.GOLD_ORES) ||
                    state.isIn(BlockTags.REDSTONE_ORES) || state.isIn(BlockTags.LAPIS_ORES) ||
                    state.isIn(BlockTags.DIAMOND_ORES) || state.isIn(BlockTags.EMERALD_ORES) ||
                    state.isOf(Blocks.ANCIENT_DEBRIS) || state.isOf(Blocks.NETHER_QUARTZ_ORE);
            case CUSTOM -> customTarget != null && state.isOf(customTarget.getBlock());
        };
    }

    private int getBalancedDistance(BlockState state) {
        if (state.isOf(Blocks.ANCIENT_DEBRIS)) return RANGE_VERY_RARE; // Netherite (6)

        if (state.isIn(BlockTags.DIAMOND_ORES) || state.isIn(BlockTags.EMERALD_ORES)) return RANGE_RARE; // Diamant (10)

        if (state.isIn(BlockTags.GOLD_ORES) || state.isIn(BlockTags.LAPIS_ORES) ||
                state.isIn(BlockTags.REDSTONE_ORES) || state.isOf(Blocks.NETHER_QUARTZ_ORE)) return RANGE_MEDIUM; // Mittel (16)

        return RANGE_COMMON; // Kohle, Eisen, Kupfer, Custom (24)
    }

    private int getSearchRadiusForMode(DetectMode mode) {
        // Gibt den maximal möglichen Radius für den Modus zurück für die Loop-Optimierung
        return switch (mode) {
            case NETHERITE -> RANGE_VERY_RARE;
            case DIAMOND -> RANGE_RARE;
            case ALL, CUSTOM, IRON -> RANGE_COMMON; // Im "ALL" Modus müssen wir weit suchen für Kohle
            default -> RANGE_MEDIUM;
        };
    }

    // --- VISUALS ---

    private void spawnSonarBeam(ServerWorld world, Vec3d startPos, BlockPos endPos, BlockState targetState) {
        // Zielkoordinaten (Mitte des Blocks)
        Vec3d targetCenter = endPos.toCenterPos();

        // Vektor vom Auge zum Ziel
        Vec3d direction = targetCenter.subtract(startPos);
        double distance = direction.length();
        Vec3d dirNormalized = direction.normalize();

        // Wir zeichnen eine Linie (Beam) aus Partikeln
        double stepSize = 0.3;

        for (double d = 0.5; d < distance; d += stepSize) {
            Vec3d particlePos = startPos.add(dirNormalized.multiply(d));

            // WICHTIG: speed=0 sorgt dafür, dass die Partikel exakt an der Stelle bleiben
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, targetState),
                    particlePos.x, particlePos.y, particlePos.z,
                    1, // Anzahl pro Punkt
                    0, 0, 0, // Kein Spread
                    0 // Keine Geschwindigkeit
            );
        }

        // Highlight am Ziel: Eine kleine Explosion aus Glitzer
        world.spawnParticles(ParticleTypes.WAX_ON, targetCenter.x, targetCenter.y, targetCenter.z, 5, 0.3, 0.3, 0.3, 0.05);
    }

    // --- NBT / DATA HANDLING ---

    private void cycleMode(ItemStack stack, PlayerEntity player) {
        DetectMode current = getMode(stack);
        DetectMode[] modes = DetectMode.values();
        DetectMode next = modes[(current.ordinal() + 1) % modes.length];
        setMode(stack, next);

        player.sendMessage(Text.literal("Detector Mode: ").formatted(Formatting.GRAY)
                .append(Text.literal(next.name).formatted(next.color)), true);

        // Kleines Klick-Geräusch beim Umschalten
        player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.5f);
    }

    private DetectMode getMode(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (nbt.contains("Mode")) {
            return DetectMode.values()[nbt.getInt("Mode", 0)];
        }
        return DetectMode.ALL;
    }

    private void setMode(ItemStack stack, DetectMode mode) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putInt("Mode", mode.ordinal()));
    }

    private void setCustomBlock(ItemStack stack, BlockState state) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt ->
                nbt.put("CustomBlock", NbtHelper.fromBlockState(state))
        );
    }

    private BlockState getCustomBlock(ItemStack stack, RegistryWrapper.WrapperLookup registryLookup) {
        if (registryLookup == null) return null;
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (nbt.contains("CustomBlock")) {
            var blockRegistry = registryLookup.getOrThrow(RegistryKeys.BLOCK);
            return NbtHelper.toBlockState(blockRegistry, nbt.getCompound("CustomBlock").orElse(null));
        }
        return null;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        DetectMode mode = getMode(stack);
        textConsumer.accept(Text.literal("Mode: ").formatted(Formatting.GRAY)
                .append(Text.literal(mode.name).formatted(mode.color)));

        if (mode == DetectMode.CUSTOM) {
            BlockState custom = getCustomBlock(stack, context.getRegistryLookup());
            if (custom != null) {
                textConsumer.accept(Text.literal("Calibrated to: ").formatted(Formatting.GRAY)
                        .append(custom.getBlock().getName().copy().formatted(Formatting.GREEN)));
            } else {
                textConsumer.accept(Text.literal("Calibrated: None (Sneak-Right-Click a block)").formatted(Formatting.RED));
            }
        } else {
            textConsumer.accept(Text.literal("Sneak + Right-Click to cycle modes").formatted(Formatting.DARK_GRAY));
        }

        // Info über Reichweiten im Tooltip
        textConsumer.accept(Text.empty());
        textConsumer.accept(Text.literal("Range: " + getSearchRadiusForMode(mode) + " Blocks").formatted(Formatting.DARK_AQUA));
    }
}