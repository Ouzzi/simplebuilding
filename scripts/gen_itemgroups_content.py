import pathlib
import re
import subprocess

root = pathlib.Path(__file__).resolve().parents[1]
text = subprocess.check_output(
    ["git", "show", "HEAD:src/main/java/com/simplebuilding/items/ModItemGroups.java"],
    cwd=root,
    text=True,
)
start = text.index("// -- Resources ---")
end = text.index("}).build());")
body = text[start:end]
body = body.replace("entries.add", "entries.accept")
body = re.sub(
    r"RegistryWrapper\.WrapperLookup lookup = displayContext\.lookup\(\);\s*"
    r"RegistryWrapper<Enchantment> enchantmentRegistry = lookup\.getOrThrow\(RegistryKeys\.ENCHANTMENT\);\s*",
    "",
    body,
)
header = """package com.simplebuilding.items;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class ModItemGroupsContent {
    private ModItemGroupsContent() {}

    public static void populate(CreativeModeTab.Output entries, HolderLookup.Provider lookup) {
        HolderLookup<Enchantment> enchantmentRegistry = lookup.lookupOrThrow(Registries.ENCHANTMENT);
"""
footer = """
    }

    private static void addEnchantAtMax(CreativeModeTab.Output entries, HolderLookup<Enchantment> registry, ResourceKey<Enchantment> key) {
        registry.get(key).ifPresent(enchantmentEntry -> {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            builder.upgrade(enchantmentEntry, enchantmentEntry.value().getMaxLevel());
            book.set(DataComponents.STORED_ENCHANTMENTS, builder.toImmutable());
            entries.accept(book);
        });
    }
}
"""
out = root / "src/main/java/com/simplebuilding/items/ModItemGroupsContent.java"
out.write_text(header + body + footer, encoding="utf-8")
print(f"Wrote {out}")
