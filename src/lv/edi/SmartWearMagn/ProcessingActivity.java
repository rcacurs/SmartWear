package lv.edi.SmartWearMagn;

//import java.io.File;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Calendar;

import lv.edi.SmartWearMagn.R;
import lv.edi.SmartWearProcessing.Segment;
import lv.edi.SmartWearProcessing.SensorDataProcessing;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
//import android.os.Environment;

@SuppressLint("HandlerLeak")
public class ProcessingActivity extends Activity implements OnGestureListener, OnLongClickListener{
	private SmartWearApplication application; // applictation object, for storing global data
	//private double[][][] savedStateOfPoints; //saved state for points
	private BluetoothService service; // bluetooth serivce that fetches data from acc Grid
	//private ReadingFromFileService readingFromFileService; // srervie that readsdata from file
	private ProcessingService processingService;
	private static NumberFormat nf; // number format object to format number
	//private boolean isOverThreshold = false; // is any of segments over threshold
	// threads
	//SaveThread saveThread; // save state thread instance
	//CalculateDifferenceThread calculateThread; // calculate difference between saved state and current state thread
	// handler message types
	public static final int TOAST_MESSAGE_STATE_SAVED=1; // message to indicate, that state saved
	public static final int MESSAGE_SHOW_DISTANCE=2; // message to indicate, that distance is calculated
	public static final int MESSAGE_CALCULATION_STOPPED=3; // message to indicate, that processing sopped
	public static final int TOAST_MESSAGE_COULD_NOT_SAVE=4; // meesage to indicate, that problem with saving
	public static final int REQUEST_LOG_FILE=143;
	public static final int REQUEST_POSTURE_REFERENCE=144; // for activity result filter
	
	//number of accelerometers to process
	public static final int ROWS = 5; //number of acc grid point rows
	public static final int COLUMNS = 4;// number of acc grid point columns
	// fields for data logging
	
	//views
	TextView averageDistanceView;
	TextView averageDistanceSagitalView;
	TextView averageDistanceCoronalView;
	TextView maxDistanceView;
	TextView maxDistanceCoronalView;
	TextView maxDistanceSagitalView;
	TextView calculationStatus;
	TextView vibrateStatusView;
	TextView alertStatusView;
	TextView personsHeightView;
	ToggleButton startCalculationButton;
	TextView thresholdView;
	TextView lastLogFileView;
	Button saveStateButton;
	TextView logFeatureSettingView;
	TextView goodTimePercentageView;
	TextView savedStateView;
	Vibrator mVibro; // vibrator instance for feedback
	MediaPlayer mp; // instance for sound alert feedback
	// fields for logging
	private FileWriter fWriter;// writer that writes files to file;
	private FileWriter fWriterSessionLog; // file writer for session logging
	//private File logFile; // log File object for logging segment distances
	//private int logFileCounter=0; // log file counter,
	
	private GestureDetector gDetector; // gesture detector for swipe activity transisions
	int fileCounter=1;
	// handler object that handles messages from other threads
	@SuppressLint({ "HandlerLeak", "HandlerLeak" })
	public final Handler handler = new Handler(){
		public void handleMessage(Message message){
			switch (message.what){
				case TOAST_MESSAGE_STATE_SAVED: // posture state saved message
					Toast.makeText(getApplicationContext(),"Saved!",Toast.LENGTH_SHORT).show();
					break;
				case MESSAGE_SHOW_DISTANCE:
					// show average distance, and good, bad times
					//averageDistanceView.setText(""+nf.format(processingService.getAverageDistance()));//+nf.format(averageDistance)+"[good:"+nf.format((double)(goodTime*100)/(goodTime+badTime))+"%/bad:"+nf.format((double)(badTime*100)/(goodTime+badTime+1))+"%]");
					//averageDistanceSagitalView.setText(""+nf.format(processingService.getAverageDistanceSagital()));
					//averageDistanceCoronalView.setText(""+nf.format(processingService.getAverageDistanceCoronal()));
					maxDistanceView.setText(""+nf.format(processingService.getMaxDistance()));//+nf.format(averageDistance)+"[good:"+nf.format((double)(goodTime*100)/(goodTime+badTime))+"%/bad:"+nf.format((double)(badTime*100)/(goodTime+badTime+1))+"%]");
					maxDistanceSagitalView.setText(""+nf.format(processingService.getMaxSagitalDistance()));
					maxDistanceCoronalView.setText(""+nf.format(processingService.getMaxCoronalDistance()));
					goodTimePercentageView.setText(""+(int)processingService.getGoodTimePercentage()+" %");
					if(processingService.isOverThreshold()){
						maxDistanceView.setTextColor(Color.RED);
					} else{
						maxDistanceView.setTextColor(Color.GREEN);
					}
					break;
				case MESSAGE_CALCULATION_STOPPED:
					startCalculationButton.setChecked(false);
					startCalculationButton.setText("Start Processing");
					application.setDataProcessingRunning(false);
					calculationStatus.setText("Stopped");				
					Calendar calendar = Calendar.getInstance();
					int day = calendar.get(Calendar.DAY_OF_MONTH);
					int month = calendar.get(Calendar.MONTH)+1;
					int year = calendar.get(Calendar.YEAR);
					int minute = calendar.get(Calendar.MINUTE);
					int hour = calendar.get(Calendar.HOUR_OF_DAY);
					String log = "#SESSION_STOP: "+day+"."+month+"."+year+", "+hour+":"+minute+" ("+message.arg1+" %)\n\r";
					try{
						fWriterSessionLog =new FileWriter(application.getSessionLogFile(), true);
						fWriterSessionLog.write(log);
						fWriterSessionLog.close();
					} catch(IOException ex){
						
					}
					Toast.makeText(getApplicationContext(), "Calculation stopped", Toast.LENGTH_SHORT).show();
					break;
				case TOAST_MESSAGE_COULD_NOT_SAVE:
					Toast.makeText(getApplicationContext(),"could not save", Toast.LENGTH_SHORT).show();
					break;
			}	
		}
    };
	
	@Override
	public void onCreate(Bundle savedInsanceState){
		super.onCreate(savedInsanceState);
		nf = NumberFormat.getInstance(); // get instance for your locale
		nf.setMaximumFractionDigits(1); // set decimal places
		nf.setMinimumFractionDigits(1); //set minimum decimal places
		mp = MediaPlayer.create(this, R.raw.beep);// instantate media player for sound alert feedback
		application = (SmartWearApplication)getApplicationContext();
		setContentView(R.layout.activity_processing_data);
		service=application.bluetoothService;
		//readingFromFileService=application.getReadingFromFileService();
		processingService=application.getProcessingService();
		averageDistanceView = (TextView)findViewById(R.id.averageDistanceView);
		averageDistanceSagitalView = (TextView)findViewById(R.id.averageDistanceSagitalView);
		maxDistanceView = (TextView)findViewById(R.id.maxDistanceView);
		maxDistanceSagitalView = (TextView)findViewById(R.id.maxDistanceSagitalView);
		maxDistanceCoronalView = (TextView) findViewById(R.id.maxDistanceCoronalView);
		averageDistanceCoronalView = (TextView)findViewById(R.id.averageDistanceCoronalView);
		calculationStatus = (TextView)findViewById(R.id.ProcessStatus);
		startCalculationButton = (ToggleButton)findViewById(R.id.start_process_button);
		goodTimePercentageView = (TextView)findViewById(R.id.goodTimePercentageView);
		savedStateView = (TextView)findViewById(R.id.SavedStateView);
		saveStateButton = (Button)findViewById(R.id.save_state_button);
		saveStateButton.setOnLongClickListener(this);
		
		mVibro = (Vibrator)getSystemService(VIBRATOR_SERVICE);
		gDetector = new GestureDetector(getApplicationContext(), this);
		thresholdView = (TextView)findViewById(R.id.thresholdSettingView);
		vibrateStatusView = (TextView)findViewById(R.id.vibrateFeedbackStatus);
		alertStatusView = (TextView)findViewById(R.id.alertFeedbackStatus);
		personsHeightView = (TextView)findViewById(R.id.personsHeight);	
		lastLogFileView = (TextView)findViewById(R.id.LogFileName);
		logFeatureSettingView = (TextView)findViewById(R.id.LogFeatureSetting);
		application.setProcessingActivity(this); // register this activity in application object
		processingService.setVibroAndAlert(this); 
		
	}
	@Override
	public void onResume(){
		super.onResume();
		
			if(application.isSoundFeedbackOn()){
				alertStatusView.setText("On");
			} else{
				alertStatusView.setText("Off");
			}
			if(application.isVibrateFeedbackOn()){
				vibrateStatusView.setText("On");
			} else{
				vibrateStatusView.setText("Off");
			}
			personsHeightView.setText(""+application.getPersonsHeight()+" [m]");
			thresholdView.setText(""+application.getThreshold()+" [cm]");
			try{
			String lastLogFileName = application.getSelectedLogFile().getName();
			lastLogFileView.setText(lastLogFileName);
			} catch(NullPointerException ex){
				lastLogFileView.setText("Not selected");
			}
			if(application.isLoggingFeatureEnabled()){
				logFeatureSettingView.setText("On");
			} else{
				logFeatureSettingView.setText("Off");
			}
			if(application.isDataProcessingRunning()){
				calculationStatus.setText("Running");
				startCalculationButton.setChecked(true);
			} else{
				calculationStatus.setText("Stopped");
				startCalculationButton.setChecked(false);
			}
			if(processingService.isProcessing()){
				calculationStatus.setText("Running");
				startCalculationButton.setChecked(true);
				startCalculationButton.setText("Stop Processing");
			} else{
				calculationStatus.setText("Stopped");
				startCalculationButton.setChecked(false);
				startCalculationButton.setText("Start Processing");
			}
			if(application.isStateSaved()){
				savedStateView.setText("Yes");
			} else{
				savedStateView.setText("No");
			}
			
	}
	public void onDestroy(){
		super.onDestroy();
//		if(processingService!=null){
//			processingService.cancel();
//			processingService=null;
//		}
//		if(calculateThread!=null){ //if calculate thread alive, destroy it
//			calculateThread=null;
//		}
		if(fWriter!=null){// if fwriter alive, destroy it
				try {
					fWriter.flush(); // properly close fWriter object 
					fWriter.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
		}
	}

	@Override
	// inflate options menu from xml
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_bluetooth_connection, menu);
        return true;
    }
	// callback for option menu item selection
	public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.bluetoth_connection_activity:
                Intent intent = new Intent(this, DataSourceActivity.class);
                startActivity(intent);
                return true;
            case R.id.drawing_accivity:
            	Intent intent2 = new Intent(this, DrawingActivity.class);
            	startActivity(intent2);
            	return true;
            case R.id.settings:
            	Intent intent3 = new Intent(this, PreferencesActivity.class);
            	startActivity(intent3);
            	return true;
            case R.id.statistics:
            	Intent intent4 = new Intent(this, PostureStatisticsActivity.class);
            	startActivity(intent4);
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
	// on long click callback for save state button
	public boolean onLongClick(View v){
		if(!application.isDataProcessingRunning()){
			if(application.bluetoothService.isConnected()||application.isReadingFromFile()){
				Log.d("LONGPRESS","pressed");
				Intent intent = new Intent(this, SaveNewPostureActivity.class);
				startActivity(intent);
			} else{
				Toast.makeText(this, "Connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
			}
		}else{
			Toast.makeText(this, "Stop Processing First", Toast.LENGTH_LONG).show();
		}
		return true;
	}
	// button callcback for save state button
	public void onClickSaveState(View v){
			if(!processingService.isProcessing()){
				if(application.bluetoothService.isConnected()||application.isReadingFromFile()){
					Log.d("SAVE_STATE", "refRow: "+application.getReferenceRow()+", refCol: "+application.getReferenceCol());
					application.refferenceStateSegments[application.getReferenceRow()][application.getReferenceCol()].center[0]=0;
					application.refferenceStateSegments[application.getReferenceRow()][application.getReferenceCol()].center[1]=0;
					application.refferenceStateSegments[application.getReferenceRow()][application.getReferenceCol()].center[2]=0;
					application.refferenceStateSegmentsInitial[application.getReferenceRow()][application.getReferenceCol()].center[0]=0;
					application.refferenceStateSegmentsInitial[application.getReferenceRow()][application.getReferenceCol()].center[1]=0;
					application.refferenceStateSegmentsInitial[application.getReferenceRow()][application.getReferenceCol()].center[2]=0;
					application.savedStateRot=SensorDataProcessing.getRotationTRIAD(application.sensorGridArray[application.getReferenceRow()][application.getReferenceCol()].getAccNorm(), application.sensorGridArray[application.getReferenceRow()][application.getReferenceCol()].getMagNorm());
					Segment.setAllSegmentOrientationsTRIAD(application.refferenceStateSegments, application.sensorGridArray);
					Segment.setAllSegmentOrientationsTRIAD(application.refferenceStateSegmentsInitial, application.sensorGridArray);
					
					Segment.setSegmentCenters(application.refferenceStateSegments, (short)application.getReferenceRow(), (short)application.getReferenceCol());
					Segment.setSegmentCenters(application.refferenceStateSegmentsInitial, (short)application.getReferenceRow(), (short)application.getReferenceCol());
					saveDefaultPosture(SmartWearApplication.DEFAULT_POSTURE_FILE, application.refferenceStateSegments);
					Toast.makeText(getApplicationContext(),"State Saved!",Toast.LENGTH_SHORT).show();
					application.setStateSaved(true);
					savedStateView.setText("Yes");
				} else{
					Toast.makeText(this,"Select and connect to data source first!", Toast.LENGTH_SHORT).show();
				}
			} else{
				Toast.makeText(getApplicationContext(),"Stop processing first!",Toast.LENGTH_SHORT).show();
			}
	
	}
	//start/stop processing button callback
	public void onClickStartProcess(View v){
		boolean on = ((ToggleButton) v).isChecked();
		if(on){
			if((service.isConnected())||(application.isReadingFromFile())){
				if(application.savedStateOfPoints!=null){
					// start processing thread
					calculationStatus.setText("Running");
					processingService.start();
			        if(application.getSelectedLogFile()!=null){
			        	try{
			        		DataOutputStream stream = new DataOutputStream(new FileOutputStream(application.getSelectedLogFile(),true));
			        		application.setSelectedLogFileStream(stream);
			        	} catch(IOException ex){
			        		
			        	}
			        }
					Calendar calendar = Calendar.getInstance();
					int day = calendar.get(Calendar.DAY_OF_MONTH);
					int month = calendar.get(Calendar.MONTH)+1;
					int year = calendar.get(Calendar.YEAR);
					int minute = calendar.get(Calendar.MINUTE);
					int hour = calendar.get(Calendar.HOUR_OF_DAY);
					String log = "#SESSION_START: "+day+"."+month+"."+year+", "+hour+":"+minute+"\n\r";
					try{
						fWriterSessionLog =new FileWriter(application.getSessionLogFile(), true);
						fWriterSessionLog.write(log);
						fWriterSessionLog.close();
					} catch(IOException ex){
						
					}
					application.setDataProcessingRunning(true);

				} else {
					Toast.makeText(getApplicationContext(), "No saved state", Toast.LENGTH_LONG).show();
					((ToggleButton) v).setChecked(false);
					application.setDataProcessingRunning(false);
				}
			} else{
				Toast.makeText(getApplicationContext(), "Connect to the bluetooth device or select reading from file", Toast.LENGTH_SHORT).show();
				((ToggleButton) v).setChecked(false);
				application.setDataProcessingRunning(false);
			}
		} else{
			if(processingService!=null){
				processingService.cancel();
				application.setDataProcessingRunning(false);
				calculationStatus.setText("Stopped");
				try{
					if(application.getSelectedLogFileStream()!=null){
					application.getSelectedLogFileStream().close();
					}
				}catch(IOException ex){
					
				}
				
				/*if(fWriter!=null){
					try {
						fWriter.flush();
						fWriter.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	
				}*/
			}
		}
	}
	
	public void onClickOpenLogFileActivity(View view){
		if(!application.isDataProcessingRunning()){
			Intent intent = new Intent(this ,ManageLogFilesActivity.class);
			startActivityForResult(intent, REQUEST_LOG_FILE);
		} else{
			Toast.makeText(this, "Yout must stop processing first", Toast.LENGTH_SHORT).show();
		}
	}
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		switch (requestCode){
		case REQUEST_LOG_FILE:
			if(resultCode==Activity.RESULT_OK){
				// file selected
				lastLogFileView.setText(application.getSelectedLogFile().getName());
				Log.d("SELECTEDFILENAME", "RESULTOK");
				
			} else{
				// file not selected
				Log.d("RESULT", " Result Canceled");
			}
			break;
		case REQUEST_POSTURE_REFERENCE:
			if(resultCode==Activity.RESULT_OK){
				String selectedPosture = data.getStringExtra("data");
				Log.d("SELECTED POSTURE", selectedPosture);
				float[][][] savedStateOfPoints = getReferencePosturePointsFromFile(selectedPosture);
				float[][][] savedStateOfPointsInitial = getReferencePosturePointsFromFile(selectedPosture);
				application.savedStateOfPoints=savedStateOfPoints;
				application.savedStateOfPointsInitial=savedStateOfPointsInitial;
				Log.d("PROCESSING_ACTIVITY_RESULT", "OK");
				break;
			}
		default:
			break;
		}
	}

	public void onClickStampLog(View view){
		if(application.isDataProcessingRunning()){
			if(application.getSelectedLogFile()!=null){
				
				application.setStampPendingFlag(true);
				Toast.makeText(this, "Stamp in log created", Toast.LENGTH_SHORT).show();
				
			} else{
				Toast.makeText(this, "Create Log file first", Toast.LENGTH_SHORT).show();
			}
		} else{
			Toast.makeText(this, "Yous must start processing first", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
    public boolean onTouchEvent(MotionEvent me) {
        return gDetector.onTouchEvent(me);
    }
	public boolean onFling(MotionEvent start, MotionEvent finish, float xVelocity, float yVelocity) {
		Intent intent;
		if (start.getRawX() > finish.getRawX()) {
			//Log.d("fling","start - x: "+start.getRawX()+" y:"+start.getRawY());
			//Log.d("fling","finish- x: "+finish.getRawX()+" y:"+finish.getRawY());
			intent = new Intent(getApplicationContext(), DrawingActivity.class);
			startActivity(intent);
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		} 
		if(start.getRawX()<finish.getRawX()){
			intent = new Intent(getApplicationContext(), DataSourceActivity.class);
			startActivity(intent);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
		}
		return true;
	}
	
	/*unimplemented gesture listener callback*/
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
	// function that converts point array to string, for matlab
	public static String pointsToString(double[][][] points){
			String stringPoints="points(:,:,1) = [";
			int rows = points.length;
			System.out.println(rows);
			int cols = points[0].length;
			
			for(int i=0;i<rows;i++){
				for(int j=0;j<cols;j++){
					stringPoints=stringPoints+" "+points[i][j][0]; // printing x coordinate
					System.out.println(i+" "+j);	
				}
				stringPoints=stringPoints+"; \r\n";
			}
			stringPoints=stringPoints+"];\r\n points(:,:,2) = [";
			
			for(int i=0;i<rows;i++){
				for(int j=0;j<cols;j++){
					stringPoints=stringPoints+" "+points[i][j][1]; // printing x coordinate
				}
				stringPoints=stringPoints+"; \r\n";
			}
			
			stringPoints=stringPoints+"];\r\n points(:,:,3) = [";
			for(int i=0;i<rows;i++){
				for(int j=0;j<cols;j++){
					stringPoints=stringPoints+" "+points[i][j][2]; // printing x coordinate	
				}
				stringPoints=stringPoints+"; \r\n";
			}
			
			stringPoints=stringPoints+"];\r\n";
			return stringPoints;
		}
	/*method that converts normalized accelerometer data to string fr matlab*/
	public static String accDataToString(double[][] acc){
		String stringAcc = "acc = [";
		int rows=acc.length;
		for(int i=0;i<rows;i++){
			for(int j=0;j<3;j++){
				stringAcc=stringAcc+acc[i][j]+" ";
			}
			stringAcc=stringAcc+";\r\n";
		}	
		stringAcc=stringAcc+"];";
		return stringAcc;
	}
	

	/*method calculates average distance between saved points and current points for whole acc grid
	 * method logs each segment distance to file*/
	public static double getAvarageDistanceWithLogging(double[][][] state1 ,double[][][] state2, FileWriter fWriter){
		int sizeColumns = state1[0].length;
		String distancesString="\r\n";
		int sizeRows = state1.length;
		
		double distance=0;
		double deltaX_s=0; // saving temporary deltaX squared
		double deltaY_s=0; // saving temporary deltaY squared
		double deltaZ_s=0; //// saving temporary deltaZ squared
		for (int i=0; i<sizeRows;i++){
			for(int k=0;k<sizeColumns;k++){
				deltaX_s=Math.pow(state1[i][k][0]-state2[i][k][0],2);
				deltaY_s=Math.pow(state1[i][k][1]-state2[i][k][1],2);
				deltaZ_s=Math.pow(state1[i][k][2]-state2[i][k][2],2);
				distance=distance+Math.sqrt(deltaX_s+deltaY_s+deltaZ_s);
				distancesString = distancesString + Math.sqrt(deltaX_s+deltaY_s+deltaZ_s)+", ";
			}
		}
		if(fWriter!=null){
			try {
				fWriter.write(distancesString);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return distance/(sizeRows*sizeColumns); //returning the result
	}
	
	/*check each point distance with reference state points, if at least one distance is bigger than threshold
	 * function returns true. This value is later checked wheter to turn on feedback
	 */
	public static boolean checkEachSegmentDistanceWithLogging(double[][][] state1 ,double[][][] state2, double threshold, FileWriter fWriter){
		int sizeColumns = state1[0].length;
		String distancesString="\r\n";
		int sizeRows = state1.length;
		boolean isOverThreshold = false;
		
		double distance=0;
		double deltaX_s=0; // saving temporary deltaX squared
		double deltaY_s=0; // saving temporary deltaY squared
		double deltaZ_s=0; //// saving temporary deltaZ squared
		for (int i=0; i<sizeRows;i++){
			for(int k=0;k<sizeColumns;k++){
				deltaX_s=Math.pow(state1[i][k][0]-state2[i][k][0],2);
				deltaY_s=Math.pow(state1[i][k][1]-state2[i][k][1],2);
				deltaZ_s=Math.pow(state1[i][k][2]-state2[i][k][2],2);
				distance=distance+Math.sqrt(deltaX_s+deltaY_s+deltaZ_s);
				distancesString = distancesString + nf.format(Math.sqrt(deltaX_s+deltaY_s+deltaZ_s))+", ";
				if(Math.sqrt(deltaX_s+deltaY_s+deltaZ_s)>threshold){
					isOverThreshold = true;
				}
			}
		}
		if(fWriter!=null){
			try {
				fWriter.write(distancesString);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//return distance/(sizeRows*sizeColumns); //returning the result
		return isOverThreshold;
	}
	
	// method thad returns reference posture from file
	public float[][][] getReferencePosturePointsFromFile(String fileName){
		DataInputStream iStream = null;
		float referencePosturePointArray[][][] = new float [5][4][3];
		File postureFile=new File(Environment.getExternalStorageDirectory()+"/AccGridPostures",fileName);
		try{
			iStream = new DataInputStream(new FileInputStream(postureFile));
			int rows = 5;
			int cols = 4;
			int axes = 3;
			for(int i = 0; i<rows;i++){
				for(int j=0; j<cols;j++){
					for(int k=0; k<axes; k++){
						referencePosturePointArray[i][j][k]=iStream.readFloat();
					}
				}
			}
			iStream.close();
		} catch(IOException ex){
			
		} finally{
			try{
				iStream.close();
			} catch(IOException ex){
				
			}
		}
		return referencePosturePointArray;
	}
	
	public void saveDefaultPosture(String fileName, Segment[][] segments){
			File postureFile = new File(Environment.getExternalStorageDirectory()+"/"+SmartWearApplication.MAIN_FILE_FOLDER,fileName);
			try{
				if(postureFile.exists()){// if log file with that name exists, delete previous version
				postureFile.delete();
				}
				postureFile.createNewFile();
				}catch(IOException ex){
					//Toast.makeText(this, "Error creating posture file "+editPostureNameView.getText()+".dat", Toast.LENGTH_LONG);
				}
				DataOutputStream stream=null;
				try{
					stream = new DataOutputStream( new FileOutputStream(postureFile,true));
					int rows = segments.length;
					int cols = segments[0].length;
					int axes = segments[0][0].center.length;
					 for(int i=0; i<rows; i++){
					   for(int j=0; j<cols; j++){
					    	for(int k = 0; k<axes; k++){
				    		stream.writeFloat(segments[i][j].center[k]);
				    	}
				    }
				}
				stream.close();

				}
				catch(IOException ex){	
				}
				finally{
					try{
						stream.close();
					} catch(IOException ex){
					
				    }
				}
	}
}
