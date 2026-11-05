package dev.deanm.highlightmobs.client;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.BiFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;

/** Variant axes the game exposes as entity data components, keyed by entity type. */
final class MobVariants {
	private record Kind<T>(DataComponentType<T> component, Function<Minecraft, List<String>> ids, Function<T, String> id,
			BiFunction<Minecraft, String, T> parse) {
		String idOf(Entity entity) {
			T value = entity.get(this.component);
			return value == null ? null : this.id.apply(value);
		}

		void apply(Entity entity, String variant) {
			T value = this.parse.apply(Minecraft.getInstance(), variant);
			if (value != null) {
				entity.setComponent(this.component, value);
			}
		}
	}

	private static <X> Kind<Holder<X>> registry(DataComponentType<Holder<X>> component, ResourceKey<? extends Registry<X>> key) {
		return new Kind<>(
			component,
			// datapack registries only exist once a world is joined
			minecraft -> minecraft.level == null
				? List.of()
				: minecraft.level.registryAccess().<X>lookup(key)
					.map(registry -> registry.listElements().map(holder -> holder.key().identifier().toString()).sorted().toList())
					.orElse(List.of()),
			holder -> holder.unwrapKey().map(k -> k.identifier().toString()).orElse(null),
			(minecraft, id) -> minecraft.level == null
				? null
				: minecraft.level.registryAccess().<X>lookup(key)
					.flatMap(registry -> registry.get(ResourceKey.create(key, Identifier.parse(id))))
					.<Holder<X>>map(holder -> holder)
					.orElse(null)
		);
	}

	private static <X> Kind<net.minecraft.world.item.EitherHolder<X>> eitherRegistry(
			DataComponentType<net.minecraft.world.item.EitherHolder<X>> component, ResourceKey<? extends Registry<X>> key) {
		return new Kind<>(component,
			minecraft -> minecraft.level == null ? List.of() : minecraft.level.registryAccess().<X>lookup(key)
				.map(registry -> registry.listElements().map(holder -> holder.key().identifier().toString()).sorted().toList()).orElse(List.of()),
			value -> value.key().map(k -> k.identifier().toString()).orElse(null),
			(minecraft, id) -> new net.minecraft.world.item.EitherHolder<>(ResourceKey.create(key, Identifier.parse(id))));
	}

	private static <E extends Enum<E>> Kind<E> enumKind(DataComponentType<E> component, Class<E> type) {
		return new Kind<>(component, minecraft -> Arrays.stream(type.getEnumConstants()).map(MobVariants::enumId).toList(), MobVariants::enumId,
			(minecraft, id) -> Arrays.stream(type.getEnumConstants()).filter(value -> enumId(value).equals(id)).findFirst().orElse(null));
	}

	private static String enumId(Enum<?> value) {
		return value instanceof StringRepresentable named ? named.getSerializedName() : value.name().toLowerCase(Locale.ROOT);
	}

	private static final Map<EntityType<?>, Kind<?>> KINDS = Map.ofEntries(
		Map.entry(EntityType.WOLF, registry(DataComponents.WOLF_VARIANT, Registries.WOLF_VARIANT)),
		Map.entry(EntityType.CAT, registry(DataComponents.CAT_VARIANT, Registries.CAT_VARIANT)),
		Map.entry(EntityType.FROG, registry(DataComponents.FROG_VARIANT, Registries.FROG_VARIANT)),
		Map.entry(EntityType.PIG, registry(DataComponents.PIG_VARIANT, Registries.PIG_VARIANT)),
		Map.entry(EntityType.COW, registry(DataComponents.COW_VARIANT, Registries.COW_VARIANT)),
		Map.entry(EntityType.CHICKEN, eitherRegistry(DataComponents.CHICKEN_VARIANT, Registries.CHICKEN_VARIANT)),
		Map.entry(EntityType.VILLAGER, registry(DataComponents.VILLAGER_VARIANT, Registries.VILLAGER_TYPE)),
		Map.entry(EntityType.ZOMBIE_VILLAGER, registry(DataComponents.VILLAGER_VARIANT, Registries.VILLAGER_TYPE)),
		Map.entry(EntityType.FOX, enumKind(DataComponents.FOX_VARIANT, net.minecraft.world.entity.animal.fox.Fox.Variant.class)),
		Map.entry(EntityType.RABBIT, enumKind(DataComponents.RABBIT_VARIANT, net.minecraft.world.entity.animal.rabbit.Rabbit.Variant.class)),
		Map.entry(EntityType.PARROT, enumKind(DataComponents.PARROT_VARIANT, net.minecraft.world.entity.animal.parrot.Parrot.Variant.class)),
		Map.entry(EntityType.AXOLOTL, enumKind(DataComponents.AXOLOTL_VARIANT, net.minecraft.world.entity.animal.axolotl.Axolotl.Variant.class)),
		Map.entry(EntityType.LLAMA, enumKind(DataComponents.LLAMA_VARIANT, net.minecraft.world.entity.animal.equine.Llama.Variant.class)),
		Map.entry(EntityType.TRADER_LLAMA, enumKind(DataComponents.LLAMA_VARIANT, net.minecraft.world.entity.animal.equine.Llama.Variant.class)),
		Map.entry(EntityType.HORSE, enumKind(DataComponents.HORSE_VARIANT, net.minecraft.world.entity.animal.equine.Variant.class)),
		Map.entry(EntityType.MOOSHROOM, enumKind(DataComponents.MOOSHROOM_VARIANT, net.minecraft.world.entity.animal.cow.MushroomCow.Variant.class)),
		Map.entry(EntityType.SHEEP, enumKind(DataComponents.SHEEP_COLOR, DyeColor.class)),
		Map.entry(EntityType.SHULKER, enumKind(DataComponents.SHULKER_COLOR, DyeColor.class))
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

	static String variantOf(Entity entity) {
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
