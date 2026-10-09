package com.simplemaps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * One waypoint of a wayfinder map (owner F2: waypoints belong to the map stack). {@code slot} 0-7 is bookmark 1-8;
 * the locator bar shows it in {@code color} or, when set, with the mob head item {@code head} as icon.
 */
public record Waypoint(int slot, int x, int z, String name, int color, Optional<Identifier> head) {
    public static final int SLOTS = 8, MAX_NAME = 32;
    /** Default colours per slot (Vanilla dye text colours: white, orange, magenta, light blue, yellow, lime, pink, cyan). */
    public static final int[] DEFAULT_COLORS = {0xFFF9FFFE, 0xFFF9801D, 0xFFC74EBD, 0xFF3AB3DA, 0xFFFED83D, 0xFF80C71F, 0xFFF38BAA, 0xFF169C9C};

    public static final Codec<Waypoint> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, SLOTS - 1).fieldOf("slot").forGetter(Waypoint::slot),
            Codec.INT.fieldOf("x").forGetter(Waypoint::x),
            Codec.INT.fieldOf("z").forGetter(Waypoint::z),
            Codec.string(0, MAX_NAME).optionalFieldOf("name", "").forGetter(Waypoint::name),
            Codec.INT.optionalFieldOf("color", 0xFFFFFFFF).forGetter(Waypoint::color),
            Identifier.CODEC.optionalFieldOf("head").forGetter(Waypoint::head)
    ).apply(i, Waypoint::new));

    public static final StreamCodec<ByteBuf, Waypoint> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Waypoint::slot,
            ByteBufCodecs.INT, Waypoint::x,
            ByteBufCodecs.INT, Waypoint::z,
            ByteBufCodecs.stringUtf8(MAX_NAME), Waypoint::name,
            ByteBufCodecs.INT, Waypoint::color,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), Waypoint::head,
            Waypoint::new);

    public Waypoint {
        slot = Math.max(0, Math.min(SLOTS - 1, slot));
        name = name == null ? "" : name.length() > MAX_NAME ? name.substring(0, MAX_NAME) : name;
        color = 0xFF000000 | color;
        head = head == null ? Optional.empty() : head;
    }

    public static Waypoint fresh(int slot, int x, int z) {
        return new Waypoint(slot, x, z, "", DEFAULT_COLORS[Math.max(0, Math.min(SLOTS - 1, slot))], Optional.empty());
    }

    public Waypoint withSlot(int newSlot) {
        return new Waypoint(newSlot, x, z, name, color, head);
    }
}
