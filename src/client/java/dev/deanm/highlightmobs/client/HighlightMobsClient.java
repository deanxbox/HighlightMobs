package dev.deanm.highlightmobs.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

public final class HighlightMobsClient implements ClientModInitializer {
	public static final String MOD_ID = "highlightmobs";
	static final HighlightMobsConfig CONFIG = HighlightMobsConfig.load();

	private static KeyMapping toggleKey;
	private static KeyMapping settingsKey;

	@Override
	public void onInitializeClient() {
		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.highlightmobs.toggle",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_H,
			"key.categories.highlightmobs"
		));
		settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.highlightmobs.settings",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_O,
			"key.categories.highlightmobs"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				CONFIG.enabled = !CONFIG.enabled;
				CONFIG.save();
				client.gui.setOverlayMessage(
					Component.translatable(CONFIG.enabled ? "highlightmobs.message.enabled" : "highlightmobs.message.disabled"),
					false
				);
			}

			while (settingsKey.consumeClick()) {
				client.setScreen(new HighlightMobsScreen(client.screen));
			}
		});
	}

	public static boolean shouldHighlight(Entity entity) {
		return CONFIG.isHighlighted(entity.getType());
	}
}
