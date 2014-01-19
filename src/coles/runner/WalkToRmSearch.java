package coles.runner;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class WalkToRmSearch extends Activity 
{
	String rbtnMatchBy1 =" Name", rbtnMatchBy2 ="Office Name";
	String inputText1, inputText2;
	Double percentX, percentY;
	Integer room;

	private EditText searchBarRoom;
	private Button bSearch;
	private String searchString;
	private static final String Burruss = "Burruss";
	
	private Spinner building_spinner;

	private DBHelper dbHelp;
	private RoomSearchAsync roomSearch;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{    
		super.onCreate(savedInstanceState);    
		setContentView(R.layout.search_rm_layout);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		dbHelp = new DBHelper(this);
		dbHelp.open();

		initViews();

	}

	private void initViews() 
	{
		/*
		 * Keep the two search bars so when user selects to search by professor name, you can have an auto-complete-esque function that will match names according to database
		 */

		searchBarRoom = (EditText)findViewById(R.id.search_bar_room);
		bSearch = (Button)findViewById(R.id.search_button_room);
		building_spinner = (Spinner)findViewById(R.id.building_spinner);
		searchBarRoom.setEnabled(true);
		bSearch.setEnabled(false);

		List<String> buildingList = new ArrayList<String>();
		buildingList.add("Burruss");

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, buildingList);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		building_spinner.setAdapter(adapter);

		AlertDialog.Builder nothingSelectedBuild = new AlertDialog.Builder(WalkToRmSearch.this);
		nothingSelectedBuild.setCancelable(false);
		nothingSelectedBuild.setMessage("Please fill out the required fields.");
		nothingSelectedBuild.setPositiveButton("OK", new DialogInterface.OnClickListener() 
		{

			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
				dialog.dismiss();
			}
		});

		nothingSelectedBuild.create();

		AlertDialog.Builder invalidRoomBuild = new AlertDialog.Builder(WalkToRmSearch.this);
		invalidRoomBuild.setCancelable(false);
		invalidRoomBuild.setMessage("Invalid Room Number.");
		invalidRoomBuild.setPositiveButton("OK", new DialogInterface.OnClickListener() 
		{

			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
				dialog.dismiss();
			}
		});

		searchBarRoom.addTextChangedListener(new TextWatcher()
		{

			@Override
			public void afterTextChanged(Editable arg0) 
			{
				if(searchBarRoom.getText().toString().equals(""))
				{
					bSearch.setEnabled(false);
				}
				else
				{
					bSearch.setEnabled(true);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) 
			{
				bSearch.setEnabled(false);
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count)
			{
				bSearch.setEnabled(true);
			}

		});

		bSearch.setOnClickListener(new OnClickListener() { 
			@Override
			public void onClick(View v) 
			{ 	
				// move to another activity screen
				searchString = searchBarRoom.getText().toString();
				
				roomSearch = new RoomSearchAsync();
				roomSearch.execute(searchString);

			} 
		}); 

	}
	
	private class RoomSearchAsync extends AsyncTask<String, Void, Void>
	{

		private DBHelper dbHelper;
		private Cursor myCursor, c;
		private Double xPer, yPer;
		private String room, building;
		private int floor;
		private Intent intent;
		
		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			dbHelper = new DBHelper(getApplicationContext());
			intent = new Intent(WalkToRmSearch.this, FloorMapActivity.class);
		}
		
		@Override
		protected Void doInBackground(String... params)
		{
			String search = params[0];
			myCursor = dbHelper.fetchRoom(Burruss, search);
			c = myCursor;
			
			
			if(c != null)
			{
				if(c.moveToFirst())
				{
					xPer = c.getDouble(c.getColumnIndexOrThrow(DBHelper.COLUMN_XPER));
					yPer = c.getDouble(c.getColumnIndexOrThrow(DBHelper.COLUMN_YPER));
					room = c.getString(c.getColumnIndexOrThrow(DBHelper.COLUMN_ROOM));
					floor = c.getInt(c.getColumnIndexOrThrow(DBHelper.COLUMN_FNUM));
					building = c.getString(c.getColumnIndexOrThrow(DBHelper.COLUMN_BUILDING));
					
					dbHelper.close();
					
					intent.putExtra("xPer1", xPer);
					intent.putExtra("yPer1", yPer);
					intent.putExtra("room#", room); 
					intent.putExtra("floor", floor);
					intent.putExtra("building", building);
					
					finish();
					startActivity(intent);
				}
			}
			return null;
		}
		
	}

}
