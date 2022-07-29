package com.jozufozu.flywheel.backend.instancing.indirect;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.jozufozu.flywheel.api.instancer.InstancedPart;
import com.jozufozu.flywheel.api.instancer.Instancer;
import com.jozufozu.flywheel.api.instancer.InstancerFactory;
import com.jozufozu.flywheel.api.struct.StructType;
import com.jozufozu.flywheel.backend.instancing.AbstractInstancer;
import com.jozufozu.flywheel.core.model.Model;

public class IndirectFactory<D extends InstancedPart> implements InstancerFactory<D> {

	protected final Map<Model, InstancedModel<D>> models = new HashMap<>();
	protected final StructType<D> type;
	private final Consumer<InstancedModel<D>> creationListener;

	public IndirectFactory(StructType<D> type, Consumer<InstancedModel<D>> creationListener) {
		this.type = type;
		this.creationListener = creationListener;
	}

	@Override
	public Instancer<D> model(Model modelKey) {
		return models.computeIfAbsent(modelKey, this::createInstancer).getInstancer();
	}

	public int getInstanceCount() {
		return models.values()
				.stream()
				.map(InstancedModel::getInstancer)
				.mapToInt(AbstractInstancer::getInstanceCount)
				.sum();
	}

	public int getVertexCount() {
		return models.values()
				.stream()
				.mapToInt(InstancedModel::getVertexCount)
				.sum();
	}

	public void delete() {
		models.values().forEach(InstancedModel::delete);
		models.clear();
	}

	/**
	 * Clear all instance data without freeing resources.
	 */
	public void clear() {
		models.values()
				.stream()
				.map(InstancedModel::getInstancer)
				.forEach(AbstractInstancer::clear);
	}

	private InstancedModel<D> createInstancer(Model model) {
		var instancer = new InstancedModel<>(type, model);
		this.creationListener.accept(instancer);
		return instancer;
	}

//	private void bindInstanceAttributes(GlVertexArray vao) {
//		vao.bindAttributes(this.vbo, this.attributeBaseIndex, this.instanceFormat, 0L);
//
//		for (int i = 0; i < this.instanceFormat.getAttributeCount(); i++) {
//			vao.setAttributeDivisor(this.attributeBaseIndex + i, 1);
//		}
//	}
}
