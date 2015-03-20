package lv.edi.SmartWearMagn;

import java.nio.ByteBuffer; 
import java.nio.ByteOrder; 
import java.nio.FloatBuffer; 
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11; 

class ColorScaleModel {
	private ByteBuffer vbb;
	private float[] vertices = { 0, 0, 0,
								 0.1f, 0, 0,
								 0.2f, 0, 0,
								 0.3f, 0, 0,
								 0.4f, 0, 0,
								 0.5f, 0, 0,
								 0.6f, 0, 0,
								 0.7f, 0, 0,
								 0.8f, 0, 0,
								 0.9f, 0, 0,
								 1, 0, 0,
								 0, 1, 0,
								 0.1f, 1, 0,
								 0.2f, 1, 0,
								 0.3f, 1, 0,
								 0.4f, 1, 0,
								 0.5f, 1, 0,
								 0.6f, 1, 0,
								 0.7f, 1, 0,
								 0.8f, 1, 0,
								 0.9f, 1, 0,
								 1, 1, 0
								};
	private byte[][] colorMap = SmartWearApplication.drawingModelColormap;
	public ColorScaleModel(SmartWearApplication app){ 
	

		
		byte indices[] =
			{ 
			0, 12, 11, 0, 1, 12,
			1, 13, 12, 1, 2, 13,
			2, 14, 13, 2, 3, 14,
			3, 15, 14, 3, 4, 15,
			4, 16, 15, 4, 5, 16,
			5, 17, 16, 5, 6, 17,
			6, 18, 17, 6, 7, 18,
			7, 19, 18, 7, 8, 19,
			8, 20, 19, 8, 9, 20,
			9, 21, 20, 9, 10, 21
			}; 
				
		mIndexBuffer = ByteBuffer.allocateDirect(indices.length); 
		mIndexBuffer.put(indices); 
		mIndexBuffer.position(0); 
		

		vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder()); 

		byte colors[] = { 
				colorMap[0][0], colorMap[0][1], colorMap[0][2], 1,
				colorMap[1][0], colorMap[1][1], colorMap[1][2], 1,
				colorMap[2][0], colorMap[2][1], colorMap[2][2], 1,
				colorMap[3][0], colorMap[3][1], colorMap[3][2], 1,
				colorMap[4][0], colorMap[4][1], colorMap[4][2], 1,
				colorMap[5][0], colorMap[5][1], colorMap[5][2], 1,
				colorMap[6][0], colorMap[6][1], colorMap[6][2], 1,
				colorMap[7][0], colorMap[7][1], colorMap[7][2], 1,
				colorMap[8][0], colorMap[8][1], colorMap[8][2], 1,
				colorMap[9][0], colorMap[9][1], colorMap[9][2], 1,
				colorMap[10][0], colorMap[10][1], colorMap[10][2], 1,
				colorMap[0][0], colorMap[0][1], colorMap[0][2], 1,
				colorMap[1][0], colorMap[1][1], colorMap[1][2], 1,
				colorMap[2][0], colorMap[2][1], colorMap[2][2], 1,
				colorMap[3][0], colorMap[3][1], colorMap[3][2], 1,
				colorMap[4][0], colorMap[4][1], colorMap[4][2], 1,
				colorMap[5][0], colorMap[5][1], colorMap[5][2], 1,
				colorMap[6][0], colorMap[6][1], colorMap[6][2], 1,
				colorMap[7][0], colorMap[7][1], colorMap[7][2], 1,
				colorMap[8][0], colorMap[8][1], colorMap[8][2], 1,
				colorMap[9][0], colorMap[9][1], colorMap[9][2], 1,
				colorMap[10][0], colorMap[10][1], colorMap[10][2], 1,
			}; 


		mColorBuffer = ByteBuffer.allocateDirect(colors.length); 
		mColorBuffer.put(colors); 
		mColorBuffer.position(0); 

	} 
	public void draw(GL10 gl){ 
		mFVertexBuffer = vbb.asFloatBuffer(); 

	    mFVertexBuffer.put(vertices);


		mFVertexBuffer.position(0);
		
		gl.glFrontFace(GL11.GL_CW);
		gl.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 0, mColorBuffer);
		gl.glVertexPointer(3, GL11.GL_FLOAT, 0, mFVertexBuffer);
		gl.glDrawElements(GL11.GL_TRIANGLES, 60, GL11.GL_UNSIGNED_BYTE, mIndexBuffer);
		gl.glFrontFace(GL11.GL_CCW);
	} 

	private FloatBuffer mFVertexBuffer; 
	private ByteBuffer mColorBuffer; 
	private ByteBuffer mIndexBuffer;
} 
