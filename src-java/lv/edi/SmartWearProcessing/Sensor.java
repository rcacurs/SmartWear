package lv.edi.SmartWearProcessing;
import static java.lang.Math.*;
/**This class represents sensor object in sensor grid
 * @author Richards Cacurs*/
public class Sensor {
	/** represents unique accelerometer identifier*/
	private final int identifier;
	/** Shows if accelerometer oriented up or down if up, then value true*/
	private final boolean isOrientationUp;
	/**integer array that represents raw accelerometer data from sensor*/
	private float[] rawAccData = new float[3];
	/**short array that represents raw magnetometer data from sensor*/
	private float[] rawMagData = new float[3];
	
	/** constructs sensor object that represents sensor from sensor grid
	 * @param identificator integer that represents sensor identifier
	 * @param orientationUp shows if integer is put up or down. true value*/
	public Sensor(int identifier, boolean isOrientationUp){
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
		if(isOrientationUp){
			return (float) ((rawAccData[0])/(sqrt(pow(rawAccData[0],2)+pow(rawAccData[1],2)+pow(rawAccData[2],2))));
		} else{
			return (float) -((rawAccData[0])/(sqrt(Math.pow(rawAccData[0],2)+pow(rawAccData[1],2)+pow(rawAccData[2],2))));
		}
	}
	
	/**returns normed accelerometer Y axis data considering accelerometer orientation in grid*/
	public synchronized float getAccNormY(){
		if(isOrientationUp){
			return (float) ((rawAccData[2])/(sqrt(pow(rawAccData[0],2)+pow(rawAccData[1],2)+pow(rawAccData[2],2))));
		} else{
			return (float) ((rawAccData[2])/(sqrt(Math.pow(rawAccData[0],2)+pow(rawAccData[1],2)+pow(rawAccData[2],2))));
		}
	}
	/**returns normed accelerometer Z axis data considering accelerometer orientation in grid*/
	public synchronized float getAccNormZ(){
		if(isOrientationUp){
			return (float) (-(rawAccData[1])/(sqrt(pow(rawAccData[0],2)+pow(rawAccData[1],2)+pow(rawAccData[2],2))));
		} else{
			return (float) ((rawAccData[1])/(Math.sqrt(pow(rawAccData[0],2)+pow(rawAccData[1],2)+pow(rawAccData[2],2))));
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
		float magDataX=rawMagData[0]/1100;
		float magDataY=rawMagData[1]/1100;
		float magDataZ=rawMagData[2]/980;
		if(isOrientationUp){
			return (float) ((magDataX)/(sqrt(pow(magDataX,2)+pow(magDataY,2)+pow(magDataZ,2))));
		} else{
			return (float) -((magDataX)/(sqrt(pow(magDataX,2)+pow(magDataY,2)+pow(magDataZ,2))));
		}
	}
	/**returns normed magnetometer Y axis data considering accelerometer orientation in grid*/
	public synchronized float getMagNormY(){
		float magDataX=rawMagData[0]/1100;
		float magDataY=rawMagData[1]/1100;
		float magDataZ=rawMagData[2]/980;
		if(isOrientationUp){
			return (float) ((magDataZ)/(sqrt(pow(magDataX,2)+pow(magDataY,2)+pow(magDataZ,2))));
		} else{
			return (float) ((magDataZ)/(sqrt(pow(magDataX,2)+pow(magDataY,2)+pow(magDataZ,2))));
		}
	}
	/**returns normed magnetometer Z axis data considering accelerometer orientation in grid*/
	public synchronized float getMagNormZ(){
		float magDataX=rawMagData[0]/1100;
		float magDataY=rawMagData[1]/1100;
		float magDataZ=rawMagData[2]/980;
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
	
	/**changes sensor raw acceleration data field array rawAccData[3]
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
