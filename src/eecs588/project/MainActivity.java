package eecs588.project;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class MainActivity extends Activity implements LocationListener {

	static final int REQUEST_ENABLE_BT = 1;  
	static final int REQUEST_ENABLE_GPS = 2;  
	static final int BT_REST_PERIOD = 60000; //one minute
	
	private BluetoothAdapter btAdap; 
	private String provider; 
	private Location location; 
	private Handler mHandler; 
	private ScheduledExecutorService scheduleTaskExecutor;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        
        final BluetoothManager btMan = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE); 
		final LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE); 

        btAdap = btMan.getAdapter(); 
        mHandler = new Handler();
        
        // make sure BT is enabled
        if (btAdap == null || !btAdap.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        
        // at this point, bluetooth should be turned on
        
        // make sure GPS is enabled
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE); 
        boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER); 
        
        if (!enabled) {
        	Intent enableGPSIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        	startActivityForResult(enableGPSIntent, REQUEST_ENABLE_GPS);
        }
        
        // at this point, GPS should be turned on
        
             
        // request location updates with 
   			// minimum time between location updates = every minute
       		// and minimum distance between updates = 100 meters (~320 ft)
        Criteria criteria = new Criteria(); 
        provider = locMan.getBestProvider(criteria, false); 
        location = locMan.getLastKnownLocation(provider); 
        
        Intent btIntent = new Intent(getBaseContext(), BTScanService.class); 
		Log.e("main activity", "about to start bt service"); 
		btIntent.putExtra("location", location); 
	    startService(btIntent); 
        
		Runnable repeatBTScans = new Runnable() {
			@Override
			public void run() {
				Intent btIntent = new Intent(getBaseContext(), BTScanService.class); 
				Log.e("main activity", "about to start bt service"); 
					
				btIntent.putExtra("location", location); 
			    startService(btIntent); 
			}
        };
        
	    scheduleTaskExecutor = Executors.newScheduledThreadPool(1); 
	    scheduleTaskExecutor.scheduleAtFixedRate(repeatBTScans, 0, 60, TimeUnit.SECONDS); 
	    
        locMan.requestLocationUpdates(provider, 30000, 0, this);

    }


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode != RESULT_OK) {
				Toast.makeText(getApplicationContext(), "Please turn on Bluetooth before opening this app!", 
						Toast.LENGTH_SHORT).show();
			}
		}

	}


	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

	@Override
	public void onLocationChanged(Location loc) {
		location = loc; 
		Toast.makeText(getApplicationContext(), "Got new location!", Toast.LENGTH_SHORT).show(); 
	}


	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}

}
