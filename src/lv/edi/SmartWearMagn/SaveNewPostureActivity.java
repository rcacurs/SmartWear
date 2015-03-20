package lv.edi.SmartWearMagn;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import lv.edi.SmartWearMagn.R;
import lv.edi.SmartWearProcessing.Segment;
import android.app.Activity;
import android.os.Bundle;
//import android.os.Environment;
//import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class SaveNewPostureActivity extends Activity{
	private SmartWearApplication application;
	private EditText editPostureNameView;
	private File postureFile;
	private BluetoothService bluetoothService;
	//private ReadingFromFileService readingFromFileService;
	//private float[][] accDataArray;
	//private float[][][] accPoints = new float[5][4][3];
	private Segment[][] segments = new Segment[SmartWearApplication.GRID_ROWS][SmartWearApplication.GRID_COLS];
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		application = (SmartWearApplication) getApplication();
		bluetoothService = application.getBluetoothService();
		//readingFromFileService = application.getReadingFromFileService();
		setContentView(R.layout.save_new_posture_activity);
		editPostureNameView =(EditText) findViewById(R.id.postureNameEdit);
		if(bluetoothService.isConnected()){
			for(int i=0;i<segments.length;i++){
				for(int j=0;j<segments[0].length;j++){
					switch(j){
					case 0:
						segments[i][j] = new Segment((float)Math.toRadians(application.getZAngle1()));
						break;
					case 1:
						segments[i][j] = new Segment((float)Math.toRadians(application.getZAngle2()));
						break;
					case 2:
						segments[i][j] = new Segment((float)Math.toRadians(application.getZAngle3()));
						break;
					case 3:
						segments[i][j] = new Segment(0.0f);
						break;
					case 4:
						segments[i][j] = new Segment((float)-Math.toRadians(application.getZAngle3()));
						break;
					case 5:
						segments[i][j] = new Segment((float)-Math.toRadians(application.getZAngle2()));
						break;
					case 6:
						segments[i][j] = new Segment((float)Math.toRadians(application.getZAngle3()));
						break;
					default:
							break;
					}

					(segments[i][j]).setSegmentOrientation(application.sensorGridArray[i][j]);
				
				}
			}
			Segment.setSegmentCenters(segments, (short)application.getReferenceRow(), (short)application.getReferenceCol());
		}	
	}
	
	public void onButtonClickSavePostureButton(View view){
		if(!(editPostureNameView.getText().toString()).equals("")){
				// saving posture
					String postureFolderName=application.getPostureDirectory().getPath();
					postureFile = new File(postureFolderName,editPostureNameView.getText()+".dat");
					try{
						if(postureFile.exists()){// if log file with that name exists, delete previous version
							postureFile.delete();
						}
						postureFile.createNewFile();
						}catch(IOException ex){
							Toast.makeText(this, "Error creating posture file "+editPostureNameView.getText()+".dat", Toast.LENGTH_LONG);
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
				Toast.makeText(this, "Posture file "+editPostureNameView.getText()+".dat succesfully created", Toast.LENGTH_LONG).show();
				finish();
			}else{
				Toast.makeText(this, "Enter posture file name!", Toast.LENGTH_LONG).show();
					
			}
		}
		public void onButtonClickClose(View view){
			finish();
		}

}
