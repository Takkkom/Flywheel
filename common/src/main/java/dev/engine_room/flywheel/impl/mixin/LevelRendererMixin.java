package dev.engine_room.flywheel.impl.mixin;

import java.util.SortedSet;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.impl.FlwImplXplat;
import dev.engine_room.flywheel.impl.event.RenderContextImpl;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.server.level.BlockDestructionProgress;

@Mixin(value = LevelRenderer.class, priority = 1001) // Higher priority to go after Sodium
abstract class LevelRendererMixin {
	@Shadow
	@Nullable
	private ClientLevel level;

	@Shadow
	@Final
	private RenderBuffers renderBuffers;

	@Shadow
	@Final
	private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

	@Unique
	@Nullable
	private RenderContextImpl flywheel$renderContext;

	//	@Inject(method = "renderLevel", at = @At("HEAD"))
	@Inject(method = "renderLevel", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;runLightUpdates()I"))
	private void flywheel$beginRender(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
		flywheel$renderContext = RenderContextImpl.create((LevelRenderer) (Object) this, level, renderBuffers, poseStack, projectionMatrix, camera, partialTick);

		VisualizationManager manager = VisualizationManager.get(level);
		if (manager != null) {
			manager.renderDispatcher().onStartLevelRender(flywheel$renderContext);
		}
	}

	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void flywheel$endRender(CallbackInfo ci) {
		flywheel$renderContext = null;
	}

	@Inject(method = "allChanged", at = @At("RETURN"))
	private void flywheel$reload(CallbackInfo ci) {
		if (level != null) {
			FlwImplXplat.INSTANCE.dispatchReloadLevelRendererEvent(level);
		}
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=blockentities"))
	private void flywheel$beforeBlockEntities(CallbackInfo ci) {
		if (flywheel$renderContext != null) {
			VisualizationManager manager = VisualizationManager.get(level);
			if (manager != null) {
				manager.renderDispatcher().afterEntities(flywheel$renderContext);
			}
		}
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V", ordinal = 0))
	private void flywheel$afterBlockEntities(CallbackInfo ci) {
		if (flywheel$renderContext != null) {
			VisualizationManager manager = VisualizationManager.get(level);
			if (manager != null) {
				manager.renderDispatcher().afterBlockEntities(flywheel$renderContext);
			}
		}
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=destroyProgress"))
	private void flywheel$beforeRenderCrumbling(CallbackInfo ci) {
		if (flywheel$renderContext != null) {
			VisualizationManager manager = VisualizationManager.get(level);
			if (manager != null) {
				manager.renderDispatcher().beforeCrumbling(flywheel$renderContext, destructionProgress);
			}
		}
	}

	@Group(name = "afterParticles", min = 2, max = 2)
	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V", shift = Shift.AFTER))
	private void flywheel$afterParticles$fabric(CallbackInfo ci) {
		if (flywheel$renderContext != null) {
			VisualizationManager manager = VisualizationManager.get(level);
			if (manager != null) {
				manager.renderDispatcher().afterParticles(flywheel$renderContext);
			}
		}
	}

	@Group(name = "afterParticles")
	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V", shift = Shift.AFTER))
	private void flywheel$afterParticles$forge(CallbackInfo ci) {
		if (flywheel$renderContext != null) {
			VisualizationManager manager = VisualizationManager.get(level);
			if (manager != null) {
				manager.renderDispatcher().afterParticles(flywheel$renderContext);
			}
		}
	}
}
