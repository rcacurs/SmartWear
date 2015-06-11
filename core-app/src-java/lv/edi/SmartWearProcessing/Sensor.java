package lv.edi.SmartWearProcessing;
import static java.lang.Math.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.util.Log;

//import android.util.Log;
/**This class represents sensor object in sensor grid
 * @author Richards Cacurs*/
public class Sensor {
	/** represents unique accelerometer identifier*/
	private final int identifier;
	private boolean filter = true; // if data should be filtered
	/** Shows if accelerometer oriented up or down if up, then value true*/
	private final boolean isOrientationUp;
	/**integer array that represents raw accelerometer data from sensor*/
	private float[] rawAccData = new float[3];
	private float[] rawAccFilteredData = new float[3];
	private float[] P_xyz_acc=new float[3]; // for Kalman filter
	/**short array that represents raw magnetometer data from sensor*/
	private float[] rawMagData = new float[3];
	private float[] rawMagFilteredData = new float[3];
	private float[] P_xyz_mag = new float[3]; // for Kalman filter
	private float[] calibratedMagData = new float[3];
	private float[][] magCalibMat;
	private float[] magCalibOffset;
	
	/** constructs sensor object that represents sensor from sensor grid
	 * @param identificator integer that represents sensor identifier
	 * @param orientationUp shows if integer is put up or down. true value*/
	public Sensor(int identifier, boolean isOrientationUp){
		for(int i=0; i<3; i++){
			P_xyz_acc[i]=1;
			P_xyz_mag[i]=1;
		}
		this.identifier = identifier;
		this.isOrientationUp = isOrientationUp;
	}
	/**returns identifier of sensor object
	 * @return identifier returns identifier of accelerometer*/
	public synchronized int getIdentifier(){
		return identifier;
	}
	/**
	 * return true if sensor orientation is up and false if orientation of sensor is down
	 * @return isOrientationUp identifier indicates if sensor is up or down
	 **/
	public synchronized boolean isOrientationUp(){
		return isOrientationUp;
	}
	/**@return rawData[0] returns raw X axis value of accelerometer*/
	public synchronized float getAccRawX(){
		return rawAccData[0];
	}
	/** returns raw Y axis value of accelerometer
	 * @return rawData[1]*/
	public synchronized float getAccRawY(){
		return rawAccData[1];
	}
	/** returns raw Z axis value of accelerometer
	 * @return rawData[2]*/
	public synchronized float getAccRawZ(){
		return rawAccData[2];
	}
	/** returns raw X axis value of magnetometer
	 * @return rawData[0]
	 */
	public synchronized float getMagRawX(){
		return rawMagData[0];
	}
	/** returns raw Y axis value of magnetometer
	 * @return rawMagData[1]
	 */
	public synchronized float getMagRawY(){
		return rawMagData[1];
	}
	/**
	 * returns raw Z axis value of magnetometer
	 * @return rawMagData[2]
	 */
	public synchronized float getMagRawZ(){
		return rawMagData[2];
	}
	/**returns normalized accelerometer X axis data considering accelerometer orientation in grid*/
	public synchronized float getAccNormX(){
		float[] accData = new float[3];
		if(filter){
			accData[0]=rawAccFilteredData[0];
			accData[1]=rawAccFilteredData[1];
			accData[2]=rawAccFilteredData[2];
			Log.d("FILTERING", "FILTERacc");
		} else{
			accData[0]=rawAccData[0];
			accData[1]=rawAccData[1];
			accData[2]=rawAccData[2];
		}
		
		if(isOrientationUp){
			return (float) ((accData[0])/(sqrt(pow(accData[0],2)+pow(accData[1],2)+pow(accData[2],2))));
		} else{
			return (float) -((accData[0])/(sqrt(pow(accData[0],2)+pow(accData[1],2)+pow(accData[2],2))));
		}
	}
	
	/**returns normed accelerometer Y axis data considering accelerometer orientation in grid*/
	public synchronized float getAccNormY(){
		float[] accData = new float[3];
		if(filter){
			Log.d("FILTERING", "FILTERmag");
			accData[0]=rawAccFilteredData[0];
			accData[1]=rawAccFilteredData[1];
			accData[2]=rawAccFilteredData[2];
		} else{
			accData[0]=rawAccData[0];
			accData[1]=rawAccData[1];
			accData[2]=rawAccData[2];
		}
		if(isOrientationUp){
			return (float) ((accData[2])/(sqrt(pow(accData[0],2)+pow(accData[1],2)+pow(accData[2],2))));
		} else{
			return (float) ((accData[2])/(sqrt(pow(accData[0],2)+pow(accData[1],2)+pow(accData[2],2))));
		}
	}
	/**returns normed accelerometer Z axis data considering accelerometer orientation in grid*/
	public synchronized float getAccNormZ(){
		float[] accData = new float[3];
		if(filter){
			accData[0]=rawAccFilteredData[0];
			accData[1]=rawAccFilteredData[1];
			accData[2]=rawAccFilteredData[2];
		} else{
			accData[0]=rawAccData[0];
			accData[1]=rawAccData[1];
			accData[2]=rawAccData[2];
		}
		if(isOrientationUp){
			return (float) (-(accData[1])/(sqrt(pow(accData[0],2)+pow(accData[1],2)+pow(accData[2],2))));
		} else{
			return (float) ((accData[1])/(sqrt(pow(accData[0],2)+pow(accData[1],2)+pow(accData[2],2))));
		}
	}
	/**returns array of normed accelerometer data with transformed coordinates*/
	public synchronized float[] getAccNorm(){
		float[] data = new float [3];
		data[0] = getAccNormX();
		data[1] = getAccNormY();
		data[2] = getAccNormZ();
		return data;
	}
	/**return normed magnetometer X axis data considering magnetometer orientation in grid*/
	public synchronized float getMagNormX(){
		float magDataX;
		float magDataY;
		float magDataZ;
	
		if(filter){
			magDataX=rawMagFilteredData[0];
			magDataY=rawMagFilteredData[1];
			magDataZ=rawMagFilteredData[2];
		} else{
			if(calibratedMagData!=null){
				magDataX=calibratedMagData[0];
				magDataY=calibratedMagData[1];
				magDataZ=calibratedMagData[2];
			} else{
				magDataX=rawMagData[0];
				magDataY=rawMagData[1];
				magDataZ=rawMagData[2];
			}
		}
		
		if(isOrientationUp){
			return (float) ((magDataX)/(sqrt(pow(magDataX,2)+pow(magDataY,2)+pow(magDataZ,2))));
		} else{
			return (float) -((magDataX)/(sqrt(pow(magDataX,2)+pow(magDataY,2)+pow(magDataZ,2))));
		}
	}
	/**returns normed magnetometer Y axis data considering accelerometer orientation in grid*/
	public synchronized float getMagNormY(){
		float magDataX;
		float magDataY;
		float magDataZ;
	
		if(filter){
			magDataX=rawMagFilteredData[0];
			magDataY=rawMagFilteredData[1];
			magDataZ=rawMagFilteredData[2];
		} else{
			if(calibratedMagData!=null){
				magDataX=calibratedMagData[0];
				magDataY=calibratedMagData[1];
				magDataZ=calibratedMagData[2];
			} else{
				magDataX=rawMagData[0];
				magDataY=rawMagData[1];
				magDataZ=rawMagData[2];
			}
		}
		if(isOrientationUp){
			return (float) ((magDataZ)/(sqrt(pow(magDataX,2)+pow(magDataY,2)+pow(magDataZ,2))));
		} else{
			return (float) ((magDataZ)/(sqrt(pow(magDataX,2)+pow(magDataY,2)+pow(magDataZ,2))));
		}
	}
	/**returns normed magnetometer Z axis data considering accelerometer orientation in grid*/
	public synchronized float getMagNormZ(){
		float magDataX;
		float magDataY;
		float magDataZ;
	
		if(filter){
			magDataX=rawMagFilteredData[0];
			magDataY=rawMagFilteredData[1];
			magDataZ=rawMagFilteredData[2];
		} else{
			if(calibratedMagData!=null){
				magDataX=calibratedMagData[0];
				magDataY=calibratedMagData[1];
				magDataZ=calibratedMagData[2];
			} else{
				magDataX=rawMagData[0];
				magDataY=rawMagData[1];
				magDataZ=rawMagData[2];
			}
		}
		if(isOrientationUp){
			return (float) (-(magDataY)/(sqrt(pow(magDataX,2)+pow(magDataY,2)+pow(magDataZ,2))));
		} else{
			return (float) ((magDataY)/(sqrt(pow(magDataX,2)+pow(magDataY,2)+pow(magDataZ,2))));
		}
	}
	
	/**returns array of normed magnetometer data with transformed coordinates*/
	public synchronized float[] getMagNorm(){
		float[] data = new float [3];
		data[0] = getMagNormX();
		data[1] = getMagNormY();
		data[2] = getMagNormZ();
		return data;
	}
	
	/**changes raw sensor data fields  for accelerometer and magnetometer sensor array rawAccData[3]
	 * @param i - accelerometer X axis raw data
	 * @param j - accelerometer Y axis raw data
	 * @param k - accelerometer Z axis raw data
	 * @param im - magnetometer X axis raw data
	 * @param jm - magnetometer Y axis raw data
	 * @param km - magnetometer Z axis raw data */
	public synchronized void updateSensorData(short i,short j, short k, short im, short jm, short km){
		rawAccData[0]=i;
		rawAccData[1]=j;
		rawAccData[2]=k;
		rawMagData[0]=im;
		rawMagData[1]=jm;
		rawMagData[2]=km;
		
		if((magCalibMat!=null)&&(magCalibOffset!=null)){
			calibratedMagData=getMagnCalibratedData();
			//Log.d("CALIB_DATA", "CALIBRATING RAW SENSRO DATA");
		} else{
			calibratedMagData=null;
		}
		// updates filtered sensor data
		if(filter){
			SensorDataProcessing.kalmanFilter(rawAccData, rawAccFilteredData, P_xyz_acc, 0.2f, 1f);
			if(calibratedMagData!=null){
				SensorDataProcessing.kalmanFilter(calibratedMagData, rawMagFilteredData, P_xyz_mag, 0.2f, 1f);
			} else{
				SensorDataProcessing.kalmanFilter(rawMagData, rawMagFilteredData, P_xyz_mag, 0.2f, 1f);
			}
		}
	}

	/**
	 * Returns calibrated magnetometer data. Calibration data must be set for sensor
	 * @return float[] calibration data
	 */
	public synchronized float[] getMagnCalibratedData(){
		float[] magnCalibData = new float[3];
		SensorDataProcessing.translateVec(rawMagData, magCalibOffset, magnCalibData);
		float[] magnCalibDataSc = new float[3];
		SensorDataProcessing.multiplyMatrix(magCalibMat, magnCalibData, magnCalibDataSc);
		return magnCalibDataSc;
	}
	
	/**
	 * Updates magnetometer calibration data
	 * @param calibData - array consisting of magnetometer calibration data. Array must be of size 12 and arrays first 9 elements are scaling matrix
	 * elements in col-maj order, and last 3 elements are offsets
	 */
	public synchronized void updateMagnCalibrationData(float[] calibData){
		magCalibMat = new float[3][3];
		magCalibOffset = new float[3];
		
		magCalibOffset[0]=calibData[0];
		magCalibOffset[1]=calibData[1];
		magCalibOffset[2]=calibData[2];
		
		magCalibMat[0][0]=calibData[3];
		magCalibMat[1][0]=calibData[4];
		magCalibMat[2][0]=calibData[5];
		magCalibMat[0][1]=calibData[6];
		magCalibMat[1][1]=calibData[7];
		magCalibMat[2][1]=calibData[8];
		magCalibMat[0][2]=calibData[9];
		magCalibMat[1][2]=calibData[10];
		magCalibMat[2][2]=calibData[11];
		
	}
	
	/**
	 * Sets magnetometer calibration data for all GRID
	 * @param calibDataFile .csv file containing calibration data
	 * @param sensorGrid sensor grid array
	 * @throws IOException 
	 */
	public static void  setGridMagnetometerCalibData(File calibDataFile, Sensor[][] sensorGrid) throws IOException{
		BufferedReader breader = new BufferedReader(new FileReader(calibDataFile));
		String str = breader.readLine();
		int numberOfSensors;
		try {
			numberOfSensors = Integer.parseInt(str);
		} catch (NumberFormatException e) {
			breader.close();
			return;
		}
		
		if(numberOfSensors==(sensorGrid.length*sensorGrid[0].length)){
			for(int i=0; i<numberOfSensors; i++){
				int[] sensorIndexes = SensorDataProcessing.getIndexes(i, sensorGrid.length, sensorGrid[0].length);
				str=breader.readLine();
				String[] elements = str.split(",");
				if(elements.length==12){
					float[] calibData = new float[12];
					for(int j=0; j<12; j++){
						calibData[j]=(float)Double.parseDouble(elements[j]);
					}
					sensorGrid[sensorIndexes[0]][sensorIndexes[1]].updateMagnCalibrationData(calibData);
				}else{
					breader.close();
					
					throw new IOException();
				}
			}
		breader.close();
		}else{
			breader.close();
			return;
		}
	}
	
	public static String printAccelerometerData(Sensor sensorGrid[][]){
		String accelerometerData="Accelerometer Grid Data \n\r";
		
		for(int i=0; i<sensorGrid.length; i++){
			for(int j=0; j<sensorGrid[0].length; j++){
				accelerometerData=accelerometerData+"|"+sensorGrid[i][j].getAccRawX()+
													" "+sensorGrid[i][j].getAccRawY()+
													" "+sensorGrid[i][j].getAccRawZ()+"|\t";
			}
			accelerometerData=accelerometerData+"\n\r";
		}
		return accelerometerData;
	}

}
