package dev.engine_room.flywheel.backend.glsl;

public enum GlslVersion {
	V150(150),
	V330(330),
	V400(400),
	V410(410),
	V420(420),
	V430(430),
	V440(440),
	V450(450),
	V460(460),
	;

	public final int version;

	GlslVersion(int version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return Integer.toString(version);
	}
}
