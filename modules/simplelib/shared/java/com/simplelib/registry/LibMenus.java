package com.simplelib.registry;

import com.simplelib.SimpleLib;
import com.simplelib.crucible.BarrelMenu;
import com.simplelib.crucible.BarrelTier;
import com.simplelib.crucible.CrucibleMenu;
import com.simplelib.crucible.CrucibleTier;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** One menu type per tier, so the client knows the slot count (and a barrel its stack limit) without extra open data. */
public final class LibMenus {
    public static final Map<CrucibleTier, MenuType<CrucibleMenu>> CRUCIBLES = new EnumMap<>(CrucibleTier.class);
    public static final Map<BarrelTier, MenuType<BarrelMenu>> BARRELS = new EnumMap<>(BarrelTier.class);

    public static void register() {
        for (CrucibleTier tier : CrucibleTier.values()) {
            CRUCIBLES.put(tier, Registry.register(BuiltInRegistries.MENU, SimpleLib.id(tier.id() + "_crucible"),
                    new MenuType<>((id, inventory) -> CrucibleMenu.client(tier, id, inventory), FeatureFlags.VANILLA_SET)));
        }
        for (BarrelTier tier : BarrelTier.values()) {
            BARRELS.put(tier, Registry.register(BuiltInRegistries.MENU, SimpleLib.id(tier.id() + "_barrel"),
                    new MenuType<>((id, inventory) -> BarrelMenu.client(tier, id, inventory), FeatureFlags.VANILLA_SET)));
        }
    }

    public static MenuType<BarrelMenu> forBarrel(BarrelTier tier) {
        return BARRELS.get(tier);
    }

    public static MenuType<CrucibleMenu> forTier(CrucibleTier tier) {
        return CRUCIBLES.get(tier);
    }

    private LibMenus() {}
}
