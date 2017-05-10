package paru.com.loktra;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import paru.com.loktra.data.LoadDataIntoMap;
import paru.com.loktra.data.Loktra;
import paru.com.loktra.service.LoktraGps;
import paru.com.loktra.util.loktraConstons;

import static paru.com.loktra.R.id.map;

/**
 * Created by parvendan on 11/05/17.
 */
public class LocationTracking extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {

    private static final String TAG = LocationTracking.class.getSimpleName();
    private GoogleMap mMap;
    private Button start, stop;
    private AppEventReceiver mAppEventReceiver;
    private static final int REQUEST_CHECK_SETTINGS = 1;
    private TextView time;
    private long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L;
    private Handler handler;
    private int Seconds, Minutes;
    private Polyline line;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_tracking);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);
        showPermission();
        initializecomponent();

    }

    private void initializecomponent() {
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        stop.setEnabled(false);
        time = (TextView) findViewById(R.id.time);
        handler = new Handler();

        mAppEventReceiver = new AppEventReceiver();
        IntentFilter filer = new IntentFilter();
        filer.addAction(loktraConstons.GPS_DISCONNECTED);
        registerReceiver(mAppEventReceiver, filer);
    }

    public void showPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "permission not present, showing persmission dialog");
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CHECK_SETTINGS);
        }
    }

    private Location getLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showPermission();
            }
            Location location = locationManager.getLastKnownLocation(provider);
            mMap.setMyLocationEnabled(true);
            if (location == null) {
                continue;
            }
            if (bestLocation == null || location.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = location;
            }
        }
        if (bestLocation == null) {
            return null;
        }
        return bestLocation;
    }

    private void loadMapCurrentLocation() {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(getLastKnownLocation().getLatitude(), getLastKnownLocation().getLongitude()), 13));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(getLastKnownLocation().getLatitude(), getLastKnownLocation().getLongitude()))
                .zoom(20)
                .bearing(90)
                .tilt(40)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        if (getLastKnownLocation() != null) {
            loadMapCurrentLocation();
        }

    }

    /**
     * Called when a view has been clicked.
     *
     * @param view The view that was clicked.
     */
    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.start:
                starservice();
                break;
            case R.id.stop:
                stopService();
                break;
            default:
                break;
        }

    }

    private void starservice() {
        start.setEnabled(false);
        stop.setEnabled(true);
        MillisecondTime = 0L;
        StartTime = 0L;
        TimeBuff = 0L;
        UpdateTime = 0L;
        Seconds = 0;
        Minutes = 0;
        StartTime = SystemClock.uptimeMillis();
        handler.postDelayed(runnable, 0);
        if (line != null) {
            line.remove();
        }
        if (getLastKnownLocation() != null) {
            loadMapCurrentLocation();
        }
        Intent startLocationService = new Intent(this, LoktraGps.class);
        startService(startLocationService);
    }

    private void stopService() {
        stop.setEnabled(false);
        start.setEnabled(true);
        Intent stopLocationService = new Intent(this, LoktraGps.class);
        stopService(stopLocationService);
        final List<LatLng> routeArray = new ArrayList<>();
        new LoadDataIntoMap(LocationTracking.this, new LoadData() {
            @Override
            public void onSuccess(List<Loktra> loktra) {
                for (Loktra i : loktra) {
                    LatLng latLng = new LatLng(i.getLati(), i.getLongtitude());
                    routeArray.add(latLng);
                }
                drawLine(routeArray);
            }
        }).execute();
        TimeBuff += MillisecondTime;
        handler.removeCallbacks(runnable);
    }

    public Runnable runnable = new Runnable() {
        public void run() {
            MillisecondTime = SystemClock.uptimeMillis() - StartTime;
            UpdateTime = TimeBuff + MillisecondTime;
            Seconds = (int) (UpdateTime / 1000);
            Minutes = Seconds / 60;
            Seconds = Seconds % 60;
            time.setVisibility(View.VISIBLE);
            time.setText("Total shift Time : " + Minutes + ":" + String.format("%02d", Seconds));
            handler.postDelayed(this, 0);
        }

    };

    public void drawLine(List<LatLng> points) {
        if (points == null) {
            Log.e("Draw Line", "got null as parameters");
            return;
        }
        line = mMap.addPolyline(new PolylineOptions().width(3).color(Color.RED));
        line.setPoints(points);
    }

    class AppEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent != null && intent.getAction().equals(loktraConstons.GPS_DISCONNECTED)) {
                Bundle bundle = intent.getBundleExtra(loktraConstons.GPS_BUNDLE);
                if (bundle != null) {
                    Status status = bundle.getParcelable(loktraConstons.GPS_STATUS);
                    if (status != null) {
                        try {
                            status.startResolutionForResult(LocationTracking.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(LocationTracking.this, permission)) {
                showPermission();
                Log.e("denied", permission);
            } else {
                if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                    if (getLastKnownLocation() != null) {
                        loadMapCurrentLocation();
                    }
                    Log.e("allowed", permission);
                } else {
                    //set to never ask again
                    Log.e("set to never ask again", permission);
                    Toast.makeText(this, "Application wont work !", Toast.LENGTH_SHORT).show();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAppEventReceiver != null) {
            unregisterReceiver(mAppEventReceiver);
            mAppEventReceiver = null;
        }
    }
}
