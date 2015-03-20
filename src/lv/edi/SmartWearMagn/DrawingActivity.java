package lv.edi.SmartWearMagn;



import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import lv.edi.SmartWearMagn.R;
import lv.edi.SmartWearProcessing.Segment;
import lv.edi.SmartWearProcessing.SensorDataProcessing;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.Toast;
//import android.graphics.Point;
//import android.graphics.Rect;
//import android.os.Build;
//import android.view.Display;
//import android.view.WindowManager;

public class DrawingActivity extends Activity {
	private int REQUEST_POSTURE1 = 123;
	private int REQUEST_POSTURE2 = 124;
	private SmartWearApplication application;
	private AccGridRenderer renderer;
	private static float ROTATION_CONSTANT=0.01f;
	private float rotationAngleZ=0;
	private float rotationAngleY=0;
	private final float[] INITIAL_VECT = {0, -40, 0};
	private final float[] INITIAL_UP_VECT = {0, 0, 1}; // CAMERA UP VECTOR
	//private Activity currentActivity;
	private GLSurfaceView view;
	private LinearLayout container;
	Context thisContext;
	

	/**
	 * @param args
	 */
	@Override
	public void onCreate(Bundle savedInstanceState){
		container = new LinearLayout(this);
		//currentActivity = (Activity)this;
		thisContext = this;
		super.onCreate(savedInstanceState);
		application = (SmartWearApplication)getApplicationContext();
		Log.d("Drawing Activity","Creating activity");
		renderer = new AccGridRenderer(false,application);
		
		view = new MyGLSurfaceView(this);
//		GLSurfaceView view2 = new GLSurfaceView(this);
		Log.d("Drawing Activity","starting to setContentVIew");
		view.setRenderer(renderer);
//		view2.setRenderer(scaleRenderer);
//		view2.setZOrderOnTop(true);
		container.addView(view);
		//container.addView(view2);
		setContentView(container); 

		//gDetector = new GestureDetector(getApplicationContext(), this);
	
        // Set the gesture detector as the double tap
        // listener.

    
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.drawing_activity_menu, menu);
        return true;
    }
	public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.bluetoth_connection_activity:
                Intent intent = new Intent(this, DataSourceActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            case R.id.processing_activity:
                Intent intent1 = new Intent(this, ProcessingActivity.class);
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent1);
                return true;
            case R.id.settings:
            	Intent intent3 = new Intent(this, PreferencesActivity.class);
            	startActivity(intent3);
            	return true;
            case R.id.load_Posture1:
            	if(!application.isDataProcessingRunning()){
            		Intent intent4 = new Intent(this, PosturesActivity.class );
            		startActivityForResult(intent4, REQUEST_POSTURE1);
               	} else{
            		Toast.makeText(this, "Stop processing first!", Toast.LENGTH_SHORT).show();
            	}
            	return true;
            case R.id.load_Posture2:
            	if(!application.isDataProcessingRunning()){
                	Intent intent5 = new Intent(this, PosturesActivity.class);
                	startActivityForResult(intent5, REQUEST_POSTURE2);
            	} else{
            		Toast.makeText(this, "Stop processing first!", Toast.LENGTH_SHORT).show();
            	}

            	return true;
            case R.id.statistics:
            	Intent intent6 = new Intent(this, PostureStatisticsActivity.class);
            	startActivity(intent6);
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    // Check which request we're responding to
		Log.d("LOAD_POSTURE", ""+requestCode);
	    if (requestCode == REQUEST_POSTURE1) {
	        if (resultCode == RESULT_OK) {
	        	Log.d("LOAD_POSTURE","ok");
	            getPosturePointsFromFile(data.getStringExtra("data"), application.refferenceStateSegments);
	            getPosturePointsFromFile(data.getStringExtra("data"), application.refferenceStateSegmentsInitial);
	            application.segmentStateDistances=Segment.compareByCenterDistances(application.refferenceStateSegments, application.currentStateSegments); 
	            application.updateDrawingModelColors(application.segmentStateDistances);
	        }
	    }
	    if (requestCode == REQUEST_POSTURE2){
	    	if(resultCode == RESULT_OK){
	    		//getPosturePointsFromFile(data.getStringExtra("data"), application.currentAccPoints);
	    		Log.d("LOAD_POSTURE","ok");
	    		getPosturePointsFromFile(data.getStringExtra("data"), application.currentStateSegments);
	            application.segmentStateDistances=Segment.compareByCenterDistances(application.refferenceStateSegments, application.currentStateSegments); 
	            application.updateDrawingModelColors(application.segmentStateDistances);
	    	}
	    }
	}
	
	class MyGLSurfaceView extends GLSurfaceView {
		GestureDetector gestureDetector;
	   // private final MyGLRenderer mRenderer;

	    public MyGLSurfaceView(Context context) {
	        super(context);
	        gestureDetector = new GestureDetector(context, new GestureListener());
	    }

	    //private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
	    private float mPreviousX;
	    private float mPreviousY;
	    float temp[] = new float[3];// temporary vector for rotations
	    float temp2[] = new float[3];
	    float tempUp[] = new float[3];
	    float tempUp2[] = new float[3];
	    float quaternionY[] = new float[4];
	    float quaternionZ[] = new float[4];

	    @Override
	    public boolean onTouchEvent(MotionEvent e) {
	        // MotionEvent reports input details from the touch screen
	        // and other input controls. In this case, you are only
	        // interested in events where the touch position changed.

	        float x = e.getX();
	        float y = e.getY();
	      // Log.d("Touch input"," x: "+x);
	      // Log.d("Touch input"," y:"+y);
	        switch (e.getAction()) {
	            case MotionEvent.ACTION_MOVE:
	            	
	                float dx = x - mPreviousX;
	                float dy = y - mPreviousY;
	                
	                rotationAngleZ+= ((dx) * ROTATION_CONSTANT);
	                rotationAngleY+= ((dy) * ROTATION_CONSTANT);//
	                // quaternion for x axis rotation
	                quaternionY[0] = (float)Math.cos(rotationAngleY/2);
	                quaternionY[1] = (float)Math.sin(rotationAngleY/2);
	                quaternionY[2] = 0;
	                quaternionY[3] = 0;
	                //quaternion for z axis rotation	
		       		 quaternionZ[0]=(float)Math.cos(rotationAngleZ/2);
		       		 quaternionZ[1]=0;
		       		 quaternionZ[2]=0;
		       		 quaternionZ[3]=(float)Math.sin(rotationAngleZ/2);
		       		 
		       		 
		       		 //rotate around x axis from initial vector position
		       		 // temporary vector for  camera up vector
		       		 float[] offset = application.currentStateSegments[SmartWearApplication.GRID_ROWS/2][SmartWearApplication.GRID_COLS/2].center;
		       		 float[] initialVectOffset = new float[3];
		       		 for(int i=0; i<3;i++){
		       			initialVectOffset[i]=INITIAL_VECT[i]-offset[i];
		       		 }
		       		 SensorDataProcessing.quatRotate(quaternionY, initialVectOffset, temp);
		       		 SensorDataProcessing.quatRotate(quaternionY, INITIAL_UP_VECT, tempUp);

		       		 //rotate around Z axis 
		       		 SensorDataProcessing.quatRotate(quaternionZ, temp, temp2);
		       		 SensorDataProcessing.quatRotate(quaternionZ, tempUp, tempUp2);
		       		 // update rotated vector coordinates
		       		 for(int i = 0; i<renderer.viewPointVector.length;i++){
		       			 renderer.viewPointVector[i]=temp2[i]+offset[i];
		       			 renderer.cameraUpVector[i]=tempUp2[i];
		       		 }           
	        }

	        mPreviousX = x;
	        mPreviousY = y;
	        
	        gestureDetector.onTouchEvent(e);// link event to gesture detector
	        return true;
	    }
	   
	}
	 private class GestureListener extends GestureDetector.SimpleOnGestureListener {

	        @Override
	        public boolean onDown(MotionEvent e) {
	            return true;
	        }
	        // event when double tap occurs
	        @Override
	        public boolean onDoubleTap(MotionEvent e) {
//	        	Intent intent = new Intent(thisContext, SavePostureIMGActivity.class);
//	        	//Bitmap screenShot = takeScreenshot(currentActivity);
//	            View v = container.getRootView();
//	            v.setDrawingCacheEnabled(true);
//	            //Bitmap screenShot = v.getDrawingCache();
//	        	v.setDrawingCacheEnabled(true);
//	        	Bitmap fullScreenBitmap = Bitmap.createBitmap(v.getDrawingCache());
//	        	container.setDrawingCacheEnabled(false);
//	        	intent.putExtra("screenBitmap",fullScreenBitmap);
//	        	startActivity(intent);
//	            //Log.d("Double Tap", "Tapped at: (" + x + "," + y + ")");
	       		 for(int i = 0; i<renderer.viewPointVector.length;i++){
	       			 renderer.viewPointVector[i]=INITIAL_VECT[i];
	       			 renderer.cameraUpVector[i]=INITIAL_UP_VECT[i];
	       			 rotationAngleZ=0;
	       			 rotationAngleY=0;
	       		 }   
	            Toast.makeText(getBaseContext(), "Camera View Reset", Toast.LENGTH_SHORT).show();
	            return true;
	        }
	    }
	 

	public void getPosturePointsFromFile(String fileName, Segment segments[][]){
		DataInputStream iStream = null;
		File postureFile= new File(application.getPostureDirectory().getPath(), fileName);
		Log.d("LOAD_POSTURE",postureFile.getPath());
		try{
			iStream = new DataInputStream(new FileInputStream(postureFile));
			int rows = segments.length;
			int cols = segments[0].length;
			int axes = segments[0][0].center.length;
			for(int i = 0; i<rows;i++){
				for(int j=0; j<cols;j++){
					for(int k=0; k<axes; k++){
						segments[i][j].center[k]=iStream.readFloat();
					}
				}
			}
			iStream.close();
		} catch(IOException ex){
			Log.d("LODA_POSTURE", "catched exception");
		} finally{
			try{
				iStream.close();
			} catch(IOException ex){
				
			}
		}
	}
	
}
