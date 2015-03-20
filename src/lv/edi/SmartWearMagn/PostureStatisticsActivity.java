package lv.edi.SmartWearMagn;



import lv.edi.SmartWearMagn.R;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

public class PostureStatisticsActivity extends Activity{
	private SmartWearApplication application;
	private GraphicalView mChartView;
	private LinearLayout chartViewLayout;
	public static final int UPDATE_STATISTICS = 88;
	
	@SuppressLint("HandlerLeak")
	public final Handler handler = new Handler(){
		public void handleMessage(Message message){
			switch (message.what){
				case UPDATE_STATISTICS: // posture state saved message
				    mChartView.repaint();
				    //Toast.makeText(getApplicationContext(), "update graph", Toast.LENGTH_SHORT).show();
					break;
				default:
					break;
					
			}	
		}
    };
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.posture_statistics_activity);
		
		application=(SmartWearApplication) getApplication();
		application.statisticsHandler=handler;
		chartViewLayout=(LinearLayout)findViewById(R.id.chart);
		mChartView = ChartFactory.getLineChartView(this, application.mDataset, application.mRenderer);
	    Log.d("chartView", " "+mChartView+" "+chartViewLayout);

	    chartViewLayout.addView(mChartView,new LayoutParams(LayoutParams.MATCH_PARENT,
	            LayoutParams.MATCH_PARENT));
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_bluetooth_connection, menu);
        return true;
    }
	
	
	
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
	            case R.id.bluetoth_connection_activity:
	            	Intent intent4 = new Intent(this, DataSourceActivity.class);
	            	startActivity(intent4);
	            	return true;
	            default:
	                return false;
	        }
	    }
	

}
