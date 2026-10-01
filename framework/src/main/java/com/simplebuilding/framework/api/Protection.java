package com.simplebuilding.framework.api;

import java.util.*;

/** Loader-neutral, server-owned permission providers. Client requests are never authoritative. */
public final class Protection {
    /** actor is null for automation; providers must not assume a player is present. */
    public record Target(UUID actor, String dimension, int x, int y, int z, boolean administrator) {}
    @FunctionalInterface public interface Check {
        boolean allows(Target target);
        /** Automation has no player authority. Providers may permit matching land owners. */
        default boolean allowsAutomation(Target source,Target target) {return allows(source)&&allows(target);}
    }
    private static final Map<Object,Map<String,Check>> PROVIDERS=new IdentityHashMap<>();
    public static void register(Object server,String id,Check check) {PROVIDERS.computeIfAbsent(server,s->new LinkedHashMap<>()).put(Objects.requireNonNull(id),Objects.requireNonNull(check));}
    public static void unregister(Object server,String id) {var checks=PROVIDERS.get(server);if(checks!=null){checks.remove(id);if(checks.isEmpty())PROVIDERS.remove(server);}}
    public static boolean active(Object server) {return PROVIDERS.containsKey(server);}
    public static boolean allows(Object server,Target target) {var checks=PROVIDERS.get(server);if(checks==null)return true;for(var check:checks.values())if(!check.allows(target))return false;return true;}
    public static boolean allowsAutomation(Object server,Target source,Target target) {var checks=PROVIDERS.get(server);if(checks==null)return true;for(var check:checks.values())if(!check.allowsAutomation(source,target))return false;return true;}
    private Protection() {}
}
