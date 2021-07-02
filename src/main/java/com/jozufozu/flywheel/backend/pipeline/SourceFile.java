package com.jozufozu.flywheel.backend.pipeline;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jozufozu.flywheel.backend.ShaderSources;
import com.jozufozu.flywheel.backend.pipeline.parse.ShaderFunction;
import com.jozufozu.flywheel.backend.pipeline.span.ErrorSpan;
import com.jozufozu.flywheel.backend.pipeline.span.Span;
import com.jozufozu.flywheel.backend.pipeline.span.StringSpan;

import net.minecraft.util.ResourceLocation;

public class SourceFile {
	// #use "valid_namespace:valid/path_to_file.glsl"
	private static final Pattern includePattern = Pattern.compile("#use \"(\\w+:[\\w./]+)\"");

	// https://regexr.com/60n3d
	public static final Pattern functionDeclaration = Pattern.compile("(\\w+)\\s+(\\w+)\\s*\\(([\\w,\\s]*)\\)\\s*\\{");

	public static final Pattern versionDetector = Pattern.compile("#version[^\\n]*");

	public final ResourceLocation name;
	private final String source;
	private final ShaderSources parent;

	// function name -> function object
	private final ImmutableMap<String, ShaderFunction> functions;
	private final ImmutableList<ResourceLocation> includes;

	// Sections of the source that must be trimmed for compilation.
	private final List<Span> elisions = new ArrayList<>();

	public SourceFile(ShaderSources parent, ResourceLocation name, String source) {
		this.parent = parent;
		this.name = name;
		this.source = source;

		functions = parseFunctions();
		includes = parseIncludes();
	}

	public String getSource() {
		return source;
	}

	public ShaderSources getParent() {
		return parent;
	}

	public ImmutableMap<String, ShaderFunction> getFunctions() {
		return functions;
	}

	public ImmutableList<ResourceLocation> getIncludes() {
		return includes;
	}

	private ImmutableList<ResourceLocation> parseIncludes() {
		Matcher uses = includePattern.matcher(source);

		List<ResourceLocation> includes = new ArrayList<>();

		while (uses.find()) {
			Span use = Span.fromMatcher(this, uses);

			elisions.add(use); // we have to trim that later

			ResourceLocation loc = new ResourceLocation(uses.group(1)); // TODO: error gracefully

			includes.add(loc);
		}

		return ImmutableList.copyOf(includes);
	}

	private ImmutableMap<String, ShaderFunction> parseFunctions() {
		Matcher matcher = functionDeclaration.matcher(source);

		Map<String, ShaderFunction> functions = new HashMap<>();

		while (matcher.find()) {
			Span type = Span.fromMatcher(this, matcher, 1);
			Span name = Span.fromMatcher(this, matcher, 2);
			Span args = Span.fromMatcher(this, matcher, 3);

			int blockStart = matcher.end();
			int blockEnd = findEndOfBlock(blockStart);

			Span self;
			Span body;
			if (blockEnd > blockStart) {
				self = new StringSpan(this, matcher.start(), blockEnd + 1);
				body = new StringSpan(this, blockStart, blockEnd);
			} else {
				self = new ErrorSpan(this, matcher.start(), matcher.end());
				body = new ErrorSpan(this, blockStart);
			}

			ShaderFunction function = new ShaderFunction(self, type, name, args, body);

			functions.put(name.get(), function);
		}

		return ImmutableMap.copyOf(functions);
	}

	private int findEndOfBlock(int end) {
		char[] rest = source.substring(end)
				.toCharArray();

		int blockDepth = 0;
		for (int i = 0; i < rest.length; i++) {
			char ch = rest[i];

			if (ch == '{') blockDepth++;
			if (ch == '}') blockDepth--;

			if (blockDepth < 0) {
				return end + i;
			}
		}

		return -1;
	}

	public String printSource() {
		StringBuilder builder = new StringBuilder();

		builder.append("Source for shader '")
				.append(name)
				.append("':\n");
		int i = 1;
		for (String s : source.split("\n")) {
			builder.append(String.format("%1$4s: ", i++))
					.append(s)
					.append('\n');
		}

		return builder.toString();
	}

	public static Stream<String> lines(String s) {
		return new BufferedReader(new StringReader(s)).lines();
	}
}
