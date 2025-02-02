package dev.engine_room.flywheel.backend.compile.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import dev.engine_room.flywheel.backend.compile.FlwPrograms;
import dev.engine_room.flywheel.backend.glsl.LoadError;
import dev.engine_room.flywheel.backend.glsl.LoadResult;
import dev.engine_room.flywheel.backend.glsl.error.ErrorBuilder;
import dev.engine_room.flywheel.lib.util.StringUtil;

public class CompilerStats {
	private final Marker marker;
	private long compileStart;

	private final Set<LoadError> loadErrors = new HashSet<>();
	private final List<FailedCompilation> shaderErrors = new ArrayList<>();
	private final List<String> programErrors = new ArrayList<>();

	private boolean errored = false;
	private int shaderCount = 0;
	private int programCount = 0;

	public CompilerStats(String marker) {
		this.marker = MarkerFactory.getMarker(marker);
	}

	public void start() {
		compileStart = System.nanoTime();
	}

	public void finish() {
		long compileEnd = System.nanoTime();
		var elapsed = StringUtil.formatTime(compileEnd - compileStart);

		FlwPrograms.LOGGER.info(marker, "Compiled %d programs (with %d link errors) and %d shaders (with %d compile errors) in %s".formatted(programCount, programErrors.size(), shaderCount, shaderErrors.size(), elapsed));
	}

	public boolean errored() {
		return errored;
	}

	public void emitErrorLog() {
		String out = "";

		if (!loadErrors.isEmpty()) {
			out += "\nErrors loading sources:\n" + loadErrors();
		}

		if (!shaderErrors.isEmpty()) {
			out += "\nShader compilation errors:\n" + compileErrors();
		}

		if (!programErrors.isEmpty()) {
			out += "\nProgram link errors:\n" + linkErrors();
		}

		FlwPrograms.LOGGER.error(marker, out);
	}

	private String compileErrors() {
		return shaderErrors.stream()
				.map(FailedCompilation::generateMessage)
				.collect(Collectors.joining("\n"));
	}

	private String linkErrors() {
		return String.join("\n", programErrors);
	}

	private String loadErrors() {
		return loadErrors.stream()
				.map(LoadError::generateMessage)
				.map(ErrorBuilder::build)
				.collect(Collectors.joining("\n"));
	}

	public void shaderResult(ShaderResult result) {
		if (result instanceof ShaderResult.Failure f) {
			shaderErrors.add(f.failure());
			errored = true;
		}
		shaderCount++;
	}

	public void linkResult(LinkResult linkResult) {
		if (linkResult instanceof LinkResult.Failure f) {
			programErrors.add(f.failure());
			errored = true;
		}
		programCount++;
	}

	public void loadResult(LoadResult loadResult) {
		if (loadResult instanceof LoadResult.Failure f) {
			loadErrors.add(f.error());
			errored = true;
		}
	}
}
