package dev.deanm.highlightmobs.client.mixin;

import dev.deanm.highlightmobs.client.HighlightMobsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	@Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
	private void highlightmobs$highlightSelectedEntity(Entity entity, CallbackInfoReturnable<Boolean> callback) {
		if (!callback.getReturnValue() && HighlightMobsClient.shouldHighlight(entity)) {
			callback.setReturnValue(true);
		}
	}
}
