package com.simplesandwiches.registry;

import com.simplesandwiches.Sandwiches;
import com.simplesandwiches.sandwich.SandwichContents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModComponents {
    public static final DataComponentType<SandwichContents> SANDWICH_CONTENTS = DataComponentType.<SandwichContents>builder()
            .persistent(SandwichContents.CODEC).networkSynchronized(SandwichContents.STREAM_CODEC).cacheEncoding().build();

    public static void register() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Sandwiches.id("sandwich_contents"), SANDWICH_CONTENTS);
    }

    private ModComponents() {}
}
