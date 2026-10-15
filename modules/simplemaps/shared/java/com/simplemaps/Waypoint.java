package com.simplemaps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * One waypoint of a wayfinder map (owner F2: waypoints belong to the map stack). {@code slot} 0-7 is bookmark 1-8.
 * Owner N65: the icon is any item ({@code icon}); {@code face} says how it is shown: {@link CubeNet#MODEL} = the 3D block
 * model, 1..6 = one side of the block (default: the front). The colour only tints the baked pixel picture of the held map.
 */
public record Waypoint(int slot, int x, int z, String name, int color, Identifier icon, int face) {
    public static final int SLOTS = 8, MAX_NAME = 32;
    /** Default colours per slot (Vanilla dye text colours: white, orange, magenta, light blue, yellow, lime, pink, cyan). */
    public static final int[] DEFAULT_COLORS = {0xFFF9FFFE, 0xFFF9801D, 0xFFC74EBD, 0xFF3AB3DA, 0xFFFED83D, 0xFF80C71F, 0xFFF38BAA, 0xFF169C9C};

    /** The old field {@code head} (mob head item) is still read; without any icon the old colour becomes the matching wool. */
    public static final Codec<Waypoint> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, SLOTS - 1).fieldOf("slot").forGetter(Waypoint::slot),
            Codec.INT.fieldOf("x").forGetter(Waypoint::x),
            Codec.INT.fieldOf("z").forGetter(Waypoint::z),
            Codec.string(0, MAX_NAME).optionalFieldOf("name", "").forGetter(Waypoint::name),
            Codec.INT.optionalFieldOf("color", 0xFFFFFFFF).forGetter(Waypoint::color),
            Identifier.CODEC.optionalFieldOf("icon").forGetter(w -> Optional.of(w.icon())),
            Identifier.CODEC.optionalFieldOf("head").forGetter(w -> Optional.empty()),
            Codec.intRange(CubeNet.MODEL, CubeNet.DOWN).optionalFieldOf("face", CubeNet.FRONT).forGetter(Waypoint::face)
    ).apply(i, (slot, x, z, name, color, icon, legacyHead, face) ->
            new Waypoint(slot, x, z, name, color, icon.or(() -> legacyHead).orElse(null), face)));

    public static final StreamCodec<ByteBuf, Waypoint> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Waypoint::slot,
            ByteBufCodecs.INT, Waypoint::x,
            ByteBufCodecs.INT, Waypoint::z,
            ByteBufCodecs.stringUtf8(MAX_NAME), Waypoint::name,
            ByteBufCodecs.INT, Waypoint::color,
            Identifier.STREAM_CODEC, Waypoint::icon,
            ByteBufCodecs.VAR_INT, Waypoint::face,
            Waypoint::new);

    public Waypoint {
        slot = Math.max(0, Math.min(SLOTS - 1, slot));
        name = name == null ? "" : name.length() > MAX_NAME ? name.substring(0, MAX_NAME) : name;
        color = 0xFF000000 | color;
        if (icon == null) icon = WaypointIcons.forColor(color);
        if (!CubeNet.validFace(face)) face = CubeNet.FRONT;
    }

    /** A new waypoint with the given icon, shown from the front. */
    public static Waypoint fresh(int slot, int x, int z, Identifier icon) {
        return new Waypoint(slot, x, z, "", DEFAULT_COLORS[Math.max(0, Math.min(SLOTS - 1, slot))], icon, CubeNet.FRONT);
    }

    /** A new waypoint with the wool of its slot colour as icon (tests, migration). */
    public static Waypoint fresh(int slot, int x, int z) {
        return fresh(slot, x, z, null);
    }

    public Waypoint withSlot(int newSlot) {
        return new Waypoint(newSlot, x, z, name, color, icon, face);
    }

    public Waypoint movedTo(int newX, int newZ) {
        return new Waypoint(slot, newX, newZ, name, color, icon, face);
    }
}
