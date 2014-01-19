package coles.runner;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class ColesActivity extends Activity 
{
	private static final int waitTime = 1500;
	private static DBHelper dbHelp;
	private boolean dataConnection;
	private Cursor myCursor;

	/**
	 * Written by Matthew Boles
	 * 
	 */

	/**
	 * Reading csv files and inserting that data into the database has been separated into two threads.
	 * Each table getting it's own thread to read and write.  Theses threads are executed inside an async task.
	 * First thing that occurs is checking if directory table is empty.  If empty, csv files are loaded into the database.
	 * If table is not empty, activity proceeds after a timer.
	 */

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);

		dataConnection = ConnectionPresent();

		dbHelp = new DBHelper(this);
		dbHelp.open();
		/*
		 *	checkIfDirectoryEmpty returns the count of all rows in the table.
		 *	If count returned is zero, then the table is empty.  There will always be
		 *	a row returned.  That's why there is no need to check if the Cursor to moveToFirst 
		 */
		if(!dataConnection)
		{
			Log.e("Connection", "No Connection");
			myCursor = dbHelp.checkIfDirectoryEmpty();
			Cursor c = myCursor;

			if(c != null)
			{
				c.moveToFirst();
				if(c.getInt(0) == 0)
				{
					dbHelp.clearBuilding();
					dbHelp.clearDirectory();

					LoadingTablesOffline loadTask = new LoadingTablesOffline();
					loadTask.execute();
				}
				else
				{
					Handler handler = new Handler();
					Runnable action = new Runnable()
					{
						@Override
						public void run() 
						{
							Intent intent = new Intent(ColesActivity.this, main.class);

							startActivity(intent);
							dbHelp.close();
							finish();
						}

					};

					handler.postDelayed(action, waitTime);
				}
			}
		}
		else
		{
			myCursor = dbHelp.fetchTimeStamp();
			Cursor timeStampCursor = myCursor;

			if(timeStampCursor != null)
			{
				if(timeStampCursor.moveToFirst())
				{
					try
					{
						Log.e("Connection", "Connected");
						String localStamp = timeStampCursor.getString(timeStampCursor.getColumnIndexOrThrow(DBHelper.COLUMN_TIMESTAMP));
						String serverTimeStamp = getTimeStamp();
						Log.e("Server Stamp", serverTimeStamp);
						Log.e("Local Stamp", localStamp);

						if(localStamp.equalsIgnoreCase(serverTimeStamp))
						{
							Handler handler = new Handler();

							Runnable action = new Runnable()
							{
								@Override
								public void run() 
								{
									Intent intent = new Intent(ColesActivity.this, main.class);

									startActivity(intent);
									dbHelp.close();
									finish();
								}
							};

							handler.postDelayed(action, waitTime);

							Log.e("TimeStampCompare", "Server and Entry Match");
						}
						else if(!localStamp.equalsIgnoreCase(serverTimeStamp))
						{
							Log.e("Time Stamp Compare", "Local and Server Stamps Do NOT Match");

							dbHelp.clearBuilding();
							dbHelp.clearDirectory();
							dbHelp.updateTimeStamp(serverTimeStamp);
							
							try
							{
								LoadingTablesOnline loadTask = new LoadingTablesOnline();
								loadTask.execute();
							}
							catch(Exception ex)
							{
								Log.e("Exception", ex.getMessage() + "");
							}
						}
					}
					catch(Exception ex)
					{
						Log.e("Exception Connection", ex.getMessage() + "");
					}
				}
			}
		}

	}

	private boolean ConnectionPresent()
	{
		ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		if(networkInfo == null)
		{
			return false;
		}
		else
		{
			return networkInfo.isConnected();
		}
	}

	private String getTimeStamp() throws IOException
	{
		URL timestampURL;
		BufferedInputStream bufferedStream;
		BufferedReader reader;

		timestampURL = new URL("http://130.218.51.52/ws/get_timestamp.php");
		HttpURLConnection connection = (HttpURLConnection)timestampURL.openConnection();
		if(connection.getResponseCode() == HttpURLConnection.HTTP_OK)
		{
			bufferedStream = new BufferedInputStream(connection.getInputStream());
			reader = new BufferedReader(new InputStreamReader(bufferedStream));

			String stamp = "";
			String line;
			while((line = reader.readLine()) != null)
			{
				stamp = line;
			}
			reader.close();
			connection.disconnect();
			return stamp;
		}
		else
		{
			return "Time Stamp Unavailable";
		}
	}

	private class BulkInsertDirectoryOffline implements Runnable
	{

		public BulkInsertDirectoryOffline()
		{
		}

		@Override
		public void run() 
		{
			try
			{
				dbHelp.bulkInsertDirectory(getResources().openRawResource(R.raw.coles_directory));
			}
			catch(IOException error)
			{
				Log.e("Bulk Insert Issue", error.getMessage() + " ");
			}
		}

	}

	private class BulkInsertBuildingOffline implements Runnable
	{
		public BulkInsertBuildingOffline()
		{
		}

		@Override
		public void run() 
		{
			try
			{
				dbHelp.bulkInsertBuilding(getResources().openRawResource(R.raw.building_table));
			}
			catch(IOException error)
			{
				Log.e("Bulk Insert Issue", error.getMessage() + " ");
			}
		}
	}

	private class BulkInsertDirectoryOnline implements Runnable
	{
		private URL url;
		private String urlAddress;

		public BulkInsertDirectoryOnline()
		{
			urlAddress = "http://130.218.51.52/globalFile/coles_directory.csv";
		}

		@Override
		public void run() 
		{
			try
			{
				url = new URL(urlAddress);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				if(connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				{
					dbHelp.bulkInsertDirectory(getResources().openRawResource(R.raw.coles_directory));
				}
				else
				{
					dbHelp.bulkInsertDirectory(connection.getInputStream());
				}
				connection.disconnect();
			}
			catch(IOException error)
			{
				Log.e("Bulk Directory", error.getMessage() + " ");
			}

		}

	}

	private class BulkInsertBuildingOnline implements Runnable
	{
		private URL url;
		private String urlAddress;

		public BulkInsertBuildingOnline()
		{
			urlAddress = "http://130.218.51.52/globalFile/building_table.csv";
		}

		@Override
		public void run() 
		{
			try
			{
				url = new URL(urlAddress);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				if(connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				{
					dbHelp.bulkInsertBuilding(getResources().openRawResource(R.raw.building_table));
				}
				else
				{
					dbHelp.bulkInsertBuilding(connection.getInputStream());
				}
				connection.disconnect();
			}
			catch(IOException error)
			{
				Log.e("Bulk Directory", error.getMessage() + " ");
			}

		}

	}
	
	private class LoadingTablesOnline extends AsyncTask<Void, Void, Integer>
	{
		private ProgressDialog dialog;
		private long startTime;
		private long endTime;
		private long total;

		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			dialog = ProgressDialog.show(ColesActivity.this, "Loading", "Loading Please Wait");
		}

		@Override
		protected Integer doInBackground(Void... params) 
		{
			try
			{
				startTime = System.currentTimeMillis();

				Thread buildingThread = new Thread(new BulkInsertBuildingOnline());
				Thread directoryThread = new Thread(new BulkInsertDirectoryOnline());

				directoryThread.start();
				buildingThread.start();

				buildingThread.join();
				directoryThread.join();
			}
			catch(Exception ex)
			{
				Log.e("Async Exception", ex.getMessage());
			}

			return 0;
		}

		@Override
		protected void onPostExecute(Integer result)
		{

			super.onPostExecute(result);

			if(dialog.isShowing())
			{
				dialog.dismiss();
			}

			endTime = System.currentTimeMillis();
			total = endTime - startTime;

			Log.e("Delta Time:", total + " ms");

			Intent intent = new Intent(ColesActivity.this, main.class);
			dbHelp.close();
			startActivity(intent);
			finish();

		}
	}
	
	private class LoadingTablesOffline extends AsyncTask<Void, Void, Integer>
	{
		private ProgressDialog dialog;
		private long startTime;
		private long endTime;
		private long total;

		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			dialog = ProgressDialog.show(ColesActivity.this, "Loading", "Loading Please Wait");
		}

		@Override
		protected Integer doInBackground(Void... params) 
		{
			try
			{
				startTime = System.currentTimeMillis();

				Thread buildingThread = new Thread(new BulkInsertBuildingOffline());
				Thread directoryThread = new Thread(new BulkInsertDirectoryOffline());

				directoryThread.start();
				buildingThread.start();

				buildingThread.join();
				directoryThread.join();
			}
			catch(Exception ex)
			{
				Log.e("Async Exception", ex.getMessage());
			}

			return 0;
		}

		@Override
		protected void onPostExecute(Integer result)
		{

			super.onPostExecute(result);


			if(dialog.isShowing())
			{
				dialog.dismiss();
			}

			endTime = System.currentTimeMillis();
			total = endTime - startTime;

			Log.e("Delta Time:", total + " ms");

			Intent intent = new Intent(ColesActivity.this, main.class);
			dbHelp.close();
			startActivity(intent);
			finish();

		}
	}
}
