package lv.edi.SmartWearMagn;

import lv.edi.SmartWearMagn.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DataSourceActivity extends Activity implements OnGestureListener{
	// constants
	//request codes for start avtvivity for result
	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_SELECT_TARGET_DEVICE = 2;
	private static final int REQUEST_SELECT_SOURCE_FILE = 3;
	
	// message types for handlers
	public static final int UPDATE_CONNECTION_STATUS = 1; // to update statysView
	public static final int UPDATE_RECEIVED_DATA_VIEW = 2; // update received data view
	public static final int UPDATE_CONNECTED = 1; // update activity that connected
	public static final int UPDATE_DISCONNECTED = 2; //update activity that bluetoothd connection has been lost
	public static final int UPDATE_TARGET_BATTERY_STATUS = 101;// update TARGET
	
	
	private SmartWearApplication application; // refference to application instance stores global data
	private Button connectButton; // button that starts and stops bluetooth connection
	private TextView targetDeviceView; // textview that shows bluetooth target device view
	private TextView connectionStatusView; //textview that shows connection status
	private GestureDetector gDetector; //Gesture detector that detects touch input on screan
	private TextView sourceFileView;
	private Button startReadButton;
	private TextView readingStatusView;
	private TextView targetBatteryLevelView;
	private ReadingFromFileService readingFromFileService = null;
	// blue tooth objects
	BluetoothDevice mDevice; // target bluetooth device
	BluetoothAdapter mBluetoothAdapter; // phones bluetooth adapter
	@SuppressLint("HandlerLeak")
	private final Handler handler = new Handler(){ // handler that handles messages from other threads
		public void handleMessage(Message message){
			switch (message.what){
			case UPDATE_CONNECTION_STATUS: // in case of message type update conection status
				if(message.arg1 == UPDATE_CONNECTED){ // if connection created
					connectionStatusView.setText("connected!"); // update status view
					connectButton.setText("Disconnect");		//update connect button with text
				} else {									// if connection lost message received
					connectButton.setText("Connect"); 	//
					connectionStatusView.setText("not connected");
					Toast.makeText(getApplicationContext(), "Not connected", Toast.LENGTH_SHORT).show();
				}
				break;
			case UPDATE_TARGET_BATTERY_STATUS:
				targetBatteryLevelView.setText(""+application.getBatteryLevel()+" %");
				Log.d("Battery message", "got message");
				break;
			default:
				break;
			}
		}
    };
	//
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState); // call for super class method
        setContentView(R.layout.activity_select_data_source); // set contentview from xml file
        //get appllication object
        application = (SmartWearApplication)getApplicationContext(); // instantate application object
        //instantate views from activity screen
        connectButton = (Button) findViewById(R.id.connect_button);
        targetDeviceView = (TextView) findViewById(R.id.target_device_view);
        connectionStatusView = (TextView) findViewById(R.id.connection_status_view);
        sourceFileView = (TextView) findViewById(R.id.selected_file_view);
        startReadButton = (Button) findViewById(R.id.start_reading_button);
        readingStatusView = (TextView) findViewById(R.id.reading_status_view);
        targetBatteryLevelView=(TextView) findViewById(R.id.target_battery_status_view);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // getting bluetooth adapter
        // checking if adapter is created, if not, then application closes
        if(mBluetoothAdapter == null){
        	Toast.makeText(this, "Sorry, but device does not support bluetooth conection \n Application will now close", Toast.LENGTH_SHORT).show();
        	finish();
        	return;
        }
        // check if bluetooth is turned on
        if(!mBluetoothAdapter.isEnabled()){
        	// intnet to open activity, to turn on bluetooth if bluetooth no turned on
        	Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        	startActivityForResult(enableIntent, REQUEST_ENABLE_BT); //start activity for result
        }
        
        gDetector = new GestureDetector(getApplicationContext(), this); // instantate gesture detecotr
        
    }
    // on start callback
    public void onStart(){
    	super.onStart();
    	// if bluetooth service has target device object
    	if(application.bluetoothService.getBluetoothDevice()!=null){
    		mDevice=application.bluetoothService.getBluetoothDevice();// get reference to that device
    		targetDeviceView.setText(application.bluetoothService.getBluetoothDevice().getName()); // update target device view
    	}
    	if(application.bluetoothService.isConnected()){ // if there is running bluetooth connection
    		connectionStatusView.setText("Connected!"); //update connection status view
    		connectButton.setText("Disconnect");// update buttons text
    	}
    	if(application.getDataSourceFile()!=null){
    		sourceFileView.setText(application.getDataSourceFile().getName());
    	} else{
    		sourceFileView.setText("not selected");
    	}
    	
    }
    // on activity destroy callback
    	public void onResume(){
    		super.onResume();
    		if(application.bluetoothService.isConnected()){
    			connectionStatusView.setText("connected!");
    			connectButton.setText("Disconnect");
    		} else{
    			connectionStatusView.setText("not connected");
    			connectButton.setText("Connect");
    		}
    		if(application.isReadingFromFile()){
    			readingStatusView.setText("reading");
    			startReadButton.setText("Stop Read");
    		} else{
    			readingStatusView.setText("not reading");
    			startReadButton.setText("Start Read");
    		}
    	}
    	public void onDestroy(){
    	super.onDestroy();
    	if(application.bluetoothService.isConnected()==true){ // if there is ongoing connection close the connection
    		application.bluetoothService.disconnectDevice(); // calling function that disconnects target bluetooth device	
    	}	
    }
     
    @Override
    public boolean onTouchEvent(MotionEvent me) { // on touch event callback
        return gDetector.onTouchEvent(me); 
    }
    //callback that inflates option menu, when menu button is pressed
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_bluetooth_connection, menu);
        return true;
        
    }
    // callback that is called when item from option menu is selected
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) { //check selecte items ID
            case R.id.processing_activity: //in case of processing activiy item selected
                Intent intent1 = new Intent(this, ProcessingActivity.class); // create intent to start ProcessingActivity
                startActivity(intent1); // start Activity
                return true;
            case R.id.drawing_accivity:// in case of drawing activity item selected
            	Intent intent2 = new Intent(this, DrawingActivity.class); //create intent to start Drawing activity
            	startActivity(intent2); // start drawing activity
            	return true;
            case R.id.settings: // in case of settings activity selected
            	Intent intent3 = new Intent(this, PreferencesActivity.class);
            	startActivity(intent3);
            	return true;
            case R.id.statistics:
            	Intent intent4 = new Intent(this, PostureStatisticsActivity.class);
            	startActivity(intent4);
            	return true;
            default:
                return false;
        }
    }
    
    // button callbacks
    //on click button Select Target Device
    public void onClickSlectTarget(View v){
    	Intent serverIntent = new Intent(this, DeviceListActivity.class); // intent to start activity that allows to select bluetooth target device
        startActivityForResult(serverIntent, REQUEST_SELECT_TARGET_DEVICE); // start activity for result
    }
    // on click connect button
    public void onClickConnect(View v){
    	if(!application.isReadingFromFile()){
	    	if(mDevice!=null){ // if bluetooth connection target device is selected
	    		if(application.bluetoothService.isConnected()==false){ //if connection is not already open
	    			Log.d("connect button","connecting to device");
	    			connectionStatusView.setText("connecting..."); // update connection status view
	    			application.bluetoothService.connectDevice(mDevice, handler); // connect target device in bluetooth service class
	    		} else{
	    			application.bluetoothService.disconnectDevice(); // if bluetooth connection on going and connect button pressed stop bluetooth connection and deisconect device
	    			connectionStatusView.setText("not connected"); //update status view
	    			connectButton.setText("Connect"); // update connect button label
	    		}
	    		
	    	} else{
	    		Toast.makeText(getApplicationContext(),"Select Target Device First",Toast.LENGTH_SHORT).show();// warn user, that target is not selected
	    	}
    	} else{
    		Toast.makeText(this, "Stop reading from file first", Toast.LENGTH_SHORT).show();
    	}
    }
    // callback for radio buttons that selects data srouce
//    public void onClickRadioButtonClicked(View v){
//    	if(application.bluetoothService.isConnected()!=true){
//	    	switch(v.getId()){
//	    	case R.id.radio_source_bluetooth:
//	    		application.setDataSource("Bluetooth");
//	    		Log.d("RADIOBUTTONCLICKED", "RADIO BUTTON CLICKED BLUETOOTH");
//	    		break;
//	    	case R.id.radio_source_file:
//	    		application.setDataSource("File");
//	    		Log.d("RADIOBUTTONCLICKED", "RADIO BUTTON CLICKED FILE");
//	    		break;
//	    	default:
//	    		break;
//	    	}
//    	} else{
//    		Toast.makeText(this, "Stop connection or reading from file", Toast.LENGTH_SHORT ).show();
//    		if(v.getId()==R.id.radio_source_bluetooth){
//    			((RadioButton)v).toggle();
//    			((RadioButton)findViewById(R.id.radio_source_file)).toggle();
//    		} else{
//    			((RadioButton)v).toggle();
//    			((RadioButton)findViewById(R.id.radio_source_bluetooth)).toggle();
//    		}
//    	}
//    }
 // onActivityResultCallback If 
    public void onClickSelectFile(View v){
    	if(application.isReadingFromFile()){
    		Toast.makeText(this, "Stop reading from file", Toast.LENGTH_SHORT).show();
    	} else{
    		// if not reading from file, open activity
    		Intent intentResult = new Intent(this, SelectDataSourceFileActivity.class);
    		startActivityForResult(intentResult, REQUEST_SELECT_SOURCE_FILE);
    	}
    }
   public void onClickStartRead(View v){
	   if(application.getDataSourceFile()!=null){ // if file selected
		   	if(!application.bluetoothService.isConnected()){
		   		if(!application.isReadingFromFile()){
		   			// stop reading
		   			if(application.getDataSourceFile()!=null){
		   				long fileSize = application.getDataSourceFile().length();
		   				Log.d("FILESIZE", " "+fileSize);
		   				Log.d("FILENAME", " "+application.getDataSourceFile().getName());
			   			if(fileSize>128){
			   				readingFromFileService=application.getReadingFromFileService();
			   				readingFromFileService.startReading(application.getDataSourceFile()); // start reading
			   				startReadButton.setText("Stop Read");
				   			application.setReadingFromFile(true);
				   			readingStatusView.setText("reading");
			   			}else{
			   				Toast.makeText(this, "Selected file has insuficient data", Toast.LENGTH_SHORT).show();
			   			}
			   			
			   			
		   			} else{
		   				Toast.makeText(this, "Selectd data source file first", Toast.LENGTH_SHORT).show();
		   			}
		   		} else{
		   			//stop reading from file
		   			application.getReadingFromFileService().stopReading();
		   			startReadButton.setText("Start Read");
		   			application.setReadingFromFile(false);
		   			readingStatusView.setText("not reading");
		   		}
			   
		   	} else{
			   Toast.makeText(this, "Disconnect bluetooth device first",Toast.LENGTH_SHORT).show();
		   	}
	   } else{
		   Toast.makeText(this, "Select Data Source File First", Toast.LENGTH_SHORT).show();
	   }
   }
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	switch(requestCode){
    	case REQUEST_ENABLE_BT: // result from bluetooth enable activity
    		if(resultCode==RESULT_CANCELED){ // if user did not turn on bluetooth adapter
    			Toast.makeText(this, "In order to use this application you must turn on bluetooth",Toast.LENGTH_SHORT).show();// warn user
    			finish(); //finish activity
    		}
    		break;
    	case REQUEST_SELECT_TARGET_DEVICE: // if
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                 mDevice = mBluetoothAdapter.getRemoteDevice(address); // instantate bluetooth target device
                 targetDeviceView.setText(mDevice.getName()); //update target device view
                 Log.d("REQUEST_SELECT_TARGET_DEVICE", "RESULT_OK");
            }
            break;
    	case REQUEST_SELECT_SOURCE_FILE:
    		if(resultCode == Activity.RESULT_OK){
    		Log.d("ACTIVITYRESULT"," RESULT OK");
			Log.d("ACTIVITYRESULT","FILE "+application.getDataSourceFile().getName());
			sourceFileView.setText(application.getDataSourceFile().getName()); // show selected file
			
    		}
    		if (resultCode == Activity.RESULT_CANCELED){
    			Log.d("ACTIVITYRESULT","SELECT SOURCE FILE  ACTIVITY CANCELED");
    		}
    		break;
    	default:
    		break;
    	}
    }
    // callback for gesture detector events
    // in case of fling gesture
    public boolean onFling(MotionEvent start, MotionEvent finish, float xVelocity, float yVelocity) {
		if (start.getRawX() > finish.getRawX()) { // if finger slided from right to left
			
			Intent intent = new Intent(getApplicationContext(), ProcessingActivity.class);// intent to start processing activity
			startActivity(intent); // start processing activity
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);// override animation for activity transition
		} 
		return true;
	}
    // firther there are gesture callbacks that are not implemented in currents case
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
}
