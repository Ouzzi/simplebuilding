package com.simplelib.api.portal;

import com.simplelib.SimpleLib;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * How a portal link behaves (BACK22 core §5.3, owner 10.11.): its start state, whether it lets a traveller through, what a passage
 * does to it and what a repair part (or a maintenance box) does. Three rules are built in; mods may register more.
 *
 * @param id       registry id
 * @param initial  state of a fresh link
 * @param after    state after a passage
 * @param mendable whether a part/box turns DORMANT or BROKEN into OPEN
 */
public record PortalRule(Identifier id, PortalState initial, PortalState after, boolean mendable) {
    private static final Map<Identifier, PortalRule> RULES = new LinkedHashMap<>();
    /** Always open both ways (Lobby, cellar door, built Overworld pairs). */
    public static final PortalRule TWO_WAY = register(new PortalRule(SimpleLib.id("two_way"), PortalState.OPEN, PortalState.OPEN, false));
    /** Dormant until activated once with a part, then open for good. */
    public static final PortalRule ACTIVATE_ONCE = register(new PortalRule(SimpleLib.id("activate_once"), PortalState.DORMANT, PortalState.OPEN, true));
    /** Open; breaks after every passage, a part repairs it. */
    public static final PortalRule BREAK_ON_TRANSIT = register(new PortalRule(SimpleLib.id("break_on_transit"), PortalState.OPEN, PortalState.BROKEN, true));

    public static synchronized PortalRule register(PortalRule rule) {
        RULES.put(rule.id(), rule);
        return rule;
    }

    public static synchronized @Nullable PortalRule get(Identifier id) {
        return RULES.get(id);
    }

    public boolean canTransit(PortalState state) {
        return state == PortalState.OPEN;
    }

    /** The state after a passage through a link in {@code state}. */
    public PortalState afterTransit(PortalState state) {
        return canTransit(state) ? after : state;
    }

    /** The state after a repair/activation, or null when the link does not need one (or the rule allows none). */
    public @Nullable PortalState mend(PortalState state) {
        return mendable && state != PortalState.OPEN ? PortalState.OPEN : null;
    }
}
