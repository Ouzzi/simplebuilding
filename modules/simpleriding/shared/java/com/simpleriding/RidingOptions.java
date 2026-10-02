package com.simpleriding;

import java.util.List;

/** Config leaf catalogue shared by the screen and completeness tests. */
public final class RidingOptions {
    public enum Kind { BOOLEAN, INTEGER, FLOAT }
    public record Option(String path, Kind kind, Object defaultValue, double minimum, double maximum) {
        public String nameKey() { return "text.autoconfig.simpleriding.option." + path; }
        public String tabKey() { return "text.autoconfig.simpleriding.option." + path.split("\\.")[0]; }
        private Object owner(RidingConfig config) throws ReflectiveOperationException {
            Object o = config;
            String[] parts = path.split("\\.");
            for (int i=0; i<parts.length-1; i++) o=o.getClass().getField(parts[i]).get(o);
            return o;
        }
        public Object get(RidingConfig config) {
            try { var o=owner(config); return o.getClass().getField(path.substring(path.lastIndexOf('.')+1)).get(o); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException(path,e); }
        }
        public void set(RidingConfig config,Object value) {
            try { var o=owner(config); o.getClass().getField(path.substring(path.lastIndexOf('.')+1)).set(o,value); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException(path,e); }
        }
    }
    public static final List<Option> ALL=List.of(
        new Option("worldGen.enableVillagerTrades", Kind.BOOLEAN, true, 0, 0),
        new Option("worldGen.enableLootTableChanges", Kind.BOOLEAN, true, 0, 0),
        new Option("enchantments.swiftRide.ghastSpeedMultiplier", Kind.FLOAT, 0.85f, 0, 1),
        new Option("enchantments.swiftRide.horseSpeedMultiplier", Kind.FLOAT, 0.3f, 0, 1),
        new Option("enchantments.swiftRide.otherSpeedMultiplier", Kind.FLOAT, 0.2f, 0, 1),
        new Option("enchantments.horseJump.jumpStrengthMultiplier", Kind.FLOAT, 0.2f, 0, 0.5),
        new Option("safety.enableTailwind", Kind.BOOLEAN, true, 0, 0),
        new Option("safety.enableLeaping", Kind.BOOLEAN, true, 0, 0),
        new Option("safety.enableArmorUtilities", Kind.BOOLEAN, true, 0, 0),
        new Option("safety.enableNautilus", Kind.BOOLEAN, true, 0, 0),
        new Option("safety.maximumSpeedBonus", Kind.FLOAT, 3.0f, 0, 3),
        new Option("safety.maximumJumpBonus", Kind.FLOAT, 1.5f, 0, 1.5),
        new Option("safety.movementDistancePerTick", Kind.FLOAT, 4.0f, 0.5, 4),
        new Option("safety.movementPacketsPerTick", Kind.INTEGER, 20, 1, 20),
        new Option("enchantments.swiftRide.nautilusSpeedMultiplier", Kind.FLOAT, 0.2f, 0, 1),
        new Option("enchantments.horseJump.nautilusDashMultiplier", Kind.FLOAT, 0.2f, 0, 0.5),
        new Option("enchantments.horseJump.featherFallingReduction", Kind.FLOAT, 0.12f, 0, 0.12),
        new Option("enchantments.horseJump.armorEnchantability", Kind.INTEGER, 15, 0, 15),
        new Option("horseshoes.enableHorseshoes", Kind.BOOLEAN, true, 0, 0),
        new Option("horseshoes.terrainBonus", Kind.FLOAT, 0.5f, 0, 1),
        new Option("horseshoes.handlingBonus", Kind.FLOAT, 0.3f, 0, 0.5),
        new Option("horseshoes.fullSetSpeedBonus", Kind.FLOAT, 0.05f, 0, 0.1),
        new Option("horseshoes.fullSetJumpBonus", Kind.FLOAT, 0.05f, 0, 0.1),
        new Option("horseshoes.fullSetFallDamageIncrease", Kind.FLOAT, 0.1f, 0, 0.25),
        new Option("horseshoes.blocksPerDurability", Kind.INTEGER, 40, 8, 400)
    );
}
