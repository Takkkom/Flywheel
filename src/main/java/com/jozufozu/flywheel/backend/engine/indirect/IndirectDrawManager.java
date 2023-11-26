package com.jozufozu.flywheel.backend.engine.indirect;

import java.util.HashMap;
import java.util.Map;

import com.jozufozu.flywheel.api.event.RenderStage;
import com.jozufozu.flywheel.api.instance.Instance;
import com.jozufozu.flywheel.api.instance.InstanceType;
import com.jozufozu.flywheel.api.model.Model;
import com.jozufozu.flywheel.api.vertex.VertexType;
import com.jozufozu.flywheel.backend.engine.InstancerKey;
import com.jozufozu.flywheel.backend.engine.InstancerStorage;
import com.jozufozu.flywheel.lib.util.Pair;

public class IndirectDrawManager extends InstancerStorage<IndirectInstancer<?>> {
	public final Map<Pair<InstanceType<?>, VertexType>, IndirectCullingGroup<?>> renderLists = new HashMap<>();

	public void flush() {
		super.flush();

		for (IndirectCullingGroup<?> value : renderLists.values()) {
			value.beginFrame();
		}
	}

	public void invalidate() {
		super.invalidate();

		renderLists.values()
				.forEach(IndirectCullingGroup::delete);
		renderLists.clear();
	}

	public boolean hasStage(RenderStage stage) {
		for (var list : renderLists.values()) {
			if (list.hasStage(stage)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected <I extends Instance> IndirectInstancer<?> create(InstanceType<I> type) {
		return new IndirectInstancer<>(type);
	}

	@Override
	protected <I extends Instance> void add(InstancerKey<I> key, IndirectInstancer<?> instancer, Model model, RenderStage stage) {
		var meshes = model.getMeshes();
		for (var entry : meshes.entrySet()) {
			var material = entry.getKey();
			var mesh = entry.getValue();

			var indirectList = (IndirectCullingGroup<I>) renderLists.computeIfAbsent(Pair.of(key.type(), mesh.vertexType()), p -> new IndirectCullingGroup<>(p.first(), p.second()));

			indirectList.add((IndirectInstancer<I>) instancer, stage, material, mesh);

			break; // TODO: support multiple meshes per model
		}
	}
}
