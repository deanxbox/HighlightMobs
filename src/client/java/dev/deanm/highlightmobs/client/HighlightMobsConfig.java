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
import java.util.Objects;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class HighlightMobsConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("HighlightMobs");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("highlightmobs.json");

	boolean enabled = true;
	Set<String> highlightedEntityTypes = new HashSet<>();

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

	boolean isHighlighted(EntityType<?> type) {
		return this.enabled && this.highlightedEntityTypes.contains(EntityType.getKey(type).toString());
	}

	boolean isSelected(EntityType<?> type) {
		return this.highlightedEntityTypes.contains(EntityType.getKey(type).toString());
	}

	void toggle(EntityType<?> type) {
		String id = EntityType.getKey(type).toString();
		if (!this.highlightedEntityTypes.remove(id)) {
			this.highlightedEntityTypes.add(id);
		}
		this.save();
	}
}
