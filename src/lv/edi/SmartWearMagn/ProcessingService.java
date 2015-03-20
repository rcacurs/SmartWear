package lv.edi.SmartWearMagn;

import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;

import lv.edi.SmartWearMagn.R;
import lv.edi.SmartWearProcessing.Segment;
import lv.edi.SmartWearProcessing.SensorDataProcessing;
import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;

public class ProcessingService {
	private SmartWearApplication application;
	private BluetoothService bluetoothService;
	//private ReadingFromFileService readingFromFileService;
	//private CalculateDifferenceThread calculateDifferenceThread;
	Timer processingTimer;
	Timer statisticsTimer;
	private boolean isOverThreshold = false;
	private float averageDistance = 0;
	private float averageDistanceSagital = 0;
	private float averageDistanceCoronal = 0;
	private float maxDistance = 0;
	//private float[][] distances= new float[5][4];
	private float maxCoronalDistance = 0;
	private float maxSagitalDistance = 0;
	//private boolean runThread = false; // while this is true thread's main cycle is executed
	private boolean isProcessing = false;
	
	private long startTime=0; // start time for good or bad posture time interval
	private long endTime=0; // end time for good or badposture time interval
	private long goodTime=1; // time when posture is good
	private long goodTimeAccum=0; // state is good accumulated
	private long badTimeAccum=0; // state is bad accumulated
	private long tic=0;
	private long toc=0;
	//private long goodTimeIntervalled; // time when state is good
	private long goodTimePrev=0;      // previous good time value
	private long badTimePrev=0;       // previous bad time value
	private long goodTimeInterv=0;    //
	private long badTimeInterv=0;     //
	private double percentageIntervalled=0;;
	private long badTime=0; // time when posture is bad
	//private long curTime=0; // current time for statistics
	private long endTimeD=500; //end time for delay time for bad time
	private boolean stateIsGoodTemp=true ; // temporary state id good time
	private boolean stateIsGood = true;	// indicator that posture is good	
	private float goodTimePercentage = (float)goodTime/((float)goodTime+(float)badTime)*100;
	private static NumberFormat nf; // number format object to format number


	
	Vibrator mVibro; // vibrator instance for feedback
	MediaPlayer mp; // instance for sound alert feedback
	
	
	
	public ProcessingService(SmartWearApplication application, BluetoothService bluetoothService, ReadingFromFileService readingFromFileService){
		this.application = application;
		this.bluetoothService = bluetoothService;
		//this.readingFromFileService = readingFromFileService;
		
		nf = NumberFormat.getInstance(); // get instance for your locale
		nf.setMaximumFractionDigits(1); // set decimal places
		nf.setMinimumFractionDigits(1); //set minimum decimal places
	}
	


	class ProcessingTask extends TimerTask{
		public void run(){
			isProcessing=true;
			isProcessing=true;
			if(bluetoothService.isConnected()||application.isReadingFromFile()){
//				for(int i=0;i<application.currentStateSegments.length;i++){
//					for(int j=0;j<application.currentStateSegments[0].length;j++){
//						(application.currentStateSegments[i][j]).setSegmentOrientation(application.sensorGridArray[i][j]);
//					
//					}
//				}
				Segment.setAllSegmentOrientationsTRIAD(application.currentStateSegments, application.sensorGridArray);
				application.currentStateRot=SensorDataProcessing.getRotationTRIAD(application.sensorGridArray[application.getReferenceRow()][application.getReferenceCol()].getAccNorm(), application.sensorGridArray[application.getReferenceRow()][application.getReferenceCol()].getMagNorm());
				Log.d("SENSORDATA","ACCELEROMETER DATA:"+application.sensorGridArray[0][0].getAccNormX()+" "+application.sensorGridArray[0][0].getAccNormY()+" "+application.sensorGridArray[0][0].getAccNormZ());
				Log.d("SENSORDATA","MAGNETOMETER DATA:"+application.sensorGridArray[0][0].getMagRawX()+" "+application.sensorGridArray[0][0].getMagRawY()+" "+application.sensorGridArray[0][0].getMagRawZ());
				Segment.setSegmentCenters(application.currentStateSegments, (short)application.getReferenceRow(), (short)application.getReferenceCol());
				Segment.compansateCentersForTilt(application.refferenceStateSegmentsInitial, application.refferenceStateSegments, application.savedStateRot,application.currentStateRot);
				application.segmentStateDistances=Segment.compareByCenterDistances(application.refferenceStateSegments, application.currentStateSegments);
				maxDistance=SensorDataProcessing.getMaxDistanceFromDistancesReduced(application.segmentStateDistances);
//				maxSagitalDistance=AccelerometerDataProcessing.getMaxDistanceSagital(application.savedStateOfPoints, application.currentAccPoints);
//				maxCoronalDistance=AccelerometerDataProcessing.getMaxDistanceCoronal(application.savedStateOfPoints, application.currentAccPoints);
				isOverThreshold = (maxDistance>=application.getThreshold());
				maxSagitalDistance = SensorDataProcessing.getMaxDistanceFromDistances(Segment.compareByDistancesSagital(application.refferenceStateSegments, application.currentStateSegments));
				maxCoronalDistance = SensorDataProcessing.getMaxDistanceFromDistances(Segment.compareByDistancesCoronal(application.refferenceStateSegments, application.currentStateSegments));
				application.updateDrawingModelColors(application.segmentStateDistances);



//				// UPDATE PORCESSING ACTIVITY UI 
				if(!isOverThreshold){
					
					//averageDistanceView.setTextColor(Color.GREEN); 
					if(stateIsGood == false){
						endTime = System.currentTimeMillis();
						stateIsGood=true;
						badTime = badTime + endTime - startTime;
						//Log.d("GOODPERCENTAGE"," badTime"+badTime);
						startTime=System.currentTimeMillis();
					}
					if(stateIsGoodTemp==false){
						stateIsGoodTemp=true;
					
					}
				} else{
//					Log.d("IS_OVER_THRESHOLD", "one");
//
//					OutputStream os;
//					try {
//						os = new FileOutputStream(Environment.getExternalStorageDirectory()+"/Download/OVER_THRESHOLD_LOG.txt");
//						final PrintStream printStream = new PrintStream(os);
//						printStream.print(Sensor.printAccelerometerData(application.sensorGridArray));
//						printStream.close();
//					} catch (FileNotFoundException e) {
//						Log.d("IS_OVER_THRESHOLD", "catched exception");
//					}
					
					
					//averageDistanceView.setTextColor(Color.RED);
					if(stateIsGood == true){
						if(stateIsGoodTemp==true){
							endTimeD=System.currentTimeMillis();
							stateIsGoodTemp=false;
						}
						if(System.currentTimeMillis()>endTimeD+500){
							endTime = System.currentTimeMillis();
							stateIsGood=false;
							goodTime = goodTime + endTime - startTime;
							//Log.d("GOODPERCENTAGE", "goodTime" +goodTime);
							startTime=System.currentTimeMillis();
							if(application.isVibrateFeedbackOn())mVibro.vibrate(200);
							if(application.isSoundFeedbackOn()){
								if(!mp.isPlaying())mp.start();
							}
						}
					}
					
					if((System.currentTimeMillis()-5000>startTime)&&(!stateIsGood)) {
						if(application.isVibrateFeedbackOn())mVibro.vibrate(1000); 
					
						if(application.isSoundFeedbackOn()){
							if(!mp.isPlaying()){// if not already playing
								mp.start(); // play alert sound if sound checkbox checked
							}
						}	
					}
				}
				toc=System.currentTimeMillis();
				if(stateIsGood){
					goodTimeAccum=goodTimeAccum+toc-tic;
					//Log.d("STATISTICS", "goodTimeAccum: "+goodTimeAccum);
				} else{
					badTimeAccum=badTimeAccum+toc-tic;
					//Log.d("STATISTICS", "badTimeAccum: "+badTimeAccum);
				}
				tic=toc;
				goodTimePercentage=((float)goodTimeAccum/((float)goodTimeAccum+(float)badTimeAccum))*100;
				Message msg = application.processingActivity.handler.obtainMessage();
			    msg.what = ProcessingActivity.MESSAGE_SHOW_DISTANCE;
				application.processingActivity.handler.sendMessage(msg);
			} else{
				isProcessing=false;
				application.setDataProcessingRunning(false);
				processingTimer.cancel();// if connection lost stop processing
				statisticsTimer.cancel();
				application.processingActivity.handler.obtainMessage(ProcessingActivity.MESSAGE_CALCULATION_STOPPED, (int)goodTimePercentage, 1).sendToTarget();
			}
		}
			
	}
	class StatisticsTask extends TimerTask{
		private double interval;
		private int count = 0;
		public StatisticsTask(double interval){
			this.interval=interval;
		}
		public void run(){
			count++;
			
			goodTimeInterv=goodTimeAccum-goodTimePrev;
			badTimeInterv=badTimeAccum-badTimePrev;
			goodTimePrev=goodTimeAccum;
			badTimePrev=badTimeAccum;
			//Log.d("STATISTICS2", "goodTimeInterv="+goodTimeInterv+" badTimeInterv="+badTimeInterv+" goodTimeAccum="+goodTimeAccum+" badTimeAccum="+badTimeAccum+" goodTimePrev="+badTimePrev+" badTimePrev= "+badTimePrev);
//			badTimeInterv=badTime-badTimePrev;
//			
//			badTimePrev=badTime;
//			goodTimePrev=goodTime;
//			
			if((badTimeInterv+goodTimeInterv)==0){
				goodTimeInterv=1;
			}
			percentageIntervalled=100*goodTimeInterv/(badTimeInterv+goodTimeInterv);
			//double percent=goodTimeInterv/(badTimeInterv+goodTimeInterv)*100;
			application.mCurrentSeries.add(interval*count, percentageIntervalled );
			//Log.d("STATISTICS2", interval*count+" "+percentageIntervalled);
			if(application.statisticsHandler!=null){
				application.statisticsHandler.obtainMessage(PostureStatisticsActivity.UPDATE_STATISTICS).sendToTarget();
		    }

		}
	}
	
	// returns if service is currently processing
	public boolean isProcessing(){
		return isProcessing;
	}
	// method that starts processing
	public void start(){
		endTime=0; // end time for good or badposture time interval
		goodTime=1; // time when posture is good
		goodTimeAccum=0; // state is good accumulated
		badTimeAccum=0; // state is bad accumulated
		tic=0;
		toc=0;
		
		goodTimePrev=0;      // previous good time value
	    badTimePrev=0;       // previous bad time value
		goodTimeInterv=0;    //
		badTimeInterv=0;     //
		percentageIntervalled=0;;
		badTime=0; // time when posture is bad
		//curTime=0; // current time for statistics
		
		stateIsGoodTemp=true ; // temporary state id good time
		stateIsGood = true;	// indicator that posture is good	
		goodTimePercentage = (float)goodTime/((float)goodTime+(float)badTime)*100;
		application.createNewDataSetForPlot();
		tic=System.currentTimeMillis();
		startTime=System.currentTimeMillis();
		processingTimer = new Timer();
		ProcessingTask processingTask = new ProcessingTask();
		processingTimer.scheduleAtFixedRate(processingTask, 0,100);
		statisticsTimer = new Timer();
		StatisticsTask statisticsTask = new StatisticsTask(application.getStatisticsInterval()); //sedonds/60 to get minutes
		statisticsTimer.schedule(statisticsTask, (long)(application.getStatisticsInterval()*60000), (long)(application.getStatisticsInterval()*60000));
	
		
	}
	// cancels processing
	public void cancel(){
		application.processingActivity.handler.obtainMessage(ProcessingActivity.MESSAGE_CALCULATION_STOPPED, (int)goodTimePercentage, 1).sendToTarget();
		isProcessing=false;
		if(processingTimer!=null){
			processingTimer.cancel();
		}
		if(statisticsTimer!=null){
			statisticsTimer.cancel();
		}
	}
	public void setVibroAndAlert(ProcessingActivity activity){
		mp = MediaPlayer.create(application.processingActivity,R.raw.beep);// instantate media player for sound alert feedback
		mVibro = (Vibrator)application.processingActivity.getSystemService(Activity.VIBRATOR_SERVICE);
	}
	public synchronized double getAverageDistance(){
		return averageDistance;
	}
	public synchronized double getAverageDistanceSagital(){
		return averageDistanceSagital;
	}
	public synchronized double getAverageDistanceCoronal(){
		return averageDistanceCoronal;
	}
	public synchronized double getMaxDistance(){
		return maxDistance;
	}
	public synchronized double getMaxSagitalDistance(){
		return maxSagitalDistance;
	}
	public synchronized double getMaxCoronalDistance(){
		return maxCoronalDistance;
	}
	public synchronized boolean isOverThreshold(){
		return isOverThreshold;
	}
	public synchronized double getGoodTimePercentage(){
		return goodTimePercentage;
	}
}
