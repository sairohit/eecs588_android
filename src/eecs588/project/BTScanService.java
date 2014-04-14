package eecs588.project;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

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
import android.util.Log;
import android.widget.Toast;

public class BTScanService extends IntentService {

	private BluetoothAdapter btAdap; 
	private boolean mScanning; 
	private Handler mHandler; 
	
	// stop scanning after 20 seconds 
	private static final long SCAN_PERIOD = 5000; 	// **************** CHANGE THIS AMOUNT *******************
	
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
       
        //scanDevices(true);		
        
        
        // ******** TESTING TO SEE WHETHER THE POST REQUEST WORKS ************************
        try {
			URL url = new URL("https://track-dev.schultetwins.com/api/v1.0/spot");
			
			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection(); 
			SSLContext sc = SSLContext.getInstance("TLS"); 
			sc.init(null, null, new java.security.SecureRandom());
			conn.setSSLSocketFactory(sc.getSocketFactory());
			
			conn.setHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					if(hostname.contains("schultetwins")) {
						return true;
					}
					return false;
				}
			});
			
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(10000);
			conn.setRequestMethod(HttpPost.METHOD_NAME);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			
			Long ts = System.currentTimeMillis()/1000; 
			String timestamp = ts.toString();
			
			List<NameValuePair> params = new ArrayList<NameValuePair>(11); 
			params.add(new BasicNameValuePair("timestamp", timestamp));
			params.add(new BasicNameValuePair("MAC", "00:11:22:33:44:55")); 
			params.add(new BasicNameValuePair("rand_mac", "1"));
			params.add(new BasicNameValuePair("name", "Sai")); 
			params.add(new BasicNameValuePair("RSSI", Integer.toString(80))); 
			params.add(new BasicNameValuePair("latitude", "123")); 
			params.add(new BasicNameValuePair("longitude", "456")); 
			params.add(new BasicNameValuePair("device", "android"));
			//params.add(new BasicNameValuePair("fitbitid", "00:11:22:33:44:55")); 
			//params.add(new BasicNameValuePair("access_addr", "00:11:22:33:44:55")); 
			//params.add(new BasicNameValuePair("serialnum", "123456"); 
			params.add(new BasicNameValuePair("passcode", "test_case")); 
			
			OutputStream outStream = conn.getOutputStream(); 
			BufferedWriter bufWriter = new BufferedWriter(new OutputStreamWriter(outStream, "UTF-8")); 
			
			StringBuilder urlParam = new StringBuilder(); 
			boolean first = true;
			for(NameValuePair pair: params) {
				if(first) {
					first = false;
				}
				else {
					urlParam.append("&"); 
				}
				
				urlParam.append(URLEncoder.encode(pair.getName(), "UTF-8")); 
				urlParam.append("="); 
				urlParam.append(URLEncoder.encode(pair.getValue(), "UTF-8")); 
			}
			
			bufWriter.write(urlParam.toString());
			bufWriter.flush();
			bufWriter.close();
			outStream.close();
			
			Log.e("URLURLURL", urlParam.toString()); 
			/* 
			 * timestamp=1397456431&MAC=00%3A11%3A22%3A33%3A44%3A55&rand_mac=1&name=Sai&RSSI=80&latitude=123&longitude=456&device=android&passcode=test_case
			 */
						
			conn.connect();	
	
			int responseCode = conn.getResponseCode();
			if(responseCode != 200) {
				//something went wrong
				Log.e("Bad response", Integer.toString(responseCode)); 
			}
			
        
        } catch (MalformedURLException ex) {
			Log.e("Error in URL", "malformed", ex); 
		} catch (IOException ex) {
			Log.e("Error in URL", "IOException", ex); 
		} catch (NoSuchAlgorithmException ex) {
			Log.e("Error in URL", "NoSuchAlgorithmEx", ex); 
		} catch (KeyManagementException ex) {
			Log.e("Error in URL", "KeyManagementException", ex); 
		} 		
		
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
					String passCode = "test_case"; 
					String macAddr = btDevice.getAddress(); 
					String device = "android";
					Long ts = System.currentTimeMillis()/1000; 
					String timestamp = ts.toString(); 
				
					Log.e("leScanCallBack", "inside the scan callback"); 
					
					HttpClient httpClient = new DefaultHttpClient(); 
					HttpPost httpPost = new HttpPost("https://track-dev.schultetwins.com/api/v1.0/spot"); 
					
					try {
						List<NameValuePair> params = new ArrayList<NameValuePair>(11); 
						params.add(new BasicNameValuePair("timestamp", timestamp));
						params.add(new BasicNameValuePair("MAC", macAddr)); 
						params.add(new BasicNameValuePair("rand_mac", "1"));
						params.add(new BasicNameValuePair("name", "sai")); 
						params.add(new BasicNameValuePair("RSSI", Integer.toString(rssi))); 
						params.add(new BasicNameValuePair("latitude", "123")); 
						params.add(new BasicNameValuePair("longitude", "456")); 
						params.add(new BasicNameValuePair("device", device));
						params.add(new BasicNameValuePair("fitbitid", macAddr)); 
						params.add(new BasicNameValuePair("access_addr", "accessAddress")); 
						params.add(new BasicNameValuePair("passcode", passCode)); 
						
						httpPost.setEntity(new UrlEncodedFormEntity(params));
						
						// execute HTTP post
						HttpResponse response = httpClient.execute(httpPost); 
						Log.e("RESPONSE", Integer.toString(response.getStatusLine().getStatusCode())); 
						
						// CHECK RESPONSE						
						if (response.getStatusLine().getStatusCode() != 200) {
							Log.e("checking response", Integer.toString(response.getStatusLine().getStatusCode())); 
							mHandler.post(new Runnable() {
								@Override
								public void run() {
									// TODO Auto-generated method stub
									Toast.makeText(getApplicationContext(), "Something went wrong while checking response!", Toast.LENGTH_SHORT).show();					
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
