package lv.edi.SmartWearMagn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

import lv.edi.SmartWearMagn.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class SavePostureIMGActivity extends Activity{
	SmartWearApplication application;
	Bitmap postureScreenBitmap;
	EditText imgFileNameView;
	@Override 
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		application = (SmartWearApplication)getApplicationContext();
		setContentView(R.layout.save_new_posture_img_activity);
		Intent i = getIntent();
		postureScreenBitmap = i.getParcelableExtra("screenBitmap");
		imgFileNameView = (EditText)findViewById(R.id.postureIMGNameEdit);
	}
	
	public void onButtonClickSavePostureButton(View v){
		String fileName = imgFileNameView.getText().toString();
		if(!(fileName.equals(""))){
		File postureIMGDirectory = application.getPostureIMGDirectory();
		String filePath=postureIMGDirectory.getPath()+"/"+fileName+".jpg";
		File postureIMGFile = new File(filePath);
		OutputStream fout = null;
			try {
			    fout = new FileOutputStream(postureIMGFile);
			    postureScreenBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fout);
			    fout.flush();
			    fout.close();
			    
			   // Toast.makeText(this, ""+fileName+".jpg succesfully created",Toast.LENGTH_SHORT).show();
			    finish();

			} catch (FileNotFoundException e) {
			    // TODO Auto-generated catch block
			    e.printStackTrace();
			} catch (IOException e) {
			    // TODO Auto-generated catch block
			    e.printStackTrace();
			}
		} else{
			Toast.makeText(this, "Enter Posture Image File Name!", Toast.LENGTH_SHORT).show();
		}
	} 
	public void onButtonClickClose(View v){
		finish();
	}
	
	public static Bitmap SavePixels(int x, int y, int w, int h, GL10 gl)
	{  
	     int b[]=new int[w*(y+h)];
	     int bt[]=new int[w*h];
	     IntBuffer ib=IntBuffer.wrap(b);
	     ib.position(0);
	     gl.glReadPixels(x, 0, w, y+h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ib);

	     for(int i=0, k=0; i<h; i++, k++)
	     {//remember, that OpenGL bitmap is incompatible with Android bitmap
	      //and so, some correction need.        
	          for(int j=0; j<w; j++)
	          {
	               int pix=b[i*w+j];
	               int pb=(pix>>16)&0xff;
	               int pr=(pix<<16)&0x00ff0000;
	               int pix1=(pix&0xff00ff00) | pr | pb;
	               bt[(h-k-1)*w+j]=pix1;
	          }
	     }


	     Bitmap sb=Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
	     return sb;
	}

}
