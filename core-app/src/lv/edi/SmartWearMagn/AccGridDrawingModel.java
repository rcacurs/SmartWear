package lv.edi.SmartWearMagn;


import java.nio.ByteBuffer; 
import java.nio.ByteOrder; 
import java.nio.FloatBuffer; 
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11; 

class AccGridDrawingModel {
	private SmartWearApplication application;
	private ByteBuffer vbb, vbb2;
	byte referenceGridColor[] = new byte[SmartWearApplication.NR_OF_SENSORS*4];
	private final int NR_ROWS = SmartWearApplication.GRID_ROWS;
	private final int NR_COLS = SmartWearApplication.GRID_COLS;
	private final int NR_SENSORS = SmartWearApplication.NR_OF_SENSORS;
	byte[] indices_saved;
	byte[] indices_current;
	public AccGridDrawingModel(SmartWearApplication app){ 
	
		application = app;
		for(int i=0;i<NR_SENSORS;i++){
			referenceGridColor[i*4]=0;
			referenceGridColor[i*4+1]=0;
			referenceGridColor[i*4+2]=0;
			referenceGridColor[i*4+3]=(byte)255;
		}
		indices_saved = new byte[NR_SENSORS*2];
		indices_current = new byte[(NR_ROWS-1)*(NR_COLS-1)*6];
//		byte indices_saved[] =
//			{ 
//			0, 1, 2, 3, 4, 5, 6,
//			13, 12, 11, 10, 9, 8, 7,
//			14, 15, 16, 17, 18, 19, 20,
//			27, 26, 25, 24, 23, 22, 21,
//			28, 29, 30, 31, 32, 33, 34,
//			41, 40, 39, 38, 37, 36, 35,
//			42, 43, 44, 45, 46, 47, 48,
//			55, 54, 53, 52, 51, 50, 49,
//			56, 57, 58, 59, 60, 61, 62,
//			55, 48, 41, 34, 27, 20, 13, 6,
//			5, 12, 19, 26, 33, 40, 47, 54, 61,
//			60, 53, 46, 39, 32, 25, 18, 11, 4,
//			3, 10, 17, 24, 31, 38, 45, 52, 59,
//			58, 51, 44, 37, 30, 23, 16, 9, 2, 
//			1, 8, 15, 22, 29, 36, 43, 50, 57,
//			56, 49, 42, 35, 28, 21, 14, 7, 0
//			}; 
		// FILLING INDEXES FOR SAVED STATE GRID
		//VERTEXES PUT INT THE BUFFER INT THIS WAY
		// grid has R rows and C cols
		//
		//
		//(R-1)*(C)+1 ...............(R-1)*(C)+C-1
		//..........................
		//
		// (C-1)*1+1  C-1+2 ....... (C-1)*1+C
		// 0 		   1 	.......  C-1 
		//
		// opengl vertex index for particular sensor can be calculated
		// 		index=r*C+c; where r particular row index (0, 1, R-1), C - number of columns, c-particular column index (0, 1, 2, 3) 
		//
		// refference state drawing pattern
		//---------  ----
		//|          |  |   |
		// --------  |  |   |
		//        |  |  |   |
		// --------  |  -----
		boolean flag=true; //direction flag currently indicates direction to left
		int index_counter=0; // counter for saved_state indexes
		byte index=0;         
		for(int r=0;r<NR_ROWS;r++){
			for(int c=0;c<NR_COLS;c++){
				if(flag==true){
					index=(byte)(r*NR_COLS+c);
					indices_saved[index_counter]=index;
				}else {
					index=(byte)(r*NR_COLS+((NR_COLS-1)-c));
					indices_saved[index_counter]=index;
				}
				index_counter++;	
			}
			flag=!flag;// flip direction flag
		}
		boolean flag2=false; // second flag to determine
		int cc;
		for(int c=0;c<NR_COLS;c++){
			if(flag){
				cc=c;
			} else{
				cc=NR_COLS-1-c;
			}
			for(int r=0;r<NR_ROWS;r++){
				if(flag2==true){
					index=(byte)((r*NR_COLS)+cc);
					indices_saved[index_counter]=index;
				} else{
					index=(byte)((NR_ROWS-1-r)*NR_COLS+cc);
					indices_saved[index_counter]=index;
				}
				index_counter++;
			}
			flag2=!flag2; // flip flag
		}
		//====================================================================
		//filling indexes for current_state grid
		// drawing via triangles
	    //  2____3  Forms one polygon
		//   | /|
		//   |/_|
		//   0   1
		// order in which corners are  put 0, 1, 3, 
//		byte indices_current[] =
//			{ 
//				0, 1, 8, 0, 8, 7, 1, 2, 9, 1, 9, 8, 2, 3, 10, 2, 10, 9, 3, 4, 11, 3, 11, 10, 4, 5, 12, 4, 12, 11, 5, 6, 13, 5, 13, 12, 
//
//				7, 8, 15, 7, 15, 14, 8, 9, 16, 8, 16, 15, 9, 10, 17, 9, 17, 16, 10, 11, 18, 10, 18, 17, 11, 12, 19, 11, 19, 18, 12, 13, 20, 12, 20, 19, 
//
//				14, 15, 22, 14, 22, 21, 15, 16, 23, 15, 23, 22, 16, 17, 24, 16, 24, 23, 17, 18, 25, 17, 25, 24, 18, 19, 26, 18, 26, 25, 19, 20, 27, 19, 27, 26, 
//
//				21, 22, 29, 21, 29, 28, 22, 23, 30, 22, 30, 29, 23, 24, 31, 23, 31, 30, 24, 25, 32, 24, 32, 31, 25, 26, 33, 25, 33, 32, 26, 27, 34, 26, 34, 33, 
//
//				28, 29, 36, 28, 36, 35, 29, 30, 37, 29, 37, 36, 30, 31, 38, 30, 38, 37, 31, 32, 39, 31, 39, 38, 32, 33, 40, 32, 40, 39, 33, 34, 41, 33, 41, 40, 
//
//				35, 36, 43, 35, 43, 42, 36, 37, 44, 36, 44, 43, 37, 38, 45, 37, 45, 44, 38, 39, 46, 38, 46, 45, 39, 40, 47, 39, 47, 46, 40, 41, 48, 40, 48, 47, 
//
//				42, 43, 50, 42, 50, 49, 43, 44, 51, 43, 51, 50, 44, 45, 52, 44, 52, 51, 45, 46, 53, 45, 53, 52, 46, 47, 54, 46, 54, 53, 47, 48, 55, 47, 55, 54, 
//
//				49, 50, 57, 49, 57, 56, 50, 51, 58, 50, 58, 57, 51, 52, 59, 51, 59, 58, 52, 53, 60, 52, 60, 59, 53, 54, 61, 53, 61, 60, 54, 55, 62, 54, 62, 61, 
//			}; 
		index_counter=0; // re using index counter
		for(int r=0;r<NR_ROWS-1;r++){
			for(int c=0;c<NR_COLS-1;c++){
				indices_current[index_counter]=(byte)(r*NR_COLS+c);
				index_counter++;
				indices_current[index_counter]=(byte)(r*NR_COLS+c+1);
				index_counter++;
				indices_current[index_counter]=(byte)((r+1)*NR_COLS+c+1);
				index_counter++;
				indices_current[index_counter]=(byte)(r*NR_COLS+c);
				index_counter++;
				indices_current[index_counter]=(byte)((r+1)*NR_COLS+c+1);
				index_counter++;
				indices_current[index_counter]=(byte)((r+1)*NR_COLS+c);
				index_counter++;
			}
		}
		mIndexBuffer = ByteBuffer.allocateDirect(indices_saved.length); 
		mIndexBuffer.put(indices_saved); 
		mIndexBuffer.position(0); 
		
		mIndexBuffer2 = ByteBuffer.allocateDirect(indices_current.length); 
		mIndexBuffer2.put(indices_current); 
		mIndexBuffer2.position(0); 
	
		vbb = ByteBuffer.allocateDirect(NR_ROWS*NR_COLS * 3 * 4);
		vbb.order(ByteOrder.nativeOrder()); 
		vbb2 = ByteBuffer.allocateDirect(NR_ROWS*NR_COLS * 3 * 4);
		vbb2.order(ByteOrder.nativeOrder()); 
		
//		mColorBuffer = ByteBuffer.allocateDirect(colors.length); 
//		mColorBuffer.put(colors); 


	} 
	public void draw(GL10 gl){ 
		mColorBuffer = ByteBuffer.allocateDirect(referenceGridColor.length);
		mColorBuffer.put(referenceGridColor);
		mColorBuffer.position(0); 
		mFVertexBuffer = vbb.asFloatBuffer(); 
		for (int i = 0; i < application.refferenceStateSegments.length; i++) {
			for(int j = 0; j < application.refferenceStateSegments[0].length; j++) {
	            mFVertexBuffer.put(application.refferenceStateSegments[i][j].getSegmentCenterX());
	            mFVertexBuffer.put(application.refferenceStateSegments[i][j].getSegmentCenterY());
	            mFVertexBuffer.put(application.refferenceStateSegments[i][j].getSegmentCenterZ());
	        }
	    }
		mFVertexBuffer.position(0);
		mFVertexBuffer2 = vbb2.asFloatBuffer(); 
	    for (int i = 0; i < application.refferenceStateSegments.length; i++) {
	        for(int j = 0; j < application.refferenceStateSegments[0].length; j++) {
	            mFVertexBuffer2.put(application.currentStateSegments[i][j].getSegmentCenterX());
	            mFVertexBuffer2.put(application.currentStateSegments[i][j].getSegmentCenterY());
	            mFVertexBuffer2.put(application.currentStateSegments[i][j].getSegmentCenterZ());
	        }
	    }
		mFVertexBuffer2.position(0);	
		
		gl.glFrontFace(GL11.GL_CW);
		gl.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 0, mColorBuffer);
		gl.glVertexPointer(3, GL11.GL_FLOAT, 0, mFVertexBuffer);
		gl.glDrawElements(GL11.GL_LINE_STRIP, indices_saved.length, GL11.GL_UNSIGNED_BYTE, mIndexBuffer);
		
		gl.glFrontFace(GL11.GL_CW);
		gl.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 0, mColorBuffer);
		gl.glVertexPointer(3, GL11.GL_FLOAT, 0, mFVertexBuffer2);
		gl.glDrawElements(GL11.GL_LINE_STRIP, indices_saved.length, GL11.GL_UNSIGNED_BYTE, mIndexBuffer);
		
		mFVertexBuffer2.position(0);
		
		mColorBuffer = ByteBuffer.allocateDirect(application.drawingModelColors.length);
		mColorBuffer.put(application.drawingModelColors);
		mColorBuffer.position(0); 
		gl.glFrontFace(GL11.GL_CW);
		gl.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 0, mColorBuffer);
		gl.glVertexPointer(3, GL11.GL_FLOAT, 0, mFVertexBuffer2);
		gl.glDrawElements(GL11.GL_TRIANGLES, indices_current.length, GL11.GL_UNSIGNED_BYTE, mIndexBuffer2);	
		gl.glFrontFace(GL11.GL_CCW);
	} 

	private FloatBuffer mFVertexBuffer; 
	private FloatBuffer mFVertexBuffer2; 
	private ByteBuffer mColorBuffer; 
	private ByteBuffer mIndexBuffer;
	private ByteBuffer mIndexBuffer2; 
} 
