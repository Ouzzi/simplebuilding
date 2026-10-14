package com.simplebuilding.modules.simpletweaks.claims;

import com.simplebuilding.framework.api.Protection.Action;
import java.util.*;

/** Right bits of a claim, the role presets and the rule sets built from them. No Minecraft classes. */
public final class ClaimRights {
    public static final int BUILD = 1, DESTROY = 2, USE = 4, CONTAINER = 8, REDSTONE = 16, ANIMALS = 32, MANAGE = 64;
    public static final int ALL = 127;
    /** What "trusted" meant before rights existed: everything except managing the rules. */
    public static final int LEGACY = BUILD | DESTROY | USE | CONTAINER | REDSTONE | ANIMALS;
    /** Old callers and unclassified actions: building and destroying are both required. */
    public static final int STRICT = BUILD | DESTROY;
    /** Marker for {@code need}: at least one right of any kind. Used for attacks on players. */
    public static final int PRESENCE = 0;
    public static final int MAX_MEMBERS = 64;
    /** Letter order is the bit order. */
    public static final String LETTERS = "BDUCRAM";

    /** Rights of one scope: everybody, plus per person. Immutable; every value is a non-empty subset of {@link #ALL}. */
    public record Rules(int everyone, Map<UUID,Integer> members) {
        public static final Rules EMPTY = new Rules(0, Map.of());
        public Rules {
            if ((everyone & ~ALL) != 0 || (everyone & MANAGE) != 0) throw new IllegalArgumentException("Everyone rights");
            if (members.size() > MAX_MEMBERS) throw new IllegalArgumentException("Trusted player cap");
            for (var e : members.entrySet())
                if (e.getKey() == null || e.getValue() == null || e.getValue() <= 0 || (e.getValue() & ~ALL) != 0) throw new IllegalArgumentException("Member rights");
            members = Map.copyOf(members);
        }
        /** Everybody-rights and personal rights add up. Owners are handled by the caller. */
        public int of(UUID player) { return everyone | members.getOrDefault(player, 0); }
        public boolean isEmpty() { return everyone == 0 && members.isEmpty(); }
        public Rules with(UUID player, int rights) {
            var next = new HashMap<>(members);
            if (rights == 0) next.remove(player); else next.put(player, rights);
            return new Rules(everyone, next);
        }
    }

    public static int bit(char letter) {
        int i = LETTERS.indexOf(Character.toUpperCase(letter));
        return i < 0 || letter > 127 ? -1 : 1 << i;
    }
    /** Role preset or -1. Case insensitive, without the leading at sign. */
    public static int role(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "guest" -> USE;
            case "helper" -> USE | CONTAINER | ANIMALS;
            case "member" -> LEGACY;
            case "manager" -> ALL;
            default -> -1;
        };
    }
    /** Right letters in canonical order, e.g. "BDU". */
    public static String letters(int rights) {
        var out = new StringBuilder();
        for (int i = 0; i < LETTERS.length(); i++) if ((rights & (1 << i)) != 0) out.append(LETTERS.charAt(i));
        return out.toString();
    }
    /** Role name if the rights are exactly a preset, otherwise the letters. */
    public static String describe(int rights) {
        for (var role : List.of("guest", "helper", "member", "manager")) if (role(role) == rights) return "@" + role;
        return letters(rights);
    }
    /** Rights a hook needs for a framework action. */
    public static int need(Action action) {
        return switch (action) {
            case BUILD -> BUILD;
            case DESTROY -> DESTROY;
            case USE -> USE;
            case CONTAINER -> CONTAINER;
            case REDSTONE -> REDSTONE;
            case ENTITY -> ANIMALS;
            case ANY -> STRICT;
        };
    }
    private ClaimRights() {}
}
