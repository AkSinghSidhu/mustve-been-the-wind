package com.windsmoke.client;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Injects the wind-displacement code into Minecraft's terrain shader source.
 *
 * <p>1.21.11 compiles shaders from source strings in {@code GlStateManager}.
 * The terrain vertex shader already receives section-local positions plus the
 * chunk origin, so we can build a world position there and animate vertices on
 * the GPU every frame.
 */
public final class WindShaderPatcher {
	private static final Logger LOGGER = LoggerFactory.getLogger("windsmoke");
	private static final Pattern POSITION = Pattern.compile(
		"vec3\\s+(pos)\\s*=\\s*Position\\b[^;]*;");
	private static final Pattern VERTEX_COLOR = Pattern.compile(
		"vertexColor\\s*=\\s*Color\\b[^;]*;");
	private static final Pattern SODIUM_INIT = Pattern.compile(
		"_vert_init\\s*\\(\\s*\\)\\s*;");
	private static final Pattern SODIUM_POSITION = Pattern.compile(
		"vec3\\s+(position)\\s*=\\s*_vert_position\\s*\\+\\s*translation\\s*;");
	private static final AtomicBoolean LOGGED = new AtomicBoolean();
	private static final AtomicBoolean LOGGED_SODIUM = new AtomicBoolean();

	private static final String HEADER = """
		uniform vec4 WindSmokeInfo;
		uniform vec4 WindSmokeWind;
		uniform vec4 WindSmokeCamera;

		float windsmoke_code(float a) {
		    return 254.0 - floor(a * 255.0 + 0.5);
		}

		float windsmoke_hash(vec3 p) {
		    p = fract(p * 0.1031);
		    p += dot(p, p.yzx + 33.33);
		    return fract((p.x + p.y) * p.z);
		}

		float windsmoke_noise(vec3 p) {
		    vec3 i = floor(p);
		    vec3 f = fract(p);
		    f = f * f * (3.0 - 2.0 * f);
		    float n000 = windsmoke_hash(i + vec3(0.0, 0.0, 0.0));
		    float n100 = windsmoke_hash(i + vec3(1.0, 0.0, 0.0));
		    float n010 = windsmoke_hash(i + vec3(0.0, 1.0, 0.0));
		    float n110 = windsmoke_hash(i + vec3(1.0, 1.0, 0.0));
		    float n001 = windsmoke_hash(i + vec3(0.0, 0.0, 1.0));
		    float n101 = windsmoke_hash(i + vec3(1.0, 0.0, 1.0));
		    float n011 = windsmoke_hash(i + vec3(0.0, 1.0, 1.0));
		    float n111 = windsmoke_hash(i + vec3(1.0, 1.0, 1.0));
		    float x00 = mix(n000, n100, f.x);
		    float x10 = mix(n010, n110, f.x);
		    float x01 = mix(n001, n101, f.x);
		    float x11 = mix(n011, n111, f.x);
		    return mix(mix(x00, x10, f.y), mix(x01, x11, f.y), f.z);
		}

		float windsmoke_fbm(vec3 p) {
		    float n = windsmoke_noise(p) * 0.62;
		    if (WindSmokeInfo.w < 0.5) {
		        return n;
		    }
		    n += windsmoke_noise(p * 2.03 + vec3(17.0, -9.0, 4.0)) * 0.28;
		    if (WindSmokeInfo.w < 1.5) {
		        return n;
		    }
		    return n + windsmoke_noise(p * 4.11 + vec3(-3.0, 11.0, 7.0)) * 0.10;
		}

		vec3 windsmoke_offset(vec3 world, float code) {
		    if (WindSmokeInfo.z != 4321.5 || code < 0.0 || code >= 64.0) {
		        return vec3(0.0);
		    }

		    float kind = floor(code / 16.0);
		    float w = clamp((code - kind * 16.0) / 15.0, 0.0, 1.0);
		    float strength = clamp(WindSmokeInfo.y, 0.0, 1.5);
		    if (strength <= 0.001) {
		        return vec3(0.0);
		    }

		    // Terrain vertices are tagged during chunk construction, so the
		    // distance limit has to be evaluated here. Skipping the noise work
		    // beyond the radius keeps far terrain effectively free.
		    if (WindSmokeCamera.w <= 0.0) {
		        return vec3(0.0);
		    }
		    float viewDistance = distance(world.xz, WindSmokeCamera.xz);
		    if (viewDistance >= WindSmokeCamera.w) {
		        return vec3(0.0);
		    }
		    float distanceFade = 1.0 - smoothstep(
		        max(0.0, WindSmokeCamera.w - 8.0),
		        WindSmokeCamera.w,
		        viewDistance
		    );

		    // Breeze is visible, gale is dramatically stronger. The motion is
		    // driven by hash noise and moving gust fronts, not a sine loop, so
		    // it never visibly repeats at a fixed period.
		    float drive = pow(strength, 0.65);
		    float t = WindSmokeInfo.x;

		    vec2 cell = floor(world.xz);
		    float seed = fract(sin(dot(cell, vec2(12.9898, 78.233))) * 43758.5453);
		    vec2 dir = normalize(WindSmokeWind.xy + vec2(0.0001, 0.0));
		    vec2 side = vec2(-dir.y, dir.x);
		    float along = dot(world.xz, dir);
		    float across = dot(world.xz, side);

		    float front = windsmoke_fbm(vec3(along * 0.065 - t * 0.55, across * 0.045, t * 0.11));
		    float gust = clamp(0.35 + 0.9 * smoothstep(0.18, 0.82, front), 0.0, 1.25);
		    float lagged = WindSmokeInfo.w < 1.5
		            ? 0.55
		            : windsmoke_fbm(vec3(
		                along * 0.065 - (t - 0.35) * 0.55,
		                across * 0.045 + 7.0,
		                (t - 0.35) * 0.11
		            ));
		    float swirl = WindSmokeInfo.w < 0.5
		            ? 0.5
		            : windsmoke_fbm(vec3(world.x * 0.55 + seed * 13.0, world.z * 0.55 - seed * 7.0, t * 0.75));
		    float flutter = WindSmokeInfo.w < 0.5
		            ? 0.0
		            : windsmoke_noise(vec3(world.x * 2.25 + seed * 9.0, world.z * 2.25, t * 2.2)) - 0.5;

		    if (kind > 0.5 && kind < 1.5) {
		        // Hanging objects are heavier and slower. Their pivot is the top
		        // (w = 0), so the body swings below it like a pendulum.
		        float swingField = windsmoke_fbm(vec3(seed * 11.0, seed * 5.0, t * 0.42));
		        float swingField2 = windsmoke_noise(vec3(seed * 7.0 + 4.0, t * 0.23, seed * 3.0));
		        float swing = (swingField - 0.5) * 1.7 + (swingField2 - 0.5) * 0.7;
		        float swingAmount = drive * gust * (0.11 + 0.20 * drive) * w;
		        vec2 xz = dir * swing * swingAmount;
		        xz += side * (windsmoke_noise(vec3(seed * 9.0, t * 0.65, 2.0)) - 0.5) * drive * 0.075 * w;
		        return vec3(xz.x, 0.0, xz.y) * distanceFade;
		    }

		    float stiffness = kind < 0.5 ? 1.0 : (kind < 2.5 ? 0.55 : 0.42);
		    float flutterScale = kind < 0.5 ? 1.0 : (kind < 2.5 ? 0.35 : 0.5);
		    float weight = kind < 2.5 ? w * w : w;
		    float bend = drive * gust * (0.12 + 0.20 * drive) * stiffness * weight
		            * (0.62 + 0.38 * swirl);
		    float lag = mix(0.65, 1.25, lagged);
		    bend *= lag;
		    float sideWave = flutter * drive * (0.10 + 0.09 * drive) * flutterScale * weight;
		    vec2 xz = dir * bend + side * sideWave;
		    return vec3(xz.x, -0.35 * dot(xz, xz), xz.y) * distanceFade;
		}
		""";

	private static final String BODY = """

		    float windsmokeCode = windsmoke_code(Color.a);
		    if (windsmokeCode >= 0.0 && windsmokeCode < 64.0) {
		        pos += windsmoke_offset(Position + vec3(ChunkPosition), windsmokeCode);
		    }
		""";

	private static final String COLOR_BODY = """

		    if (windsmokeCode >= 0.0 && windsmokeCode < 64.0) {
		        vertexColor.a = 1.0;
		    }
		""";

	private static final String SODIUM_INIT_BODY = """

	    float windsmokeCode = windsmoke_code(_vert_color.a);
	    if (windsmokeCode >= 0.0 && windsmokeCode < 64.0) {
	        _vert_color.a = 1.0;
	    }
	""";

	private static final String SODIUM_POSITION_BODY = """

	    // Sodium's u_RegionOffset is relative to the camera, so position is
	    // camera-relative. Wind noise and its distance fade need world space.
	    vec3 windsmokeWorld = position + WindSmokeCamera.xyz;
	    if (windsmokeCode >= 0.0 && windsmokeCode < 64.0) {
	        position += windsmoke_offset(windsmokeWorld, windsmokeCode);
	    }
	""";

	private WindShaderPatcher() {
	}

	public static String patch(String source) {
		if (source == null || source.contains("WindSmokeInfo")) {
			return source;
		}

		if (source.contains("_vert_init")
			&& source.contains("a_Color")
			&& source.contains("u_RegionOffset")) {
			return patchSodium(source);
		}

		if (!source.contains("in vec4 Color;") || !source.contains("ChunkPosition")) {
			return source;
		}

		Matcher position = POSITION.matcher(source);
		if (!position.find()) {
			return source;
		}

		Matcher vertexColor = VERTEX_COLOR.matcher(source);
		if (!vertexColor.find()) {
			return source;
		}

		int main = source.indexOf("void main()");
		if (main < 0 || position.start() < main) {
			return source;
		}

		try {
			StringBuilder out = new StringBuilder(source);

			// Insert from the end backwards so earlier offsets stay valid. The
			// Color input is read-only in GLSL, so restore alpha on the
			// writable vertexColor output instead.
			out.insert(vertexColor.end(), COLOR_BODY);
			out.insert(position.end(), BODY);
			out.insert(main, HEADER);

			if (LOGGED.compareAndSet(false, true)) {
				LOGGER.info("[windsmoke] patched terrain shader for GPU wind sway");
			}

			WindShaderUniforms.shadersChanged();
			return out.toString();
		} catch (RuntimeException exception) {
			LOGGER.warn("[windsmoke] could not patch terrain shader", exception);
			return source;
		}
	}

	private static String patchSodium(String source) {
		Matcher init = SODIUM_INIT.matcher(source);
		Matcher position = SODIUM_POSITION.matcher(source);
		int main = source.indexOf("void main()");

		if (!init.find() || !position.find() || main < 0 || position.start() < main) {
			return source;
		}

		try {
			StringBuilder out = new StringBuilder(source);

			// Sodium's vertex shader builds a camera-relative position from
			// u_RegionOffset and the section draw id. Decode the alpha tag
			// after _vert_init, restore the colour alpha, then displace the
			// camera-relative position before the MVP transform.
			out.insert(position.end(), SODIUM_POSITION_BODY);
			out.insert(init.end(), SODIUM_INIT_BODY);
			out.insert(main, HEADER);

			if (LOGGED_SODIUM.compareAndSet(false, true)) {
				LOGGER.info("[windsmoke] patched Sodium terrain shader for GPU wind sway");
			}

			WindShaderUniforms.shadersChanged();
			return out.toString();
		} catch (RuntimeException exception) {
			LOGGER.warn("[windsmoke] could not patch Sodium terrain shader", exception);
			return source;
		}
	}
}
