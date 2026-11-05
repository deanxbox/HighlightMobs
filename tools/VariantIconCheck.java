package dev.deanm.highlightmobs.client;

import java.util.*;
import java.util.zip.ZipFile;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

/** Offline catalogue, enum-texture and icon-label regression checks; no world or OpenGL needed. */
public final class VariantIconCheck {
    static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var field = MobVariants.class.getDeclaredField("KINDS");
        field.setAccessible(true);
        var kinds = (Map<?, ?>) field.get(null);
        require(kinds.size() == 15, "Version-specific variant axes: " + kinds.size());
        require(!MobVariants.has(EntityType.CREEPER), "Nonvariant mob must toggle directly");
        require(MobVariants.has(EntityType.CAT) && MobVariants.has(EntityType.FROG) && MobVariants.has(EntityType.WOLF), "Registry variants retained");
        require(!MobVariants.has(EntityType.PIG) && !MobVariants.has(EntityType.COW) && !MobVariants.has(EntityType.CHICKEN), "No future farm variants");
        var counts = Map.ofEntries(Map.entry(EntityType.FOX, 2), Map.entry(EntityType.RABBIT, 7),
            Map.entry(EntityType.PARROT, 5), Map.entry(EntityType.AXOLOTL, 5), Map.entry(EntityType.LLAMA, 4),
            Map.entry(EntityType.TRADER_LLAMA, 4), Map.entry(EntityType.HORSE, 7), Map.entry(EntityType.MOOSHROOM, 2),
            Map.entry(EntityType.SHEEP, 16), Map.entry(EntityType.SHULKER, 16));
        int textures = 0;
        var missing = new ArrayList<String>();
        try (ZipFile jar = new ZipFile(args[0])) {
            for (var entry : counts.entrySet()) {
                var ids = MobVariants.options(entry.getKey());
                require(ids.size() == entry.getValue() && new HashSet<>(ids).size() == ids.size(), "Complete unique enum: " + entry.getKey());
                for (String id : ids) {
                    var path = MobIcons.variantTexture(EntityType.getKey(entry.getKey()), id);
                    if (jar.getEntry("assets/" + path.getNamespace() + "/" + path.getPath()) == null) { missing.add(path.toString()); }
                    textures++;
                }
            }
        }
        require(missing.isEmpty(), "Missing enum variant textures: " + missing);
        Font font = new Font(id -> null, false) {
            @Override public int width(String value) { return value.length() * 6; }
        };
        var shortLabel = Component.literal("Cat");
        require(MobIcons.fit(font, shortLabel, 152) == shortLabel, "Short labels preserved");
        var longLabel = MobIcons.fit(font, Component.literal("[x] A very long mob variant name"), 152);
        require(longLabel.getString().endsWith("...") && font.width(longLabel.getString()) <= 104, "Long labels fit icon clearance");
        System.out.println("PASS: 15 variant axes, " + textures + " enum variant assets, nonvariant direct-toggle classification and short/long icon labels");
    }
}
