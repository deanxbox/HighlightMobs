package dev.deanm.highlightmobs.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Draws the flat front view of each mob's head. Every front-facing cube of the vanilla head model is cut out of the
 * vanilla texture and stitched into one small image, so resource packs and model mods (EMF/Fresh Animations) never
 * change the icons.
 */
final class MobIcons {
	private static final Logger LOGGER = LoggerFactory.getLogger("HighlightMobs");

	private record Face(ResourceLocation texture, float aspect) {
	}

	/** A finished icon; pixels are row-major in the same int format as the source texture. */
	record FaceImage(int width, int height, int[] pixels) {
	}

	private record Quad(float minX, float maxX, float minY, float maxY, float depth,
			float uLeft, float uRight, float vTop, float vBottom) {
		float area() {
			return (maxX - minX) * (maxY - minY);
		}
	}

	/** Default appearances available before a world's variant registries and entity renderers exist. */
	private static final Map<String, String> DEFAULT_TEXTURES = Map.ofEntries(
		Map.entry("armadillo", "armadillo"),
		Map.entry("axolotl", "axolotl/axolotl_lucy"),
		Map.entry("bat", "bat"),
		Map.entry("blaze", "blaze"),
		Map.entry("bogged", "skeleton/bogged"),
		Map.entry("cat", "cat/tabby"),
		Map.entry("cave_spider", "spider/cave_spider"),
		Map.entry("chicken", "chicken"),
		Map.entry("cod", "fish/cod"),
		Map.entry("cow", "cow/cow"),
		Map.entry("creaking_transient", "creaking/creaking"),
		Map.entry("dolphin", "dolphin"),
		Map.entry("donkey", "horse/donkey"),
		Map.entry("drowned", "zombie/drowned"),
		Map.entry("elder_guardian", "guardian_elder"),
		Map.entry("ender_dragon", "enderdragon/dragon"),
		Map.entry("endermite", "endermite"),
		Map.entry("evoker", "illager/evoker"),
		Map.entry("fox", "fox/fox"),
		Map.entry("frog", "frog/temperate_frog"),
		Map.entry("giant", "zombie/zombie"),
		Map.entry("glow_squid", "squid/glow_squid"),
		Map.entry("guardian", "guardian"),
		Map.entry("horse", "horse/horse_white"),
		Map.entry("husk", "zombie/husk"),
		Map.entry("illusioner", "illager/illusioner"),
		Map.entry("llama", "llama/creamy"),
		Map.entry("magma_cube", "slime/magmacube"),
		Map.entry("mooshroom", "cow/red_mooshroom"),
		Map.entry("mule", "horse/mule"),
		Map.entry("ocelot", "cat/ocelot"),
		Map.entry("panda", "panda/panda"),
		Map.entry("parrot", "parrot/parrot_red_blue"),
		Map.entry("phantom", "phantom"),
		Map.entry("pig", "pig/pig"),
		Map.entry("piglin_brute", "piglin/piglin_brute"),
		Map.entry("pillager", "illager/pillager"),
		Map.entry("polar_bear", "bear/polarbear"),
		Map.entry("pufferfish", "fish/pufferfish"),
		Map.entry("rabbit", "rabbit/brown"),
		Map.entry("ravager", "illager/ravager"),
		Map.entry("salmon", "fish/salmon"),
		Map.entry("silverfish", "silverfish"),
		Map.entry("skeleton_horse", "horse/horse_skeleton"),
		Map.entry("snow_golem", "snow_golem"),
		Map.entry("stray", "skeleton/stray"),
		Map.entry("trader_llama", "llama/creamy"),
		Map.entry("tropical_fish", "fish/tropical_a"),
		Map.entry("turtle", "turtle/big_sea_turtle"),
		Map.entry("vex", "illager/vex"),
		Map.entry("vindicator", "illager/vindicator"),
		Map.entry("wandering_trader", "wandering_trader"),
		Map.entry("witch", "witch"),
		Map.entry("wither_skeleton", "skeleton/wither_skeleton"),
		Map.entry("wither_skull", "wither/wither"),
		Map.entry("zoglin", "hoglin/zoglin"),
		Map.entry("zombie_horse", "horse/horse_zombie"),
		Map.entry("zombified_piglin", "piglin/zombified_piglin"));

	private static final Face NONE = new Face(ResourceLocation.withDefaultNamespace("missingno"), 0);
	/** Mobs whose front view is a thin sliver: their own item reads much better. */
	private static final Map<String, String> MOB_ITEMS = Map.of(
		"cod", "cod", "salmon", "salmon", "tropical_fish", "tropical_fish");
	/** Mobs whose head is meant to be seen tilted (the snout points down at the viewer). */
	private static final java.util.Set<String> KEEP_POSE = java.util.Set.of("hoglin", "zoglin");
	/** Entities with no item of their own: a fitting item texture instead. */
	private static final Map<String, String> ITEM_ICONS = Map.ofEntries(
		Map.entry("area_effect_cloud", "dragon_breath"),
		Map.entry("boat", "oak_boat"),
		Map.entry("chest_boat", "oak_chest_boat"),
		Map.entry("block_display", "glass"),
		Map.entry("text_display", "name_tag"),
		Map.entry("item_display", "item_frame"),
		Map.entry("interaction", "stick"),
		Map.entry("marker", "debug_stick"),
		Map.entry("dragon_fireball", "dragon_breath"),
		Map.entry("fireball", "fire_charge"),
		Map.entry("small_fireball", "fire_charge"),
		Map.entry("leash_knot", "lead"),
		Map.entry("lightning_bolt", "lightning_rod"),
		Map.entry("llama_spit", "slime_ball"),
		Map.entry("experience_orb", "experience_bottle"),
		Map.entry("evoker_fangs", "totem_of_undying"),
		Map.entry("ominous_item_spawner", "ominous_trial_key"),
		Map.entry("player", "player_head"),
		Map.entry("mannequin", "player_head"),
		Map.entry("shulker_bullet", "shulker_shell"),
		Map.entry("breeze_wind_charge", "wind_charge"),
		Map.entry("wind_charge", "wind_charge"),
		Map.entry("wither_skull", "wither_skeleton_skull"),
		Map.entry("fishing_bobber", "fishing_rod"),
		Map.entry("eye_of_ender", "ender_eye"),
		Map.entry("item", "paper"));
	private static final Map<String, Face> FACES = new HashMap<>();
	private static final List<ResourceLocation> UPLOADED = new ArrayList<>();
	private static @Nullable Map<ModelLayerLocation, LayerDefinition> vanillaLayers;
	private static int nextId = -1000;
	private static int nextTexture;
	private static @Nullable Level cachedLevel;

	private MobIcons() {
	}

	/** Shortens a button label with an ellipsis so it clears the 16px icon on the left (and stays centred). */
	static net.minecraft.network.chat.Component fit(net.minecraft.client.gui.Font font, net.minecraft.network.chat.Component text, int buttonWidth) {
		int max = buttonWidth - 48;
		String full = text.getString();
		if (font.width(full) <= max) {
			return text;
		}
		int end = full.length();
		while (end > 1 && font.width(full.substring(0, end) + "...") > max) {
			end--;
		}
		return net.minecraft.network.chat.Component.literal(full.substring(0, end) + "...");
	}

	static void draw(GuiGraphics graphics, EntityType<?> type, int x, int y, int size) {
		draw(graphics, type, null, x, y, size);
	}

	static void draw(GuiGraphics graphics, EntityType<?> type, @Nullable String variant, int x, int y, int size) {
		try {
			drawIcon(graphics, type, variant, x, y, size);
		} catch (RuntimeException exception) {
			// an icon must never crash the settings screen (e.g. item components are unbound before a world is joined)
		}
	}

	private static void drawIcon(GuiGraphics graphics, EntityType<?> type, @Nullable String variant, int x, int y, int size) {
		Level level = Minecraft.getInstance().level;
		if (level != cachedLevel) {
			UPLOADED.forEach(Minecraft.getInstance().getTextureManager()::release);
			UPLOADED.clear();
			FACES.clear();
			cachedLevel = level;
		}
		if ((!HighlightMobsScreen.isMob(type) && !EntityType.getKey(type).getPath().equals("wither_skull"))
				|| MOB_ITEMS.containsKey(EntityType.getKey(type).getPath())) {
			if (Minecraft.getInstance().level == null) {
				drawFace(graphics, menuItem(type), x, y, size);
				return;
			}
			// boats, minecarts, projectiles etc. have no face: use the matching item texture
			ResourceLocation key = EntityType.getKey(type);
			ItemStack stack = BuiltInRegistries.ITEM.getOptional(key.withPath(MOB_ITEMS.getOrDefault(key.getPath(), key.getPath())))
				.map(ItemStack::new).orElse(ItemStack.EMPTY);
			if (stack.isEmpty()) {
				stack = BuiltInRegistries.ITEM.getOptional(ResourceLocation.withDefaultNamespace(ITEM_ICONS.getOrDefault(key.getPath(), "paper")))
					.map(ItemStack::new).orElse(ItemStack.EMPTY);
			}
			if (!stack.isEmpty()) {
				graphics.renderFakeItem(stack, x, y);
			}
			return;
		}

		Face face = face(type, variant);
		if (face == NONE) {
			if (Minecraft.getInstance().level == null) {
				drawFace(graphics, menuItem(type), x, y, size);
			} else {
				var egg = net.minecraft.world.item.SpawnEggItem.byId(type);
				if (egg != null) {
					graphics.renderFakeItem(new ItemStack(egg), x, y);
				}
			}
			return;
		}
		drawFace(graphics, face, x, y, size);
	}

	private static void drawFace(GuiGraphics graphics, Face face, int x, int y, int size) {
		if (face == NONE) {
			return;
		}
		int width = face.aspect() >= 1 ? size : Math.max(1, Math.round(size * face.aspect()));
		int height = face.aspect() >= 1 ? Math.max(1, Math.round(size / face.aspect())) : size;
		int left = x + (size - width) / 2;
		int top = y + (size - height) / 2;
		graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, face.texture(), left, top, 0.0F, 0.0F, width, height, width, height);
	}

	private static Face face(EntityType<?> type, @Nullable String variant) {
		Level level = Minecraft.getInstance().level;
		return FACES.computeIfAbsent(EntityType.getKey(type) + "|" + variant, key -> {
			try {
				Face found = buildFace(type, variant, level);
				return found == null ? NONE : found;
			} catch (RuntimeException | IOException exception) {
				LOGGER.warn("Could not build a face icon for {}", EntityType.getKey(type), exception);
				return NONE;
			}
		});
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static @Nullable Face buildFace(EntityType<?> type, @Nullable String variant, @Nullable Level level) throws IOException {
		ResourceLocation key = EntityType.getKey(type);
		ResourceLocation texture = defaultTexture(key);
		if (level == null && variant != null) {
			ResourceLocation candidate = variantTexture(key, variant);
			if (textureResource(candidate) != null) {
				texture = candidate;
			}
		}
		ModelPart root = vanillaModel(key);
		// World data is needed only for explicitly selected variants, not for the default icons.
		if (level != null && (variant != null || !key.getNamespace().equals("minecraft"))) {
			Entity entity = type.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
			if (entity != null) {
				entity.setId(nextId--);
				if (variant != null) {
					MobVariants.apply(entity, variant);
				}
				EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
				if (renderer instanceof LivingEntityRenderer living) {
					texture = living.getTextureLocation((LivingEntityRenderState) renderer.createRenderState(entity, 1.0F));
					if (root == null) {
						root = living.getModel().root();
					}
				}
				if (entity instanceof net.minecraft.world.entity.animal.frog.Frog frog) {
					texture = frog.getVariant().value().texture();
				}
			}
		}
		boolean skull = key.getPath().equals("wither_skull");
		if (texture == null || (root == null && !skull)) {
			return null;
		}

		var resource = textureResource(texture);
		if (resource == null) {
			return null;
		}
		FaceImage face;
		try (InputStream stream = resource.get(); NativeImage source = NativeImage.read(stream)) {
			int[] pixels = new int[source.getWidth() * source.getHeight()];
			for (int y = 0; y < source.getHeight(); y++) {
				for (int x = 0; x < source.getWidth(); x++) {
					pixels[y * source.getWidth() + x] = source.getPixel(x, y);
				}
			}
			ResourceLocation overlay = faceOverlay(key);
			if (overlay != null) {
				var layer = textureResource(overlay);
				if (layer != null) {
					try (InputStream input = layer.get(); NativeImage image = NativeImage.read(input)) {
						if (image.getWidth() == source.getWidth() && image.getHeight() == source.getHeight()) {
							for (int y = 0; y < image.getHeight(); y++) {
								for (int x = 0; x < image.getWidth(); x++) {
									int color = image.getPixel(x, y);
									int index = y * image.getWidth() + x;
									pixels[index] = overlayPixel(pixels[index], color);
								}
							}
						}
					}
				}
			}
			face = skull ? renderSkull(pixels, source.getWidth(), source.getHeight())
				: renderFace(root, pixels, source.getWidth(), source.getHeight());
		}
		if (face == null) {
			return null;
		}
		return upload(face);
	}

	private static Face upload(FaceImage face) {
		NativeImage image = new NativeImage(face.width(), face.height(), true);
		for (int y = 0; y < face.height(); y++) {
			for (int x = 0; x < face.width(); x++) {
				image.setPixel(x, y, face.pixels()[y * face.width() + x]);
			}
		}
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(HighlightMobsClient.MOD_ID, "face/" + nextTexture++);
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
		UPLOADED.add(id);
		return new Face(id, (float) face.width() / face.height());
	}

	static ResourceLocation defaultTexture(ResourceLocation entity) {
		String id = entity.getPath();
		return entity.withPath("textures/entity/" + DEFAULT_TEXTURES.getOrDefault(id, id + "/" + id) + ".png");
	}

	static ResourceLocation variantTexture(ResourceLocation entity, String variant) {
		String id = entity.getPath();
		String name = ResourceLocation.parse(variant).getPath();
		String path = switch (id) {
			case "cat", "llama", "rabbit" -> id + "/" + (name.equals("evil") ? "caerbannog" : name);
			case "trader_llama" -> "llama/" + name;
			case "frog" -> "frog/" + name + "_frog";
			case "pig", "cow", "chicken" -> id + "/" + name + "_" + id;
			case "horse" -> "horse/horse_" + (name.equals("dark_brown") ? "darkbrown" : name);
			case "parrot" -> "parrot/parrot_" + (name.equals("gray") ? "grey" : name);
			case "axolotl", "shulker" -> id + "/" + id + "_" + name;
			case "mooshroom" -> "cow/" + name + "_mooshroom";
			case "wolf" -> "wolf/wolf" + (name.equals("pale") ? "" : "_" + name);
			case "fox" -> "fox/" + (name.equals("snow") ? "snow_fox" : "fox");
			default -> DEFAULT_TEXTURES.getOrDefault(id, id + "/" + id);
		};
		return entity.withPath("textures/entity/" + path + ".png");
	}

	static int overlayPixel(int base, int overlay) {
		return (overlay >>> 24) > 127 ? overlay : base;
	}

	static @Nullable ResourceLocation faceOverlay(ResourceLocation entity) {
		return switch (entity.getPath()) {
			case "bogged" -> entity.withPath("textures/entity/skeleton/bogged_overlay.png");
			case "enderman" -> entity.withPath("textures/entity/enderman/enderman_eyes.png");
			default -> null;
		};
	}

	private static net.minecraft.server.packs.resources.@Nullable IoSupplier<InputStream> textureResource(ResourceLocation texture) {
		if (texture.getNamespace().equals("minecraft")) {
			return Minecraft.getInstance().getVanillaPackResources()
				.getResource(net.minecraft.server.packs.PackType.CLIENT_RESOURCES, texture);
		}
		return Minecraft.getInstance().getResourceManager().getResource(texture)
			.<net.minecraft.server.packs.resources.IoSupplier<InputStream>>map(resource -> resource::open).orElse(null);
	}

	/** No ItemStacks here: their components may not be bound until a server/world is joined. */
	private static Face menuItem(EntityType<?> type) {
		ResourceLocation key = EntityType.getKey(type);
		return FACES.computeIfAbsent("item/" + key, unused -> {
			try {
				String id = MOB_ITEMS.getOrDefault(key.getPath(), key.getPath());
				ResourceLocation texture = key.withPath("textures/item/" + id + ".png");
				var resource = textureResource(texture);
				if (resource == null) {
					id = ITEM_ICONS.getOrDefault(id, id);
					texture = ResourceLocation.withDefaultNamespace("textures/item/" + id + ".png");
					resource = textureResource(texture);
				}
				if (resource == null) {
					texture = ResourceLocation.withDefaultNamespace("textures/block/" + (id.equals("tnt") ? "tnt_side" : id) + ".png");
					resource = textureResource(texture);
				}
				boolean skull = id.equals("player_head");
				if (skull) {
					resource = textureResource(ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png"));
				}
				if (resource == null) {
					resource = textureResource(ResourceLocation.withDefaultNamespace("textures/item/" + (id.equals("debug_stick") ? "stick" : "paper") + ".png"));
				}
				if (resource == null) {
					return NONE;
				}
				try (InputStream stream = resource.get(); NativeImage image = NativeImage.read(stream)) {
					int[] pixels = new int[image.getWidth() * image.getHeight()];
					for (int y = 0; y < image.getHeight(); y++) {
						for (int x = 0; x < image.getWidth(); x++) {
							pixels[y * image.getWidth() + x] = image.getPixel(x, y);
						}
					}
					return upload(skull ? renderSkull(pixels, image.getWidth(), image.getHeight())
						: new FaceImage(image.getWidth(), Math.min(image.getWidth(), image.getHeight()), pixels));
				}
			} catch (RuntimeException | IOException exception) {
				LOGGER.warn("Could not build a menu item icon for {}", key, exception);
				return NONE;
			}
		});
	}

	/** The unmodified vanilla model for this entity, baked straight from Mojang's layer definitions. */
	static @Nullable ModelPart vanillaModel(ResourceLocation entity) {
		if (vanillaLayers == null) {
			vanillaLayers = LayerDefinitions.createRoots();
		}
		String model = switch (entity.getPath()) {
			case "creaking_transient" -> "creaking";
			case "pufferfish" -> "pufferfish_small";
			case "tropical_fish" -> "tropical_fish_small";
			default -> entity.getPath();
		};
		LayerDefinition layer = vanillaLayers.get(new ModelLayerLocation(entity.withPath(model), "main"));
		if (layer == null) {
			return null;
		}
		ModelPart root = layer.bakeRoot();
		if (entity.getPath().equals("enderman") || entity.getPath().equals("bogged")) {
			ModelPart head = root.getChild("head");
			if (head.hasChild("hat")) {
				head.getChild("hat").visible = false;
			}
			if (head.hasChild("mushrooms")) {
				head.getChild("mushrooms").visible = false;
			}
		}
		if (entity.getPath().equals("endermite")) {
			return root.getChild("segment0"); // segment1 is larger, but it is the torso, not the face
		}
		if (KEEP_POSE.contains(entity.getPath())) {
			return root;
		}
		// tilted heads (horses, bats...) would be seen at an angle: show every part straight on
		for (ModelPart part : root.getAllParts().toList()) {
			part.xRot = 0;
			part.yRot = 0;
			part.zRot = 0;
		}
		return root;
	}

	/** Flattens the front of the model's head into a small image. Pure, so it can be checked outside the game. */
	static @Nullable FaceImage renderFace(ModelPart root, int[] texture, int textureWidth, int textureHeight) {
		List<Quad> headQuads = new ArrayList<>();
		List<Quad> allQuads = new ArrayList<>();
		Vector3f point = new Vector3f();
		Vector3f normal = new Vector3f();
		root.visit(new PoseStack(), (pose, path, index, cube) -> {
			ModelPart part = root;
			if (!part.visible) {
				return;
			}
			for (String name : path.split("/")) {
				if (!name.isEmpty()) {
					part = part.getChild(name);
					if (!part.visible) {
						return;
					}
				}
			}
			if (part.skipDraw) {
				return;
			}
			String lower = path.toLowerCase(Locale.ROOT);
			// wither: centre head only; horses: the neck cube sits in head_parts and the mane runs down the neck;
			// villagers: the flat hat brim; warden: the side tendrils (they shrink the face to a third)
			boolean head = lower.contains("head") && !lower.contains("left_head") && !lower.contains("right_head")
				&& !lower.endsWith("head_parts") && !lower.contains("mane") && !lower.contains("hat_rim") && !lower.contains("tendril");
			for (ModelPart.Polygon polygon : cube.polygons) {
				pose.pose().transformDirection(polygon.normal().x(), polygon.normal().y(), polygon.normal().z(), normal);
				if (normal.z() > -0.5F) {
					continue; // not facing the viewer
				}
				float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE, z = 0;
				ModelPart.Vertex[] vertices = polygon.vertices();
				float[] xs = new float[vertices.length];
				float[] ys = new float[vertices.length];
				for (int i = 0; i < vertices.length; i++) {
					// Legacy vertices are in pixels; the model pose transforms blocks.
					pose.pose().transformPosition(vertices[i].pos().x() / 16.0F, vertices[i].pos().y() / 16.0F, vertices[i].pos().z() / 16.0F, point);
					xs[i] = point.x() * 16.0F;
					ys[i] = point.y() * 16.0F;
					z += point.z() * 16.0F / vertices.length;
					minX = Math.min(minX, xs[i]);
					maxX = Math.max(maxX, xs[i]);
					minY = Math.min(minY, ys[i]);
					maxY = Math.max(maxY, ys[i]);
				}
				if (maxX - minX < 0.5F || maxY - minY < 0.5F) {
					continue;
				}
				// which texture edge sits at the left/top of the screen rectangle (handles mirrored cubes)
				float uLeft = 0, uRight = 0, vTop = 0, vBottom = 0;
				int nl = 0, nr = 0, nt = 0, nb = 0;
				for (int i = 0; i < vertices.length; i++) {
					if (Math.abs(xs[i] - minX) < 0.01F) { uLeft += vertices[i].u(); nl++; }
					if (Math.abs(xs[i] - maxX) < 0.01F) { uRight += vertices[i].u(); nr++; }
					if (Math.abs(ys[i] - minY) < 0.01F) { vTop += vertices[i].v(); nt++; }
					if (Math.abs(ys[i] - maxY) < 0.01F) { vBottom += vertices[i].v(); nb++; }
				}
				Quad quad = new Quad(minX, maxX, minY, maxY, z, uLeft / nl, uRight / nr, vTop / nt, vBottom / nb);
				allQuads.add(quad);
				if (head) {
					headQuads.add(quad);
				}
			}
		});

		List<Quad> quads = headQuads;
		if (quads.isEmpty()) {
			// no part called "head" (slimes, fish, ghasts...): use the biggest front face of the whole model
			quads = allQuads.isEmpty() ? List.of() : List.of(allQuads.stream().max(Comparator.comparingDouble(Quad::area)).get());
		}
		if (quads.isEmpty()) {
			return null;
		}
		// Vanilla head subtrees are already isolated above; proximity cropping can discard a bat's other ear.
		return compose(quads, texture, textureWidth, textureHeight);
	}

	private static FaceImage compose(List<Quad> quads, int[] texture, int textureWidth, int textureHeight) {
		float left = Float.MAX_VALUE, right = -Float.MAX_VALUE, top = Float.MAX_VALUE, bottom = -Float.MAX_VALUE;
		for (Quad quad : quads) {
			left = Math.min(left, quad.minX());
			right = Math.max(right, quad.maxX());
			top = Math.min(top, quad.minY());
			bottom = Math.max(bottom, quad.maxY());
		}
		// draw at texel resolution: scaled-down models (cats, bats, babies) would otherwise lose their eyes
		Quad main = quads.stream().max(Comparator.comparingDouble(Quad::area)).get();
		float scale = Math.max(1.0F, Math.min(4.0F, Math.abs(main.uRight() - main.uLeft()) * textureWidth / (main.maxX() - main.minX())));
		int width = Math.max(1, Math.round((right - left) * scale));
		int height = Math.max(1, Math.round((bottom - top) * scale));
		int[] pixels = new int[width * height];

		// far parts first so nearer ones (snout, hat) paint over them
		List<Quad> ordered = new ArrayList<>(quads);
		ordered.sort(Comparator.comparingDouble(Quad::depth).reversed());
		for (Quad quad : ordered) {
			int x0 = Math.round((quad.minX() - left) * scale), x1 = Math.max(x0 + 1, Math.round((quad.maxX() - left) * scale));
			int y0 = Math.round((quad.minY() - top) * scale), y1 = Math.max(y0 + 1, Math.round((quad.maxY() - top) * scale));
			for (int py = y0; py < y1 && py < height; py++) {
				for (int px = x0; px < x1 && px < width; px++) {
					float fx = (px + 0.5F - x0) / (x1 - x0);
					float fy = (py + 0.5F - y0) / (y1 - y0);
					int tx = clamp((int) Math.floor((quad.uLeft() + (quad.uRight() - quad.uLeft()) * fx) * textureWidth), textureWidth);
					int ty = clamp((int) Math.floor((quad.vTop() + (quad.vBottom() - quad.vTop()) * fy) * textureHeight), textureHeight);
					int pixel = texture[ty * textureWidth + tx];
					if ((pixel >>> 24) > 8) {
						pixels[py * width + px] = pixel;
					}
				}
			}
		}
		return new FaceImage(width, height, pixels);
	}

	private static int clamp(int value, int size) {
		return Math.max(0, Math.min(size - 1, value));
	}

	/** A flat isometric sprite of the projectile's 8px cube, using its vanilla 64x64 UV layout. */
	static FaceImage renderSkull(int[] texture, int width, int height) {
		int[] pixels = new int[64 * 64];
		skullSide(pixels, texture, width, height, 32, 0, 32, 16, -32, 16, 8, 0, 1.0F);
		skullSide(pixels, texture, width, height, 0, 16, 32, 16, 0, 32, 8, 8, 0.9F);
		skullSide(pixels, texture, width, height, 32, 32, 32, -16, 0, 32, 16, 8, 0.7F);
		return new FaceImage(64, 64, pixels);
	}

	private static void skullSide(int[] output, int[] texture, int width, int height,
			int ox, int oy, int ax, int ay, int bx, int by, int u, int v, float shade) {
		float determinant = ax * by - ay * bx;
		for (int y = 0; y < 64; y++) {
			for (int x = 0; x < 64; x++) {
				float dx = x + 0.5F - ox, dy = y + 0.5F - oy;
				float s = (dx * by - dy * bx) / determinant;
				float t = (ax * dy - ay * dx) / determinant;
				if (s < 0 || s >= 1 || t < 0 || t >= 1) {
					continue;
				}
				int tx = clamp((int) ((u + s * 8) * width / 64.0F), width);
				int ty = clamp((int) ((v + t * 8) * height / 64.0F), height);
				int color = texture[ty * width + tx];
				int r = (int) (((color >>> 16) & 255) * shade);
				int g = (int) (((color >>> 8) & 255) * shade);
				int b = (int) ((color & 255) * shade);
				output[y * 64 + x] = (color & 0xFF000000) | r << 16 | g << 8 | b;
			}
		}
	}
}

