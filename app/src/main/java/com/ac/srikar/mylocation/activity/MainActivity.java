package com.ac.srikar.mylocation.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ac.srikar.mylocation.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    // Logcat Tag.
    protected static final String LOG_TAG = MainActivity.class.getSimpleName();

    // Constant used in Check Play Services Method.
    protected static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    // The desired interval for location updates.
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

    // The fastest rate for active location updates.
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Saved Keys
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates";
    private static final String LOCATION_KEY = "location-value";
    private static final String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time";

    // Provides the entry point to Google Play services.
    protected GoogleApiClient mGoogleApiClient;

    // Represents a geographical location.
    protected Location mLastLocation;

    // Stores parameters for requests to the FusedLocationProviderApi.
    protected LocationRequest mLocationRequest;

    // UI TextViews
    private TextView mLatitude;
    private TextView mLongitude;
    private TextView mAccuracy;
    private TextView mAltitude;
    private TextView mSpeed;
    private TextView mLastUpdateTimeTextView;

    // UI Buttons
    private Button myLocationButton;
    private Button startUpdatesButton;
    private Button stopUpdatesButton;

    // String variable to store time
    private String mLastUpdateTime;

    // Boolean to check whether the user is requesting Location Updates
    private boolean mRequestingLocationUpdates = false;

    private CoordinatorLayout coordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        // Initialize TextViews
        mLatitude = (TextView) findViewById(R.id.latitude);
        mLongitude = (TextView) findViewById(R.id.longitude);
        mAccuracy = (TextView) findViewById(R.id.accuracy);
        mAltitude = (TextView) findViewById(R.id.altitude);
        mSpeed = (TextView) findViewById(R.id.speed);
        mLastUpdateTimeTextView = (TextView) findViewById(R.id.lastUpdateTime);

        // Initialize Buttons
        myLocationButton = (Button) findViewById(R.id.myLocationButton);
        startUpdatesButton = (Button) findViewById(R.id.startUpdatesButton);
        stopUpdatesButton = (Button) findViewById(R.id.stopUpdatesButton);

        if (checkPlayServices()) {
            buildGoogleApiClient();
        }

        updateValuesFromBundle(savedInstanceState);
    }

    /**
     * Check whether the Google Play Services is installed on the device or not
     */
    protected boolean checkPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        final int resultCode = googleAPI.isGooglePlayServicesAvailable(this);
        // When Play services not found in device
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(resultCode)) {
                Dialog dialog = googleAPI.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
                // Show Error dialog to install Play services
                if (dialog != null) {
                    dialog.show();
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            MainActivity.this.finish();
                        }
                    });
                    return false;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    /**
     * Sets up the location request.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    /**
     * Connect the Google Api client when the application is started
     */
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    /**
     * Start the location updates when the screen is resumed to save battery
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    /**
     * Stop the location updates when the screen is paused to save battery
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mRequestingLocationUpdates) {
            stopLocationUpdates();
        }
    }

    /**
     * Disconnect the Google Api client when the application is exited
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Disable My location, Start Updates Button and enable Start Updates button
     * if requesting location updates.
     * Enable My location, Start Updates button and disable Stop Updates button
     * if not requesting location updates.
     */
    private void setButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
            myLocationButton.setEnabled(false);
            startUpdatesButton.setEnabled(false);
            stopUpdatesButton.setEnabled(true);
        } else {
            myLocationButton.setEnabled(true);
            startUpdatesButton.setEnabled(true);
            stopUpdatesButton.setEnabled(false);
        }
    }

    /**
     * Update the location when my Location Button is Clicked
     */
    public void myLocationButton(View view) {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        mGoogleApiClient.connect();
    }

    /**
     * Start Location Updates when Start Updates button is clicked.
     */
    public void startLocationUpdatesButton(View view) {
        mRequestingLocationUpdates = true;
        setButtonsEnabledState();
        startLocationUpdates();
    }

    /**
     * Start Location Updates.
     */
    private void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Stop Location Updates when Stop Updates button is clicked.
     */
    public void stopLocationUpdatesButton(View view) {
        mRequestingLocationUpdates = false;
        setButtonsEnabledState();
        stopLocationUpdates();
    }

    /**
     * Stop Location Updates.
     */
    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /**
     * Update the TextViews when Location is found
     */
    private void displayLocationUI() {
        Log.v(LOG_TAG, "User Location: " + String.valueOf(mLastLocation));
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        if (mLastLocation != null) {
            mLatitude.setText(String.valueOf(mLastLocation.getLatitude()));
            mLongitude.setText(String.valueOf(mLastLocation.getLongitude()));
            mAccuracy.setText(String.valueOf(mLastLocation.getAccuracy()));
            mAltitude.setText(String.valueOf(mLastLocation.getAltitude()));
            mSpeed.setText(String.valueOf(mLastLocation.getSpeed()));
            mLastUpdateTimeTextView.setText(mLastUpdateTime);
        } else {
            Snackbar.make(coordinatorLayout, "No Location Found", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_share) {
            return true;
        } else if (id == R.id.action_about) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        displayLocationUI();
    }

    /**
     * Called when a new Location is found.
     */
    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocationUI();
    }

    /**
     * Called when connection to GoogleApiClient is suspended.
     */
    @Override
    public void onConnectionSuspended(int i) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(LOG_TAG, "Connection failed: ConnectionResult.getErrorCode() = " + i);
        Snackbar.make(coordinatorLayout, i, Snackbar.LENGTH_LONG);
    }

    /**
     * Called when connection to GoogleApiClient is failed.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(LOG_TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    /**
     * Save the activity state
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mLastLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                setButtonsEnabledState();
            }

            // Update the value of mLastLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocationis not null.
                mLastLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(
                        LAST_UPDATED_TIME_STRING_KEY);
            }
            displayLocationUI();
        }
    }
}
