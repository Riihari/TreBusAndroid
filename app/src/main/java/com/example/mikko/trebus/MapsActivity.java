package com.example.mikko.trebus;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleMap.OnMyLocationClickListener,
        GoogleMap.OnMyLocationButtonClickListener {

    private GoogleMap mMap;
    private Hashtable<String, Marker> mMarkers = new Hashtable<String, Marker>();
    private Handler mHandler = new Handler();

    private static final int MARKER_WIDTH = 80;
    private static final int MARKER_HEIGHT = 80;
    private static final int MARKER_TEXT_SIZE = 35;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Tampere and move the camera
        LatLng tampere = new LatLng(61.49911, 23.78712);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tampere, 13));
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }

        mHandler.post(busFetcher);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 0) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "My location button click", Toast.LENGTH_SHORT).show();
        Log.d("Buses", "Bus infos in hashtable: " + mMarkers.size());

        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();

    }

    private void fetchBusInformation() {
        // TODO: Move to proper place
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://data.itsfactory.fi/siriaccess/vm/json";

        JsonObjectRequest request =
                new JsonObjectRequest(Request.Method.GET, url,null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
//                        Log.d("Buses", response.toString());
                        try {
                            Hashtable<String, BusInfo> newBusData = new Hashtable<String, BusInfo>();

                            JSONObject Siri = response.getJSONObject("Siri");
                            JSONObject ServiceDelivery = Siri.getJSONObject("ServiceDelivery");
                            JSONArray VehicleMonitoringDelivery = ServiceDelivery.getJSONArray("VehicleMonitoringDelivery");
                            Log.d("Buses", "VehicleMonitoringDelivery.length: " + VehicleMonitoringDelivery.length());
                            for(int i = 0; i < VehicleMonitoringDelivery.length(); i++) {
                                JSONObject VehicleMonitoringDeliveryItem = VehicleMonitoringDelivery.getJSONObject(i);
                                JSONArray VehicleActivity = VehicleMonitoringDeliveryItem.getJSONArray("VehicleActivity");
                                Log.d("Buses", "VehicleActivity.length: " + VehicleActivity.length());
                                for(int j = 0; j < VehicleActivity.length(); j++) {
                                    JSONObject VehicleActivityItem = VehicleActivity.getJSONObject(j);
                                    JSONObject MonitoredVehicleJourney = VehicleActivityItem.getJSONObject("MonitoredVehicleJourney");

                                    BusInfo busInfo = new BusInfo();
                                    busInfo.line = MonitoredVehicleJourney.getJSONObject("LineRef").getString("value");
                                    busInfo.origin = MonitoredVehicleJourney.getJSONObject("OriginName").getString("value");
                                    busInfo.destination = MonitoredVehicleJourney.getJSONObject("DestinationName").getString("value");
                                    JSONObject VehicleLocation = MonitoredVehicleJourney.getJSONObject("VehicleLocation");
                                    busInfo.longitude = VehicleLocation.getDouble("Longitude");
                                    busInfo.latitude = VehicleLocation.getDouble("Latitude");
                                    String VehicleRef = MonitoredVehicleJourney.getJSONObject("VehicleRef").getString("value");
//                                    Log.d("Buses", busInfo.line + ": " + busInfo.origin + " - " + busInfo.destination + " Lat: " + busInfo.latitude + " Lon: " + busInfo.longitude);

                                    newBusData.put(VehicleRef, busInfo);
                                }
                            }

                            Set<String> newKeys = newBusData.keySet();
                            for(String key: newKeys) {
                                BusInfo busInfo = newBusData.get(key);
                                LatLng location = new LatLng(busInfo.latitude, busInfo.longitude);
                                if(mMarkers.containsKey(key)) {
                                    Marker marker = mMarkers.get(key);
                                    marker.setPosition(location);
                                } else {
                                    Bitmap busMarker = createBusMarker(busInfo.line);
                                    mMarkers.put(
                                            key,
                                            mMap.addMarker(new MarkerOptions()
                                                    .position(location)
                                                    .title(busInfo.line)
                                                    .icon(BitmapDescriptorFactory.fromBitmap(busMarker))
                                                    .anchor(0.5f, 0.5f)));
                                }
                            }

                            Set<String> oldKeys = mMarkers.keySet();
                            ArrayList<String> removedKeys = new ArrayList<String>();
                            int i = 0;
                            Log.d("Buses","newDataSize: " + newBusData.size());
                            for(String key: oldKeys) {
                                i++;
                                if(newBusData.containsKey(key) == false) {
                                    Marker marker = mMarkers.get(key);
                                    marker.remove();
                                    removedKeys.add(key);
                                }
                            }
                            Log.d("Buses", "Key amoutn: " + i);
                            Log.d("Buses", "marker count: " + mMarkers.size());
                            Iterator<String> iter = removedKeys.iterator();
                            while(iter.hasNext()) {
                                String key = iter.next();
                                mMarkers.remove(key);
                            }
                            Log.d("Buses", "marker count after remove: " + mMarkers.size());

                        } catch (JSONException e) {
                            Log.d("Buses", "Error parsing Siri" + e.toString());
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Buses", "Error receiving bus locations: " + error.getMessage());
                    }
                });

        queue.add(request);
    }

    private Runnable busFetcher = new Runnable() {
        @Override
        public void run() {
            fetchBusInformation();

            mHandler.postDelayed(this, 5000);
        }
    };

    public Bitmap createBusMarker(String line) {
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(MARKER_WIDTH, MARKER_HEIGHT, conf);
        Canvas canvas1 = new Canvas(bmp);

        // paint defines the text color, stroke width and size
        Paint textPaint = new Paint();
        textPaint.setTextSize(MARKER_TEXT_SIZE);
        textPaint.setColor(Color.WHITE);
        textPaint.setFakeBoldText(true);
        Rect textBounds = new Rect();
        textPaint.getTextBounds(line, 0, line.length(), textBounds);


        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setColor(Color.BLUE);

        // modify canvas
        canvas1.drawCircle(MARKER_WIDTH / 2, MARKER_HEIGHT / 2, MARKER_HEIGHT / 2, circlePaint);
        canvas1.drawText(line, (MARKER_WIDTH / 2) - (textBounds.width() / 2), (MARKER_HEIGHT / 2) + (textBounds.height() / 2), textPaint);

        return bmp;
    }
}
