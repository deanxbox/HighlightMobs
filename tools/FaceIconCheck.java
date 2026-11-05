package dev.deanm.highlightmobs.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import net.minecraft.resources.Identifier;

/**
 * Offline regression check. Compile/run with the client runtime classpath.
 * Arguments: Minecraft client jar, preview PNG output. No game or OpenGL context needed.
 */
public final class FaceIconCheck {
	public static void main(String[] args) throws Exception {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
		try (ZipFile jar = new ZipFile(args[0])) {
			String[] cats = {"all_black", "black", "british_shorthair", "calico", "jellie", "persian",
				"ragdoll", "red", "siamese", "tabby", "white"};
			BufferedImage preview = new BufferedImage(cats.length * 80, 80, BufferedImage.TYPE_INT_ARGB);
			var hashes = new HashSet<Integer>();
			for (int i = 0; i < cats.length; i++) {
				var face = render(jar, "cat", "cat/cat_" + cats[i] + ".png");
				require(face.width() >= 5 && face.height() >= 5, "Cat face lost texel detail: " + cats[i]);
				hashes.add(Arrays.hashCode(face.pixels()));
				for (int y = 0; y < 80; y++) {
					for (int x = 0; x < 80; x++) {
						preview.setRGB(i * 80 + x, y, face.pixels()[(y * face.height() / 80) * face.width() + x * face.width() / 80]);
					}
				}
			}
			require(hashes.size() == cats.length, "Cat variants must have distinct faces");
			var bat = render(jar, "bat", "bat/bat.png");
			require(bat.width() == 8 && bat.height() == 6, "Bat must retain both ears and face");
			var warden = render(jar, "warden", "warden/warden.png");
			require(warden.width() == 16 && warden.height() == 16, "Warden tendrils must not shrink its face");
			for (String id : new String[] {"villager", "wandering_trader", "zombie_villager"}) {
				var face = render(jar, id, id + "/" + id + ".png");
				require((float) face.width() / face.height() >= 0.7F, id + " head has excessive padding");
			}
			var zoglin = render(jar, "zoglin", "hoglin/zoglin.png");
			require((float) zoglin.width() / zoglin.height() < 1.5F, "Zoglin must show a face, not a thin strip");
			var creeper = render(jar, "creeper", "creeper/creeper.png");
			require(creeper.width() == 8 && creeper.height() == 8, "Creeper regression");
			var wolf = render(jar, "wolf", "wolf/wolf.png");
			require(wolf.width() == 6 && wolf.height() == 8, "Wolf regression");
			var endermite = render(jar, "endermite", "endermite/endermite.png");
			require(endermite.width() == 4 && endermite.height() == 3, "Endermite must use the front segment, not its torso");
			var frogHashes = new HashSet<Integer>();
			for (String variant : new String[] {"temperate", "warm", "cold"}) {
				var frog = render(jar, "frog", "frog/frog_" + variant + ".png");
				require(frog.width() == 7 && frog.height() == 5, "Frog must retain both eyes and mouth");
				frogHashes.add(Arrays.hashCode(frog.pixels()));
			}
			require(frogHashes.size() == 3, "Frog variants must keep distinct textures");
			var enderman = render(jar, "enderman", "enderman/enderman.png");
			require(enderman.width() == 8 && enderman.height() == 8, "Enderman must not include the unused hat");
			require(Arrays.stream(enderman.pixels()).anyMatch(c -> ((c >>> 16) & 255) > ((c >>> 8) & 255) + 50),
				"Enderman purple eye overlay is missing");
			var bogged = render(jar, "bogged", "skeleton/bogged.png");
			require(bogged.width() == 8 && bogged.height() == 8, "Bogged must include its moss face layer, but not mushrooms or unused hat");
			require(MobIcons.overlayPixel(0xFF123456, 0) == 0xFF123456, "Transparent overlays must preserve the base");
			var faces = new BufferedImage(16, 8, BufferedImage.TYPE_INT_ARGB);
			faces.setRGB(0, 0, 8, 8, bogged.pixels(), 0, 8);
			faces.setRGB(8, 0, 8, 8, enderman.pixels(), 0, 8);
			ImageIO.write(faces, "png", new File(args[1] + ".faces.png"));
			int mobs = 0;
			for (var type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
				if (!HighlightMobsScreen.isMob(type)) {
					continue;
				}
				var id = net.minecraft.world.entity.EntityType.getKey(type);
				var path = MobIcons.defaultTexture(id);
				require(jar.getEntry("assets/" + path.getNamespace() + "/" + path.getPath()) != null, "Missing no-world texture: " + id);
				require(MobIcons.vanillaModel(id) != null, "Missing no-world model: " + id);
				var face = render(jar, id.getPath(), path.getPath().substring("textures/entity/".length()));
				require(Arrays.stream(face.pixels()).anyMatch(c -> (c >>> 24) > 8), "Empty no-world face: " + id);
				mobs++;
			}
			System.out.println("PASS: default textures and models for " + mobs + " vanilla mobs without a world/client instance");
			try (var input = jar.getInputStream(jar.getEntry("assets/minecraft/textures/entity/wither/wither.png"))) {
				var texture = ImageIO.read(input);
				var skull = MobIcons.renderSkull(texture.getRGB(0, 0, texture.getWidth(), texture.getHeight(), null, 0, texture.getWidth()),
					texture.getWidth(), texture.getHeight());
				require(skull.width() == 64 && skull.height() == 64, "Skull sprite size");
				require(skull.pixels()[0] == 0 && (skull.pixels()[32 * 64 + 32] >>> 24) == 255,
					"Skull must have transparent corners and an opaque centre");
				var skullPreview = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
				skullPreview.setRGB(0, 0, 64, 64, skull.pixels(), 0, 64);
				ImageIO.write(skullPreview, "png", new File(args[1] + ".skull.png"));
			}
			ImageIO.write(preview, "png", new File(args[1]));
			System.out.println("PASS: 11 cat faces; bat, warden, villager family, zoglin, creeper, wolf, endermite, 3 frogs, bogged, enderman and skull checks");
		}
	}

	private static MobIcons.FaceImage render(ZipFile jar, String mob, String texture) throws Exception {
		var entry = jar.getEntry("assets/minecraft/textures/entity/" + texture);
		require(entry != null, "Missing texture: " + texture);
		BufferedImage image;
		try (var input = jar.getInputStream(entry)) {
			image = ImageIO.read(input);
		}
		int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
		var overlay = MobIcons.faceOverlay(Identifier.withDefaultNamespace(mob));
		if (overlay != null) {
			try (var input = jar.getInputStream(jar.getEntry("assets/" + overlay.getNamespace() + "/" + overlay.getPath()))) {
				var layer = ImageIO.read(input);
				for (int y = 0; y < image.getHeight(); y++) {
					for (int x = 0; x < image.getWidth(); x++) {
						int i = y * image.getWidth() + x;
						pixels[i] = MobIcons.overlayPixel(pixels[i], layer.getRGB(x, y));
					}
				}
			}
		}
		var root = MobIcons.vanillaModel(Identifier.withDefaultNamespace(mob));
		require(root != null, "Missing vanilla model: " + mob);
		var face = MobIcons.renderFace(root, pixels, image.getWidth(), image.getHeight());
		require(face != null, "Missing face: " + mob);
		return face;
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
