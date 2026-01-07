package me.lninyu.foo;

import org.bukkit.attribute.AttributeInstance;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class LegacyUtil {
    public static void removeLegacyAttribute(@NotNull AttributeInstance attributeInstance, UUID modifierId) {
        var modifier = attributeInstance.getModifier(modifierId);

        if (modifier != null) {
            attributeInstance.removeModifier(modifier);
        }
    }
}
