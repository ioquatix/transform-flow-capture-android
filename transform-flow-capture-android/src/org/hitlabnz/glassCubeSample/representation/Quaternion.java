package org.hitlabnz.glassCubeSample.representation;

import android.hardware.SensorManager;

/**
 * 
 * Quaternion class which extends a simple Vector4f to allow elegant representation of rotations in the 3D space.
 * 
 * Quaternions basically are like rotation vectors, that they define a rotation axis (x,y,z) and an angle (w) that the
 * object will be rotated around that axis.
 * 
 * @author Leigh Beattie, Alexander Pacha
 * 
 */
public class Quaternion extends Vector4f {

	/**
	 * Serialisation UID
	 */
	private static final long serialVersionUID = 6709866838801785550L;

	/**
	 * This matrix contains the quaternion in the homogenised rotation matrix representation (4x4)
	 */
	private Matrixf4x4 matrix;

	/**
	 * This variable is used to synchronise the matrix with the current quaternion values. If someone has changed the
	 * quaternion numbers then the matrix will need to be updated.
	 * To save on processing we only really want to update the matrix when someone wants to fetch it, instead of
	 * whenever someone sets a quaternion value.
	 */
	private boolean dirty = false;

	/**
	 * Initialises a new instance of the Quaternion class with the identity quaternion.
	 */
	public Quaternion() {
		super();
		matrix = new Matrixf4x4();
		loadIdentityQuat();
	}

	/**
	 * Normalise this Quaternion into a unity Quaternion.
	 */
	public void normalise() {
		this.dirty = true;
		float mag = (float) Math.sqrt(getW() * getW() + getX() * getX() + getY() * getY() + getZ() * getZ());
		setW(getW() / mag);
		setX(getX() / mag);
		setY(getY() / mag);
		setZ(getZ() / mag);
	}

	@Override
	public void normalize() {
		normalise();
	}

	/**
	 * Copies the values from the given quaternion to this one
	 * 
	 * @param quat The quaternion to copy from
	 */
	public void set(Quaternion quat) {
		this.dirty = true;
		setX(quat.getX());
		setY(quat.getY());
		setZ(quat.getZ());
		setW(quat.getW());
	}

	/**
	 * Multiply this quaternion by the input quaternion and store the result in the out quaternion
	 * 
	 * @param input
	 * @param output
	 */
	// Stack variables
	Vector4f inputCopy = new Vector4f();

	public void multiplyByQuat(Quaternion input, Quaternion output) {

		if (input != output) {
			output.points[3] = (points[3] * input.points[3] - points[0] * input.points[0] - points[1] * input.points[1] - points[2]
					* input.points[2]); // w = w1w2 - x1x2 - y1y2 - z1z2
			output.points[0] = (points[3] * input.points[0] + points[0] * input.points[3] + points[1] * input.points[2] - points[2]
					* input.points[1]); // x = w1x2 + x1w2 + y1z2 - z1y2
			output.points[1] = (points[3] * input.points[1] + points[1] * input.points[3] + points[2] * input.points[0] - points[0]
					* input.points[2]); // y = w1y2 + y1w2 + z1x2 - x1z2
			output.points[2] = (points[3] * input.points[2] + points[2] * input.points[3] + points[0] * input.points[1] - points[1]
					* input.points[0]); // z = w1z2 + z1w2 + x1y2 - y1x2
		} else {
			inputCopy.points[0] = (input.points[0]);
			inputCopy.points[1] = (input.points[1]);
			inputCopy.points[2] = (input.points[2]);
			inputCopy.points[3] = (input.points[3]);

			output.points[3] = (points[3] * inputCopy.points[3] - points[0] * inputCopy.points[0] - points[1]
					* inputCopy.points[1] - points[2] * inputCopy.points[2]); // w = w1w2 - x1x2 - y1y2 - z1z2
			output.points[0] = (points[3] * inputCopy.points[0] + points[0] * inputCopy.points[3] + points[1]
					* inputCopy.points[2] - points[2] * inputCopy.points[1]); // x = w1x2 + x1w2 + y1z2 - z1y2
			output.points[1] = (points[3] * inputCopy.points[1] + points[1] * inputCopy.points[3] + points[2]
					* inputCopy.points[0] - points[0] * inputCopy.points[2]); // y = w1y2 + y1w2 + z1x2 - x1z2
			output.points[2] = (points[3] * inputCopy.points[2] + points[2] * inputCopy.points[3] + points[0]
					* inputCopy.points[1] - points[1] * inputCopy.points[0]); // z = w1z2 + z1w2 + x1y2 - y1x2
		}

		output.dirty = true;
	}

	/**
	 * Multiply this quaternion by the input quaternion and store the result in the out quaternion
	 * 
	 * @param input
	 * @param output
	 */
	Quaternion bufferQuaternion;

	public void multiplyByQuat(Quaternion input) {
		if (bufferQuaternion == null)
			bufferQuaternion = new Quaternion();
		this.dirty = true;
		bufferQuaternion.copyVec4(this);
		multiplyByQuat(input, bufferQuaternion);
		this.copyVec4(bufferQuaternion);
		// float W = (getW() * input.getW() - getX() * input.getX() - getY() * input.getY() - getZ() * input.getZ());
		// //w = w1w2 - x1x2 - y1y2 - z1z2
		// float X = (getW() * input.getX() + getX() * input.getW() + getY() * input.getZ() - getZ() * input.getY());
		// //x = w1x2 + x1w2 + y1z2 - z1y2
		// float Y = (getW() * input.getY() + getY() * input.getW() + getZ() * input.getX() - getX() * input.getZ());
		// //y = w1y2 + y1w2 + z1x2 - x1z2
		// float Z = (getW() * input.getZ() + getZ() * input.getW() + getX() * input.getY() - getY() * input.getX());
		// //z = w1z2 + z1w2 + x1y2 - y1x2
		// setW(W);
		// setX(X);
		// setY(Y);
		// setZ(Z);
	}

	public void multiplyByScalar(float scalar) {
		this.dirty = true;
		setX(getX() * scalar);
		setY(getY() * scalar);
		setZ(getZ() * scalar);
		setW(getW() * scalar);
	}

	/**
	 * Add a quaternion to this quaternion
	 * 
	 * @param input The quaternion that you want to add to this one
	 */
	public void addQuat(Quaternion input) {
		this.dirty = true;
		addQuat(input, this);
	}

	/**
	 * Add this quaternion and another quaternion together and store the result in the output quaternion
	 * 
	 * @param input The quaternion you want added to this quaternion
	 * @param output The quaternion you want to store the output in.
	 */
	public void addQuat(Quaternion input, Quaternion output) {
		output.setX(getX() + input.getX());
		output.setY(getY() + input.getY());
		output.setZ(getZ() + input.getZ());
		output.setW(getW() + input.getW());
	}

	/**
	 * Subtract a quaternion to this quaternion
	 * 
	 * @param input The quaternion that you want to subtracted from this one
	 */
	public void subQuat(Quaternion input) {
		this.dirty = true;
		subQuat(input, this);
	}

	/**
	 * Subtract another quaternion from this quaternion and store the result in the output quaternion
	 * 
	 * @param input The quaternion you want subtracted from this quaternion
	 * @param output The quaternion you want to store the output in.
	 */
	public void subQuat(Quaternion input, Quaternion output) {
		output.setX(getX() - input.getX());
		output.setY(getY() - input.getY());
		output.setZ(getZ() - input.getZ());
		output.setW(getW() - input.getW());
	}

	private void convertQuatToMatrix() {
		float x = points[0];
		float y = points[1];
		float z = points[2];
		float w = points[3];

		matrix.setX0(1 - 2 * (y * y) - 2 * (z * z)); // 1 - 2y2 - 2z2
		matrix.setX1(2 * (x * y) + 2 * (w * z)); // 2xy - 2wz
		matrix.setX2(2 * (x * z) - 2 * (w * y)); // 2xz + 2wy
		matrix.setX3(0);
		matrix.setY0(2 * (x * y) - 2 * (w * z)); // 2xy + 2wz
		matrix.setY1(1 - 2 * (x * x) - 2 * (z * z)); // 1 - 2x2 - 2z2
		matrix.setY2(2 * (y * z) + 2 * (w * x)); // 2yz + 2wx
		matrix.setY3(0);
		matrix.setZ0(2 * (x * z) + 2 * (w * y)); // 2xz + 2wy
		matrix.setZ1(2 * (y * z) - 2 * (w * x)); // 2yz - 2wx
		matrix.setZ2(1 - 2 * (x * x) - 2 * (y * y)); // 1 - 2x2 - 2y2
		matrix.setZ3(0);
		matrix.setW0(0);
		matrix.setW1(0);
		matrix.setW2(0);
		matrix.setW3(1);
	}

	/**
	 * Returns a column major matrix[16] of the rotation matrix for this quaternion.
	 * 
	 * @return float[16] matrix
	 */
	private float[] toMatrixColMajor() {

		float x = points[0];
		float y = points[1];
		float z = points[2];
		float w = points[3];

		float[] mat = matrix.getMatrix();

		mat[0] = 1 - 2 * (y * y) - 2 * (z * z); // 1 - 2y2 - 2z2
		mat[1] = 2 * (x * y) + 2 * (w * z); // 2xy - 2wz
		mat[2] = 2 * (x * z) - 2 * (w * y); // 2xz + 2wy
		mat[3] = 0;
		mat[4] = 2 * (x * y) - 2 * (w * z); // 2xy + 2wz
		mat[5] = 1 - 2 * (x * x) - 2 * (z * z); // 1 - 2x2 - 2z2
		mat[6] = 2 * (y * z) + 2 * (w * x); // 2yz + 2wx
		mat[7] = 0;
		mat[8] = 2 * (x * z) + 2 * (w * y); // 2xz + 2wy
		mat[9] = 2 * (y * z) - 2 * (w * x); // 2yz - 2wx
		mat[10] = 1 - 2 * (x * x) - 2 * (y * y); // 1 - 2x2 - 2y2
		mat[11] = 0;
		mat[12] = 0;
		mat[13] = 0;
		mat[14] = 0;
		mat[15] = 1;

		return mat;
	}

	/**
	 * Retuns a row major version of the rotation matrix for this quaternion.
	 * 
	 * @return float[16] matrix
	 */
	private float[] toMatrixRowMajor() {

		float x = getX();
		float y = getY();
		float z = getZ();
		float w = getW();

		float[] mat = matrix.getMatrix();

		if (mat.length == 16) {
			mat[0] = 1 - 2 * (y * y) - 2 * (z * z); // 1 - 2y2 - 2z2
			mat[4] = 2 * (x * y) + 2 * (w * z); // 2xy - 2wz
			mat[8] = 2 * (x * z) - 2 * (w * y); // 2xz + 2wy
			mat[12] = 0;
			mat[1] = 2 * (x * y) - 2 * (w * z); // 2xy + 2wz
			mat[5] = 1 - 2 * (x * x) - 2 * (z * z); // 1 - 2x2 - 2z2
			mat[9] = 2 * (y * z) + 2 * (w * x); // 2yz + 2wx
			mat[13] = 0;
			mat[2] = 2 * (x * z) + 2 * (w * y); // 2xz + 2wy
			mat[6] = 2 * (y * z) - 2 * (w * x); // 2yz - 2wx
			mat[10] = 1 - 2 * (x * x) - 2 * (y * y); // 1 - 2x2 - 2y2
			mat[14] = 0;
			mat[3] = 0;
			mat[7] = 0;
			mat[11] = 0;
			mat[15] = 1;
		}

		return mat;
	}

	/**
	 * Get an axis angle representation of this quaternion.
	 * 
	 * @param output Vector4f axis angle.
	 */
	public void toAxisAngle(Vector4f output) {
		if (getW() > 1)
			normalise(); // if w>1 acos and sqrt will produce errors, this cant happen if quaternion is normalised
		float angle = 2 * (float) Math.acos(getW());
		float x;
		float y;
		float z;

		float s = (float) Math.sqrt(1 - getW() * getW()); // assuming quaternion normalised then w is less than 1, so
															// term always positive.
		if (s < 0.001) { // test to avoid divide by zero, s is always positive due to sqrt
			// if s close to zero then direction of axis not important
			x = points[0]; // if it is important that axis is normalised then replace with x=1; y=z=0;
			y = points[1];
			z = points[2];
		} else {
			x = points[0] / s; // normalise axis
			y = points[1] / s;
			z = points[2] / s;
		}

		output.points[0] = x;
		output.points[1] = y;
		output.points[2] = z;
		output.points[3] = angle;
	}

	/**
	 * Returns the heading, attitude and bank of this quaternion as euler angles in the double array respectively
	 * 
	 * @return An array of size 3 containing the euler angles for this quaternion
	 */
	public double[] toEulerAngles() {
		double[] ret = new double[3];

		ret[0] = Math.atan2(2 * points[1] * getW() - 2 * points[0] * points[2], 1 - 2 * (points[1] * points[1]) - 2
				* (points[2] * points[2])); // atan2(2*qy*qw-2*qx*qz , 1 - 2*qy2 - 2*qz2)
		ret[1] = Math.asin(2 * points[0] * points[1] + 2 * points[2] * getW()); // asin(2*qx*qy + 2*qz*qw)
		ret[2] = Math.atan2(2 * points[0] * getW() - 2 * points[1] * points[2], 1 - 2 * (points[0] * points[0]) - 2
				* (points[2] * points[2])); // atan2(2*qx*qw-2*qy*qz , 1 - 2*qx2 - 2*qz2)

		return ret;
	}

	/**
	 * Sets the quaternion to an identity quaternion of 0,0,0,1.
	 */
	public void loadIdentityQuat() {
		this.dirty = true;
		setX(0);
		setY(0);
		setZ(0);
		setW(1);
	}

	@Override
	public String toString() {
		return String.format("{X: %.2f, Y: %.2f, Z: %.2f, W: %.2f}", getX(), getY(), getZ(), getW());
	}

	/**
	 * This is an internal method used to build a quaternion from a rotation matrix and then sets the current quaternion
	 * from that matrix.
	 * 
	 */
	private void generateQuaternionFromMatrix() {

		float qx;
		float qy;
		float qz;
		float qw;

		float[] mat = matrix.getMatrix();
		int[] indices = null;

		if (this.matrix.size() == 16) {
			if (this.matrix.isColumnMajor())
				indices = Matrixf4x4.matIndCol16_3x3;
			else
				indices = Matrixf4x4.matIndRow16_3x3;
		} else {
			if (this.matrix.isColumnMajor())
				indices = Matrixf4x4.matIndCol9_3x3;
			else
				indices = Matrixf4x4.matIndRow9_3x3;
		}

		int m00 = indices[0];
		int m01 = indices[1];
		int m02 = indices[2];

		int m10 = indices[3];
		int m11 = indices[4];
		int m12 = indices[5];

		int m20 = indices[6];
		int m21 = indices[7];
		int m22 = indices[8];

		float tr = mat[m00] + mat[m11] + mat[m22];
		if (tr > 0) {
			float S = (float) Math.sqrt(tr + 1.0) * 2; // S=4*qw
			qw = 0.25f * S;
			qx = (mat[m21] - mat[m12]) / S;
			qy = (mat[m02] - mat[m20]) / S;
			qz = (mat[m10] - mat[m01]) / S;
		} else if ((mat[m00] > mat[m11]) & (mat[m00] > mat[m22])) {
			float S = (float) Math.sqrt(1.0 + mat[m00] - mat[m11] - mat[m22]) * 2; // S=4*qx
			qw = (mat[m21] - mat[m12]) / S;
			qx = 0.25f * S;
			qy = (mat[m01] + mat[m10]) / S;
			qz = (mat[m02] + mat[m20]) / S;
		} else if (mat[m11] > mat[m22]) {
			float S = (float) Math.sqrt(1.0 + mat[m11] - mat[m00] - mat[m22]) * 2; // S=4*qy
			qw = (mat[m02] - mat[m20]) / S;
			qx = (mat[m01] + mat[m10]) / S;
			qy = 0.25f * S;
			qz = (mat[m12] + mat[m21]) / S;
		} else {
			float S = (float) Math.sqrt(1.0 + mat[m22] - mat[m00] - mat[m11]) * 2; // S=4*qz
			qw = (mat[m10] - mat[m01]) / S;
			qx = (mat[m02] + mat[m20]) / S;
			qy = (mat[m12] + mat[m21]) / S;
			qz = 0.25f * S;
		}

		setX(qx);
		setY(qy);
		setZ(qz);
		setW(qw);
	}

	/**
	 * You can set the values for this quaternion based off a rotation matrix. If the matrix you supply is not a
	 * rotation matrix this will fail. You MUST provide a 4x4 matrix.
	 * 
	 * @param matrix A column major rotation matrix
	 */
	public void setColumnMajor(float[] matrix) {

		this.matrix.setMatrix(matrix);
		this.matrix.setColumnMajor(true);

		generateQuaternionFromMatrix();
	}

	/**
	 * You can set the values for this quaternion based off a rotation matrix. If the matrix you supply is not a
	 * rotation matrix this will fail.
	 * 
	 * @param matrix A column major rotation matrix
	 */
	public void setRowMajor(float[] matrix) {

		this.matrix.setMatrix(matrix);
		this.matrix.setColumnMajor(false);

		generateQuaternionFromMatrix();
	}

	/**
	 * Set this quaternion from axis angle values. All rotations are in degrees.
	 * 
	 * @param x The rotation around the x axis
	 * @param y The rotation around the y axis
	 * @param z The rotation around the z axis
	 */
	public void setEulerAngle(float azimuth, float pitch, float roll) {

		double heading = Math.toRadians(roll);
		double attitude = Math.toRadians(pitch);
		double bank = Math.toRadians(azimuth);

		double c1 = Math.cos(heading / 2);
		double s1 = Math.sin(heading / 2);
		double c2 = Math.cos(attitude / 2);
		double s2 = Math.sin(attitude / 2);
		double c3 = Math.cos(bank / 2);
		double s3 = Math.sin(bank / 2);
		double c1c2 = c1 * c2;
		double s1s2 = s1 * s2;
		setW((float) (c1c2 * c3 - s1s2 * s3));
		setX((float) (c1c2 * s3 + s1s2 * c3));
		setY((float) (s1 * c2 * c3 + c1 * s2 * s3));
		setZ((float) (c1 * s2 * c3 - s1 * c2 * s3));

		dirty = true;
	}

	/**
	 * Rotation is in degrees. Set this quaternion from the supplied axis angle.
	 * 
	 * @param vec The vector of rotation
	 * @param rot The angle of rotation around that vector.
	 */
	public void setAxisAngle(Vector3f vec, float rot) {
		double s = Math.sin(Math.toRadians(rot / 2));
		setX(vec.getX() * (float) s);
		setY(vec.getY() * (float) s);
		setZ(vec.getZ() * (float) s);
		setW((float) Math.cos(Math.toRadians(rot / 2)));

		dirty = true;
	}

	public Matrixf4x4 getMatrix4x4() {
		// toMatrixColMajor();
		if (dirty) {
			convertQuatToMatrix();
			dirty = false;
		}
		return this.matrix;
	}

	/**
	 * Get a linear interpolation between this quaternion and the input quaternion, storing the result in the output
	 * quaternion.
	 * 
	 * @param input The quaternion to be slerped with this quaternion.
	 * @param output The quaternion to store the result in.
	 * @param t The ratio between the two quaternions where 0 <= t <= 1.0 . Increase value of t will bring rotation
	 *            closer to the input quaternion.
	 */
	public void slerp(Quaternion input, Quaternion output, float t) {
		// Calculate angle between them.
		// double cosHalftheta = this.dotProduct(input);
		Quaternion bufferQuat = null;
		float cosHalftheta = this.dotProduct(input);

		if (cosHalftheta < 0) {
			bufferQuat = new Quaternion();
			cosHalftheta = -cosHalftheta;
			bufferQuat.points[0] = (-input.points[0]);
			bufferQuat.points[1] = (-input.points[1]);
			bufferQuat.points[2] = (-input.points[2]);
			bufferQuat.points[3] = (-input.points[3]);
		} else {
			bufferQuat = input;
		}
		/**
		 * if(dot < 0.95f){ double angle = Math.acos(dot); double ratioA = Math.sin((1 - t) * angle); double ratioB =
		 * Math.sin(t * angle); double divisor = Math.sin(angle);
		 * 
		 * //Calculate Quaternion output.setW((float)((this.getW() * ratioA + input.getW() * ratioB)/divisor));
		 * output.setX((float)((this.getX() * ratioA + input.getX() * ratioB)/divisor));
		 * output.setY((float)((this.getY() * ratioA + input.getY() * ratioB)/divisor));
		 * output.setZ((float)((this.getZ() * ratioA + input.getZ() * ratioB)/divisor)); } else{ lerp(input, output, t);
		 * }
		 */
		// if qa=qb or qa=-qb then theta = 0 and we can return qa
		if (Math.abs(cosHalftheta) >= 1.0) {
			output.points[0] = (this.points[0]);
			output.points[1] = (this.points[1]);
			output.points[2] = (this.points[2]);
			output.points[3] = (this.points[3]);
		} else {
			double sinHalfTheta = Math.sqrt(1.0 - cosHalftheta * cosHalftheta);
			// if theta = 180 degrees then result is not fully defined
			// we could rotate around any axis normal to qa or qb
			// if(Math.abs(sinHalfTheta) < 0.001){
			// output.setW(this.getW() * 0.5f + input.getW() * 0.5f);
			// output.setX(this.getX() * 0.5f + input.getX() * 0.5f);
			// output.setY(this.getY() * 0.5f + input.getY() * 0.5f);
			// output.setZ(this.getZ() * 0.5f + input.getZ() * 0.5f);
			// lerp(bufferQuat, output, t);
			// }
			// else{
			double halfTheta = Math.acos(cosHalftheta);

			double ratioA = Math.sin((1 - t) * halfTheta) / sinHalfTheta;
			double ratioB = Math.sin(t * halfTheta) / sinHalfTheta;

			// Calculate Quaternion
			output.points[3] = ((float) (points[3] * ratioA + bufferQuat.points[3] * ratioB));
			output.points[0] = ((float) (this.points[0] * ratioA + bufferQuat.points[0] * ratioB));
			output.points[1] = ((float) (this.points[1] * ratioA + bufferQuat.points[1] * ratioB));
			output.points[2] = ((float) (this.points[2] * ratioA + bufferQuat.points[2] * ratioB));
		}

		output.dirty = true;
	}

	/**
	 * Spherical linear interpolation between this quaternion and the other quaternion, based on the alpha value in the
	 * range
	 * [0,1]. Taken from. Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/
	 * 
	 * @param end the end quaternion
	 * @param alpha alpha in the range [0,1]
	 * @return this quaternion for chaining
	 */
	public void slerp2(Quaternion end, Quaternion output, float alpha) {
		if (this.equals(end)) {
			output.set(this);
			return;
		}

		float result = dotProduct(end);

		if (result < 0.0) {
			// Negate the second quaternion and the result of the dot product
			end.multiplyByScalar(-1);
			result = -result;
		}

		// Set the first and second scale for the interpolation
		float scale0 = 1 - alpha;
		float scale1 = alpha;

		// Check if the angle between the 2 quaternions was big enough to
		// warrant such calculations
		if ((1 - result) > 0.1) {// Get the angle between the 2 quaternions,
			// and then store the sin() of that angle
			final double theta = Math.acos(result);
			final double invSinTheta = 1f / Math.sin(theta);

			// Calculate the scale for q1 and q2, according to the angle and
			// it's sine value
			scale0 = (float) (Math.sin((1 - alpha) * theta) * invSinTheta);
			scale1 = (float) (Math.sin((alpha * theta)) * invSinTheta);
		}

		// Calculate the x, y, z and w values for the quaternion by using a
		// special form of linear interpolation for quaternions.
		final float x = (scale0 * this.getX()) + (scale1 * end.getX());
		final float y = (scale0 * this.getY()) + (scale1 * end.getY());
		final float z = (scale0 * this.getZ()) + (scale1 * end.getZ());
		final float w = (scale0 * this.getW()) + (scale1 * end.getW());
		output.setXYZW(x, y, z, w);
	}

	/**
	 * Compares the values of two quaternions. Returns true if all values are exactly the same, otherwise false.
	 */
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Quaternion)) {
			return false;
		}
		final Quaternion comp = (Quaternion) o;
		return this.getX() == comp.getX() && this.getY() == comp.getY() && this.getZ() == comp.getZ()
				&& this.getW() == comp.getW();

	}

	// Override methods from the super-class in order to set the rotationMatrix dirty whenever a value in this quaternion is changed

	/* (non-Javadoc)
	 * @see
	 * com.example.rotationvectorsample.representation.Vector4f#copyVec4(com.example.rotationvectorsample.representation
	 * .Vector4f) */
	@Override
	public void copyVec4(Vector4f vec) {
		super.copyVec4(vec);
		this.dirty = true;
	}

	/* (non-Javadoc)
	 * @see
	 * com.example.rotationvectorsample.representation.Vector4f#add(com.example.rotationvectorsample.representation
	 * .Vector4f) */
	@Override
	public void add(Vector4f vector) {
		super.add(vector);
		this.dirty = true;
	}

	/* (non-Javadoc)
	 * @see
	 * com.example.rotationvectorsample.representation.Vector4f#add(com.example.rotationvectorsample.representation
	 * .Vector3f, float) */
	@Override
	public void add(Vector3f vector, float w) {
		super.add(vector, w);
		this.dirty = true;
	}

	/* (non-Javadoc)
	 * @see
	 * com.example.rotationvectorsample.representation.Vector4f#subtract(com.example.rotationvectorsample.representation
	 * .Vector4f) */
	@Override
	public void subtract(Vector4f vector) {
		super.subtract(vector);
		this.dirty = true;
	}

	/* (non-Javadoc)
	 * @see
	 * com.example.rotationvectorsample.representation.Vector4f#subtract(com.example.rotationvectorsample.representation
	 * .Vector4f, com.example.rotationvectorsample.representation.Vector4f) */
	@Override
	public void subtract(Vector4f vector, Vector4f output) {
		super.subtract(vector, output);
		this.dirty = true;
	}

	/* (non-Javadoc)
	 * @see
	 * com.example.rotationvectorsample.representation.Vector4f#subdivide(com.example.rotationvectorsample.representation
	 * .Vector4f) */
	@Override
	public void subdivide(Vector4f vector) {
		super.subdivide(vector);
		this.dirty = true;
	}

	/* (non-Javadoc)
	 * @see
	 * com.example.rotationvectorsample.representation.Vector4f#copyFromV3f(com.example.rotationvectorsample.representation
	 * .Vector3f, float) */
	@Override
	public void copyFromV3f(Vector3f input, float w) {
		super.copyFromV3f(input, w);
		this.dirty = true;
	}

	/* (non-Javadoc)
	 * @see com.example.rotationvectorsample.representation.Vector4f#setX(float) */
	@Override
	public void setX(float x) {
		super.setX(x);
		this.dirty = true;
	}

	/* (non-Javadoc)
	 * @see com.example.rotationvectorsample.representation.Vector4f#setY(float) */
	@Override
	public void setY(float y) {
		super.setY(y);
		this.dirty = true;
	}

	/* (non-Javadoc)
	 * @see com.example.rotationvectorsample.representation.Vector4f#setZ(float) */
	@Override
	public void setZ(float z) {
		super.setZ(z);
		this.dirty = true;
	}

	/* (non-Javadoc)
	 * @see com.example.rotationvectorsample.representation.Vector4f#setW(float) */
	@Override
	public void setW(float w) {
		super.setW(w);
		this.dirty = true;
	}

	/* (non-Javadoc)
	 * @see com.example.rotationvectorsample.representation.Vector4f#setXYZW(float, float, float, float) */
	@Override
	public void setXYZW(float x, float y, float z, float w) {
		super.setXYZW(x, y, z, w);
		this.dirty = true;
	}

	public float[] getOrientationValues() {

		float azimuth;
		float pitch;
		float roll;

		float val[] = new float[3];
		SensorManager.getOrientation(getMatrix4x4().matrix, val);

		azimuth = (float) Math.toDegrees(val[0]);
		pitch = (float) Math.toDegrees(val[1]);
		roll = (float) Math.toDegrees(val[2]);

		val[0] = azimuth;
		val[1] = pitch;
		val[2] = roll;

		return val;
	}

}
