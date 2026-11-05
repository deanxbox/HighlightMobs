package dev.deanm.highlightmobs.client;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

/** Variant axes exposed by this Minecraft version's entity getters/setters. */
final class MobVariants {
	private record Kind<T>(Function<Entity, T> get, BiConsumer<Entity, T> set,
			Function<Minecraft, List<String>> ids, Function<T, @Nullable String> id,
			BiFunction<Minecraft, String, @Nullable T> parse) {
		@Nullable String idOf(Entity entity) {
			T value = this.get.apply(entity);
			return value == null ? null : this.id.apply(value);
		}
		void apply(Entity entity, String variant) {
			T value = this.parse.apply(Minecraft.getInstance(), variant);
			if (value != null) {
				this.set.accept(entity, value);
			}
		}
	}

	private static <X> Kind<Holder<X>> registry(ResourceKey<? extends Registry<X>> key,
			Function<Entity, Holder<X>> get, BiConsumer<Entity, Holder<X>> set) {
		return new Kind<>(get, set,
			minecraft -> minecraft.level == null ? List.of() : minecraft.level.registryAccess().<X>lookup(key)
				.map(registry -> registry.listElements().map(holder -> holder.key().location().toString()).sorted().toList()).orElse(List.of()),
			holder -> holder.unwrapKey().map(k -> k.location().toString()).orElse(null),
			(minecraft, id) -> minecraft.level == null ? null : minecraft.level.registryAccess().<X>lookup(key)
				.flatMap(registry -> registry.get(ResourceKey.create(key, ResourceLocation.parse(id))))
				.<Holder<X>>map(holder -> holder).orElse(null));
	}

	private static Kind<VillagerType> villager(Function<Entity, VillagerType> get, BiConsumer<Entity, VillagerType> set) {
		return new Kind<>(get, set,
			minecraft -> BuiltInRegistries.VILLAGER_TYPE.keySet().stream().map(Object::toString).sorted().toList(),
			value -> BuiltInRegistries.VILLAGER_TYPE.getKey(value).toString(),
			(minecraft, id) -> BuiltInRegistries.VILLAGER_TYPE.getOptional(ResourceLocation.parse(id)).orElse(null));
	}

	private static <E extends Enum<E>> Kind<E> enumKind(Class<E> type, Function<Entity, E> get, BiConsumer<Entity, E> set) {
		return new Kind<>(get, set, minecraft -> Arrays.stream(type.getEnumConstants()).map(MobVariants::enumId).toList(), MobVariants::enumId,
			(minecraft, id) -> Arrays.stream(type.getEnumConstants()).filter(value -> enumId(value).equals(id)).findFirst().orElse(null));
	}

	private static String enumId(Enum<?> value) {
		return value instanceof StringRepresentable named ? named.getSerializedName() : value.name().toLowerCase(Locale.ROOT);
	}

	private static final Map<EntityType<?>, Kind<?>> KINDS = Map.ofEntries(
		Map.entry(EntityType.WOLF, registry(Registries.WOLF_VARIANT, e -> ((Wolf) e).getVariant(), (e, value) -> ((Wolf) e).setVariant(value))),
		Map.entry(EntityType.CAT, registry(Registries.CAT_VARIANT, e -> ((Cat) e).getVariant(), (e, value) -> ((Cat) e).setVariant(value))),
		Map.entry(EntityType.FROG, registry(Registries.FROG_VARIANT, e -> ((Frog) e).getVariant(), (e, value) -> ((Frog) e).setVariant(value))),
		Map.entry(EntityType.VILLAGER, villager(e -> ((Villager) e).getVillagerData().getType(), (e, value) -> ((Villager) e).setVillagerData(((Villager) e).getVillagerData().setType(value)))),
		Map.entry(EntityType.ZOMBIE_VILLAGER, villager(e -> ((ZombieVillager) e).getVillagerData().getType(), (e, value) -> ((ZombieVillager) e).setVillagerData(((ZombieVillager) e).getVillagerData().setType(value)))),
		Map.entry(EntityType.FOX, enumKind(Fox.Variant.class, e -> ((Fox) e).getVariant(), (e, value) -> ((Fox) e).setVariant(value))),
		Map.entry(EntityType.RABBIT, enumKind(Rabbit.Variant.class, e -> ((Rabbit) e).getVariant(), (e, value) -> ((Rabbit) e).setVariant(value))),
		Map.entry(EntityType.PARROT, enumKind(Parrot.Variant.class, e -> ((Parrot) e).getVariant(), (e, value) -> ((Parrot) e).setVariant(value))),
		Map.entry(EntityType.AXOLOTL, enumKind(net.minecraft.world.entity.animal.axolotl.Axolotl.Variant.class, e -> ((net.minecraft.world.entity.animal.axolotl.Axolotl) e).getVariant(), (e, value) -> ((net.minecraft.world.entity.animal.axolotl.Axolotl) e).setVariant(value))),
		Map.entry(EntityType.LLAMA, enumKind(Llama.Variant.class, e -> ((Llama) e).getVariant(), (e, value) -> ((Llama) e).setVariant(value))),
		Map.entry(EntityType.TRADER_LLAMA, enumKind(Llama.Variant.class, e -> ((Llama) e).getVariant(), (e, value) -> ((Llama) e).setVariant(value))),
		Map.entry(EntityType.HORSE, enumKind(net.minecraft.world.entity.animal.horse.Variant.class, e -> ((Horse) e).getVariant(), (e, value) -> ((Horse) e).setVariant(value))),
		Map.entry(EntityType.MOOSHROOM, enumKind(MushroomCow.Variant.class, e -> ((MushroomCow) e).getVariant(), (e, value) -> ((MushroomCow) e).setVariant(value))),
		Map.entry(EntityType.SHEEP, enumKind(DyeColor.class, e -> ((Sheep) e).getColor(), (e, value) -> ((Sheep) e).setColor(value))),
		Map.entry(EntityType.SHULKER, enumKind(DyeColor.class, e -> ((net.minecraft.world.entity.monster.Shulker) e).getColor(), (e, value) -> ((net.minecraft.world.entity.monster.Shulker) e).setVariant(java.util.Optional.of(value))))
	);

	private MobVariants() {
	}

	static boolean has(EntityType<?> type) {
		return KINDS.containsKey(type);
	}

	/** Every variant id of the type; empty when it has none or the registry needs a world. */
	static List<String> options(EntityType<?> type) {
		Kind<?> kind = KINDS.get(type);
		return kind == null ? List.of() : kind.ids().apply(Minecraft.getInstance());
	}

	static @Nullable String variantOf(Entity entity) {
		Kind<?> kind = KINDS.get(entity.getType());
		return kind == null ? null : kind.idOf(entity);
	}

	static String label(String id) {
		return labelOf(id);
	}

	/** Puts the entity into the given variant so its icon shows that variant's look. */
	static void apply(Entity entity, String variant) {
		Kind<?> kind = KINDS.get(entity.getType());
		if (kind != null) {
			kind.apply(entity, variant);
		}
	}

	private static String labelOf(String id) {
		String path = id.substring(id.indexOf(':') + 1).replace('_', ' ');
		StringBuilder out = new StringBuilder();
		for (String word : path.split(" ")) {
			if (!word.isEmpty()) {
				out.append(out.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
			}
		}
		return out.toString();
	}
}
