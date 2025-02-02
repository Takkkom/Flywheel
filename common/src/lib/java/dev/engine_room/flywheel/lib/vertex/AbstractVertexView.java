package dev.engine_room.flywheel.lib.vertex;

public abstract class AbstractVertexView implements VertexView {
	protected long ptr;
	protected int vertexCount;

	@Override
	public long ptr() {
		return ptr;
	}

	@Override
	public void ptr(long ptr) {
		this.ptr = ptr;
	}

	@Override
	public int vertexCount() {
		return vertexCount;
	}

	@Override
	public void vertexCount(int vertexCount) {
		this.vertexCount = vertexCount;
	}
}
