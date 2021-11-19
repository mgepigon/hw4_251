package edu.ucsb.ece150.locationplus;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import com.google.android.gms.common.api.PendingResult;
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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback {

    //Shared Preferences
    private SharedPreferences myPreferences;

    //Geofence Variables
    private boolean existingGeofence;
    private Geofence mGeofence;
    private GeofencingClient mGeofencingClient;
    private PendingIntent mPendingIntent = null;
    private MarkerOptions destLocOpt;
    private FloatingActionButton cancel;
    private Circle geofenceRadius;
    private boolean deleteGeofence;
    private double geoLat;
    private double geoLong;

    //Map Variables
    private GnssStatus.Callback mGnssStatusCallback;
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
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
    private Marker currentLoc;
    private MarkerOptions currentLocOpt;

    //Toolbar variables
    private Toolbar mToolbar;

    //Misc
    private ImageView locked;
    private ImageView satView;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Log.d("MAP_READY", "here.create");

        //Delete Geofence if Entered
        Intent intent = getIntent();
        deleteGeofence = intent.getBooleanExtra("Geofence", false);
        // Set up Google Maps
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Shared Preferences
        myPreferences = getApplicationContext().getSharedPreferences("appPref", Context.MODE_PRIVATE);

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
                if (countSat != 0) {
                    for (int i = 0; i < countSat; i++) {
                        Satellite satellite = new Satellite(status.getAzimuthDegrees(i), status.getElevationDegrees(i),
                                status.getCn0DbHz(i), status.getConstellationType(i), status.getSvid(i), status.usedInFix(i));
                        mSatellites.add(satellite);
                        if (status.usedInFix(i)) {
                            inFix++;
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
                //Show count and show in Fix
                String text = "Total: " + mSatellites.size() +
                        "\nIn Fix: " + inFix;
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
                        .setTitle("Satellite " + satellite.mSVid + " Info")
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

        mHide = myPreferences.getBoolean("mHide", true);
        //Set up satellite button & lock
        satView = findViewById(R.id.satellite);
        locked = findViewById(R.id.currentLocation);
        satView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHide = !mHide;
                Log.d("hide", "Clicked!" + mHide);
                showList();
            }
        });

        //Set up locked button
        mLocked = myPreferences.getBoolean("mLocked", true);
        locked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLocked = !mLocked;
                lockCam();
                Log.d("lock", "Clicked!" + mLocked);
            }
        });

        // Set up Toolbar
        mToolbar = (Toolbar) findViewById(R.id.appToolbar);
        setSupportActionBar(mToolbar);

        // Set up Misc
        cancel = findViewById(R.id.cancelGeofence);
        cancel.setVisibility(View.GONE);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog builder = new AlertDialog.Builder(MapsActivity.this)
                        .setMessage("Delete this destination?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //Take out what's on the map
                                removeGeofence();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Close Window -- Do nothing
                            }
                        }).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.d("MAP_READY", "here.ready");

        //Geofence Removal after arriving
        if (deleteGeofence){
            removeGeofence();
        }
        else {
            currentLocation();
        }
        // [TODO] Implement behavior when Google Maps is ready -- make marker, move location

        //If Geofence exists, make note of it
        existingGeofence = myPreferences.getBoolean("existingGeofence", false);

        // Do nothing on a marker click
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            public boolean onMarkerClick(Marker marker) {
                return true;
            }
        });

        // [TODO] In addition, add a listener for long clicks (which is the starting point for
        // creating a Geofence for the destination and listening for transitions that indicate
        // arrival) -- create alert dialog

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                //If there is no marker on the map
                if (!existingGeofence) {
                    AlertDialog builder = new AlertDialog.Builder(MapsActivity.this)
                            .setTitle("Confirm Destination")
                            .setMessage("Set (" + latLng.longitude + "," + latLng.latitude + ") as your destination?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    //Store destination coordinates
                                    SharedPreferences.Editor editor = myPreferences.edit();
                                    editor.putFloat("geoLat", (float) latLng.latitude);
                                    editor.putFloat("geoLong", (float) latLng.longitude);
                                    editor.putBoolean("existingGeofence", true);
                                    editor.apply();
                                    //Make Geofence
                                    makeGeofence(latLng);
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // Close Window -- Do nothing
                                }
                            }).show();
                }
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        // [TODO] Implement behavior when a location update is received
        //Log.d("MAP_READY", "here.location");
        //Log.d("Geofence", "exists? " + existingGeofence);
        mLat =  location.getLatitude();
        mLong = location.getLongitude();
        LatLng latlng = new LatLng(mLat, mLong);

        //Just move existing marker position per location found
        currentLoc.setPosition(latlng);

        //Update Map Camera
        lockCam();
        //Update Geofence existence
        existingGeofence = myPreferences.getBoolean("existingGeofence", false);

        //Store coordinates
        SharedPreferences.Editor editor = myPreferences.edit();
        editor.putFloat("mLat", (float) mLat);
        editor.putFloat("mLong", (float) mLong);
        editor.apply();
    }

    public void currentLocation(){
        currentLocOpt = new MarkerOptions()
                .position(new LatLng(mLat, mLong))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        currentLoc = mMap.addMarker(currentLocOpt);
    }

    public void showList(){
        // Show/Hide satellite list
        if (mHide) {
            if (existingGeofence){
                cancel.setVisibility(View.VISIBLE);
            }
            satList.setVisibility(View.GONE);
            satCount.setVisibility(View.GONE);
            locked.setVisibility(View.VISIBLE);
            mapFragment.getView().setVisibility(View.VISIBLE);
        } else {
            cancel.setVisibility(View.GONE);
            satList.setVisibility(View.VISIBLE);
            satCount.setVisibility(View.VISIBLE);
            locked.setVisibility(View.INVISIBLE);
            mapFragment.getView().setVisibility(View.GONE);
        }
    }

    public void lockCam(){
        //Lock / Unlock Camera
        if (mLocked) {
            locked.setColorFilter(Color.GREEN);
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(new LatLng (mLat, mLong),18)));
            mMap.getUiSettings().setAllGesturesEnabled(false);
        } else {
            locked.setColorFilter(Color.TRANSPARENT);
            mMap.getUiSettings().setAllGesturesEnabled(true);
        }
    }

    @SuppressLint("MissingPermission")
    public void makeGeofence(LatLng latLng){
        //If Geofence exists, make it
        existingGeofence = myPreferences.getBoolean("existingGeofence", false);
        if (existingGeofence){
            int geo_rad = 100;
            //Create Geofence Object
            mGeofence = new Geofence.Builder()
                    .setRequestId("destination")
                    .setCircularRegion(latLng.latitude, latLng.longitude, geo_rad)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build();
            //Add Geofence to the map
            mGeofencingClient.addGeofences(getGeofenceRequest(), getGeofencePendingIntent())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            //Add Geofence Radius to Map
                            CircleOptions radOption = new CircleOptions()
                                    .center(latLng)
                                    .strokeColor(Color.argb(50, 70, 70, 70))
                                    .fillColor(Color.argb(100, 150, 150, 150))
                                    .radius(geo_rad);
                            geofenceRadius = mMap.addCircle(radOption);
                            //Create a Geofence marker
                            destLocOpt = new MarkerOptions()
                                    .position(latLng)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
                            mMap.addMarker(destLocOpt);
                            //Create cancel button
                            cancel.setVisibility(View.VISIBLE);
                            //Toast.makeText(MapsActivity.this, "Geofence Created", Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    public void removeGeofence(){
        //Remove map marker & circle
        mMap.clear();
        currentLocation();
        cancel.setVisibility(View.GONE);
        //Remove Geofence
        mGeofencingClient.removeGeofences(getGeofencePendingIntent());
        //Tell system that no Geofence exists
        myPreferences.edit().remove("existingGeofence").apply();
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
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
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
        Log.d("MAP_READY", "here.resume");
        // Normal Operations -- Data Recovery

        showList();
        //Store Current Coordinates
        mLat = myPreferences.getFloat("mLat", 0);
        mLong = myPreferences.getFloat("mLong", 0);
        //Store Destination Coordinates
        geoLat = myPreferences.getFloat("geoLat", 0);
        geoLong = myPreferences.getFloat("geoLong", 0);
        LatLng latLng = new LatLng(geoLat, geoLong);
        //Create Geofence if needed
        makeGeofence(latLng);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // [TODO] Data saving
        Log.d("MAP_READY", "here.pause");
        myPreferences = getApplicationContext().getSharedPreferences("appPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = myPreferences.edit();

        //Store Information
        editor.putBoolean("mLocked", mLocked);
        editor.putBoolean("mHide", mHide);
        //longitude & latitude
        Log.d("Coords Stored:" , "Lat: " + mLat + "Long: " + mLong);
        Log.d("Destination Stored:" , "Lat: " + geoLat + "Long: " + geoLong);
        editor.apply();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStop() {
        super.onStop();
        Log.d("MAP_READY", "here.stop");
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
