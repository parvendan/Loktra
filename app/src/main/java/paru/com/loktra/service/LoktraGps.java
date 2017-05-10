package paru.com.loktra.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import paru.com.loktra.data.SaveLatAndLong;
import paru.com.loktra.util.loktraConstons;


/**
 * Created by parvendan on 11/05/17.
 */

public class LoktraGps extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    protected static final String TAG = LoktraGps.class.getSimpleName();
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    protected GoogleApiClient mGoogleApiClient;
    protected Location mCurrentLocation;
    private Double latitude;
    private Double longitude;
    private LocationSettingsRequest.Builder builder;
    private LocationRequest mLocationRequestHighAccuracy;


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        buildGoogleApiClient();
        builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequestHighAccuracy);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        checkGPSEnabled();
        Log.d(TAG, "On start called");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                stopLocationUpdates();
                mGoogleApiClient.disconnect();
            }
        }
        mGoogleApiClient = null;
        super.onDestroy();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkGPSEnabled();
        }
        if (mGoogleApiClient != null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mCurrentLocation != null) {
                latitude = mCurrentLocation.getLatitude();
                longitude = mCurrentLocation.getLongitude();
                new SaveLatAndLong(this, latitude, longitude).execute();
            }
            Log.i(TAG, "onConnected" + "Lati : " + latitude + "Long : " + longitude);
            startLocationUpdates();
        }
    }


    @Override
    public void onConnectionSuspended(int i) {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        checkGPSEnabled();
        Log.i(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "connection failed" + connectionResult.getErrorMessage());
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        latitude = mCurrentLocation.getLatitude();
        longitude = mCurrentLocation.getLongitude();
        Log.i(TAG, "onLocationChanged" + "Lati : " + latitude + "Long : " + longitude);
        new SaveLatAndLong(this, latitude, longitude).execute();

    }

    protected void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "permission not present, showing persmission dialog");
        } else {
            if (mGoogleApiClient != null) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequestHighAccuracy, this);
            }
        }
    }

    protected void stopLocationUpdates() {
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    public void checkGPSEnabled() {
        if (builder == null) {
            if (mLocationRequestHighAccuracy == null) {
                createLocationRequest();
                return;
            }
            builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequestHighAccuracy);
            Log.e(TAG, "builder is null!");
        }
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates states = result.getLocationSettingsStates();
                Log.d(TAG, "location states" + states);
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.d(TAG, "location service is enabled");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Intent intent = new Intent();
                        intent.setAction(loktraConstons.GPS_DISCONNECTED);
                        Bundle bun = new Bundle();
                        bun.putParcelable(loktraConstons.GPS_STATUS, status);
                        intent.putExtra(loktraConstons.GPS_BUNDLE, bun);
                        sendBroadcast(intent);
                        break;
                }
            }
        });
    }

    private void createLocationRequest() {
        mLocationRequestHighAccuracy = new LocationRequest();
        mLocationRequestHighAccuracy.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequestHighAccuracy.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequestHighAccuracy.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

}
