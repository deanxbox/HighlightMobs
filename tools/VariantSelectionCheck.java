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
import net.minecraft.world.entity.EntityTypes;

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
            Map.entry(EntityTypes.FOX, 2), Map.entry(EntityTypes.RABBIT, 7),
            Map.entry(EntityTypes.PARROT, 5), Map.entry(EntityTypes.AXOLOTL, 5),
            Map.entry(EntityTypes.LLAMA, 4), Map.entry(EntityTypes.TRADER_LLAMA, 4),
            Map.entry(EntityTypes.HORSE, 7), Map.entry(EntityTypes.MOOSHROOM, 2),
            Map.entry(EntityTypes.SHEEP, 16), Map.entry(EntityTypes.SHULKER, 16));
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
        Object wolf = kinds.get(EntityTypes.WOLF);
        var id = (Function<Object, String>) wolf.getClass().getDeclaredMethod("id").invoke(wolf);
        check("minecraft:pale".equals(id.apply(Holder.Reference.createStandAlone(null,
            ResourceKey.create(Registries.WOLF_VARIANT, Identifier.withDefaultNamespace("pale"))))), "Registry holder id extraction");
        check(MobVariants.label("minecraft:british_shorthair").equals("British Shorthair"), "Variant label formatting");
        check(!MobVariants.has(EntityTypes.CREEPER) && MobVariants.options(EntityTypes.CREEPER).isEmpty(), "Nonvariant mob regression");
        check(HighlightMobsScreen.isMob(EntityTypes.VILLAGER) && HighlightMobsScreen.isMob(EntityTypes.IRON_GOLEM), "MISC living mobs classified");
        check(!HighlightMobsScreen.isMob(EntityTypes.ARMOR_STAND) && !HighlightMobsScreen.isMob(EntityTypes.ARROW), "Nonmob classification");
        System.out.println("PASS: 18 variant axes; " + enumVariants + " enum variants, texture availability, id round trips, registry holder IDs, labels and mob classification");
    }
}
