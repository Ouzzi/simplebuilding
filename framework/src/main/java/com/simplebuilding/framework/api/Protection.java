package com.simplebuilding.framework.api;

import java.util.*;

/** Loader-neutral, server-owned permission providers. Client requests are never authoritative. */
public final class Protection {
    /** actor is null for automation; providers must not assume a player is present. */
    public record Target(UUID actor, String dimension, int x, int y, int z, boolean administrator) {}
    /** Kind of player action. Providers without per-action rights treat every action like {@link #ANY}. */
    public enum Action {BUILD,DESTROY,USE,CONTAINER,REDSTONE,ENTITY,
        /** Unspecified or mixed: the strictest reading (build and destroy). Old callers without an action mean this. */
        ANY}
    @FunctionalInterface public interface Check {
        boolean allows(Target target);
        /** Per-action rights. Default: the action-less decision, so older providers stay strict. */
        default boolean allows(Target target,Action action) {return allows(target);}
        /** Automation has no player authority. Providers may permit matching land owners. */
        default boolean allowsAutomation(Target source,Target target) {return allows(source)&&allows(target);}
    }
    private static final Map<Object,Map<String,Check>> PROVIDERS=new IdentityHashMap<>();
    public static void register(Object server,String id,Check check) {PROVIDERS.computeIfAbsent(server,s->new LinkedHashMap<>()).put(Objects.requireNonNull(id),Objects.requireNonNull(check));}
    public static void unregister(Object server,String id) {var checks=PROVIDERS.get(server);if(checks!=null){checks.remove(id);if(checks.isEmpty())PROVIDERS.remove(server);}}
    public static boolean active(Object server) {return PROVIDERS.containsKey(server);}
    /** Old callers: no action given, so {@link Action#ANY} (strict). */
    public static boolean allows(Object server,Target target) {return allows(server,target,Action.ANY);}
    public static boolean allows(Object server,Target target,Action action) {Objects.requireNonNull(action);var checks=PROVIDERS.get(server);if(checks==null)return true;for(var check:checks.values())if(!check.allows(target,action))return false;return true;}
    public static boolean allowsAutomation(Object server,Target source,Target target) {var checks=PROVIDERS.get(server);if(checks==null)return true;for(var check:checks.values())if(!check.allowsAutomation(source,target))return false;return true;}
    private Protection() {}
}
