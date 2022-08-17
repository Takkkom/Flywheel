package com.jozufozu.flywheel.mixin;

import java.nio.ByteBuffer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.jozufozu.flywheel.extension.BufferBuilderExtension;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements BufferBuilderExtension {
	@Shadow
	private ByteBuffer buffer;

	@Shadow
	private int nextElementByte;

	@Shadow
	private int vertices;

	@Shadow
	@Nullable
	private VertexFormatElement currentElement;

	@Shadow
	private int elementIndex;

	@Shadow
	private VertexFormat format;

	@Shadow
	private VertexFormat.Mode mode;

	@Shadow
	private boolean building;

	@Shadow
	private void ensureCapacity(int increaseAmount) {
	}

	@Override
	public void flywheel$freeBuffer() {
		if (this.buffer != null) {
			MemoryUtil.memFree(this.buffer);
			this.buffer = null;
		}
	}

	@Override
	public void flywheel$injectForRender(@NotNull ByteBuffer buffer, @NotNull VertexFormat format, int vertexCount) {
		this.building = true;
		this.mode = VertexFormat.Mode.QUADS;

		this.buffer = buffer;
		this.format = format;
		this.vertices = vertexCount;

		this.currentElement = this.format.getElements().get(0);
		this.elementIndex = 0;
	}
}
