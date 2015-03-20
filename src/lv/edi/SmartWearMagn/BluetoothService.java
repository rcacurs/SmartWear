package lv.edi.SmartWearMagn;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

/**
 * 
 * @author richard service that connects to selected target device and fetches data from bluetooth device
 *
 */

public class BluetoothService {
	private final int NR_OF_SENSORS = SmartWearApplication.NR_OF_SENSORS;
	private boolean isConnected=false; // connection indicator
	private final UUID M_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //UUID for SPP profile;
	private BluetoothSocket mSocket; // bluetooth socket from this object data streams can be created
	private BluetoothDevice mDevice;// bluetooth connection target device
	private Handler bluetoothActivityHandler; // handler to the bluetooth connection activity
	private SmartWearApplication application;
	private static final short STAMP = (short)0xFFFE;
	private int bytes_in_packet=13;
	
	Queue<double[][]>normalizedAccelerometerDataQueue = new LinkedList<double[][]>();// Queue for normalized accelerometer data arrays
	boolean accDataQueueIsAvaiable=true; // indicator for locking normalizedAccelerometerDataQueue
	double[][] normalizedAccelerometerData = new double[NR_OF_SENSORS][3]; // array for storing accelerometer data
	// threads
	ConnectThread connectThread; // instance of thread that creates bluetooth connection
	ReceiveThread receiveThread; // instance of thread that continously fetches data from bluetooth adapter
	//returns if service is connected to the device
	public BluetoothService(SmartWearApplication application){
		this.application=application;
	}
	public boolean isConnected(){ // accesor method that shows if service is connected to the bluetooth target device
		return isConnected;
	}
	
	public BluetoothDevice getBluetoothDevice(){
		return mDevice; // returns target Device
	}
	
	public void connectDevice(BluetoothDevice targetDevice, Handler handler){ // method for bluetooth connection
		bluetoothActivityHandler=handler; // handler to the activity, who statrts connection
		mDevice = targetDevice; // getting the target device
		connectThread = new ConnectThread(mDevice); //create new connection thread, with specified target device
		connectThread.start();	// start connection
	}
	public void disconnectDevice(){ //method for stopping connection
		connectThread.cancel(); // stop connect Thread
		isConnected=false; //set connected indicator to false
		connectThread = null; // setConnectThread refference variable to null, so tha jvm recycler can delete obect from memory
		bluetoothActivityHandler.obtainMessage(DataSourceActivity.UPDATE_CONNECTION_STATUS,DataSourceActivity.UPDATE_DISCONNECTED,1, "").sendToTarget(); // update bluetooth connection activity, that bluetooth connection is open
		
		Log.d("diconnect device","disconnected");
	}
	//thread that creates bluetooth connection to selected device
	public class ConnectThread extends Thread{
		private BluetoothDevice mDevice; // connection target device
		ConnectThread(BluetoothDevice mDevice){ // connect thread constructor
			this.mDevice = mDevice; // set target device
		}
		public void run(){ 
			try{
    			mSocket = mDevice.createInsecureRfcommSocketToServiceRecord(M_UUID); // create rfcomm protocol socket
    			Log.d("CONNECTING DEVICE","created RFCOMM socket");
    			mSocket.connect(); // connect to the bluetooth device
    			Log.d("CONNECTING DEVICE","connection estabilished");
    			isConnected=true; // set connected indicator to true
    			bluetoothActivityHandler.obtainMessage(DataSourceActivity.UPDATE_CONNECTION_STATUS, DataSourceActivity.UPDATE_CONNECTED,1).sendToTarget(); // update connection activity
    			receiveThread = new ReceiveThread(mSocket); // create instanct to new recieveThread
    			receiveThread.start(); // start receive thread
    		} catch(IOException ex){ // if exception accures 
    			Log.d("connection thread","could not connect");
    			bluetoothActivityHandler.obtainMessage(DataSourceActivity.UPDATE_CONNECTION_STATUS,DataSourceActivity.UPDATE_DISCONNECTED,1, "").sendToTarget();
    			isConnected=false;
    		}
		}
		public void cancel() { // calback that allows to cancel bluetooth connection
			try{
				receiveThread.cancel(); // stop recevie thread
				mSocket.close();	// close bluetooth socket
			}
			catch(IOException ex){	
				// catch exceltio, exceptoin may accure if mSocket is already closed
			}
		}
	}
	
	// thread that reads data from bluetooth connection
	public class ReceiveThread extends Thread{
		private BluetoothSocket socket; // bluetooth socket
		private InputStream mInputStream; // data input stream 
		private boolean packet_indicator = false; // acc data packet indicator, destinguishes packet frame
		private boolean escape_indicator = false; // indicator that shows, that we received escape symbol
		private int bytes_received = 0; // number of bytes received in one packet frame
		//private int packets_received = 0; //shows number of packets received
		private int b; // value read from input stream
		private short packet[] = new short[bytes_in_packet]; // array that stores one data packet (one accelerometer data)
		private final int PACKET_SEPERATOR = 0xFF; // symbol that defines packets start and end
		private final int PACKET_ESCAPE = 0xFE; // escape symbol, that tells us that next sybol is escape symbol
		//private double length; // for data normalization
		// receice thread constructor. constructor just gets refference to mSocket instance
		ReceiveThread(BluetoothSocket mSocket){
			socket = mSocket; 
		}
		// thread run
		public void run(){
			try{
				
				mInputStream = socket.getInputStream(); // creating input stream from socket
				Log.d("ReceiveThread","StreamCreated");
			} catch (IOException ex){
				Log.e("Receive Thread","Could not create stream");
				isConnected=false;
			}
			
			while(true){ // this cycle repeatedly reads data from input stream, and forms acc data array with normalized accelerometer data
				try{	// the cycle breaks if exception accures
						//Log.d("READbYTE","PrepearingToReadByte");
						b =  mInputStream.read(); // reading one byte from input stream
						//Log.d("READBYTE"," "+b);
						if(packet_indicator==true){ // if we received start symbol of packet
							if(escape_indicator==true){ // if previous byte was escape symbol
								switch(b){
								case  0x00: // in case we have symbol that matched with packet seperator
									bytes_received++; // updating bytes received  counter
									packet[bytes_received-1]=PACKET_SEPERATOR; //adding received data byte to packet parray
									break;
								case  0x01:
									bytes_received++;
									packet[bytes_received-1]=PACKET_ESCAPE;// in case we have symbol that matched escape symbol 
									break;
								default:
									break;
								}
								escape_indicator=false; // unsetting the escape indicator
							} else{ // in case we don't expect escape character
								switch(b){ // checking what the received symbol is
								case  PACKET_SEPERATOR: //in case of packet seperator
									if(bytes_received>0){ //if we have at least one byte received
										if(bytes_received>=bytes_in_packet){ // if we received 7 bytes, then start to from packet 
											bytes_received = 0; // resetting received byte counter
											if(packet[0]<SmartWearApplication.NR_OF_SENSORS){ // if received data packet
												short accx = (short)(packet[1]*256+packet[2]);
												short accy = (short)(packet[3]*256+packet[4]);
												short accz = (short)(packet[5]*256+packet[6]);
												short magx = (short)(packet[7]*256+packet[8]);
												short magy = (short)(packet[9]*256+packet[10]);
												short magz = (short)(packet[11]*256+packet[12]);
												double accMagnitude = Math.sqrt(Math.pow(accx, 2)+Math.pow(accy, 2)+Math.pow(accz, 2));
												double magMagnitude = Math.sqrt(Math.pow(magx, 2)+Math.pow(magy, 2)+Math.pow(magz, 2));
												if(((accMagnitude<20800)&&(accMagnitude>11000)&&(magMagnitude>0)&&(magMagnitude<2000))){
												application.sensorArray[packet[0]].updateSensorData(accx, // forming accelerometer x data from two received data bytes
																									accy, // forming accelerometer y data from two received data bytes
																									accz, // forming accelerometer z data from two received data bytes
																									magx, // forming magnetometer  x data from two received data bytes
																									magy,// forming magnetometer  y data from two received data bytes
																									magz);// forming magnetometer z data from two recieved data bytes
												}
											} else{// if received battery status packet
												short battery_level_raw=(short)(packet[1]*256+packet[2]);
												double battery_level=(((battery_level_raw)-730)/294.0)*100;
												battery_level=(short)(battery_level/10)*10;
												application.setBatteryLevel((short)battery_level);
												//bluetoothActivityHandler.obtainMessage(DataSourceActivity.UPDATE_TARGET_BATTERY_STATUS).sendToTarget(); // update connection activity
												bluetoothActivityHandler.obtainMessage(DataSourceActivity.UPDATE_TARGET_BATTERY_STATUS,DataSourceActivity.UPDATE_DISCONNECTED,1, "").sendToTarget();
												Log.d("battery level packet", "received: "+battery_level_raw+" : "+battery_level);
											}
						
											if((application.getSelectedLogFile()!=null)&&(application.isDataProcessingRunning())){
												long beginTime=System.currentTimeMillis();
												Log.d("LOG_PACKET", " begin"+beginTime);
												logPacket(packet, application.getSelectedLogFileStream());
												if(application.stampPendingFlag()){
													logStamp(application.getSelectedLogFileStream());
													application.setStampPendingFlag(false);
												}
												Log.d("LOG_PACKET", " end , required time "+(System.currentTimeMillis()-beginTime));
											}
//											
										}
										packet_indicator=false; // reset packet indicator, bacause we have received all packet data
										bytes_received=0;// reset received byte counter
									}
									break;
								case PACKET_ESCAPE: // if received byte packet escape
									escape_indicator=true; // set escape indicator
									break;
								default: // in case of all other values 
									bytes_received++; // increase byte counter
									packet[bytes_received-1]=(short)b; // add read byte to packet array
									break;	
								}
							}							
						} else if(b==PACKET_SEPERATOR){ // condition for new packet start
							packet_indicator=true;
						}		
				} catch(IOException ex){
					// exception can acure if bluetooth connection is lost
					isConnected=false;
					bluetoothActivityHandler.obtainMessage(DataSourceActivity.UPDATE_CONNECTION_STATUS,DataSourceActivity.UPDATE_DISCONNECTED,1, "").sendToTarget();
					
					Log.d("RECEIVE_DATA","Catched exception" +ex.toString());
					break; // break the main cycle	
				}
			}
		}
		public void cancel(){ // cancel method closes input stream
			try{			  // this results to that exception is thrown, and main cycle is braked
				mInputStream.close();
				if(application.getSelectedLogFileStream()!=null){
					application.getSelectedLogFileStream().close();
				}
			} catch(IOException ex){
				
			}
		}
	}
	// method thar writes one reconstructed data packet to log file
	// short[] packet is array with packet, and file is log file object were to write
	public void logPacket(short packet[], DataOutputStream stream){
		
		try{
			stream.writeShort(packet[0]); // write data to file
			stream.writeShort((short)(packet[1]*256+packet[2]));
			stream.writeShort((short)(packet[3]*256+packet[4]));
			stream.writeShort((short)(packet[5]*256+packet[6]));
			stream.writeShort((short)(packet[7]*256+packet[8]));
			stream.writeShort((short)(packet[9]*256+packet[10]));
			stream.writeShort((short)(packet[11]*256+packet[12]));
			Log.d("LOG_PACKET", "stream size: "+stream.size());
			if(stream.size()>=10000){
				stream.close();
				stream = new DataOutputStream(new FileOutputStream(application.getSelectedLogFile(),true));
			}
		} catch(IOException ex){
		}
		
	}
	public static void logStamp(DataOutputStream stream){
		try{
			stream.writeShort(STAMP); // write data to file
			Log.d("STAMP", "stamp created");
		} catch(IOException ex){
		}
	}
}


