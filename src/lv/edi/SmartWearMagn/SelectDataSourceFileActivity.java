package lv.edi.SmartWearMagn;

import java.io.File;
import java.util.Arrays;

import lv.edi.SmartWearMagn.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class SelectDataSourceFileActivity extends Activity {
	public static final String EXTRA_SELECTED_FILE_NAME="selectedFileName";
	//private Button closeActivityButton; //button that closes this activity
	private String[] filesArray; // array that stores file names currently in log file folder
	private File logFileDirectory; // represents folder on SD cards where log files are stored
	private SmartWearApplication application; // object that stores all global data
	private ArrayAdapter<String> adapter;
	private ListView fileNameListView;
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_data_source_file);
		//closeActivityButton = (Button)findViewById(R.id.button_close_select_file_activity);
		fileNameListView = (ListView)findViewById(R.id.list_files);
		application = (SmartWearApplication)getApplication();
		logFileDirectory = application.getLogFileDirectory();
		filesArray = logFileDirectory.list(); // getting all log files from log file directory into string
		if(filesArray!=null){
			Arrays.sort(filesArray);
			adapter = new ArrayAdapter<String>(this, R.layout.file_name, filesArray); // construct array adapter for file name list
			fileNameListView.setAdapter(adapter);
		}
		fileNameListView.setOnItemClickListener(mOnItemClickListener);
		  Button closeButton = (Button) findViewById(R.id.button_close_select_file_activity);
	        closeButton.setOnClickListener(new OnClickListener() {
	            public void onClick(View v) {
	            	setResult(Activity.RESULT_CANCELED); //set result activity, closed with close button
	        		finish();
	            }
	        });
	}
	
//	public void onClickCloseActivity(View v){
//		setResult(Activity.RESULT_CANCELED); //set result activity, closed with close button
//		finish();
//		
//	}
	private OnItemClickListener mOnItemClickListener = new OnItemClickListener(){
		public void onItemClick(@SuppressWarnings("rawtypes") AdapterView parent, View v, int position, long id){
			//Log.d("LISTITEMCLICK", "CLICKED "+((TextView)v).getText());
			File selectedFile = new File(application.getLogFileDirectory()+"/"+((TextView)v).getText());
			application.setDataSourceFile(selectedFile);
			setResult(Activity.RESULT_OK);//, intent);
			finish();
		}
	};

}
