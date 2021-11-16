package edu.ucsb.ece150.locationplus;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback {

    //Geofence Variables
    private Geofence mGeofence;
    private GeofencingClient mGeofencingClient;
    private PendingIntent mPendingIntent = null;

    //Map Variables
    private GnssStatus.Callback mGnssStatusCallback;
    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private double mLat;
    private double mLong;
    private boolean mLocked;

    //Satellite Variables
    private ListView satList;
    private TextView satCount;
    private ArrayList<Satellite> mSatellites;
    private ArrayAdapter adapter;
    private boolean mHide;
    private int inFix;

    //Toolbar variables
    private Toolbar mToolbar;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Set up Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Set up Geofencing Client
        mGeofencingClient = LocationServices.getGeofencingClient(MapsActivity.this);

        // Set up Satellite List
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        satCount = findViewById(R.id.satCount);
        mGnssStatusCallback = new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(GnssStatus status) {
                // [TODO] Implement behavior when the satellite status is updated
                mSatellites.clear();
                int countSat = status.getSatelliteCount();
                inFix = 0;
                // Do only when there's more than 1 satellite
                if (countSat != 0){
                    for (int i = 0; i < countSat; i++){
                        Satellite satellite = new Satellite(status.getAzimuthDegrees(i), status.getElevationDegrees(i),
                                status.getCn0DbHz(i), status.getConstellationType(i), status.getSvid(i), status.usedInFix(i));
                        mSatellites.add(satellite);
                        if (status.usedInFix(i)){
                            inFix++;
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
                //Show count and show in Fix
                String text = "Total: " + mSatellites.size() + "\n" +
                        "In Fix: " + inFix;
                satCount.setText(text);
                satCount.setTextColor(Color.WHITE);
            }
        };

        //Satellite Array list creation
        // [TODO] Additional setup for viewing satellite information (lists, adapters, etc.)
        mSatellites = new ArrayList<Satellite>();
        satList = findViewById(R.id.satellite_list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mSatellites);
        satList.setAdapter(adapter);
        satList.setVisibility(View.GONE);
        satCount.setVisibility(View.GONE);

        satList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Creates dialog box with info
                Satellite satellite = mSatellites.get(position);
                AlertDialog builder = new AlertDialog.Builder(MapsActivity.this)
                        .setTitle("Satellite " + satellite.mSVid  +" Info")
                        .setMessage("Azimuth: " + satellite.mAzimuth + "\n" +
                                "Elevation: " + satellite.mElevation + "\n\n" +
                                "C/N0: " + satellite.mCND + "dB Hz \n\n" +
                                "Constellation: " + satellite.mConstellation + "\n" +
                                "SVID: " + satellite.mSVid + "\n")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
            }
        });

        mHide = true;
        //Set up satellite button
        ImageView satView = findViewById(R.id.satellite);
        satView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Satellite view --> hides map
                if (!mHide){
                    satList.setVisibility(View.VISIBLE);
                    satCount.setVisibility(View.VISIBLE);
                    mapFragment.getView().setVisibility(View.GONE);
                }else{
                    satList.setVisibility(View.GONE);
                    satCount.setVisibility(View.GONE);
                    mapFragment.getView().setVisibility(View.VISIBLE);
                }
                mHide = !mHide;
            }
        });

        //Set up locked button
        mLocked = false;
        ImageView locked = findViewById(R.id.currentLocation);
        locked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Lock/unlock camera to Location
                mLocked = !mLocked;
                // Make button green or something
            }
        });

        // Set up Toolbar
        mToolbar = (Toolbar) findViewById(R.id.appToolbar);
        setSupportActionBar(mToolbar);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // [TODO] Implement behavior when Google Maps is ready

        // [TODO] In addition, add a listener for long clicks (which is the starting point for
        // creating a Geofence for the destination and listening for transitions that indicate
        // arrival) -- create alert dialog

//        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener(){
//            @Override
//            public void onMapLongClick(LatLng latLng){
//                //Create a geofence marker
//                MarkerOptions geofenceMarker = new MarkerOptions();
//                geofenceMarker.position(latLng);
//                geofenceMarker.title("Geofence @ " + latLng.latitude + " : "+ latLng.longitude);er
//                mMap.addMarker(geofenceMarker);
//            }
//        });
    }

    @Override
    public void onLocationChanged(Location location) {
        // [TODO] Implement behavior when a location update is received
        mLat= location.getLatitude();
        mLong = location.getLongitude();
        LatLng latlng = new LatLng(mLat, mLong);

        Log.d("LOCATIONALLY", "Latitude: " + mLat + "");
        Log.d("LOCATIONALLY", "Longitude: " + mLong + "");

        MarkerOptions currentLoc = new MarkerOptions()
                .position(new LatLng(mLat, mLong))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        mMap.clear();
        mMap.addMarker(currentLoc);

        //Zoom into current location at the start
        if (mLocked){
            Log.d("LOCKED", "Status: Locked");
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(latlng,15)));
            mMap.getUiSettings().setAllGesturesEnabled(false);
        }
        else{
            Log.d("LOCKED", "Status: Unlocked");
            mMap.getUiSettings().setAllGesturesEnabled(true);
        }

        //Satellite Updates

    }

    /*
     * The following three methods onProviderDisabled(), onProviderEnabled(), and onStatusChanged()
     * do not need to be implemented -- they must be here because this Activity implements
     * LocationListener.
     *
     * You may use them if you need to.
     */
    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    private GeofencingRequest getGeofenceRequest() {
        // [TODO] Set the initial trigger (i.e. what should be triggered if the user is already
        // inside the Geofence when it is created)

        return new GeofencingRequest.Builder()
                //.setInitialTrigger()  <--  Add triggers here
                .addGeofence(mGeofence)
                .build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if(mPendingIntent != null)
            return mPendingIntent;

        Intent intent = new Intent(MapsActivity.this, GeofenceBroadcastReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(MapsActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mPendingIntent;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStart() throws SecurityException {
        super.onStart();

        // [TODO] Ensure that necessary permissions are granted
        int PERMISSION_ALL = 69;
        // Permissions Needed
        String[] PERMISSIONS = {
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET
        };

        // Asks for all Permissions
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        //Grabbing location updates w/ satellite information
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        mLocationManager.registerGnssStatusCallback(mGnssStatusCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // [TODO] Data recovery
    }

    @Override
    protected void onPause() {
        super.onPause();

        // [TODO] Data saving
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStop() {
        super.onStop();

        mLocationManager.removeUpdates(this);
        mLocationManager.unregisterGnssStatusCallback(mGnssStatusCallback);
    }

    // Helper function to check for multiple permissions
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}
