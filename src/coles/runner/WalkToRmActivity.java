package coles.runner;

import java.util.ArrayList;
import java.util.List;

import coles.runner.R;
import coles.runner.FloorMapActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;

public class WalkToRmActivity extends Activity {
	String rbtnMatchBy1 = " Name", rbtnMatchBy2 = "Office Name";
	String inputText1, inputText2;
	Double percentX, percentY;
	Integer room;

	private EditText searchBarRoom;
	private Button bSearch;
	private String searchString, building;

	private Spinner building_spinner;

	private DBHelper dbHelp;
	private Cursor myCursor;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_rm_layout);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		dbHelp = new DBHelper(this);
		dbHelp.open();

		initViews();

	}

	private void initViews() {
		/*
		 * Keep the two search bars so when user selects to search by professor
		 * name, you can have an auto-complete-esque function that will match
		 * names according to database
		 */

		searchBarRoom = (EditText) findViewById(R.id.search_bar_room);
		bSearch = (Button) findViewById(R.id.search_button_room);
		building_spinner = (Spinner) findViewById(R.id.building_spinner);
		searchBarRoom.setEnabled(true);
		bSearch.setEnabled(false);

		List<String> buildingList = new ArrayList<String>();
		buildingList.add("Burruss");

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, buildingList);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		building_spinner.setAdapter(adapter);

		building_spinner
				.setOnItemSelectedListener(new OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int pos, long id) {
						parent.getContext();

						building = parent.getItemAtPosition(pos).toString();

						if (building == "Burruss") {
							building = "bb_rooms";
						}

					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						// TODO Auto-generated method stub

					}

				});

		AlertDialog.Builder nothingSelectedBuild = new AlertDialog.Builder(
				WalkToRmActivity.this);
		nothingSelectedBuild.setCancelable(false);
		nothingSelectedBuild.setMessage("Please fill out the required fields.");
		nothingSelectedBuild.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

		nothingSelectedBuild.create();

		AlertDialog.Builder invalidRoomBuild = new AlertDialog.Builder(
				WalkToRmActivity.this);
		invalidRoomBuild.setCancelable(false);
		invalidRoomBuild.setMessage("Invalid Room Number.");
		invalidRoomBuild.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

		searchBarRoom.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable arg0) {
				if (searchBarRoom.getText().toString().equals("")) {
					bSearch.setEnabled(false);
				} else {
					bSearch.setEnabled(true);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				bSearch.setEnabled(false);
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				bSearch.setEnabled(true);
			}

		});

		bSearch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// move to another activity screen
				searchString = searchBarRoom.getText().toString();

				Intent intent = new Intent(WalkToRmActivity.this,
						FloorMapActivity.class);

				myCursor = dbHelp.fetchRoom(building, searchString);

				Cursor c = myCursor;

				if (c != null) {
					if (c.moveToFirst()) {
						Double xPer = c.getDouble(c
								.getColumnIndexOrThrow(DBHelper.COLUMN_XPER));
						Double yPer = c.getDouble(c
								.getColumnIndexOrThrow(DBHelper.COLUMN_YPER));

						intent.putExtra("xPer1", xPer);
						intent.putExtra("yPer1", yPer);
						intent.putExtra("room#", c.getString(c
								.getColumnIndexOrThrow(DBHelper.COLUMN_ROOM)));
						intent.putExtra("floor", c.getInt(c
								.getColumnIndexOrThrow(DBHelper.COLUMN_FNUM)));
						intent.putExtra(
								"building",
								c.getString(c
										.getColumnIndexOrThrow(DBHelper.COLUMN_BUILDING)));

						dbHelp.close();
						finish();

						startActivity(intent);
					}
				}

			}
		});

	}

}
