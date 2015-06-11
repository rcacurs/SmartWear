package lv.edi.SmartWearMagn;

import java.io.File;
import java.io.IOException;

import lv.edi.SmartWearMagn.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class CreateLogFileActivity extends Activity{
	private EditText fileNameEditText;
	private File logFile;
	private SmartWearApplication application; // reference to application object containing global data
	//private static final String TAG_CREATE_LOG_FILE = "CREATING LOG FILE";
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_log_file_activity);
		fileNameEditText=(EditText)findViewById(R.id.logFileNameEdit);
		application=(SmartWearApplication)getApplication();
		/**Calendar now = Calendar.getInstance();
		fileNameEditText.setText(now.toString());**/
	}
	public void onButtonClickClose(View view){
		finish();
	}
	public void onButtonClickCreateLogFile(View view){
		
		File logFileDirectory = application.getLogFileDirectory();
		//Log.d(TAG_CREATE_LOG_FILE,logFileDirectory.toString());
		//Log.d(TAG_CREATE_LOG_FILE, logFileDirectory.getAbsolutePath());
		logFile = new File(logFileDirectory ,fileNameEditText.getText()+".dat");
		//Log.d(TAG_CREATE_LOG_FILE, logFile.getPath());
		try{
			if(logFile.exists()){// if log file with that name exists, delete previous version
				logFile.delete();
			}
			logFile.createNewFile();
		}catch(IOException ex){
			Toast.makeText(this, "Error creating file "+fileNameEditText.getText()+".dat", Toast.LENGTH_LONG);
		}
		
		Toast.makeText(this, "Log file "+fileNameEditText.getText()+".dat succesfully created", Toast.LENGTH_LONG).show();
		finish();
	}
}
