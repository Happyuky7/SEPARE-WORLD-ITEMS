package com.github.happyuky7.swi.mc.compat;

import net.minecraft.world.effect.MobEffectInstance;
//#if MC >= 1.21.11
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
//#elseif MC >= 1.19.3
//$$ import net.minecraft.core.registries.BuiltInRegistries;
//$$ import net.minecraft.resources.ResourceLocation;
//#else
//$$ import net.minecraft.core.Registry;
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
//#if MC < 1.20.5
//$$ import net.minecraft.world.effect.MobEffect;
//#endif

/**
 * Potion effect ids ({@code minecraft:speed}) <-> effect instances.
 */
public final class Effects {

    private Effects() {
    }

    public static String id(MobEffectInstance effect) {
        //#if MC >= 1.21.11
        return effect.getEffect().unwrapKey().map(k -> k.identifier().toString()).orElse(null);
        //#elseif MC >= 1.20.5
        //$$ return effect.getEffect().unwrapKey().map(k -> k.location().toString()).orElse(null);
        //#elseif MC >= 1.19.3
        //$$ ResourceLocation key = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect());
        //$$ return key != null ? key.toString() : null;
        //#else
        //$$ ResourceLocation key = Registry.MOB_EFFECT.getKey(effect.getEffect());
        //$$ return key != null ? key.toString() : null;
        //#endif
    }

    /**
     * @return the effect instance, or null when the id is unknown on this server
     */
    public static MobEffectInstance create(String id, int duration, int amplifier, boolean ambient, boolean visible, boolean icon) {
        if (id == null) {
            return null;
        }
        //#if MC >= 1.21.11
        Identifier key = Identifier.tryParse(id);
        return key == null ? null : BuiltInRegistries.MOB_EFFECT.get(key)
                .map(holder -> new MobEffectInstance(holder, duration, amplifier, ambient, visible, icon))
                .orElse(null);
        //#elseif MC >= 1.20.5
        //$$ ResourceLocation key = ResourceLocation.tryParse(id);
        //$$ return key == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(key)
        //$$         .map(holder -> new MobEffectInstance(holder, duration, amplifier, ambient, visible, icon))
        //$$         .orElse(null);
        //#else
        //$$ ResourceLocation key = ResourceLocation.tryParse(id);
        //#if MC >= 1.19.3
        //$$ MobEffect effect = key == null ? null : BuiltInRegistries.MOB_EFFECT.get(key);
        //#else
        //$$ MobEffect effect = key == null ? null : Registry.MOB_EFFECT.get(key);
        //#endif
        //$$ return effect == null ? null : new MobEffectInstance(effect, duration, amplifier, ambient, visible, icon);
        //#endif
    }
}
