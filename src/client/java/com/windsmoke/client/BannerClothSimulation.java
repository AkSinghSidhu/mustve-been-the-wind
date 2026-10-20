package com.windsmoke.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.joml.Vector3f;

/**
 * Small position-based cloth simulation used by the banner flag.
 *
 * <p>The previous banner animation was a pair of sine waves. That made every
 * banner repeat the same motion forever, regardless of what the wind was
 * actually doing. This simulation instead integrates a grid of particles from
 * the live wind strength/direction, gravity, damping and a hash-based
 * turbulence field. The top row is pinned to the bar, and the cloth is kept in
 * front of the bar and pole by a positional constraint.
 *
 * <p>State is keyed by an explicit per-banner identity token, so adjacent
 * banners get independent motion and a banner keeps its shape while it is on
 * screen. Only banners near the camera are stepped, the number of fully
 * simulated banners is capped, and the renderer is called once per pattern
 * layer, so each cloth advances at most once per fixed timestep.
 */
final class BannerClothSimulation {
	static final int COLUMNS = 8;
	static final int ROWS = 16;
	static final int STRIDE = COLUMNS + 1;
	static final int POINTS = STRIDE * (ROWS + 1);
	static final float WIDTH = 20.0F;
	static final float HEIGHT = 40.0F;
	static final float FRONT_OFFSET = -2.3F;

	private static final float STEP_SECONDS = 1.0F / 60.0F;
	private static final long STEP_NANOS = 16_666_667L;
	private static final float GRAVITY = 130.0F;
	private static final float DAMPING = 0.99F;
	private static final float STIFFNESS = 0.9F;
	private static final int SOLVER_ITERATIONS = 5;
	private static final float POLE_RADIUS = 2.2F;
	private static final float BAR_RADIUS = 2.2F;
	private static final long STALE_NANOS = 60_000_000_000L;
	private static final Map<Integer, Cloth> CLOTHS = new HashMap<>();
	private static final Set<Integer> ACTIVE_THIS_FRAME = new HashSet<>();
	private static long lastCleanupNanos;
	private static int activeFrame = -1;

	private BannerClothSimulation() {
	}

	/**
	 * Called once at the start of each rendered frame. The active set is only a
	 * budget for the current frame; it is not simulation state.
	 */
	static void beginFrame(int frame) {
		if (frame != activeFrame) {
			activeFrame = frame;
			ACTIVE_THIS_FRAME.clear();
		}
	}

	/**
	 * Reserve a full-quality simulation slot for this banner. Pattern layers
	 * call this repeatedly for the same banner and must not consume extra slots.
	 */
	static boolean tryActivate(int token) {
		if (ACTIVE_THIS_FRAME.contains(token)) {
			return true;
		}
		if (ACTIVE_THIS_FRAME.size() >= WindSmokeConfig.maxBannerSimulations()) {
			return false;
		}
		ACTIVE_THIS_FRAME.add(token);
		return true;
	}

	static void clear() {
		CLOTHS.clear();
		ACTIVE_THIS_FRAME.clear();
		activeFrame = -1;
	}

	static Cloth acquire(int token, float baseY, float baseZ) {
		long now = System.nanoTime();
		if (now - lastCleanupNanos > 10_000_000_000L) {
			lastCleanupNanos = now;
			removeStale(now);
		}

		Cloth cloth = CLOTHS.get(token);
		if (cloth == null) {
			cloth = new Cloth(token, baseY, baseZ);
			CLOTHS.put(token, cloth);
		} else if (cloth.baseY != baseY || cloth.baseZ != baseZ) {
			cloth.reset(baseY, baseZ);
		}
		cloth.lastSeenNanos = now;
		return cloth;
	}

	private static void removeStale(long now) {
		Iterator<Map.Entry<Integer, Cloth>> iterator = CLOTHS.entrySet().iterator();
		while (iterator.hasNext()) {
			if (now - iterator.next().getValue().lastSeenNanos > STALE_NANOS) {
				iterator.remove();
			}
		}
	}

	static final class Cloth {
		private final int seed;
		private final float[] restX = new float[POINTS];
		private final float[] restY = new float[POINTS];
		private final float[] restZ = new float[POINTS];
		private final float[] x = new float[POINTS];
		private final float[] y = new float[POINTS];
		private final float[] z = new float[POINTS];
		private final float[] previousX = new float[POINTS];
		private final float[] previousY = new float[POINTS];
		private final float[] previousZ = new float[POINTS];
		private final float[] velocityX = new float[POINTS];
		private final float[] velocityY = new float[POINTS];
		private final float[] velocityZ = new float[POINTS];
		private final float[] normals = new float[POINTS * 3];
		private final int[] constraintA;
		private final int[] constraintB;
		private final float[] constraintRest;
		private final float[] constraintStiffness;

		private float baseY;
		private float baseZ;
		private float time;
		private long lastStepNanos;
		private long lastSeenNanos;
		private boolean standing;
		private boolean resting;
		private int geometryFrame = -1;
		private float barCenterY;
		private float barCenterZ;

		private Cloth(int seed, float baseY, float baseZ) {
			this.seed = seed;
			this.constraintA = new int[constraintCount()];
			this.constraintB = new int[constraintA.length];
			this.constraintRest = new float[constraintA.length];
			this.constraintStiffness = new float[constraintA.length];
			this.reset(baseY, baseZ);
		}

		private static int constraintCount() {
			int structural = COLUMNS * (ROWS + 1) + (COLUMNS + 1) * ROWS;
			int shear = 2 * COLUMNS * ROWS;
			int bend = (COLUMNS - 1) * (ROWS + 1) + (COLUMNS + 1) * (ROWS - 1);
			return structural + shear + bend;
		}

		private void reset(float newBaseY, float newBaseZ) {
			this.baseY = newBaseY;
			this.baseZ = newBaseZ;
			// Standing banners have the centre pole; wall banners do not. The
			// bar is the same box in both models, just at a different pivot.
			this.standing = baseY < -30.0F;
			this.barCenterY = baseY + 1.0F;
			this.barCenterZ = this.standing ? 0.0F : baseZ;
			int index = 0;

			for (int row = 0; row <= ROWS; row++) {
				for (int column = 0; column <= COLUMNS; column++) {
					int point = row * STRIDE + column;
					this.restX[point] = (column / (float)COLUMNS - 0.5F) * WIDTH;
					this.restY[point] = baseY + row / (float)ROWS * HEIGHT;
					this.restZ[point] = baseZ + FRONT_OFFSET;
					this.x[point] = this.restX[point];
					this.y[point] = this.restY[point];
					this.z[point] = this.restZ[point];
					this.previousX[point] = this.x[point];
					this.previousY[point] = this.y[point];
					this.previousZ[point] = this.z[point];
					this.velocityX[point] = 0.0F;
					this.velocityY[point] = 0.0F;
					this.velocityZ[point] = 0.0F;
				}
			}

			for (int row = 0; row <= ROWS; row++) {
				for (int column = 0; column <= COLUMNS; column++) {
					if (column < COLUMNS) {
						this.addConstraint(index++, column, row, column + 1, row, 1.0F);
					}
					if (row < ROWS) {
						this.addConstraint(index++, column, row, column, row + 1, 1.0F);
					}
					if (column < COLUMNS && row < ROWS) {
						this.addConstraint(index++, column, row, column + 1, row + 1, 0.55F);
						this.addConstraint(index++, column + 1, row, column, row + 1, 0.55F);
					}
					if (column + 2 <= COLUMNS) {
						this.addConstraint(index++, column, row, column + 2, row, 0.18F);
					}
					if (row + 2 <= ROWS) {
						this.addConstraint(index++, column, row, column, row + 2, 0.18F);
					}
				}
			}

			this.time = 0.0F;
			this.lastStepNanos = 0L;
			this.lastSeenNanos = System.nanoTime();
			this.resting = false;
			this.geometryFrame = -1;
		}

		private void addConstraint(int index, int columnA, int rowA, int columnB, int rowB, float stiffness) {
			int a = rowA * STRIDE + columnA;
			int b = rowB * STRIDE + columnB;
			float dx = this.restX[b] - this.restX[a];
			float dy = this.restY[b] - this.restY[a];
			float dz = this.restZ[b] - this.restZ[a];
			this.constraintA[index] = a;
			this.constraintB[index] = b;
			this.constraintRest[index] = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
			this.constraintStiffness[index] = stiffness;
		}

		void advance(Vector3f localWind, float strength) {
			long now = System.nanoTime();
			this.lastSeenNanos = now;
			this.resting = false;

			if (this.lastStepNanos == 0L) {
				this.lastStepNanos = now;
				return;
			}

			long elapsed = now - this.lastStepNanos;
			if (elapsed < STEP_NANOS) {
				return;
			}

			int steps = (int)Math.min(3L, elapsed / STEP_NANOS);
			if (steps < 1) {
				return;
			}
			this.lastStepNanos = elapsed > STEP_NANOS * 4L ? now : this.lastStepNanos + steps * STEP_NANOS;

			for (int step = 0; step < steps; step++) {
				this.step(localWind, strength);
			}
		}

		private void step(Vector3f localWind, float strength) {
			float drive = (float)Math.pow(Math.max(0.0F, Math.min(1.5F, strength)), 0.65F);
			float dt = STEP_SECONDS;
			this.time += dt;

			for (int point = 1; point < POINTS; point++) {
				this.previousX[point] = this.x[point];
				this.previousY[point] = this.y[point];
				this.previousZ[point] = this.z[point];
			}
			// The top row is pinned, but keeping its previous position in sync
			// also prevents a spurious velocity from being extracted below.
			for (int column = 0; column <= COLUMNS; column++) {
				this.previousX[column] = this.x[column];
				this.previousY[column] = this.y[column];
				this.previousZ[column] = this.z[column];
			}

			for (int row = 1; row <= ROWS; row++) {
				float vertical = row / (float)ROWS;
				// The top is held by the bar; the lower cloth catches much more
				// air. A softer falloff than v^2 lets the upper half ripple too.
				float weight = vertical;

				for (int column = 0; column <= COLUMNS; column++) {
					int point = row * STRIDE + column;
					float u = column / (float)COLUMNS;
					float gust = fbm(this.seed, u * 1.75F, vertical * 1.35F, this.time * 0.72F);
					float turbulence = (fbm(this.seed ^ 0x9E3779B9, u * 4.2F, vertical * 3.4F, this.time * 1.9F) - 0.5F) * 2.0F;

					// Use the local surface normal so wind that is already
					// parallel to the flag still makes it flutter instead of
					// translating the whole sheet sideways.
					int left = row * STRIDE + Math.max(0, column - 1);
					int right = row * STRIDE + Math.min(COLUMNS, column + 1);
					int top = Math.max(0, row - 1) * STRIDE + column;
					int bottom = Math.min(ROWS, row + 1) * STRIDE + column;
					float ux = this.x[right] - this.x[left];
					float uy = this.y[right] - this.y[left];
					float uz = this.z[right] - this.z[left];
					float vx = this.x[bottom] - this.x[top];
					float vy = this.y[bottom] - this.y[top];
					float vz = this.z[bottom] - this.z[top];
					float nx = vy * uz - vz * uy;
					float ny = vz * ux - vx * uz;
					float nz = vx * uy - vy * ux;
					float normalLength = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
					if (normalLength < 1.0E-5F) {
						nx = 0.0F;
						ny = 0.0F;
						nz = -1.0F;
					} else {
						nx /= normalLength;
						ny /= normalLength;
						nz /= normalLength;
					}

					float incidence = localWind.x * nx + localWind.z * nz;
					float pressure = drive * (90.0F + 230.0F * gust) * weight;
					float normalForce = incidence * pressure
						+ turbulence * drive * 165.0F * weight;
					float tangential = drive * (18.0F + 42.0F * gust) * weight;

					this.velocityX[point] += (nx * normalForce + localWind.x * tangential) * dt;
					this.velocityY[point] += (GRAVITY - drive * (16.0F + 38.0F * gust) * weight) * dt;
					this.velocityZ[point] += (nz * normalForce + localWind.z * tangential) * dt;

					this.velocityX[point] *= DAMPING;
					this.velocityY[point] *= DAMPING;
					this.velocityZ[point] *= DAMPING;

					this.x[point] += this.velocityX[point] * dt;
					this.y[point] += this.velocityY[point] * dt;
					this.z[point] += this.velocityZ[point] * dt;
				}
			}

			for (int iteration = 0; iteration < SOLVER_ITERATIONS; iteration++) {
				for (int constraint = 0; constraint < this.constraintA.length; constraint++) {
					int a = this.constraintA[constraint];
					int b = this.constraintB[constraint];
					float dx = this.x[b] - this.x[a];
					float dy = this.y[b] - this.y[a];
					float dz = this.z[b] - this.z[a];
					float distanceSquared = dx * dx + dy * dy + dz * dz;
					if (distanceSquared < 1.0E-8F) {
						continue;
					}

					float distance = (float)Math.sqrt(distanceSquared);
					float correction = (distance - this.constraintRest[constraint]) / distance
						* STIFFNESS * this.constraintStiffness[constraint];
					float weightA = a <= COLUMNS ? 0.0F : 1.0F;
					float weightB = b <= COLUMNS ? 0.0F : 1.0F;
					float totalWeight = weightA + weightB;
					if (totalWeight == 0.0F) {
						continue;
					}

					float shareA = weightA / totalWeight;
					float shareB = weightB / totalWeight;
					this.x[a] += dx * correction * shareA;
					this.y[a] += dy * correction * shareA;
					this.z[a] += dz * correction * shareA;
					this.x[b] -= dx * correction * shareB;
					this.y[b] -= dy * correction * shareB;
					this.z[b] -= dz * correction * shareB;
				}

				// Keep the attachment edge exactly on the bar.
				for (int column = 0; column <= COLUMNS; column++) {
					this.x[column] = this.restX[column];
					this.y[column] = this.restY[column];
					this.z[column] = this.restZ[column];
				}
				// The cloth is allowed to slide around the pole and bar instead
				// of being stopped by a flat invisible wall. That is what makes
				// wind from behind wrap the flag around the stick.
				this.resolveCollisions();
			}

			for (int point = STRIDE; point < POINTS; point++) {
				this.velocityX[point] = (this.x[point] - this.previousX[point]) / dt;
				this.velocityY[point] = (this.y[point] - this.previousY[point]) / dt;
				this.velocityZ[point] = (this.z[point] - this.previousZ[point]) / dt;
			}
		}

		/**
		 * Put the cloth back on its undeformed attachment plane. Distant banners
		 * use this instead of paying for a full simulation, and abandoned
		 * simulation state is not allowed to carry into a later frame.
		 */
		void rest() {
			if (this.resting) {
				return;
			}
			System.arraycopy(this.restX, 0, this.x, 0, POINTS);
			System.arraycopy(this.restY, 0, this.y, 0, POINTS);
			System.arraycopy(this.restZ, 0, this.z, 0, POINTS);
			System.arraycopy(this.restX, 0, this.previousX, 0, POINTS);
			System.arraycopy(this.restY, 0, this.previousY, 0, POINTS);
			System.arraycopy(this.restZ, 0, this.previousZ, 0, POINTS);
			java.util.Arrays.fill(this.velocityX, 0.0F);
			java.util.Arrays.fill(this.velocityY, 0.0F);
			java.util.Arrays.fill(this.velocityZ, 0.0F);
			this.lastStepNanos = 0L;
			this.resting = true;
			this.geometryFrame = -1;
		}

		/**
		 * Compute per-vertex normals at most once per rendered frame. All pattern
		 * layers for this banner then reuse the same position and normal arrays.
		 */
		void prepareGeometry(int frame) {
			if (this.geometryFrame == frame) {
				return;
			}
			this.geometryFrame = frame;

			for (int row = 0; row <= ROWS; row++) {
				for (int column = 0; column <= COLUMNS; column++) {
					int left = Math.max(0, column - 1);
					int right = Math.min(COLUMNS, column + 1);
					int top = Math.max(0, row - 1);
					int bottom = Math.min(ROWS, row + 1);
					int index = row * STRIDE + column;
					int leftIndex = row * STRIDE + left;
					int rightIndex = row * STRIDE + right;
					int topIndex = top * STRIDE + column;
					int bottomIndex = bottom * STRIDE + column;

					float ux = this.x[rightIndex] - this.x[leftIndex];
					float uy = this.y[rightIndex] - this.y[leftIndex];
					float uz = this.z[rightIndex] - this.z[leftIndex];
					float vx = this.x[bottomIndex] - this.x[topIndex];
					float vy = this.y[bottomIndex] - this.y[topIndex];
					float vz = this.z[bottomIndex] - this.z[topIndex];

					float nx = vy * uz - vz * uy;
					float ny = vz * ux - vx * uz;
					float nz = vx * uy - vy * ux;
					float length = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
					if (length < 1.0E-5F) {
						nx = 0.0F;
						ny = 0.0F;
						nz = -1.0F;
					} else {
						nx /= length;
						ny /= length;
						nz /= length;
					}

					int normalIndex = index * 3;
					this.normals[normalIndex] = nx;
					this.normals[normalIndex + 1] = ny;
					this.normals[normalIndex + 2] = nz;
				}
			}
		}

		float[] xs() {
			return this.x;
		}

		float[] ys() {
			return this.y;
		}

		float[] zs() {
			return this.z;
		}

		float[] normals() {
			return this.normals;
		}

		private void resolveCollisions() {
			for (int point = STRIDE; point < POINTS; point++) {
				if (this.standing && this.y[point] > -42.5F) {
					this.resolvePole(point);
				}
				if (this.x[point] > -10.5F && this.x[point] < 10.5F) {
					this.resolveBar(point);
				}
			}
		}

		private void resolvePole(int point) {
			float dx = this.x[point];
			float dz = this.z[point];
			float distanceSquared = dx * dx + dz * dz;
			if (distanceSquared >= POLE_RADIUS * POLE_RADIUS) {
				return;
			}

			float distance = (float)Math.sqrt(distanceSquared);
			if (distance < 1.0E-4F) {
				dx = 0.0F;
				dz = -1.0F;
				distance = 1.0F;
			}
			this.x[point] = dx / distance * POLE_RADIUS;
			this.z[point] = dz / distance * POLE_RADIUS;
		}

		private void resolveBar(int point) {
			float dy = this.y[point] - this.barCenterY;
			float dz = this.z[point] - this.barCenterZ;
			float distanceSquared = dy * dy + dz * dz;
			if (distanceSquared >= BAR_RADIUS * BAR_RADIUS) {
				return;
			}

			float distance = (float)Math.sqrt(distanceSquared);
			if (distance < 1.0E-4F) {
				dy = -1.0F;
				dz = 0.0F;
				distance = 1.0F;
			}
			this.y[point] = this.barCenterY + dy / distance * BAR_RADIUS;
			this.z[point] = this.barCenterZ + dz / distance * BAR_RADIUS;
		}

	}

	private static float fbm(long seed, float x, float y, float z) {
		return noise(seed, x, y, z) * 0.65F
			+ noise(seed ^ 0x51ED270BL, x * 2.13F + 13.7F, y * 2.13F - 7.1F, z * 2.13F + 3.3F) * 0.35F;
	}

	private static float noise(long seed, float x, float y, float z) {
		int x0 = (int)Math.floor(x);
		int y0 = (int)Math.floor(y);
		int z0 = (int)Math.floor(z);
		float tx = smooth(x - x0);
		float ty = smooth(y - y0);
		float tz = smooth(z - z0);
		int seedLow = (int)seed;
		int seedHigh = (int)(seed >>> 32);

		float c000 = hash(seedLow, seedHigh, x0, y0, z0);
		float c100 = hash(seedLow, seedHigh, x0 + 1, y0, z0);
		float c010 = hash(seedLow, seedHigh, x0, y0 + 1, z0);
		float c110 = hash(seedLow, seedHigh, x0 + 1, y0 + 1, z0);
		float c001 = hash(seedLow, seedHigh, x0, y0, z0 + 1);
		float c101 = hash(seedLow, seedHigh, x0 + 1, y0, z0 + 1);
		float c011 = hash(seedLow, seedHigh, x0, y0 + 1, z0 + 1);
		float c111 = hash(seedLow, seedHigh, x0 + 1, y0 + 1, z0 + 1);

		float x00 = c000 + (c100 - c000) * tx;
		float x10 = c010 + (c110 - c010) * tx;
		float x01 = c001 + (c101 - c001) * tx;
		float x11 = c011 + (c111 - c011) * tx;
		float y0f = x00 + (x10 - x00) * ty;
		float y1f = x01 + (x11 - x01) * ty;
		return y0f + (y1f - y0f) * tz;
	}

	private static float smooth(float value) {
		return value * value * (3.0F - 2.0F * value);
	}

	private static float hash(int seedLow, int seedHigh, int x, int y, int z) {
		int hash = seedLow;
		hash = hash * 31 + seedHigh;
		hash = hash * 31 + x;
		hash = hash * 31 + y;
		hash = hash * 31 + z;
		hash ^= hash >>> 16;
		hash *= 0x7FEB352D;
		hash ^= hash >>> 15;
		hash *= 0x846CA68B;
		hash ^= hash >>> 16;
		return (hash & 0xFFFFFF) / 16777215.0F;
	}
}
