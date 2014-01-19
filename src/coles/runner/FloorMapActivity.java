package coles.runner;

import coles.runner.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class FloorMapActivity extends Activity
{
	
	private String contents;
	private static final int fourthFloor = 4;
	private static final int thirdFloor = 3;
	private static final int secondFloor = 2;
	private static final int firstFloor = 1;



	/**
	 * AsyncTask params <Params, Progress, Result>
	 * Params:info passed to be used in task
	 * Progress: type of progress that is accomplished
	 * Result:result returned after task has finished
	 * 
	 * Basically for this activity you need all your method in the other thread because of
	 * we are changing the background around and all.  Without having the methods all in the thread,
	 * doesn't let the ProgressDialog show for some reason (I think because of the animation stuff)
	 * 
	 */



	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.floormap);
		ProgressTask task = new ProgressTask();
		task.execute();

		final Button btnWalkToRm = (Button) findViewById(R.id.btnWalkRm); 

		btnWalkToRm.setOnClickListener(new OnClickListener() 
		{ 
			@Override
			public void onClick(View v) 
			{ 
				IntentIntegrator integrator = new IntentIntegrator(FloorMapActivity.this);
				integrator.initiateScan();
			} 
		});
		
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

		try 
		{
			if (scanResult != null) 
			{
				
				contents = scanResult.getContents();
				scanResult.getFormatName();
				
				Intent i = null;
				i = new Intent(FloorMapActivity.this, FloorMapWithQRActivity.class);

				String[] values;
				String delimiter = ":";
				values = contents.split(delimiter);
				double xPer2 = Double.parseDouble(values[1]);
				double yPer2 = Double.parseDouble(values[2]);
				int floor2 = Integer.parseInt(values[3]);

				Bundle extras = getIntent().getExtras();

				double xPer1 = extras.getDouble("xPer1");
				double yPer1 = extras.getDouble("yPer1");
				int floor1 = extras.getInt("floor");
				extras.getString("building");
				String room = extras.getString("room#");

				Bundle b = new Bundle();
				b.putString("room", room);
				b.putDouble("xPer1", xPer1);
				b.putDouble("yPer1", yPer1);

				b.putDouble("xPer2", xPer2);
				b.putDouble("yPer2", yPer2);

				b.putInt("floor2", floor2);
				b.putInt("floor1", floor1);
				b.putString("QR", contents);

				i.putExtras(b);
				finish();

				startActivity(i);

			}
			else
			{
				
			}

		} catch (Exception e) {

		}
	}

	
	private void setBackgroundAnimations(Double xPer, Double yPer, Double xPer2, Double yPer2) 
	{
		View content = getWindow().findViewById(Window.ID_ANDROID_CONTENT);
		Log.d("DISPLAY", content.getWidth() + " x " + content.getHeight());

		DisplayMetrics displayMetrics =  new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);


		int ht = displayMetrics.heightPixels;
		int wt = displayMetrics.widthPixels;
		ImageView arrow1 = (ImageView) findViewById(R.id.arrow);
		ImageView arrow2 = (ImageView) findViewById(R.id.arrowred);
		
		Animation myFadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.tween);
		Animation myFadeInAnimation2 = AnimationUtils.loadAnimation(this, R.anim.tween);
		
		Double percentX = xPer;
		Double percentY = yPer;

		Double percentX2 = xPer2;
		Double percentY2 = yPer2;

		int x = (int)(wt * percentX);
		int y = (int) (ht * percentY);

		int x2 = (int) (wt* percentX2);
		int y2 = (int) (ht * percentY2);

		arrow1.setPadding(x, 0, 0, y);
		arrow2.setPadding(x2, 0, 0, y2);
		arrow1.startAnimation(myFadeInAnimation);
		arrow2.startAnimation(myFadeInAnimation2);
	}
	

	private void popUpAlert(String roomNumString) 
	{
		AlertDialog.Builder popUpBuild = new AlertDialog.Builder(this);

		popUpBuild.setMessage("Destination is room " + roomNumString + ". For Walking Directions, Find QR code By Nearest Elevator and Click QR Button At Top");
		popUpBuild.setCancelable(false);
		popUpBuild.setPositiveButton("OK", new DialogInterface.OnClickListener() 
		{

			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
				dialog.cancel();
			}
		});

		final AlertDialog popUpAlert = popUpBuild.create();

		popUpAlert.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.floor_map_menu, menu);
		return true;
	}

	private class ProgressTask extends AsyncTask <Void, Void, Integer>
	{
		private FrameLayout frame; 
		private ProgressDialog dialog;
		private Bundle extras;
		private String roomNumString;
		private Double xPer, yPer;
		private Double xPer2, yPer2;
		private int roomNumInt, start;

		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			frame = (FrameLayout) findViewById(R.id.RootView);
			dialog = ProgressDialog.show(FloorMapActivity.this, "Loading", "Loading Please Wait");
			
			extras = getIntent().getExtras();
			roomNumInt = extras.getInt("floor");
			
			roomNumString = extras.getString("room#");
			setTitle("Walk to BB-" + roomNumString);
			
			xPer = extras.getDouble("xPer1");
			yPer = extras.getDouble("yPer1");

			//These are for floor elevators
			xPer2 = 0.696;
			yPer2 = 0.315;

			setBackgroundAnimations(xPer, yPer, xPer2, yPer2);

		}

		@Override
		protected Integer doInBackground(Void... params)
		{
			//Params is set up as an array
			//int start = params[0];
			int i = 0;
			start = roomNumInt;

			if(start < firstFloor)
			{
				//Error message (dialog probably)
			}
			else if(start == firstFloor)
			{
				i=1;
			}
			else if(start == secondFloor)
			{
				i=2;
			}
			else if(start == thirdFloor)
			{
				i=3;
			}
			else if(start == fourthFloor)
			{
				i=4;
			}
			else
			{
				//Error Message
			}
			return i;

		}

		@Override
		protected void onPostExecute(Integer result)
		{

			super.onPostExecute(result);

			if(dialog.isShowing())
			{
				dialog.dismiss();
			}

			if(result == 1)
			{
				frame.setBackgroundResource(R.drawable.bb_1st_smaller);
				popUpAlert(roomNumString);
			}
			else if(result == 2)
			{
				frame.setBackgroundResource(R.drawable.bb_2nd_smaller);
				popUpAlert(roomNumString);
			}
			else if(result == 3)
			{
				frame.setBackgroundResource(R.drawable.bb_3rd_smaller);
				popUpAlert(roomNumString);
			}
			else
			{
				frame.setBackgroundResource(R.drawable.bb_4th_smaller);
				popUpAlert(roomNumString);
			}

			
		}

	}

}


