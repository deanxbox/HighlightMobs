package dev.deanm.highlightmobs.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class HighlightMobsConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("HighlightMobs");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("highlightmobs.json");

	boolean enabled = true;
	Set<String> highlightedEntityTypes = new HashSet<>();
	// entity id -> selected variant ids; no entry means every variant of a selected type
	Map<String, Set<String>> highlightedVariants = new HashMap<>();

	static HighlightMobsConfig load() {
		if (!Files.exists(PATH)) {
			return new HighlightMobsConfig();
		}

		try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
			HighlightMobsConfig config = GSON.fromJson(reader, HighlightMobsConfig.class);
			if (config == null) {
				return new HighlightMobsConfig();
			}

			config.highlightedEntityTypes = config.highlightedEntityTypes == null
				? new HashSet<>()
				: new HashSet<>(config.highlightedEntityTypes);
			config.highlightedEntityTypes.removeIf(Objects::isNull);
			if (config.highlightedVariants == null) {
				config.highlightedVariants = new HashMap<>();
			}
			config.highlightedVariants.values().removeIf(Objects::isNull);
			return config;
		} catch (IOException | JsonParseException exception) {
			LOGGER.warn("Could not read {}. Using default HighlightMobs settings.", PATH, exception);
			return new HighlightMobsConfig();
		}
	}

	void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException exception) {
			LOGGER.warn("Could not save HighlightMobs settings to {}.", PATH, exception);
		}
	}

	boolean isHighlighted(Entity entity) {
		if (!this.enabled) {
			return false;
		}
		String id = EntityType.getKey(entity.getType()).toString();
		if (!this.highlightedEntityTypes.contains(id)) {
			return false;
		}
		Set<String> variants = this.highlightedVariants.get(id);
		if (variants == null || variants.isEmpty()) {
			return true;
		}
		String variant = MobVariants.variantOf(entity);
		return variant == null || variants.contains(variant);
	}

	boolean isSelected(EntityType<?> type) {
		return this.highlightedEntityTypes.contains(EntityType.getKey(type).toString());
	}

	void select(java.util.Collection<EntityType<?>> types) {
		this.highlightedEntityTypes.clear();
		this.highlightedVariants.clear();
		types.forEach(type -> this.highlightedEntityTypes.add(EntityType.getKey(type).toString()));
		this.save();
	}

	void clear() {
		this.highlightedEntityTypes.clear();
		this.highlightedVariants.clear();
		this.save();
	}

	/** True when only some variants of a selected type are highlighted. */
	boolean isPartial(EntityType<?> type) {
		Set<String> variants = this.highlightedVariants.get(EntityType.getKey(type).toString());
		return isSelected(type) && variants != null && !variants.isEmpty();
	}

	boolean isVariantSelected(EntityType<?> type, String variant) {
		if (!isSelected(type)) {
			return false;
		}
		Set<String> variants = this.highlightedVariants.get(EntityType.getKey(type).toString());
		return variants == null || variants.isEmpty() || variants.contains(variant);
	}

	void selectAllVariants(EntityType<?> type) {
		String id = EntityType.getKey(type).toString();
		this.highlightedEntityTypes.add(id);
		this.highlightedVariants.remove(id);
		this.save();
	}

	void deselectType(EntityType<?> type) {
		String id = EntityType.getKey(type).toString();
		this.highlightedEntityTypes.remove(id);
		this.highlightedVariants.remove(id);
		this.save();
	}

	/** Toggles one variant; `all` is every known variant so "all selected" collapses back to the whole type. */
	void toggleVariant(EntityType<?> type, String variant, List<String> all) {
		Set<String> current = new HashSet<>(isSelected(type) ? variantsOrAll(type, all) : Set.of());
		if (!current.remove(variant)) {
			current.add(variant);
		}
		if (current.isEmpty()) {
			deselectType(type);
		} else if (current.containsAll(all)) {
			selectAllVariants(type);
		} else {
			String id = EntityType.getKey(type).toString();
			this.highlightedEntityTypes.add(id);
			this.highlightedVariants.put(id, current);
			this.save();
		}
	}

	private Set<String> variantsOrAll(EntityType<?> type, List<String> all) {
		Set<String> variants = this.highlightedVariants.get(EntityType.getKey(type).toString());
		return variants == null || variants.isEmpty() ? new HashSet<>(all) : variants;
	}

	void toggle(EntityType<?> type) {
		String id = EntityType.getKey(type).toString();
		if (!this.highlightedEntityTypes.remove(id)) {
			this.highlightedEntityTypes.add(id);
		}
		this.save();
	}
}
