package dev.deanm.highlightmobs.client;

import java.util.*;
import java.util.function.*;
import java.util.zip.ZipFile;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.EitherHolder;

/** Offline component-id and enum regression checks; no Minecraft instance/world needed. */
public final class VariantSelectionCheck {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var field = MobVariants.class.getDeclaredField("KINDS");
        field.setAccessible(true);
        Map<EntityType<?>, Object> kinds = (Map<EntityType<?>, Object>) field.get(null);
        check(kinds.size() == 18, "All 18 supported mob/variant axes must be retained");
        Map<EntityType<?>, Integer> counts = Map.ofEntries(
            Map.entry(EntityType.FOX, 2), Map.entry(EntityType.RABBIT, 7),
            Map.entry(EntityType.PARROT, 5), Map.entry(EntityType.AXOLOTL, 5),
            Map.entry(EntityType.LLAMA, 4), Map.entry(EntityType.TRADER_LLAMA, 4),
            Map.entry(EntityType.HORSE, 7), Map.entry(EntityType.MOOSHROOM, 2),
            Map.entry(EntityType.SHEEP, 16), Map.entry(EntityType.SHULKER, 16));
        int enumVariants = 0;
        try (ZipFile jar = new ZipFile(args[0])) {
            for (var entry : counts.entrySet()) {
                var type = entry.getKey();
                var ids = MobVariants.options(type);
                check(ids.size() == entry.getValue(), "Missing enum variants for " + EntityType.getKey(type) + ": " + ids);
                Object kind = kinds.get(type);
                var id = (Function<Object, String>) kind.getClass().getDeclaredMethod("id").invoke(kind);
                var parse = (BiFunction<Object, String, Object>) kind.getClass().getDeclaredMethod("parse").invoke(kind);
                check(parse.apply(null, "not_a_variant") == null, "Unknown enum ids must be ignored");
                for (String name : ids) {
                    check(name.equals(id.apply(parse.apply(null, name))), "Enum component round trip: " + name);
                    var texture = MobIcons.variantTexture(EntityType.getKey(type), name);
                    check(jar.getEntry("assets/" + texture.getNamespace() + "/" + texture.getPath()) != null,
                        "Missing variant texture: " + EntityType.getKey(type) + " " + name + " -> " + texture);
                    enumVariants++;
                }
            }
        }
        Object chicken = kinds.get(EntityType.CHICKEN);
        var component = chicken.getClass().getDeclaredMethod("component").invoke(chicken);
        check(component == DataComponents.CHICKEN_VARIANT, "Chicken component must retain EitherHolder support");
        var id = (Function<Object, String>) chicken.getClass().getDeclaredMethod("id").invoke(chicken);
        var parse = (BiFunction<Object, String, Object>) chicken.getClass().getDeclaredMethod("parse").invoke(chicken);
        check("minecraft:warm".equals(id.apply(parse.apply(null, "minecraft:warm"))), "Chicken EitherHolder id round trip");
        Object wolf = kinds.get(EntityType.WOLF);
        id = (Function<Object, String>) wolf.getClass().getDeclaredMethod("id").invoke(wolf);
        check("minecraft:pale".equals(id.apply(Holder.Reference.createStandAlone(null,
            ResourceKey.create(Registries.WOLF_VARIANT, Identifier.withDefaultNamespace("pale"))))), "Registry holder id extraction");
        check(MobVariants.label("minecraft:british_shorthair").equals("British Shorthair"), "Variant label formatting");
        check(!MobVariants.has(EntityType.CREEPER) && MobVariants.options(EntityType.CREEPER).isEmpty(), "Nonvariant mob regression");
        check(HighlightMobsScreen.isMob(EntityType.VILLAGER) && HighlightMobsScreen.isMob(EntityType.IRON_GOLEM), "MISC living mobs classified");
        check(!HighlightMobsScreen.isMob(EntityType.ARMOR_STAND) && !HighlightMobsScreen.isMob(EntityType.ARROW), "Nonmob classification");
        System.out.println("PASS: 18 variant axes; " + enumVariants + " enum variants, texture availability, id round trips, chicken EitherHolder, labels and mob classification");
    }
}
