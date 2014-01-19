package coles.runner;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

public class WalkToBldgActivity extends Activity implements LocationListener 
{

	private Button startSearch;
	private Spinner buildingSpin;
	private String building_name;
	private String etBuildingAbbrev;
	private String etBuildingNumber;

	private AutoCompleteTextView buildingAbbrevField;

	private double latitude_dest, longitude_dest;
	private double latitude_start, longitude_start;

	private LocationManager locationManager;

	private DBHelper dbHelp;
	private Cursor myCursor;
	
	private ArrayList<String> buildingArrayList;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{    
		super.onCreate(savedInstanceState);    
		setContentView(R.layout.walktobldg);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		dbHelp = new DBHelper(this);
		dbHelp.open();

		locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if(location != null)
		{
			latitude_start = (location.getLatitude());
			longitude_start = (location.getLongitude());
		}
		
		
		startSearch = (Button)findViewById(R.id.search_button_building);
		startSearch.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View arg0)
			{
				myCursor = dbHelp.fetchBuilding(building_name);

				Cursor c = myCursor;
				
				if(c != null)
				{
					if(c.moveToFirst())
					{
						latitude_dest = c.getDouble(c.getColumnIndexOrThrow(DBHelper.COLUMN_LAT));
						longitude_dest = c.getDouble(c.getColumnIndexOrThrow(DBHelper.COLUMN_LONG));

						Intent mapIntent = new Intent(android.content.Intent.ACTION_VIEW, 
								Uri.parse("http://maps.google.com/maps?saddr="+latitude_start+","+longitude_start
										+"&daddr="+latitude_dest+","+longitude_dest)); 

						dbHelp.close();

						finish();

						startActivity(mapIntent); 
					}
				}
			}
			
		});
		
		InitViews();
	}

	private void InitViews() 
	{
		buildingSpin = (Spinner)findViewById(R.id.building_spinner);

		buildingAbbrevField = (AutoCompleteTextView)findViewById(R.id.building_abbrev);
		buildingAbbrevField.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View arg0) 
			{
				buildingAbbrevField.setText("");
			}
			
		});
		
		buildingAbbrevField.addTextChangedListener(new TextWatcher()
		{

			@Override
			public void afterTextChanged(Editable s) 
			{
				String searchFieldName = buildingAbbrevField.getText().toString();
				int i = buildingArrayList.indexOf(searchFieldName);
				buildingSpin.setSelection(i);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) 
			{
				
			}
			
		});
		buildingAbbrevField.setThreshold(2);

		myCursor = dbHelp.fetchAllBuilding();
		startManagingCursor(myCursor);

		Cursor c = myCursor;

		buildingArrayList = new ArrayList<String>();

		if(c!=null)
		{
			/**
			 * Have to do a while loop here because Cursor.moveToNext() returns true if next record is found.
			 * If statement will only execute once.
			 * Building Name Column of records are added to arrayList and that arrayList is used for the adapter.
			 */
			while(c.moveToNext())
			{
				buildingArrayList.add(c.getString(c.getColumnIndexOrThrow(DBHelper.COLUMN_BNAME)));
			}
		}


		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, buildingArrayList);
		buildingAbbrevField.setAdapter(arrayAdapter);

		// Initialize the location fields

		String[] FROM = new String[]{DBHelper.COLUMN_BNAME, DBHelper.COLUMN_ABBREV, DBHelper.COLUMN_BNUM};
		int[] TO = new int[]{android.R.id.text1};
		SimpleCursorAdapter spinAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, myCursor, FROM, TO);
		spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		buildingSpin.setAdapter(spinAdapter);

		buildingSpin.setOnItemSelectedListener(new OnItemSelectedListener()
		{

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id)
			{
				parent.getContext();

				Cursor spinC = (Cursor)buildingSpin.getSelectedItem();
				building_name = spinC.getString(spinC.getColumnIndexOrThrow(DBHelper.COLUMN_BNAME));

				etBuildingAbbrev = spinC.getString(spinC.getColumnIndexOrThrow(DBHelper.COLUMN_ABBREV));
				etBuildingNumber = spinC.getString(spinC.getColumnIndexOrThrow(DBHelper.COLUMN_BNUM));

				if(etBuildingAbbrev.equals(""))
				{
					buildingAbbrevField.setText("Building # " + etBuildingNumber);
				}
				else
				{
					buildingAbbrevField.setText(etBuildingAbbrev + " " + "Building # " + etBuildingNumber);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}

		});
	}

	/* Request updates at startup */
	@Override
	protected void onResume() 
	{
		super.onResume();
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 400, 1, this);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		locationManager.removeUpdates(this);
	}
	
	@Override
	public void onLocationChanged(Location location) 
	{
		latitude_start = (location.getLatitude());
		longitude_start = (location.getLongitude());

	}

	@Override
	public void onProviderDisabled(String provider) 
	{

	}

	@Override
	public void onProviderEnabled(String provider) 
	{

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) 
	{

	}

}
