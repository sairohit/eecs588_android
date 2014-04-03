package eecs588.project;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

public class BTScanService extends IntentService {

	private BluetoothAdapter btAdap; 
	private boolean mScanning; 
	private Handler mHandler; 
	
	// stop scanning after 20 seconds 
	private static final long SCAN_PERIOD = 20000; 
	
	public BTScanService() {
		super("BTScanService");
	}

	// this service will scan bluetooth every 5 minutes, capture the list of devices, and ship 'em off to the server
	// we don't need to deal with broadcast receivers cuz we don't need to report to the main activity
	// once we do our scan and post, the intent just stops itself
	
	@Override
	protected void onHandleIntent(Intent intent) {
		final BluetoothManager btMan = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE); 
        btAdap = btMan.getAdapter(); 
		
        scanDevices(true);
		
	}
	
	protected void scanDevices(final boolean enable) {
		if (enable) {
			//stops scanning after the predefined period
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false; 
					btAdap.stopLeScan(bleScanCallback);
				}
			}, SCAN_PERIOD); 
			
			mScanning = true; 
			btAdap.startLeScan(bleScanCallback); 
		} 
		else {
			mScanning = false;
			btAdap.stopLeScan(bleScanCallback);
		}
	}

	private BluetoothAdapter.LeScanCallback bleScanCallback = 
			new BluetoothAdapter.LeScanCallback() {
				@Override
				public void onLeScan(BluetoothDevice btDevice, int rssi, byte[] scanRecord) {
					String macAddr = btDevice.getAddress(); 
					String device = "android"; 
					//String passcode = "eecs588isalright"; 
					String passcode = "test_site"; 
					Long ts = System.currentTimeMillis()/1000; 
					String timestamp = ts.toString(); 
				
					HttpClient httpClient = new DefaultHttpClient(); 
					HttpPost httpPost = new HttpPost("https://track-dev.schultetwins.com/api/v1.0/spot"); 
					
					try {
						List<NameValuePair> params = new ArrayList<NameValuePair>(7); 
						params.add(new BasicNameValuePair("MAC", macAddr)); 
						params.add(new BasicNameValuePair("RSSI", Integer.toString(rssi))); 
						params.add(new BasicNameValuePair("timestamp", timestamp));
						params.add(new BasicNameValuePair("device", device));
						params.add(new BasicNameValuePair("passcode", passcode));
						params.add(new BasicNameValuePair("latitude", "LATITUDE")); 
						params.add(new BasicNameValuePair("longitude", "LONGITUDE")); 
						httpPost.setEntity(new UrlEncodedFormEntity(params));
						
						//execute HTTP post
						HttpResponse response = httpClient.execute(httpPost); 
					} 
					catch (ClientProtocolException ex) {						
						//process the error 
					}
					catch (IOException ex) {						
						//process the error 					
					}
									
				}
			};
}
