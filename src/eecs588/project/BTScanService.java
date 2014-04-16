package eecs588.project;


import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class BTScanService extends IntentService {

	private BluetoothAdapter btAdap; 
	private boolean mScanning; 
	private Handler mHandler; 
	private Location location; 
	
	// stop scanning after 20 seconds 
	private static final long SCAN_PERIOD = 20000; 	
	
	public BTScanService() {
		super("BTScanService");
        mHandler = new Handler(); 
	}

	// this service will scan bluetooth every 5 minutes, capture the list of devices, and ship 'em off to the server
	// we don't need to deal with broadcast receivers cuz we don't need to report to the main activity
	// once we do our scan and post, the intent just stops itself
	
	@Override
	protected void onHandleIntent(Intent intent) {
		final BluetoothManager btMan = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE); 
        btAdap = btMan.getAdapter(); 
        location = (Location) intent.getExtras().get("location"); 
        Log.e("intent location", location.toString()); 
        scanDevices(true);	
     
	}
	
	protected void scanDevices(final boolean enable) {
		if (enable) {
			// stops scanning after the predefined period
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false; 
					btAdap.stopLeScan(bleScanCallback);
				}
			}, SCAN_PERIOD); 
			
			mScanning = true; 
			Log.e("scanDevices", "about to call startLeScan"); 
			btAdap.startLeScan(bleScanCallback); 
		} 
		else {
			mScanning = false;
			btAdap.stopLeScan(bleScanCallback);
		}
	}

	protected BluetoothAdapter.LeScanCallback bleScanCallback = 
		new BluetoothAdapter.LeScanCallback() {
			@Override
			public void onLeScan(BluetoothDevice btDevice, int rssi, byte[] scanRecord) {
				String passCode = "test_site"; 
				String macAddr = btDevice.getAddress(); 
				final String macAddress = macAddr; 
				String device = "android";
				Long ts = System.currentTimeMillis()/1000; 
				String timestamp = ts.toString(); 
				String latitude = Float.toString((float) location.getLatitude());
				String longitude = Float.toString((float) location.getLongitude());
				
				HttpResponse response;
			
				Log.e("leScanCallBack", "inside the scan callback"); 
				
				try {
				
					HttpParams httpParams = new BasicHttpParams(); 
					HttpProtocolParams.setContentCharset(httpParams, "UTF-8");
					
					SSLSocketFactory sslsf = SSLSocketFactory.getSocketFactory();
					sslsf.setHostnameVerifier(new X509HostnameVerifier() {							
						@Override
						public void verify(String host, String[] cns, String[] subjectAlts)
								throws SSLException {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void verify(String host, X509Certificate cert) throws SSLException {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void verify(String host, SSLSocket ssl) throws IOException {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public boolean verify(String host, SSLSession session) {
							if(host.contains("schultetwins")) {
								return true;
							}
							return false;
						}
					});
							
					SchemeRegistry registry = new SchemeRegistry();
					registry.register(new Scheme("https", sslsf, 443)); 
					ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(httpParams, registry);
					
					HttpClient httpClient = new DefaultHttpClient(manager, httpParams); 
					HttpPost httpPost = new HttpPost("https://track-dev.schultetwins.com/api/v1.0/spot"); 					
					
					List<NameValuePair> params = new ArrayList<NameValuePair>(11); 
					params.add(new BasicNameValuePair("timestamp", timestamp));
					params.add(new BasicNameValuePair("MAC", macAddr)); 
					params.add(new BasicNameValuePair("rand_mac", "1"));
					params.add(new BasicNameValuePair("name", "Sai")); 
					params.add(new BasicNameValuePair("RSSI", Integer.toString(rssi))); 
					params.add(new BasicNameValuePair("latitude", latitude)); 
					params.add(new BasicNameValuePair("longitude", longitude)); 
					params.add(new BasicNameValuePair("device", device));
					//params.add(new BasicNameValuePair("fitbitid", macAddr)); 
					//params.add(new BasicNameValuePair("access_addr", macAddr)); 
					//params.add(new BasicNameValuePair("serialnum", serialNum); 
					params.add(new BasicNameValuePair("passcode", passCode)); 
					
					httpPost.setEntity(new UrlEncodedFormEntity(params));
					//Log.e("encoded url", httpPost.get
					
					// only execute HTTP post if the BLE device has a static address
					if (macAddr.startsWith("C") || macAddr.startsWith("D") || macAddr.startsWith("E") ||
							macAddr.startsWith("F")) {
						
						response = httpClient.execute(httpPost); 
					}
					
					Log.e("RESPONSE", Integer.toString(response.getStatusLine().getStatusCode()));
					
					// CHECK RESPONSE						
					if (response.getStatusLine().getStatusCode() != 200) {
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								Toast.makeText(getApplicationContext(), "Something went wrong while checking response!", Toast.LENGTH_SHORT).show();					
							}
						});
					} 
					else {
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								Toast.makeText(getApplicationContext(), "Successfully sent details for " + macAddress, Toast.LENGTH_SHORT).show();					
							}
						});
					}
				} 
				catch (ClientProtocolException ex) {						
					// process the error 
					Log.e("POST error", "client protocol ex", ex); 
				}
				catch (IOException ex) {						
					// process the error 	
					Log.e("POST error", "IOexception", ex); 
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							Toast.makeText(getApplicationContext(), "Something went wrong while POSTing!", Toast.LENGTH_SHORT).show();					
						}
				});
				
			}
		}
	};

}


/* Websites that I used for reference: 
 * Using HttpsURLConnection: http://stackoverflow.com/questions/9767952/how-to-add-parameters-to-httpurlconnection-using-post
 * SSLContext: http://stackoverflow.com/questions/16504527/how-to-do-an-https-post-from-android
 */
