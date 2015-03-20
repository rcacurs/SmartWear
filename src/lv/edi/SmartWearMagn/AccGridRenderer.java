package lv.edi.SmartWearMagn;

import javax.microedition.khronos.egl.EGLConfig; 
import javax.microedition.khronos.opengles.GL10; 
import android.opengl.GLSurfaceView; 
import android.opengl.GLU;

class AccGridRenderer implements GLSurfaceView.Renderer 
{ 
	private SmartWearApplication application;
	volatile public double viewPointVector[] = {0, -40, 0};
	volatile public double cameraUpVector[] = {0, 0, 1};
	
	public AccGridRenderer(boolean useTranslucentBackground, SmartWearApplication app) { 
		mTranslucentBackground = useTranslucentBackground;
		application = app;
		mModel = new AccGridDrawingModel(application);
	} 
	public void onDrawFrame(GL10 gl){ 
				gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT); 
				gl.glMatrixMode(GL10.GL_MODELVIEW);
				gl.glLoadIdentity(); //7 
				gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
				gl.glEnableClientState(GL10.GL_COLOR_ARRAY); 
				gl.glDisable(GL10.GL_CULL_FACE);
				
				//
			    GLU.gluLookAt(gl, (float)viewPointVector[0], (float)viewPointVector[1], (float)viewPointVector[2], application.currentStateSegments[(int)(SmartWearApplication.GRID_ROWS/2)][(int)(SmartWearApplication.GRID_COLS/2)].center[0], application.currentStateSegments[(int)(SmartWearApplication.GRID_ROWS/2)][(int)(SmartWearApplication.GRID_COLS/2)].center[1], application.currentStateSegments[(int)(SmartWearApplication.GRID_ROWS/2)][(int)(SmartWearApplication.GRID_COLS/2)].center[2], (float)cameraUpVector[0], (float)cameraUpVector[1], (float)cameraUpVector[2]); 	
				mModel.draw(gl);		
	} 
	public void onSurfaceChanged(GL10 gl, int width, int height){ 
				gl.glViewport(0, 0, width, height); 
				float ratio = (float) width / height; 
				gl.glMatrixMode(GL10.GL_PROJECTION);
				gl.glLoadIdentity(); 
				gl.glFrustumf(-ratio, ratio, -1, 1, 1, 140);
	} 
	public void onSurfaceCreated(GL10 gl, EGLConfig config){ 
				gl.glDisable(GL10.GL_DITHER);
				gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, 
				GL10.GL_FASTEST); 
				if (mTranslucentBackground){ 
					gl.glClearColor(0,0,0,0); 
				} 
				else { 
					gl.glClearColor(1,1,1,1); 
				} 
				gl.glEnable(GL10.GL_CULL_FACE);
				gl.glEnable(GL10.GL_DEPTH_TEST);
	} 
	private boolean mTranslucentBackground; 
	private AccGridDrawingModel mModel; 
	
} 
