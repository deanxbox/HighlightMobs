package dev.deanm.highlightmobs.client;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.world.entity.EntityTypes;
public final class ConfigSelectionCheck {
  static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
  public static void main(String[] args) throws Exception {
    Path dir = Files.createTempDirectory("hm-config-check-");
    var field = FabricLoaderImpl.class.getDeclaredField("configDir");
    field.setAccessible(true);
    field.set(FabricLoaderImpl.INSTANCE, dir);
    net.minecraft.SharedConstants.tryDetectVersion();
    net.minecraft.server.Bootstrap.bootStrap();
    var c = HighlightMobsConfig.load();
    var wolf = EntityTypes.WOLF;
    var ids = List.of("minecraft:pale", "minecraft:woods", "minecraft:ashen");
    check(c.highlightedEntityTypes.isEmpty(), "fresh selection");
    c.toggleVariant(wolf, ids.get(0), ids);
    check(c.isPartial(wolf) && c.isVariantSelected(wolf, ids.get(0)) && !c.isVariantSelected(wolf, ids.get(1)), "partial selection");
    c = HighlightMobsConfig.load();
    check(c.isPartial(wolf) && c.isVariantSelected(wolf, ids.get(0)), "partial selection persisted");
    c.toggleVariant(wolf, ids.get(1), ids);
    c.toggleVariant(wolf, ids.get(2), ids);
    check(c.isSelected(wolf) && !c.isPartial(wolf), "all selected collapses");
    c.toggleVariant(wolf, ids.get(1), ids);
    check(c.isPartial(wolf) && !c.isVariantSelected(wolf, ids.get(1)) && c.isVariantSelected(wolf, ids.get(2)), "remove from all");
    c.toggleVariant(wolf, ids.get(0), ids);
    c.toggleVariant(wolf, ids.get(2), ids);
    check(!c.isSelected(wolf), "last removal deselects mob");
    c.selectAllVariants(wolf);
    c.deselectType(wolf);
    check(!c.isSelected(wolf) && c.highlightedVariants.isEmpty(), "deselect all clears variants");
    c.selectAllVariants(wolf);
    c.select(List.of(EntityTypes.CAT));
    check(!c.isSelected(wolf) && c.isSelected(EntityTypes.CAT) && c.highlightedVariants.isEmpty(), "quick select replaces selection");
    c.clear();
    check(HighlightMobsConfig.load().highlightedEntityTypes.isEmpty(), "clear persisted");
    Files.writeString(dir.resolve("highlightmobs.json"), "{\"enabled\":false,\"highlightedEntityTypes\":[\"minecraft:wolf\"]}");
    c=HighlightMobsConfig.load();
    check(!c.enabled && c.isSelected(wolf) && !c.isPartial(wolf), "legacy config migration");
    Files.writeString(dir.resolve("highlightmobs.json"), "{\"highlightedEntityTypes\":null,\"highlightedVariants\":null}");
    c=HighlightMobsConfig.load();
    check(c.highlightedEntityTypes.isEmpty() && c.highlightedVariants.isEmpty(), "null config sanitized");
    System.out.println("PASS: partial/all/none selections, persistence, quick select, legacy and null configs; disposable config="+dir);
  }
}
