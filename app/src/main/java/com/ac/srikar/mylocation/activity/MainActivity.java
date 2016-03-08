package com.ac.srikar.mylocation.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
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
    private static final String STATE_RESOLVING_ERROR_KEY = "state-resolving-error";

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

    // Boolean to check whether the app is already resolving an error
    private boolean mResolvingError = false;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";

    private static final int REQUEST_LOCATION = 11;
    private static final int REQUEST_LOCATION_UPDATES = 12;

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
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) && (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            // Check Permissions Now
            if ((ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) &&
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Display UI and wait for user interaction
                Snackbar.make(coordinatorLayout, "Permission are necessary to access location", Snackbar.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_LOCATION_UPDATES);
            }
        } else {
            // Permission has been granted, continue as usual
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
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
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) && (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            // Check Permissions Now
            if ((ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) &&
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Display UI and wait for user interaction
                Snackbar.make(coordinatorLayout, "Permission are necessary to access location", Snackbar.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_LOCATION);
            }
        } else {
            // Permission has been granted, continue as usual
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            displayLocationUI();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                displayLocationUI();
            } else {
                // Permission was denied or request was cancelled
            }
        } else if (requestCode == REQUEST_LOCATION_UPDATES) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } else {
                // Permission was denied or request was cancelled
            }
        }
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
        Log.i(LOG_TAG, "Connection failed");
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            showErrorDialog(connectionResult.getErrorCode());
            mResolvingError = true;
        }
    }

    /**
     * Show the Error dialog
     */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errorDialog");
    }

    /**
     * Called from ErrorDialogFragment when the dialog is dismissed.
     */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /**
     * A fragment to display an error dialog
     */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MainActivity) getActivity()).onDialogDismissed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        }
    }

    /**
     * Save the activity state
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mLastLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        savedInstanceState.putBoolean(STATE_RESOLVING_ERROR_KEY, mResolvingError);
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
                // mCurrentLocation is not null.
                mLastLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(
                        LAST_UPDATED_TIME_STRING_KEY);
            }

            if (savedInstanceState.keySet().contains(STATE_RESOLVING_ERROR_KEY)) {
                mResolvingError = savedInstanceState.getBoolean(STATE_RESOLVING_ERROR_KEY, false);
            }
            displayLocationUI();
        }
    }
}
