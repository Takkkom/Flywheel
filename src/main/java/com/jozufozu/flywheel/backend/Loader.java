package com.jozufozu.flywheel.backend;

import com.jozufozu.flywheel.api.material.Material;
import com.jozufozu.flywheel.api.struct.StructType;
import com.jozufozu.flywheel.api.vertex.VertexType;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.jozufozu.flywheel.core.ComponentRegistry;
import com.jozufozu.flywheel.core.compile.ContextShader;
import com.jozufozu.flywheel.core.compile.InstancedArraysCompiler;
import com.jozufozu.flywheel.core.crumbling.CrumblingRenderer;
import com.jozufozu.flywheel.core.source.FileResolution;
import com.jozufozu.flywheel.core.source.ShaderLoadingException;
import com.jozufozu.flywheel.core.source.ShaderSources;
import com.jozufozu.flywheel.core.source.error.ErrorReporter;
import com.jozufozu.flywheel.util.StringUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * The main entity for loading shaders.
 *
 * <p>
 * This class is responsible for invoking the loading, parsing, and compilation stages.
 * </p>
 */
public class Loader implements ResourceManagerReloadListener {

	Loader() {
		// Can be null when running datagenerators due to the unfortunate time we call this
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft != null) {
			ResourceManager manager = minecraft.getResourceManager();
			if (manager instanceof ReloadableResourceManager) {
				((ReloadableResourceManager) manager).registerReloadListener(this);
			}
		}
	}

	@Override
	public void onResourceManagerReload(ResourceManager manager) {
		Backend.refresh();

		var errorReporter = new ErrorReporter();
		ShaderSources sources = new ShaderSources(errorReporter, manager);

		Backend.LOGGER.info("Loaded all shader sources in " + sources.getLoadTime());

		FileResolution.run(errorReporter, sources);

		if (errorReporter.hasErrored()) {
			errorReporter.dump();
			throw new ShaderLoadingException("Failed to resolve all source files, see log for details");
		}

		sources.postResolve();

		Backend.LOGGER.info("Successfully resolved all source files.");

		FileResolution.checkAll(errorReporter);

		Backend.LOGGER.info("All shaders passed checks.");

		long compileStart = System.nanoTime();

		for (Material material : ComponentRegistry.materials) {
			for (StructType<?> structType : ComponentRegistry.structTypes) {
				for (VertexType vertexType : ComponentRegistry.vertexTypes) {
					for (ContextShader contextShader : ComponentRegistry.contextShaders) {
						var ctx = new InstancedArraysCompiler.Context(vertexType, material, structType.getInstanceShader(), contextShader);
						InstancedArraysCompiler.INSTANCE.getProgram(ctx);
					}
				}
			}
		}

		long compileEnd = System.nanoTime();

		Backend.LOGGER.info("Compiled all programs in " + StringUtil.formatTime(compileEnd - compileStart));

		ClientLevel world = Minecraft.getInstance().level;
		if (Backend.canUseInstancing(world)) {
			// TODO: looks like it might be good to have another event here
			InstancedRenderDispatcher.resetInstanceWorld(world);
			CrumblingRenderer.reset();
		}

	}
}
