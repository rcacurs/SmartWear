package lv.edi.SmartWearMagn;

//import java.io.DataInputStream;
import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
import java.util.Arrays;

import lv.edi.SmartWearMagn.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
//import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PosturesActivity extends Activity{
	private ListView postureListView;
	private File postureFileDirectory = null;
	private String postureFilesArray[];
	private ArrayAdapter<String> adapter;
	private SmartWearApplication application;
	private BluetoothService service;
	protected void onCreate(Bundle savedInstanceState){
		application = (SmartWearApplication) getApplication();
		service = application.getBluetoothService();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.posture_reference_activity);
		postureFileDirectory = new File(application.getPostureDirectory().getPath());
		if(!postureFileDirectory.exists()){
			postureFileDirectory.mkdirs();
		}
		postureListView = (ListView)findViewById(R.id.posture_reference_list);
		postureListView.setOnItemClickListener(mOnItemClickListener);
		
	}
	public void onResume(){
		super.onResume();
		postureFilesArray=postureFileDirectory.list();
		if(postureFilesArray!=null){
			Arrays.sort(postureFilesArray);
			adapter = new ArrayAdapter<String>(this,R.layout.file_name, postureFilesArray );
			postureListView.setAdapter(adapter);
		}
	}
	public void onClickClosePostureReference(View view){
		 setResult(Activity.RESULT_CANCELED);     
		 finish();
	}
	public void onClickSavePosture(View view){
		if(((service.isConnected()))||(application.isReadingFromFile())){
			Intent intent = new Intent(this, SaveNewPostureActivity.class);
			startActivity(intent);
		} else{
			Toast.makeText(this, "Connect to bluetooth device or start reading from file", Toast.LENGTH_LONG).show();
		}	
	}
	
	private OnItemClickListener mOnItemClickListener = new OnItemClickListener(){
		public void onItemClick(AdapterView<?> parent, View v, int position, long id){
			Log.d("LISTITEMCLICK", "CLICKED "+((TextView)v).getText());
			String selectedPosture = ""+(((TextView)v).getText());
			Log.d("LOAD_POSTURE", selectedPosture);
			Intent returnIntent = new Intent();
			returnIntent.putExtra("data",selectedPosture);
			setResult(Activity.RESULT_OK, returnIntent);//, intent);
			finish();
		}
	};
	
}
