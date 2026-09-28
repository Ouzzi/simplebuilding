package com.simplebuilding.compat;

import com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock;
import com.simplebuilding.blocks.custom.TieredChestBlock;
import com.simplebuilding.blocks.entity.custom.FurnaceTierPerks;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.tweaks.block.LaunchpadBlock;
import com.simplebuilding.tweaks.block.entity.ChunkLoaderBlockEntity;
import com.simplebuilding.tweaks.block.entity.LaunchpadBlockEntity;
import com.simplebuilding.tweaks.block.entity.OwnedBlockEntity;
import com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity;
import com.simplebuilding.util.HopperFilterMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jetbrains.annotations.Nullable;

/**
 * What the optional Jade plugin ({@code common/src/jade/java}) shows for the mod's blocks, without
 * any Jade class: a block (entity) becomes a list of {@link Line}s - a translation key plus plain
 * arguments - that the server can put into Jade's data tag ({@link #write}) and the client reads
 * back ({@link #read}) and turns into components. Keeping it here lets the server game tests check
 * the values; the Jade classes only adapt it.
 *
 * <p>Two kinds of {@link Topic}: the server ones read block entity fields the client does not have
 * (or not reliably: the owner's name, launchpad charges, the potion pad's cooldown, forced chunks,
 * the hopper filter) and travel through Jade's server data; the state ones only need the block state
 * the client already has (piston durability, chest slots, furnace speed) and also work when the
 * server has no Jade.
 */
public final class BlockInfo {

    /** Most potion pad effects listed; a potion with more ends in "...". */
    public static final int MAX_EFFECT_LINES = 3;

    private BlockInfo() {
    }

    /** One switchable piece of information; {@link #id()} is the path of its Jade config id. */
    public enum Topic {
        OWNER("owner", true),
        PAD_STATUS("pad_status", true),
        HOPPER_FILTER("hopper_filter", true),
        PISTON_DURABILITY("piston_durability", false),
        CHEST_SLOTS("chest_slots", false),
        FURNACE_SPEED("furnace_speed", false);

        private final String id;
        private final boolean server;

        Topic(String id, boolean server) {
            this.id = id;
            this.server = server;
        }

        public String id() {
            return id;
        }

        /** Whether the lines come from the server ({@link #serverLines}) or the block state ({@link #stateLines}). */
        public boolean fromServer() {
            return server;
        }

        /** The key of this topic's lines in Jade's server data tag. */
        public String dataKey() {
            return "simplebuilding:" + id;
        }
    }

    /** A piece of an argument: plain text, or a translation key when {@code translate}. */
    public record Piece(String text, boolean translate) {
        public Component toComponent() {
            return translate ? Component.translatable(text) : Component.literal(text);
        }
    }

    /** One argument of a line: its pieces, written one after the other. */
    public record Arg(List<Piece> pieces) {
        public static Arg literal(Object value) {
            return new Arg(List.of(new Piece(String.valueOf(value), false)));
        }

        public static Arg translatable(String key) {
            return new Arg(List.of(new Piece(key, true)));
        }

        /** The raw pieces joined (translation keys stay keys); for the tests and the narration. */
        public String text() {
            StringBuilder builder = new StringBuilder();
            for (Piece piece : pieces) {
                builder.append(piece.text());
            }
            return builder.toString();
        }

        public Component toComponent() {
            MutableComponent result = Component.empty();
            for (Piece piece : pieces) {
                result.append(piece.toComponent());
            }
            return result;
        }
    }

    /** One tooltip line: {@code Component.translatable(key, args...)}. */
    public record Line(String key, List<Arg> args) {
        public static Line of(String key, Arg... args) {
            return new Line(key, List.of(args));
        }

        /** {@link Arg#text()} of every argument, in order. */
        public List<String> argTexts() {
            List<String> texts = new ArrayList<>(args.size());
            for (Arg arg : args) {
                texts.add(arg.text());
            }
            return texts;
        }

        public Component toComponent() {
            Object[] components = new Object[args.size()];
            for (int i = 0; i < components.length; i++) {
                components[i] = args.get(i).toComponent();
            }
            return Component.translatable(key, components);
        }
    }

    // =====================================================================================
    // SERVER TOPICS (block entity -> lines, sent through Jade's server data)
    // =====================================================================================

    /** Whether {@code topic} has anything to say about this block entity (decides Jade's registration, too). */
    public static boolean handles(Topic topic, BlockEntity be) {
        return switch (topic) {
            case OWNER -> be instanceof OwnedBlockEntity;
            case PAD_STATUS -> be instanceof LaunchpadBlockEntity || be instanceof PotionPadBlockEntity || be instanceof ChunkLoaderBlockEntity;
            case HOPPER_FILTER -> be instanceof ModHopperBlockEntity;
            default -> false;
        };
    }

    /** The lines of a server topic for {@code be}; empty for a state topic or a block entity it does not handle. */
    public static List<Line> serverLines(Topic topic, BlockEntity be) {
        List<Line> lines = new ArrayList<>();
        switch (topic) {
            case OWNER -> {
                if (be instanceof OwnedBlockEntity owned && owned.getOwner() != null) {
                    lines.add(Line.of("jade.simplebuilding.owner", Arg.literal(ownerName(be.getLevel(), owned.getOwner()))));
                }
            }
            case PAD_STATUS -> padStatus(be, lines);
            case HOPPER_FILTER -> {
                if (be instanceof ModHopperBlockEntity hopper) {
                    hopperFilter(hopper, lines);
                }
            }
            default -> {
            }
        }
        return lines;
    }

    /**
     * The owner's name: from the online player, else from the server's name cache (players who
     * joined before), else the UUID itself.
     */
    public static String ownerName(@Nullable Level level, UUID owner) {
        MinecraftServer server = level == null ? null : level.getServer();
        if (server != null) {
            ServerPlayer online = server.getPlayerList().getPlayer(owner);
            if (online != null) {
                return online.getName().getString();
            }
            String cached = server.services().nameToIdCache().get(owner).map(NameAndId::name).orElse(null);
            if (cached != null && !cached.isEmpty()) {
                return cached;
            }
        }
        return owner.toString();
    }

    private static void padStatus(BlockEntity be, List<Line> lines) {
        if (be instanceof LaunchpadBlockEntity launchpad) {
            BlockState state = be.getBlockState();
            int max = state.getBlock() instanceof LaunchpadBlock pad && be.getLevel() != null
                    ? pad.capacityAt(be.getLevel(), be.getBlockPos())
                    : LaunchpadBlock.maxCharges(LaunchpadBlock.ENDERITE_TIER);
            lines.add(Line.of("jade.simplebuilding.launchpad.charges", Arg.literal(launchpad.getCharges()), Arg.literal(max)));
        } else if (be instanceof PotionPadBlockEntity potionPad) {
            PotionContents stored = potionPad.getStored();
            if (stored == null || !stored.hasEffects()) {
                lines.add(Line.of("jade.simplebuilding.potion_pad.empty"));
            } else {
                int shown = 0;
                for (MobEffectInstance effect : stored.getAllEffects()) {
                    if (shown == MAX_EFFECT_LINES) {
                        lines.add(Line.of("jade.simplebuilding.potion_pad.more"));
                        break;
                    }
                    lines.add(Line.of("jade.simplebuilding.potion_pad.effect", effectArg(effect)));
                    shown++;
                }
            }
            int cooldown = potionPad.getCooldown();
            lines.add(cooldown > 0
                    ? Line.of("jade.simplebuilding.potion_pad.cooldown", Arg.literal((cooldown + 19) / 20))
                    : Line.of("jade.simplebuilding.potion_pad.ready"));
        } else if (be instanceof ChunkLoaderBlockEntity loader) {
            int chunks = loader.ownForced().size();
            lines.add(chunks > 0
                    ? Line.of("jade.simplebuilding.chunk_loader.active", Arg.literal(chunks))
                    : Line.of("jade.simplebuilding.chunk_loader.idle"));
        }
    }

    /** "Speed II": the effect's name, and vanilla's potency numeral from level II on. */
    private static Arg effectArg(MobEffectInstance effect) {
        List<Piece> pieces = new ArrayList<>();
        pieces.add(new Piece(effect.getEffect().value().getDescriptionId(), true));
        if (effect.getAmplifier() > 0) {
            pieces.add(new Piece(" ", false));
            pieces.add(new Piece("potion.potency." + effect.getAmplifier(), true));
        }
        return new Arg(pieces);
    }

    private static void hopperFilter(ModHopperBlockEntity hopper, List<Line> lines) {
        HopperFilterMode mode = hopper.getFilterMode();
        lines.add(Line.of("jade.simplebuilding.hopper.mode",
                Arg.translatable("simplebuilding.hopper_filter." + mode.name().toLowerCase(Locale.ROOT))));
        if (mode == HopperFilterMode.NONE) {
            return;
        }
        List<Piece> items = new ArrayList<>();
        for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
            ItemStack ghost = hopper.getGhostItem(slot);
            if (ghost.isEmpty()) {
                continue;
            }
            if (!items.isEmpty()) {
                items.add(new Piece(", ", false));
            }
            items.add(new Piece(ghost.getItem().getDescriptionId(), true));
        }
        lines.add(items.isEmpty()
                ? Line.of("jade.simplebuilding.hopper.nothing")
                : Line.of("jade.simplebuilding.hopper.items", new Arg(items)));
    }

    // =====================================================================================
    // STATE TOPICS (block state -> lines, computed on the client)
    // =====================================================================================

    /** The lines of a state topic for {@code state}; empty for a server topic or a block it does not handle. */
    public static List<Line> stateLines(Topic topic, BlockState state) {
        List<Line> lines = new ArrayList<>();
        switch (topic) {
            case PISTON_DURABILITY -> {
                int max = NetheriteBreakerPistonBlock.maxDurabilityOf(state);
                if (max > 0) {
                    lines.add(Line.of("tooltip.simplebuilding.netherite_piston.durability",
                            Arg.literal(NetheriteBreakerPistonBlock.durabilityOf(state)), Arg.literal(max)));
                }
            }
            case CHEST_SLOTS -> {
                if (state.getBlock() instanceof TieredChestBlock chest) {
                    boolean isDouble = state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE;
                    lines.add(Line.of("jade.simplebuilding.chest.slots", Arg.literal(chest.tier().slots() * (isDouble ? 2 : 1))));
                    if (chest.tier().stackMultiplier() > 1) {
                        lines.add(Line.of("jade.simplebuilding.chest.stacks", Arg.literal(chest.tier().stackMultiplier())));
                    }
                }
            }
            case FURNACE_SPEED -> {
                int factor = FurnaceTierPerks.speedFactor(state);
                if (factor > 1) {
                    lines.add(Line.of("jade.simplebuilding.furnace.speed", Arg.literal(factor)));
                }
            }
            default -> {
            }
        }
        return lines;
    }

    // =====================================================================================
    // TRANSPORT (server data tag)
    // =====================================================================================

    /** Puts {@code lines} under the topic's key into Jade's server data; nothing for no lines. */
    public static void write(CompoundTag data, Topic topic, List<Line> lines) {
        if (lines.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        for (Line line : lines) {
            CompoundTag lineTag = new CompoundTag();
            lineTag.putString("k", line.key());
            ListTag args = new ListTag();
            for (Arg arg : line.args()) {
                ListTag pieces = new ListTag();
                for (Piece piece : arg.pieces()) {
                    CompoundTag pieceTag = new CompoundTag();
                    pieceTag.putString("s", piece.text());
                    if (piece.translate()) {
                        pieceTag.putBoolean("t", true);
                    }
                    pieces.add(pieceTag);
                }
                args.add(pieces);
            }
            lineTag.put("a", args);
            list.add(lineTag);
        }
        data.put(topic.dataKey(), list);
    }

    /** The lines {@link #write} put into {@code data} for {@code topic}; empty when there are none. */
    public static List<Line> read(CompoundTag data, Topic topic) {
        ListTag list = data.getListOrEmpty(topic.dataKey());
        List<Line> lines = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag lineTag = list.getCompoundOrEmpty(i);
            String key = lineTag.getStringOr("k", "");
            if (key.isEmpty()) {
                continue;
            }
            ListTag argsTag = lineTag.getListOrEmpty("a");
            List<Arg> args = new ArrayList<>(argsTag.size());
            for (int a = 0; a < argsTag.size(); a++) {
                ListTag piecesTag = argsTag.getListOrEmpty(a);
                List<Piece> pieces = new ArrayList<>(piecesTag.size());
                for (int p = 0; p < piecesTag.size(); p++) {
                    CompoundTag pieceTag = piecesTag.getCompoundOrEmpty(p);
                    pieces.add(new Piece(pieceTag.getStringOr("s", ""), pieceTag.getBooleanOr("t", false)));
                }
                args.add(new Arg(pieces));
            }
            lines.add(new Line(key, args));
        }
        return lines;
    }
}
