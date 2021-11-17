package edu.ucsb.ece150.locationplus;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceTransitionJobIntentService extends JobIntentService {

    private NotificationChannel mNotificationChannel;
    private NotificationManager mNotificationManager;
    private NotificationManagerCompat mNotificationManagerCompat;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, GeofenceTransitionJobIntentService.class, 0, intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onHandleWork(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if(geofencingEvent.hasError()) {
            Log.e("Geofence", GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode()));
            return;
        }

        // [TODO] This is where you will handle detected Geofence transitions. If the user has
        // arrived at their destination (is within the Geofence), then
        // 1. Create a notification and display it
        // 2. Go back to the main activity (via Intent) to handle cleanup (Geofence removal, etc.)
        notifyUser(getApplicationContext());

        Intent deleteGeofence = new Intent(this, MapsActivity.class);
        deleteGeofence.putExtra("Geofence", true);
        startActivity(deleteGeofence);
    }

    public void notifyUser(Context context){
        if (Build.VERSION.SDK_INT < 26) {
            Log.d("GeoTransition", "Notification Failed: SDK < 26");
            return;
        }
        Log.d("GeoTransition", "Notification Sent");
        //Create notification manager
        mNotificationManager =(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //Create notification channel
        mNotificationChannel = new NotificationChannel("default",
                "Geofence_Enter",
                NotificationManager.IMPORTANCE_DEFAULT);

        mNotificationChannel.setDescription("Geofence_Transition");
        mNotificationManager.createNotificationChannel(mNotificationChannel);

        //Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "default")
                .setSmallIcon(R.drawable.arrived)
                .setContentTitle("Arrived at Destination")
                .setContentText("Nice")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true);

        mNotificationManagerCompat = NotificationManagerCompat.from(context);
        mNotificationManager.notify(0, builder.build());
    }
}
