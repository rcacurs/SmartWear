package lv.edi.SmartWearMagn;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
//import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

public class ReadingFromFileService {
	private File logFile = null; // Log file from which to read
	private SmartWearApplication application = null; // application for global data
	private Timer timer = null; // Timer object that repeadedly reads from file
	private MyTimerTask timerTask = null; // Timer task that repetedly reads from file
	private FileInputStream fileInputStream = null;
	private DataInputStream dataInputStream = null;
	public Queue <double[][]> normalizedAccDataQueue = new LinkedList<double[][]>();
	public boolean isAccDataQueueAvaiable = true;
	
	
	// constructor
	public ReadingFromFileService(SmartWearApplication application){
		this.application = application;
	}
	
	public void startReading(File logFile){
		this.logFile=logFile;
		try {
			fileInputStream = new FileInputStream(logFile);
			dataInputStream = new DataInputStream(fileInputStream);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			try {
				dataInputStream.close();
				fileInputStream.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		timerTask = new MyTimerTask();
		timer = new Timer();
		timer.scheduleAtFixedRate(timerTask, 0, 100);
		
	}
	
	public void stopReading(){
		try {
			fileInputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		timer.cancel();
		timer = null;
		timerTask = null;
		application.setReadingFromFile(false);
	}
	public class MyTimerTask extends TimerTask{
		
		public void run(){ // this task reads one data frame of Acc Grid data from data file
			int accNumber;
			short accXData;
			short accYData;
			short accZData;
			short magXData;
			short magYData;
			short magZData;	
			try{
				for(int i=0; i<(SmartWearApplication.GRID_COLS*SmartWearApplication.GRID_ROWS); i++){
					accNumber = dataInputStream.readShort();
					Log.d("REDING ROM FILE SERVICE", "acc Number"+accNumber);
					if(accNumber == (short)0xFFFE){
						accNumber = dataInputStream.readShort(); // if read value correspond to mark
					}
					if(accNumber<SmartWearApplication.NR_OF_SENSORS){
						accXData = dataInputStream.readShort(); // read XData value
						accYData = dataInputStream.readShort(); // read YData value
						accZData = dataInputStream.readShort(); //read ZData value
						
						magXData = dataInputStream.readShort(); // read XData value
						magYData = dataInputStream.readShort(); // read YData value
						magZData = dataInputStream.readShort(); // read ZData value
				
						
						Log.d("REDING ROM FILE SERVICE", "X: "+accXData+" Y:"+accYData+" Z: "+accZData);
						application.sensorArray[accNumber].updateSensorData(accXData, accYData, accZData, magXData, magYData, magZData);
					}
				}
				
			} catch(EOFException ex2){ // exeption when file end reached
				try { // recreate stream, to start reading from file again
					try {
						fileInputStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					fileInputStream = new FileInputStream(logFile);
					dataInputStream = new DataInputStream(fileInputStream);
					
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					try {
						dataInputStream.close();
						fileInputStream.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					e.printStackTrace();
				}
				
			} catch(IOException ex){
				
			}
		}
	}
	
}
