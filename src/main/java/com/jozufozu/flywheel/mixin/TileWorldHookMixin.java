package com.jozufozu.flywheel.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.jozufozu.flywheel.backend.instancing.InstanceManager;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
@Mixin(value = World.class, priority = 1100) // this and create.mixins.json have high priority to load after Performant
public class TileWorldHookMixin {

	final World self = (World) (Object) this;

	@Shadow
	@Final
	public boolean isRemote;

	@Shadow
	@Final
	protected List<TileEntity> tileEntitiesToBeRemoved;

	@Inject(at = @At("TAIL"), method = "addTileEntity")
	private void onAddTile(TileEntity te, CallbackInfoReturnable<Boolean> cir) {
		if (isRemote) {
			InstancedRenderDispatcher.getTiles(self)
					.queueAdd(te);
		}
	}

	/**
	 * Without this we don't unload instances when a chunk unloads.
	 */
	@Inject(at = @At(value = "INVOKE", target = "Ljava/util/Set;clear()V", ordinal = 0), method = "tickBlockEntities")
	private void onChunkUnload(CallbackInfo ci) {
		if (isRemote) {
			InstanceManager<TileEntity> kineticRenderer = InstancedRenderDispatcher.getTiles(self);
			for (TileEntity tile : tileEntitiesToBeRemoved) {
				kineticRenderer.remove(tile);
			}
		}
	}
}
