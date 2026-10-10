package labymod_menu.common;

public final class CameraPose {
	private final float x;
	private final float y;
	private final float z;
	private final float yaw;
	private final float pitch;
	private final float roll;

	public CameraPose(float x, float y, float z, float yaw, float pitch, float roll) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
	}

	public float x() {
		return x;
	}

	public float y() {
		return y;
	}

	public float z() {
		return z;
	}

	public float yaw() {
		return yaw;
	}

	public float pitch() {
		return pitch;
	}

	public float roll() {
		return roll;
	}

	public static CameraPose interpolate(CameraPose start, CameraPose end, float progress) {
		return new CameraPose(
				AnimationMath.lerp(progress, start.x, end.x),
				AnimationMath.lerp(progress, start.y, end.y),
				AnimationMath.lerp(progress, start.z, end.z),
				AnimationMath.lerp(progress, start.yaw, end.yaw),
				AnimationMath.lerp(progress, start.pitch, end.pitch),
				AnimationMath.lerp(progress, start.roll, end.roll));
	}

	public float[] toArray() {
		return new float[] { x, y, z, yaw, pitch, roll };
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CameraPose)) {
			return false;
		}
		CameraPose pose = (CameraPose) other;
		return Float.compare(x, pose.x) == 0
				&& Float.compare(y, pose.y) == 0
				&& Float.compare(z, pose.z) == 0
				&& Float.compare(yaw, pose.yaw) == 0
				&& Float.compare(pitch, pose.pitch) == 0
				&& Float.compare(roll, pose.roll) == 0;
	}

	@Override
	public int hashCode() {
		int result = Float.floatToIntBits(x);
		result = 31 * result + Float.floatToIntBits(y);
		result = 31 * result + Float.floatToIntBits(z);
		result = 31 * result + Float.floatToIntBits(yaw);
		result = 31 * result + Float.floatToIntBits(pitch);
		result = 31 * result + Float.floatToIntBits(roll);
		return result;
	}

	@Override
	public String toString() {
		return "CameraPose[x=" + x + ", y=" + y + ", z=" + z
				+ ", yaw=" + yaw + ", pitch=" + pitch + ", roll=" + roll + "]";
	}
}
