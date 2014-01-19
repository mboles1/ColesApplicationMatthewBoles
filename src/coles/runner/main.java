package coles.runner;

import java.util.ArrayList;

import coles.runner.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class main extends Activity implements OnClickListener
{

	private DBHelper dbHelp;
	private Cursor myCursor;
	private String receivedAudioCommand;
	private String subStringOfCommand;

	/*
	 * Written by Matthew Boles
	 * 
	 */


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.realmain);

		/*
		if (AppStatus.getInstance(this).isOnline(this))
		{

		} 
		else {  
			Toast internet = Toast.makeText(main.this, "No internet connection is detected", Toast.LENGTH_SHORT);
			internet.setGravity(Gravity.TOP, internet.getXOffset() / 2, internet.getYOffset() / 2);
			internet.show();   
		}
		 */

		ImageButton maps = (ImageButton)findViewById(R.id.mapsBTN);
		maps.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent myInt = new Intent(main.this, MapsDirectionsWelcome.class);
				startActivity(myInt);
			}
		});
		ImageButton directory = (ImageButton) findViewById(R.id.directoryBTN);
		directory.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) 
			{

				Intent myInt = new Intent(main.this, DirectoryActivity.class);
				startActivity(myInt);

			}
		});

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.audio_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		startVoiceRecognitionActivity();

		return true;
	}

	private void startVoiceRecognitionActivity() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		//uses free form text input
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		//Puts a customized message to the prompt
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
				"");
		startActivityForResult(intent, 0);
	}

	/**
	 * Handles the results from the recognition activity.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == RESULT_OK) {


			dbHelp = new DBHelper(this);
			dbHelp.open();


			AlertDialog.Builder invalidAudioCommand = new AlertDialog.Builder(main.this);
			invalidAudioCommand.setCancelable(false);
			invalidAudioCommand.setMessage("Sorry, I Don't Recognize That Command, Try Again");
			invalidAudioCommand.setPositiveButton("OK", new DialogInterface.OnClickListener() 
			{
				@Override
				public void onClick(DialogInterface dialog, int which) 
				{
					dialog.dismiss();
				}
			});

			final AlertDialog invalidAudioCommandDialog = invalidAudioCommand.create();

			// Fill the list view with the strings the recognizer thought it could have heard
			ArrayList<String> matches = new ArrayList<String>();

			matches.ensureCapacity(0);

			matches.addAll(data.getStringArrayListExtra(
					RecognizerIntent.EXTRA_RESULTS));		

			Intent floorMapIntent = new Intent(main.this, FloorMapActivity.class);

			if(matches.contains("walk to building") || matches.contains("go to building"))
			{
				Intent mapsIntent = new Intent (main.this, WalkToBldgActivity.class);
				//matches.clear();
				startActivity(mapsIntent);
			}
			if(matches.contains("walk to room") || matches.contains("go to room"))
			{
				Intent roomIntent = new Intent(main.this, WalkToRmActivity.class);
				//matches.clear();
				startActivity(roomIntent);
			}


			else
			{
				try
				{
					receivedAudioCommand = matches.get(0);

					subStringOfCommand = receivedAudioCommand.substring(13,16);

					matches.clear();

					myCursor = dbHelp.fetchSpecificRoom(subStringOfCommand);

					Cursor c = myCursor;

					if(c != null)
					{
						if(c.moveToFirst())
						{
							Double xPer = c.getDouble(c.getColumnIndexOrThrow(DBHelper.COLUMN_XPER));
							Double yPer = c.getDouble(c.getColumnIndexOrThrow(DBHelper.COLUMN_YPER));

							floorMapIntent.putExtra("xPer1", xPer);
							floorMapIntent.putExtra("yPer1", yPer);
							floorMapIntent.putExtra("room#", c.getString(c.getColumnIndexOrThrow(DBHelper.COLUMN_ROOM))); 
							floorMapIntent.putExtra("floor", c.getInt(c.getColumnIndexOrThrow(DBHelper.COLUMN_FNUM)));
							floorMapIntent.putExtra("building", c.getString(c.getColumnIndexOrThrow(DBHelper.COLUMN_BUILDING)));

							dbHelp.close();
							startActivity(floorMapIntent);

						}
					}
				}
				catch(Exception e)
				{
					invalidAudioCommandDialog.show();
				}

				super.onActivityResult(requestCode, resultCode, data);
			}

		}


	}
}	
