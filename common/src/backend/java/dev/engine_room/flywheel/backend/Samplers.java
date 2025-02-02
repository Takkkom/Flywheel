package dev.engine_room.flywheel.backend;

import dev.engine_room.flywheel.backend.gl.GlTextureUnit;

public class Samplers {
	public static final GlTextureUnit DIFFUSE = GlTextureUnit.T0;
	public static final GlTextureUnit OVERLAY = GlTextureUnit.T1;
	public static final GlTextureUnit LIGHT = GlTextureUnit.T2;
	public static final GlTextureUnit CRUMBLING = GlTextureUnit.T3;
	public static final GlTextureUnit INSTANCE_BUFFER = GlTextureUnit.T4;
	public static final GlTextureUnit LIGHT_LUT = GlTextureUnit.T5;
	public static final GlTextureUnit LIGHT_SECTIONS = GlTextureUnit.T6;
}
