package lv.edi.SmartWearMagn;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import lv.edi.SmartWearMagn.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ManageLogFilesActivity extends Activity implements OnItemClickListener{
	private ArrayAdapter<String> adapter = null;
	private SmartWearApplication application = null;
	private ListView manageFilesListView = null;
	private File logFileDirectory = null;
	private String[] filesArray;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.manage_files_activity);
		application = (SmartWearApplication)getApplication();
		manageFilesListView = (ListView)findViewById(R.id.manage_files_list);
		logFileDirectory = application.getLogFileDirectory();
		manageFilesListView.setOnItemClickListener(this);
		
	}
	public void onResume(){
		super.onResume();
		filesArray=logFileDirectory.list();
		if(filesArray!=null){
			Arrays.sort(filesArray);
			adapter = new ArrayAdapter<String>(this,R.layout.file_name, filesArray );
			manageFilesListView.setAdapter(adapter);
		}
	}
	
	public void onClickCloseManageFilesActivity(View view){
		setResult(Activity.RESULT_CANCELED);
		finish();
	}
	public void onClickCreateNewFile(View view){
		Intent intent = new Intent(this, CreateLogFileActivity.class);
		startActivity(intent);
	}
	public void onClickUnselectFile(View view){
		application.setSelectedLogFile(null);
		finish();
		
	}
	
	public void onItemClick(AdapterView<?>  parent, View v, int position, long id){
		File selectedFile = new File(application.getLogFileDirectory()+"/"+((TextView)v).getText());
		application.setSelectedLogFile(selectedFile);
		try{
		 DataOutputStream stream = new DataOutputStream(new FileOutputStream(selectedFile,true));

		 application.setSelectedLogFileStream(stream);
		} catch(IOException ex){
		}
		setResult(Activity.RESULT_OK);//, intent);
		finish();
		
	}

}
