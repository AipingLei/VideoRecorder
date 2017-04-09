/**
 * Copyright 2010 Per-Erik Bergman (per-erik.bergman@jayway.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package demo.recorder.gles;

import android.graphics.Bitmap;

/**
 * SimplePlane is a setup class for Mesh that creates a plane mesh.
 * 
 * @author Per-Erik Bergman (per-erik.bergman@jayway.com)
 * 
 */
public class SimplePlane extends Mesh {
	private final Drawable2d mRectDrawable = new Drawable2d(Drawable2d.Prefab.FULL_RECTANGLE);
	private Texture2dProgram mProgram;
	private int mTextureID = -1;

	/**
	 * Create a plane with a default with and height of 1 unit.
	 */
	public SimplePlane (Texture2dProgram program,Bitmap bitmap) {
		mProgram = program;
		mTextureID = createTextureObject(bitmap);
	}

	/**
	 * Releases resources.
	 * <p>
	 * This must be called with the appropriate EGL context current (i.e. the one that was
	 * current when the constructor was called).  If we're about to destroy the EGL context,
	 * there's no value in having the caller make it current just to do this cleanup, so you
	 * can pass a flag that will tell this function to skip any EGL-context-specific cleanup.
	 */
	public void release(boolean doEglCleanup) {
		if (mProgram != null) {
			if (doEglCleanup) {
				mProgram.release();
			}
			mProgram = null;
		}
	}

	/**
	 * Returns the program currently in use.
	 */
	public Texture2dProgram getProgram() {
		return mProgram;
	}

	/**
	 * Changes the program.  The previous program will be released.
	 * <p>
	 * The appropriate EGL context must be current.
	 */
	public void changeProgram(Texture2dProgram program) {
		mProgram.release();
		mProgram = program;
	}

	/**
	 * Creates a texture object suitable for use with drawFrame().
	 */
	public int createTextureObject(Bitmap aBitmap) {
		return mProgram.createBitampTexture(aBitmap);
	}


	/**
	 * Create a plane.
	 * 
	 * @param width
	 *            the width of the plane.
	 * @param height
	 *            the height of the plane.
	 */
	public SimplePlane(float width, float height) {
		// Mapping coordinates for the vertices
		float textureCoordinates[] = { 0.0f, 1.0f, //
				1.0f, 1.0f, //
				0.0f, 0.0f, //
				1.0f, 0.0f, //
		};

		short[] indices = new short[] { 0, 1, 2, 1, 3, 2 };

		float[] vertices = new float[] { -0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f,
				-0.5f, 0.5f, 0.0f, 0.5f, 0.5f, 0.0f };

		setIndices(indices);
		setVertices(vertices);
		setTextureCoordinates(textureCoordinates);
	}


	public int getTextureID() {
		return mTextureID;
	}

	/**
	 * Draws a viewport-filling rect, texturing it with the specified texture object.
	 */
	public void drawFrame( float[] texMatrix) {
		// Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
		mProgram.draw(GlUtil.IDENTITY_MATRIX, mRectDrawable.getVertexArray(), 0,
				mRectDrawable.getVertexCount(), mRectDrawable.getCoordsPerVertex(),
				mRectDrawable.getVertexStride(),
				texMatrix, mRectDrawable.getTexCoordArray(), mTextureID,
				mRectDrawable.getTexCoordStride());
	}
}
