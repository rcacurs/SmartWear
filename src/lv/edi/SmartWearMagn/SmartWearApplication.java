package lv.edi.SmartWearMagn;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import lv.edi.SmartWearProcessing.Segment;
import lv.edi.SmartWearProcessing.Sensor;
import lv.edi.SmartWearProcessing.SensorDataProcessing;

import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
/**Class for storing global data within application*/
public class SmartWearApplication extends Application implements OnSharedPreferenceChangeListener{
	private static final String TAG_APPLICATION = "Application Object";
	private static SmartWearApplication singleton;
	static final String MAIN_FILE_FOLDER = "SmartWear";
	private static final String SESSION_LOG_FOLDER = "SessionLog";
	private static final String LOG_FOLDER = "LogFiles";
	private static final String POSTURE_FOLDER = "Postures";
	private static final String POSTURE_IMG_FOLDER = "PosturesIMG";
	private static final String CALIBRATION_DATA_FOLDER = "CalibrationData";
	private static final String CALIBRATION_DATA_FILE_NAME = "calibration_data.csv";
	
	static final String DEFAULT_POSTURE_FILE = "DefaultPosture.dat";
	private short battery_level=100;
	public static final byte NR_OF_SENSORS = 63; // number of sensors in grid
	static final byte GRID_COLS = 7; // number of sensor columns in sensor grid
	static final byte GRID_ROWS = 9; // number of snsor rows in sensor grid
    private float Z_ANGLE1 = 0; // 65
	private float Z_ANGLE2 = 0; // 45
	private float Z_ANGLE3 = 0; // 25
	private int referenceRow=1;
	private int referenceCol=3;
	private float colormapSensitivity=1;
	
	BluetoothService bluetoothService; // blue tooth service instance
	private ReadingFromFileService readingFromFileService; // service that retrieves accDataFromFile
	private ProcessingService processingService; // service that processes accelerometer data compares saved and current posture
	Activity bluetoothConnectionActivity;// blue tooth connection activity instance
	ProcessingActivity processingActivity; // instance of processing activity
	
	Sensor[] sensorArray = new Sensor[NR_OF_SENSORS]; // array of Sensor object refferences
	
	
	// sensors in sensor grid are counted from lower right sensor
	// ..........................................
	//.........................sensorArray[0][1]  sensorArray[0][0]

	Sensor[][] sensorGridArray = new Sensor[GRID_ROWS][GRID_COLS]; // array of Sensor object refferences ordered in array that represents grid
	
	Segment[][] currentStateSegments = new Segment[GRID_ROWS][GRID_COLS];
	Segment[][] refferenceStateSegmentsInitial = new Segment[GRID_ROWS][GRID_COLS];
	Segment[][] refferenceStateSegments = new Segment[GRID_ROWS][GRID_COLS];

	
	float[][] segmentStateDistances = new float[GRID_ROWS][GRID_COLS];
	
	float[][][] savedStateOfPoints =new float[5][4][3]; // savedStateOfAccPoints array compensated
	float[][][] currentAccPoints = new float[5][4][3]; // current Accelerometer grid state points
	float[][][] savedStateOfPointsInitial = new float[5][4][3]; // saved state of points
	float[][][] savedStateOfAngles = new float[3][4][2];
	float[][][] currentAccAngles = new float[3][4][2];
	float[][] savedStateRot = new float[3][3];
	float[][] currentStateRot = new float[3][3];
//	static final byte drawingModelColormap[][]={{0, (byte)54, (byte)255},
//												{0, (byte)159, (byte)255},
//												{0, (byte)255, (byte)255},
//												{96, (byte)255, (byte)159},
//												{(byte)191, (byte)255, (byte)64},
//												{(byte)255, (byte)223, 0},
//												{(byte)255, (byte)128, 0},
//												{(byte)255, (byte)32, 0},
//												{(byte)191, 0, 0}};
	static final byte drawingModelColormap[][]={{0, (byte)57, (byte)255},
												{0, (byte)128, (byte)255},
												{0, (byte)198, (byte)255},
												{8, (byte)255, (byte)247},
												{(byte)43, (byte)255, (byte)212},
												{(byte)78, (byte)255, (byte)177},
												{(byte)126, (byte)255, (byte)129},
												{(byte)190, (byte)255, 65},
												{(byte)224, (byte)225, 31},
												{(byte)255, (byte)170, (byte)0},
												{(byte)255, 0, 0}
												};
	
												
	byte[] drawingModelColors = new byte[GRID_COLS*GRID_ROWS*4]; // color array for 3D model
	private SharedPreferences prefs; // object that represetns global preferences
	private boolean isVibrateFeedbackOn; // tells if vibrate feedback is on
	private boolean isSoundFeedbackOn; // tells if sound feedback is on
	private float thresholdSetting; // threshold setting
	private float statisticsIntervalSetting;// interval setting for threshold
	private boolean isLoggingFeatureEnabled; // logging feature setting
	private float accSegmentDistance;// vertical distance between accelerometer segments
	private float personsHeight; //persons height
	private String algorithm; // selected algorithm
	protected File logDirectory; // directory were log files will be stored
	private File selectedLogFile; // Last created log file, that should be used for data logging
	private DataOutputStream logFileStream; // output stream for selected log file
	//private File defaultPostureFile; // defaultPostureFile. last saved posture is saved there
	private boolean isDataProcessingRunning = false;
	private boolean stampPendingFlag=false;
	private String dataSource="Bluetooth";
	private boolean isReadingFromFile=false;
	private File dataSourceFile=null;
	private File sessionLogFile;
	private File sessionLogDirectory;
	private File postureDirectory;
	private File postureDirectoryIMG;
	private File calibrationDataFile;
	private Calendar calendar;
	private boolean stateSaved=false;
	protected Handler statisticsHandler;
	
	protected XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
	/** The main renderer that includes all the renderers customizing a chart. */
	protected XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
	/** The most recently added series. */
	protected XYSeries mCurrentSeries;
	/** The most recently created renderer, customizing the current series. */

	@Override

	// called when application object is created
	public void onCreate(){
		super.onCreate();
		
		// creating sensor objects, and filling sensor arrays;
		// create directory for log files
		logDirectory = new File(Environment.getExternalStorageDirectory() + "/"+MAIN_FILE_FOLDER+"/"+LOG_FOLDER);
		Log.d(TAG_APPLICATION, logDirectory.getPath());
		sessionLogDirectory = new File(Environment.getExternalStorageDirectory() + "/"+MAIN_FILE_FOLDER+"/"+SESSION_LOG_FOLDER);
		postureDirectory = new File(Environment.getExternalStorageDirectory()+"/"+MAIN_FILE_FOLDER+"/"+POSTURE_FOLDER);
		postureDirectoryIMG = new File(Environment.getExternalStorageDirectory()+"/"+MAIN_FILE_FOLDER+"/"+POSTURE_IMG_FOLDER);
		calibrationDataFile = new File(Environment.getExternalStorageDirectory()+"/"+MAIN_FILE_FOLDER+"/"+CALIBRATION_DATA_FOLDER+"/"+CALIBRATION_DATA_FILE_NAME);
		
		if(!logDirectory.exists()){
		   logDirectory.mkdirs(); //directory is created;
		}
		Log.d(TAG_APPLICATION, "session log directory"+sessionLogDirectory.getPath());
		if(!sessionLogDirectory.exists()){
			Log.d(TAG_APPLICATION, "creating directory"+sessionLogDirectory.getPath());
			sessionLogDirectory.mkdirs();
		}
		if(!postureDirectory.exists()){
			postureDirectory.mkdirs();
		}
		if(!postureDirectoryIMG.exists()){
			postureDirectoryIMG.mkdirs();
		}
		sessionLogFile = new File(Environment.getExternalStorageDirectory()+"/"+MAIN_FILE_FOLDER+"/"+SESSION_LOG_FOLDER,"session.log");
		try{
			sessionLogFile.createNewFile();
		} catch(IOException ex){
			
		}
		Log.d(TAG_APPLICATION, logDirectory.getPath());
		
		//Segment.setDefaultCenters(currentStateSegments);
		// read default saved state from file
		
		singleton = this;
		calendar = Calendar.getInstance();
		prefs = PreferenceManager.getDefaultSharedPreferences(this); // getting the reference to preferences
		prefs.registerOnSharedPreferenceChangeListener(this); // set application class to be listener of preference changes
		bluetoothService = new BluetoothService(singleton); // creating new bluetooth service instance
		isVibrateFeedbackOn = prefs.getBoolean("vibrate_feedback", false); // get value from shared preferences
		isSoundFeedbackOn = prefs.getBoolean("alert_feedback", false); // get value from shared preferences
		String thresholdSettingS = prefs.getString("set_threshold", "3.0");
		String personsHeightS = prefs.getString("set_height", "1.83");
		String statisticsSettingS = prefs.getString("set_statistics_interval", "3.0");
		
		String angle1 = prefs.getString("set_angle1", "0.0");
		if(!angle1.equals("")){
			Z_ANGLE1=Float.parseFloat(angle1);
		} else{
			Z_ANGLE1=0;
		}
		
		String angle2 = prefs.getString("set_angle2", "0.0");
		if(!angle2.equals("")){
			Z_ANGLE2=Float.parseFloat(angle2);
		} else{
			Z_ANGLE2=0;
		}
		
		String angle3 = prefs.getString("set_angle3", "0.0");
		if(!angle1.equals("")){
			Z_ANGLE3=Float.parseFloat(angle3);
		} else{
			Z_ANGLE3=0;
		}
		
		String refRow=prefs.getString("set_reference_row","1");
		
		if(!refRow.equals("")){
			referenceRow=Integer.parseInt(refRow);
		} else{
			referenceRow=1;
		}
		
        String refCol=prefs.getString("set_reference_col","3");
		
		if(!refCol.equals("")){
			referenceCol=Integer.parseInt(refCol);
		} else{
			referenceCol=3;
		}
		
		
		if(!thresholdSettingS.equals("")){
			thresholdSetting = Float.parseFloat(thresholdSettingS);
		} else{
			thresholdSetting = 3.0f;
		}
		if(!personsHeightS.equals("")){
			personsHeight = Float.parseFloat(personsHeightS); // getting threshold setting from shared preferences
			//Segment.initialCross[0][2] = 100*Float.parseFloat(personsHeightS)*3/(8*GRID_ROWS*2); // getting person's height setting, and calculating vertical distance between acc segments
			//Segment.initialCross[2][2] = -100*Float.parseFloat(personsHeightS)*3/(8*GRID_ROWS*2); // getting person's height setting, and calculating vertical distance between acc segments
		} else{
			personsHeight = 1.83f;
			accSegmentDistance = (float)(100*1.83*3/(8*GRID_ROWS*2));
			//Segment.initialCross[2][2] = -(float)(100*1.83*3/(8*GRID_ROWS*2)); 
		}
		
		if(!statisticsSettingS.equals("")){
			statisticsIntervalSetting = Float.parseFloat(statisticsSettingS);
		} else{
			statisticsIntervalSetting = 1.0f;
		}
		
		
		//algorithm = prefs.getString("set_algorithm", "distances");
		//isLoggingFeatureEnabled = prefs.getBoolean("log_enable", false);
		
		// create directory for log files
		logDirectory = new File(Environment.getExternalStorageDirectory() + "/"+MAIN_FILE_FOLDER+"/"+LOG_FOLDER);
		Log.d(TAG_APPLICATION, logDirectory.getPath());
		sessionLogDirectory = new File(Environment.getExternalStorageDirectory() + "/"+MAIN_FILE_FOLDER+"/"+SESSION_LOG_FOLDER);
		postureDirectory = new File(Environment.getExternalStorageDirectory()+"/"+MAIN_FILE_FOLDER+"/"+POSTURE_FOLDER);
		postureDirectoryIMG = new File(Environment.getExternalStorageDirectory()+"/"+MAIN_FILE_FOLDER+"/"+POSTURE_IMG_FOLDER);
		if(!logDirectory.exists()){
		   logDirectory.mkdirs(); //directory is created;
		}
		Log.d(TAG_APPLICATION, "session log directory"+sessionLogDirectory.getPath());
		if(!sessionLogDirectory.exists()){
			Log.d(TAG_APPLICATION, "creating directory"+sessionLogDirectory.getPath());
			sessionLogDirectory.mkdirs();
		}
		if(!postureDirectory.exists()){
			postureDirectory.mkdirs();
		}
		if(!postureDirectoryIMG.exists()){
			postureDirectoryIMG.mkdirs();
		}
		sessionLogFile = new File(Environment.getExternalStorageDirectory()+"/"+MAIN_FILE_FOLDER+"/"+SESSION_LOG_FOLDER,"session.log");
		try{
			sessionLogFile.createNewFile();
		} catch(IOException ex){
			
		}
		Log.d(TAG_APPLICATION, logDirectory.getPath());
		readingFromFileService = new ReadingFromFileService(singleton); // create service that reads data from file
		processingService = new ProcessingService(singleton, bluetoothService, readingFromFileService);
	
		
		int[] sensorIndexes;
		for(int i=0;i<NR_OF_SENSORS;i++){
			sensorIndexes=SensorDataProcessing.getIndexes(i, GRID_ROWS, GRID_COLS);// get array of sensor grid indexes
			if(sensorIndexes[1]%2==0){
				sensorArray[i]=new Sensor(i,true);	// create sensor object
				sensorGridArray[sensorIndexes[0]][sensorIndexes[1]]=sensorArray[i]; // save reference of sensor object in sensor Grid Array
			} else{
				sensorArray[i]=new Sensor(i, false);
				sensorGridArray[sensorIndexes[0]][sensorIndexes[1]]=sensorArray[i]; // save reference to sensor obect in sensor grid array
			}
			
		}
		for(int i=0;i<GRID_ROWS;i++){
			for(int j=0;j<GRID_COLS;j++){
				switch(j){
				case 0:
					currentStateSegments[i][j] = new Segment(-(float)Math.toRadians(Z_ANGLE1));
					refferenceStateSegments[i][j] = new Segment(-(float)Math.toRadians(Z_ANGLE1));
					refferenceStateSegmentsInitial[i][j] = new Segment(-(float)Math.toRadians(Z_ANGLE1));
					break;
				case 1:
					currentStateSegments[i][j] = new Segment(-(float)Math.toRadians(Z_ANGLE2));
					refferenceStateSegments[i][j] = new Segment(-(float)Math.toRadians(Z_ANGLE2));
					refferenceStateSegmentsInitial[i][j] = new Segment(-(float)Math.toRadians(Z_ANGLE2));
					break;
				case 2:
					currentStateSegments[i][j] = new Segment(-(float)Math.toRadians(Z_ANGLE3));
					refferenceStateSegments[i][j] = new Segment(-(float)Math.toRadians(Z_ANGLE3));
					refferenceStateSegmentsInitial[i][j] = new Segment(-(float)Math.toRadians(Z_ANGLE3));
					break;
				case 3:
					currentStateSegments[i][j] = new Segment(0f);
					refferenceStateSegments[i][j] = new Segment(0f);
					refferenceStateSegmentsInitial[i][j] = new Segment(0f);
					break;
				case 4:
					currentStateSegments[i][j] = new Segment((float)Math.toRadians(Z_ANGLE3));
					refferenceStateSegments[i][j] = new Segment((float)Math.toRadians(Z_ANGLE3));
					refferenceStateSegmentsInitial[i][j] = new Segment((float)Math.toRadians(Z_ANGLE3));
					break;
				case 5:
					currentStateSegments[i][j] = new Segment((float)Math.toRadians(Z_ANGLE2));
					refferenceStateSegments[i][j] = new Segment((float)Math.toRadians(Z_ANGLE2));
					refferenceStateSegmentsInitial[i][j] = new Segment((float)Math.toRadians(Z_ANGLE2));
					break;
				case 6:
					currentStateSegments[i][j] = new Segment((float)Math.toRadians(Z_ANGLE1));
					refferenceStateSegments[i][j] = new Segment((float)Math.toRadians(Z_ANGLE1));
					refferenceStateSegmentsInitial[i][j] = new Segment((float)Math.toRadians(Z_ANGLE1));
					break;
				default:
					break;
				}
				
			}
		}
		// default acc grid colors
		for(int i = 0; i<GRID_ROWS*GRID_COLS;i++){
			drawingModelColors[i*4]=0;
			drawingModelColors[i*4+1]=0;
			drawingModelColors[i*4+2]=(byte)255;
			drawingModelColors[i*4+3]=(byte)255;
		}
		//Segment.setDefaultCenters(currentStateSegments);
		// read default saved state from file
		getPosturePointsFromFile(DEFAULT_POSTURE_FILE, refferenceStateSegments);
		getPosturePointsFromFile(DEFAULT_POSTURE_FILE, refferenceStateSegmentsInitial);
		createNewDataSetForPlot();
		
		// load calibration data if it exists
		if(calibrationDataFile.exists()){
			Log.d("CALIB_DATA", "WE HAVE CALIBRATION DATA");
			try {
				Sensor.setGridMagnetometerCalibData(calibrationDataFile, sensorGridArray);
			} catch (IOException e) {
				Log.d("CLIB_DATA", "PROBLEM LOADING CALIBRATION VALUES FROM CALIBRATION FILE");
			}
		} else{
			Log.d("CALIB_DATA", "WE DON'T HAVE CALIBRATION DATA");
		}
	}
	
	public boolean isVibrateFeedbackOn(){
		return isVibrateFeedbackOn;
	}
	public boolean isSoundFeedbackOn(){
		return isSoundFeedbackOn;
	}
	public float getSegmentDistance(){
		return accSegmentDistance;
	}
	public float getThreshold(){
		return thresholdSetting;
	}
	public float getPersonsHeight(){
		return personsHeight;
	}
	public synchronized String getAlgorithm(){
		return algorithm;
	}
	public synchronized File getLogFileDirectory(){
		return logDirectory;
	}
	public synchronized File getSelectedLogFile(){
		return selectedLogFile;
	}
	public synchronized void setSelectedLogFileStream(DataOutputStream stream){
		logFileStream=stream;
	}
	public synchronized DataOutputStream getSelectedLogFileStream(){
		return logFileStream;
	}

	public synchronized File getPostureDirectory(){
		return postureDirectory;
	}
	public synchronized File getPostureIMGDirectory(){
		return postureDirectoryIMG;
	}
	public synchronized void setSelectedLogFile(File lastLogFile){
		this.selectedLogFile=lastLogFile;
	}
	public synchronized void setStampPendingFlag(boolean value){
		stampPendingFlag=value;
	}
	public synchronized boolean stampPendingFlag(){
		return stampPendingFlag;
	}
	
	// tells if logging feature in preferences is enabled
	public synchronized boolean isLoggingFeatureEnabled(){
		return isLoggingFeatureEnabled;
	}
	
	public SmartWearApplication getInstance(){
		return singleton;
	}
	public synchronized void setDataProcessingRunning(boolean value){
		isDataProcessingRunning = value;
	}
	public synchronized boolean isDataProcessingRunning(){
		return isDataProcessingRunning;
	}
	// returns currently selected data source
	public synchronized String getDataSource(){
		return dataSource;
	}
	// sets currently selected data source
	public synchronized void setDataSource(String dataSource){
		this.dataSource=dataSource;
	}
	// returns if currently application reading from log file
	public synchronized boolean isReadingFromFile(){
		return isReadingFromFile;
	}
	// set reading from file status
	public synchronized void setReadingFromFile(boolean isReadingFromFile){
		this.isReadingFromFile = isReadingFromFile;
	}
	public synchronized void setDataSourceFile(File dataSourceFile){
		this.dataSourceFile=dataSourceFile;
	}
	public synchronized File getDataSourceFile(){
		return dataSourceFile;
	}
	// return object, that reads acc data from file
	public synchronized ReadingFromFileService getReadingFromFileService(){
		return readingFromFileService;
	}
	// returns reference to bluetooth service instance
	public synchronized BluetoothService getBluetoothService(){
		return bluetoothService;
	}
	public synchronized ProcessingService getProcessingService(){
		return processingService;
	}
	// returns instance of processingActivity
	public synchronized ProcessingActivity getPorcessingActivity(){
		return processingActivity;
	}
	// sets instance of processing activity
	public synchronized void setProcessingActivity(ProcessingActivity processingActivity){
		this.processingActivity=processingActivity;
	}
	
	//shared preference lister's callback, called when pregerences are changed
	public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key){
		if(key.equals("vibrate_feedback")){
			Log.d(TAG_APPLICATION,"Vibrate setting changed");
			isVibrateFeedbackOn = sharedPrefs.getBoolean(key,false);
			Log.d(TAG_APPLICATION, "vibrate value changed to "+isVibrateFeedbackOn);
		}
		if(key.equals("alert_feedback")){
			Log.d(TAG_APPLICATION,"Alert setting changed");
			isSoundFeedbackOn = sharedPrefs.getBoolean(key, false);
			Log.d(TAG_APPLICATION, "Alert setting changed to "+isSoundFeedbackOn);
		}
		if(key.equals("set_threshold")){
			if(!(sharedPrefs.getString("set_threshold", "3.0").equals(""))){
				thresholdSetting = Float.parseFloat(sharedPrefs.getString("set_threshold", "3.0")); // getting changed threshold setting from shared preference
				Log.d(TAG_APPLICATION, "threshold Setting changed to "+thresholdSetting);
			} else
				thresholdSetting = 3.0f;
		}
		if(key.equals("set_height")){
			if(!(sharedPrefs.getString("set_height", "1.83").equals(""))){
				personsHeight =  Float.parseFloat(sharedPrefs.getString("set_height", "1.83"));
				//Segment.initialCross[0][2] = 100*personsHeight*3/(8*GRID_ROWS*2); // getting changed height setting from shared preference
				//Segment.initialCross[2][2] = -100*personsHeight*3/(8*GRID_ROWS*2);
				//Log.d(TAG_APPLICATION, "acc segment distance changed to "+accSegmentDistance+" Persons height: "+personsHeight+"$"+ 100*personsHeight*3/(8*GRID_ROWS*2)+"$segments coords: "+Segment.initialCross[0][2]+" "+Segment.initialCross[1][2]+" "+Segment.initialCross[2][2]+" "+Segment.initialCross[3][2]);
			} else{
				//Segment.initialCross[0][2] = (float)100*1.83f*3/(8*GRID_ROWS*2); // getting changed height setting from shared preference
			   // Segment.initialCross[2][2] = (float)-100*1.83f*3/(8*GRID_ROWS*2)key.;
			}
		}
		if(key.equals("set_statistics_interval")){
			if(!(sharedPrefs.getString("set_statistics_interval", "1.0").equals(""))){
				statisticsIntervalSetting=Float.parseFloat(sharedPrefs.getString("set_statistics_interval", "1.0"));
				Log.d("statistics updated",""+statisticsIntervalSetting);
			}
			else{
				statisticsIntervalSetting=1.0f;
				Log.d("statistics updated",""+statisticsIntervalSetting);
			}
				
			
		}
		if(key.equals("select_algorithm")){
			algorithm = sharedPrefs.getString("select_algorithm", "distances");
			Log.d(TAG_APPLICATION, "algorithm select setting changed to "+algorithm );
		}
		if(key.equals("log_enable")){
			isLoggingFeatureEnabled=sharedPrefs.getBoolean(key,false);
			Log.d(TAG_APPLICATION, "log enable setting changed to "+ isLoggingFeatureEnabled);
			
		}
		if(key.equals("set_angle1")){
			if(!(sharedPrefs.getString("set_angle1", "0.0").equals(""))){
				Z_ANGLE1 = Float.parseFloat(sharedPrefs.getString("set_angle1", "0.0")); // getting changed threshold setting from shared preference
				Log.d(TAG_APPLICATION, "angle1 changed to "+Z_ANGLE1);
			} else{
				Z_ANGLE1 = 0.0f;
		    }
			for(int i=0;i<GRID_ROWS;i++){
				currentStateSegments[i][0].setSegmentZRotation(-(float)Math.toRadians(Z_ANGLE1));
				refferenceStateSegments[i][0].setSegmentZRotation(-(float)Math.toRadians(Z_ANGLE1));
				refferenceStateSegmentsInitial[i][0].setSegmentZRotation(-(float)Math.toRadians(Z_ANGLE1));
				
				currentStateSegments[i][6].setSegmentZRotation((float)Math.toRadians(Z_ANGLE1));
				refferenceStateSegments[i][6].setSegmentZRotation((float)Math.toRadians(Z_ANGLE1));
				refferenceStateSegmentsInitial[i][6].setSegmentZRotation((float)Math.toRadians(Z_ANGLE1));
			}
		}
		if(key.equals("set_angle2")){
			if(!(sharedPrefs.getString("set_angle2", "0.0").equals(""))){
				Z_ANGLE2 = Float.parseFloat(sharedPrefs.getString("set_angle2", "0.0")); // getting changed threshold setting from shared preference
				Log.d(TAG_APPLICATION, "angle2 changed to "+Z_ANGLE2);
			} else{
				Z_ANGLE2 = 0.0f;
		    }
			
			for(int i=0;i<GRID_ROWS;i++){
				currentStateSegments[i][1].setSegmentZRotation(-(float)Math.toRadians(Z_ANGLE2));
				refferenceStateSegments[i][1].setSegmentZRotation(-(float)Math.toRadians(Z_ANGLE2));
				refferenceStateSegmentsInitial[i][1].setSegmentZRotation(-(float)Math.toRadians(Z_ANGLE2));
				
				currentStateSegments[i][5].setSegmentZRotation((float)Math.toRadians(Z_ANGLE2));
				refferenceStateSegments[i][5].setSegmentZRotation((float)Math.toRadians(Z_ANGLE2));
				refferenceStateSegmentsInitial[i][5].setSegmentZRotation((float)Math.toRadians(Z_ANGLE2));
			}

		}
		
		if(key.equals("set_angle3")){
			if(!(sharedPrefs.getString("set_angle3", "0.0").equals(""))){
				Z_ANGLE3 = Float.parseFloat(sharedPrefs.getString("set_angle3", "0.0")); // getting changed threshold setting from shared preference
				Log.d(TAG_APPLICATION, "angle3 changed to "+Z_ANGLE3);
			} else{
				Z_ANGLE3 = 0.0f;
		    }
			
			for(int i=0;i<GRID_ROWS;i++){
				currentStateSegments[i][2].setSegmentZRotation(-(float)Math.toRadians(Z_ANGLE3));
				refferenceStateSegments[i][2].setSegmentZRotation(-(float)Math.toRadians(Z_ANGLE3));
				refferenceStateSegmentsInitial[i][2].setSegmentZRotation(-(float)Math.toRadians(Z_ANGLE3));
				
				currentStateSegments[i][4].setSegmentZRotation((float)Math.toRadians(Z_ANGLE3));
				refferenceStateSegments[i][4].setSegmentZRotation((float)Math.toRadians(Z_ANGLE3));
				refferenceStateSegmentsInitial[i][4].setSegmentZRotation((float)Math.toRadians(Z_ANGLE3));
			}
		}
		
		if(key.equals("set_reference_row")){
			if(!(sharedPrefs.getString("set_reference_row", "1").equals(""))){
				referenceRow = Integer.parseInt(sharedPrefs.getString("set_reference_row", "1")); // getting changed threshold setting from shared preference
				currentStateSegments[referenceRow][referenceCol].center[0]=0;
				currentStateSegments[referenceRow][referenceCol].center[1]=0;
				currentStateSegments[referenceRow][referenceCol].center[2]=0;
				
				
				refferenceStateSegments[referenceRow][referenceCol].center[0]=0;
				refferenceStateSegments[referenceRow][referenceCol].center[1]=0;
				refferenceStateSegments[referenceRow][referenceCol].center[2]=0;
				
				refferenceStateSegmentsInitial[referenceRow][referenceCol].center[0]=0;
				refferenceStateSegmentsInitial[referenceRow][referenceCol].center[1]=0;
				refferenceStateSegmentsInitial[referenceRow][referenceCol].center[2]=0;
				
				Segment.setSegmentCenters(refferenceStateSegments, (short)getReferenceRow(), (short)getReferenceCol());
				Segment.setSegmentCenters(refferenceStateSegmentsInitial, (short)getReferenceRow(), (short)getReferenceCol());
				saveDefaultPosture(SmartWearApplication.DEFAULT_POSTURE_FILE, refferenceStateSegments);
				
				Log.d(TAG_APPLICATION, "reference row changed to "+referenceRow);
			} else{
				referenceRow = 1;
		    }
		}
		
		if(key.equals("set_reference_col")){
			if(!(sharedPrefs.getString("set_reference_col", "3").equals(""))){
				referenceCol = Integer.parseInt(sharedPrefs.getString("set_reference_col", "3")); // getting changed threshold setting from shared preference
				Log.d(TAG_APPLICATION, "reference col changed to "+referenceCol);
				
				currentStateSegments[referenceRow][referenceCol].center[0]=0;
				currentStateSegments[referenceRow][referenceCol].center[1]=0;
				currentStateSegments[referenceRow][referenceCol].center[2]=0;
				
				
				refferenceStateSegments[referenceRow][referenceCol].center[0]=0;
				refferenceStateSegments[referenceRow][referenceCol].center[1]=0;
				refferenceStateSegments[referenceRow][referenceCol].center[2]=0;
				
				refferenceStateSegmentsInitial[referenceRow][referenceCol].center[0]=0;
				refferenceStateSegmentsInitial[referenceRow][referenceCol].center[1]=0;
				refferenceStateSegmentsInitial[referenceRow][referenceCol].center[2]=0;
				
				Segment.setSegmentCenters(refferenceStateSegments, (short)getReferenceRow(), (short)getReferenceCol());
				Segment.setSegmentCenters(refferenceStateSegmentsInitial, (short)getReferenceRow(), (short)getReferenceCol());
				saveDefaultPosture(SmartWearApplication.DEFAULT_POSTURE_FILE, refferenceStateSegments);
			} else{
				referenceCol = 3;
		    }
		}
		if(key.equals("set_colormap_sensitivity")){
			if(!(sharedPrefs.getString("set_colormap_sensitivity", "1").equals(""))){
				colormapSensitivity = Float.parseFloat(sharedPrefs.getString("set_colormap_sensitivity", "1")); // getting changed threshold setting from shared preference
				Log.d(TAG_APPLICATION, "colormap sensitivity changed to "+colormapSensitivity);
			} else{
				colormapSensitivity = 1;
		    }
		}
	}
	public synchronized File getSessionLogFile(){
		return sessionLogFile;
	}
	public float getZAngle1(){
		return Z_ANGLE1;
	}
	
	public float getZAngle2(){
		return Z_ANGLE2;
	}
	public float getZAngle3(){
		return Z_ANGLE3;
	}
	public int getReferenceRow(){
		return referenceRow;
	}
	public int getReferenceCol(){
		return referenceCol;
	}
	public synchronized Calendar getCalendar(){
		return calendar;
	}
	public synchronized float getStatisticsInterval(){
		return statisticsIntervalSetting;
	}
	public boolean isStateSaved(){
		return stateSaved;
	}
	public void setStateSaved(boolean value){
		stateSaved=true;
	}
	public byte[] getDrawingModelColors(){
		return drawingModelColors;
	}
	public short getBatteryLevel(){
		return battery_level;
	}
	public void setBatteryLevel(short battery_level){
		this.battery_level=battery_level;
	}

	public void updateDrawingModelColors(float[][] distances){
		int rows = distances.length;;
		int cols = distances[0].length;
		int colorIndex = 0;
		float distance;
		int colorMapIndex;
		for(int i=0;i<rows;i++){
			for(int j=0;j<cols;j++){
				distance=distances[i][j];
				colorMapIndex=(int)(colormapSensitivity*distance*(10/(thresholdSetting)));//offset for colormap
				if(colorMapIndex>=11){
					colorMapIndex=10;
				}
				drawingModelColors[colorIndex*4]=drawingModelColormap[colorMapIndex][0];
				drawingModelColors[colorIndex*4+1]=drawingModelColormap[colorMapIndex][1];
				drawingModelColors[colorIndex*4+2]=drawingModelColormap[colorMapIndex][2];
				drawingModelColors[colorIndex*4+3]=1;	

				//Log.d("COLORMAPING",""+colorIndex);
				colorIndex++;
			}
		}
	}
	// returns array of references of sensor object. array is indexes by sensor indexes
	public synchronized Sensor[]  getSensorArray(){
		return sensorArray;
	}
	public void createNewDataSetForPlot(){
		mRenderer = new XYMultipleSeriesRenderer();
		 // values for statistics screen
		// set some properties on the main renderer
	    mRenderer.setApplyBackgroundColor(true);
	    mRenderer.setBackgroundColor(Color.argb(100, 50, 50, 50));
	    mRenderer.setAxisTitleTextSize(30);
	    mRenderer.setChartTitleTextSize(20);
	    mRenderer.setLabelsTextSize(30);
	    mRenderer.setLegendTextSize(10);
	    mRenderer.setShowLegend(false);
	    mRenderer.setMargins(new int[] { 30, 80, 15, 50 });
	    double range[]={0,100, 0, 120};
	    mRenderer.setPanLimits(range);
	    mRenderer.setZoomLimits(range);
	    mRenderer.setRange(range);
	    mRenderer.setZoomButtonsVisible(true);
	    mRenderer.setPointSize(5);
	    mRenderer.setXTitle("Time [min]");
	    mRenderer.setYTitle("Good Time Percentage [%]");
	    mRenderer.setXLabelsPadding(5);
	    mRenderer.setYLabelsPadding(10);
	    mRenderer.setGridColor(Color.argb(100, 0, 0, 0));
	    mRenderer.setShowGrid(true);
	    
	    mCurrentSeries = new XYSeries("Good Posture Time Percentage");
	    mDataset.addSeries(mCurrentSeries);
	    
	    XYSeriesRenderer renderer = new XYSeriesRenderer();
	    renderer.setPointStyle(PointStyle.CIRCLE);
	    renderer.setFillPoints(true);
        mRenderer.addSeriesRenderer(renderer);
	    mCurrentSeries.add(0, 0);

		
		mDataset = new XYMultipleSeriesDataset();
	    mCurrentSeries = new XYSeries("Good Posture Time Percentage");
	    mDataset.addSeries(mCurrentSeries);
	    
	}
	
	public void getPosturePointsFromFile(String fileName, Segment segments[][]){
		DataInputStream iStream = null;
		File postureFile= new File(Environment.getExternalStorageDirectory() + "/"+MAIN_FILE_FOLDER+"/", fileName);
		if(!postureFile.exists()){
			try {
				postureFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Toast.makeText(this, "error creating file", Toast.LENGTH_LONG).show();
			}
		}
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
