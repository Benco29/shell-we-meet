package benco.shellwemeet;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.geo.GeoPoint;
import com.backendless.persistence.DataQueryBuilder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import benco.shellwemeet.listeners.OnGeocoderFinishedListener;
import benco.shellwemeet.utils.Constants;
import benco.shellwemeet.utils.DBUtil;
import benco.shellwemeet.utils.DateValidator;
import benco.shellwemeet.utils.FileConn;
import benco.shellwemeet.utils.LoaderDialog;

public class AddLocationActivity extends AppCompatActivity implements LocationListener {

    private final String TAG = "AddLocationActivity";

    Context context;
    Location currentLocation;
    LocationManager locationManager;
    View locationForm;
    DBUtil database;

    Button continueBtn;
    LocationManager mLocationManager;
    FileConn fileConn;

    EditText birthDateTxt;
    TextView locationTxt;
    String username;

    GeoPoint geoPoint = new GeoPoint();
    LoaderDialog loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_location);
        Log.i(TAG, "onCreate: starting");

        setPointer();
        checkForPermission();
        updateLocData();
    }

    private void setPointer() {
        Log.i(TAG, "setPointer: started setPointer");
        context = this;
        fileConn = new FileConn(context);
        database = new DBUtil(context);

        birthDateTxt = findViewById(R.id.addLocBirthDate);
        locationTxt = findViewById(R.id.addLocText);

        continueBtn = findViewById(R.id.addLocContinueBtn);

        // getting the location manager from the system services - the location manager is an object that
        // deals with our connection to the location services (for instance asking for updates)
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        String loaderMsg = "Updating your info...";
        loader = new LoaderDialog(context, loaderMsg);
        locationForm = findViewById(R.id.addLocForm);

        loader.showAlertDialogLoader(true);
        locationForm.setVisibility(View.GONE);
        Log.i(TAG, "setPointer: started showProgress");
        Intent prevIntent = getIntent();
        final String currentUser = prevIntent.getStringExtra("username");

        //get location from file
        String location = "Unknown";
        try {
            String[] userDetails = fileConn.read().split("#");
            location = FileConn.getLocationFromStringArray(userDetails);
            username = FileConn.getUsernameFromStringArray(userDetails);
            locationTxt.setText(location);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "setPointer: "+e);
            locationTxt.setText(location);
        }

        //adding a textChangeListener so that we could know when the user is done typing his birthDate, and then activate the Continue Button
        birthDateTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                checkBtnValidity();
            }
        });

        continueBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                //checking if the date is valid in one of these two formats 05/06/1989 or 05.06.1989
                final String birthDate = birthDateTxt.getText().toString().trim();
                DateValidator validator = new DateValidator();

                //checking if the date is NOT valid
                if (!validator.isDateValid(birthDate, Constants.DOT_DATE_FORMAT) && !validator.isDateValid(birthDate, Constants.SLASH_DATE_FORMAT)) {
                    Toast.makeText(context, "Invalid date, please enter a correct birth date.", Toast.LENGTH_LONG).show();
                    return;
                }
                final int age = validator.ageCalculator(birthDate/*, new SimpleDateFormat(Constants.SLASH_DATE_FORMAT)*/);

                loader.setAlertMessage("Updating your info");
                LoaderDialog.showAlertAgain(loader);
                //getting the current user
                DataQueryBuilder builder = DataQueryBuilder.create();
                builder.setWhereClause("name='" + currentUser + "'");
                Backendless.Persistence.of(BackendlessUser.class).find(builder, new AsyncCallback<List<BackendlessUser>>() {
                    @Override
                    public void handleResponse(List<BackendlessUser> response) {
                        if (response == null || response.isEmpty()) {
                            Toast.makeText(context, "NO GOOOOOOOOOD", Toast.LENGTH_LONG).show();
                            return;
                        }
                        final BackendlessUser user = response.get(0);
                        final String currentUserName = user.getProperty("name").toString();

                        user.setProperty("birthDate", birthDate);
                        //user.setProperty("location", geoPoint); //do not update user location in database
                        user.setProperty("age", age);

                        //updating the user in the backendless database
                        Backendless.UserService.update(user, new AsyncCallback<BackendlessUser>() {
                            @Override
                            public void handleResponse(BackendlessUser response) {
                                Toast.makeText(context, "User info updated", Toast.LENGTH_SHORT).show();
                                Intent addProfilePhoto = new Intent(context, AddProfileActivity.class);
                                addProfilePhoto.putExtra("username", currentUserName);
                                startActivity(addProfilePhoto);
                                LoaderDialog.stopAlertDialog(loader);
                            }

                            @Override
                            public void handleFault(BackendlessFault fault) {
                                Toast.makeText(context, "Error: "+fault.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.i(TAG, "handleFault: "+fault.toString());
                                LoaderDialog.stopAlertDialog(loader);
                                locationForm.setVisibility(View.VISIBLE);
                            }
                        });
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        Toast.makeText(context, "Error: " + fault.getMessage(), Toast.LENGTH_LONG).show();
                        Log.i(TAG, "handleFault: ");
                        LoaderDialog.stopAlertDialog(loader);
                        locationForm.setVisibility(View.VISIBLE);
                    }
                });

            }
        });
    }

    private void checkBtnValidity() {
        if (!birthDateTxt.getText().toString().isEmpty() && birthDateTxt.getText().toString().length() >= 7) {
            continueBtn.setEnabled(true);
        } else {
            continueBtn.setEnabled(false);
        }
    }

    private void checkForPermission() {
        Log.i(TAG, "checkForPermission: started");
        //creating a list that holds all the permissions we need to ask
        List<String> permList = new ArrayList<>();

        //checking if we already have permission to use ACCESS_COARSE_LOCATION, if not then add it to the list of permissions to ask for
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        //checking if we already have permission to use ACCESS_FINE_LOCATION, if not then add it to the list of permissions to ask for
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        //if permList isn't empty then we have at least one permission to ask for
        if (!permList.isEmpty()) {
            //asks for permissions for all the items on the list
            ActivityCompat.requestPermissions(this, permList.toArray(new String[permList.size()]), Constants.LOCATION_PERM_REQUEST_CODE);
        } else {//we already have all the permissions we need
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10_000, 1, this);
        }
        Log.i(TAG, "checkForPermission: ended");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.LOCATION_PERM_REQUEST_CODE) { // if we are getting the
            // result of our location permission request

            // make sure that we now have permission for to access the two
            // types of location
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //request location updates - gets the provider, minimum time
                // between updates, minimum distance between updates,
                // location listener (in our case this is the current
                // instance of MainActivity which implements LocationListener)
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10_000, 500, this);
            } else {
                Toast.makeText(context, "You must allow location services for this app to work", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateLocData() {
        Location lastKnownLocation = getLocation();// get the last known location
        if (lastKnownLocation == null) { // if getLocation returned null then we stop this method
            Log.i(TAG, "updateLocData: location is null");
            return;
        }
        //getting the latitude and longitude as double
        double latitude = lastKnownLocation.getLatitude();
        double longitude = lastKnownLocation.getLongitude();

        final String latLong = latitude+"\n"+longitude;

        geoPoint.setLatitude(latitude);
        geoPoint.setLongitude(longitude);

        if (Geocoder.isPresent()) {
            getCityName(lastKnownLocation, new OnGeocoderFinishedListener() {
                @Override
                public void onFinished(List<Address> results) {
                    Address address = results.size() == 0 ? null : results.get(0);
                    String currentLoc = "";
                    if (address == null) {
                        currentLoc += latLong;
                    } else {
                        currentLoc += address.getCountryName() + "\n";
                        currentLoc += address.getAdminArea() + "\n";
                        currentLoc += address.getThoroughfare() + "\n";
                        currentLoc += address.getLatitude() + "\n";
                        currentLoc += address.getLongitude() + "\n";
                        currentLoc += address.getSubThoroughfare();

                        geoPoint.addMetadata("Country", address.getCountryName());
                        geoPoint.addMetadata("City", address.getAdminArea()==null? "Unknown" : address.getAdminArea());

                        if (locationTxt.getText().toString().equals("Unknown")) {
                            locationTxt.setText(currentLoc);//This will display the final address.
                        }

                    }
                    database.uploadGeoPointRelation(username, geoPoint);
                    locationTxt.setText(currentLoc);
                    Toast.makeText(context, "Location found", Toast.LENGTH_SHORT).show();
                    LoaderDialog.stopAlertDialog(loader);
                    locationForm.setVisibility(View.VISIBLE);
                }
            });
        } else { //geodecoder is not present

            locationTxt.setText(latLong);
            LoaderDialog.stopAlertDialog(loader);
            locationForm.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void getCityName(final Location location, final OnGeocoderFinishedListener listener) {
        new AsyncTask<Void, Integer, List<Address>>() {
            @Override
            protected List<Address> doInBackground(Void... arg0) {
                Geocoder coder = new Geocoder(context, Locale.getDefault());//was Locale.ENGLISH
                List<Address> results = null;
                try {
                    results = coder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "doInBackground: "+e.toString());
                    //Toast.makeText(context, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                return results;
            }

            @Override
            protected void onPostExecute(List<Address> results) {
                if (results != null && listener != null) {
                    listener.onFinished(results);
                }
            }
        }.execute();
    }

    private Location getLocation() {
        Log.i(TAG, "getLocation: started");
        // we need to make sure that we have permission before using the location services
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "You must allow location for this app to work...", Toast.LENGTH_LONG).show();
            checkForPermission();
            Log.i(TAG, "getLocation: processing");
            return null;
        }
        // if we do have permission:
        // get the location according to the gps and according to the network -
        // we want to compare them and return the more accurate one

//        Location locationGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); //returns null
//        Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);//returns null

        Location myLocation = getLastKnownLocation();

//        long gpsLocationTime = locationGps != null ? locationGps.getTime() : 0;
//        long netLocationTime = locationNet != null ? locationNet.getTime() : 0;
//        Log.i(TAG, "05.7 getLocation: returning location");
//        return gpsLocationTime > netLocationTime ? locationGps : locationNet;

        return myLocation;
    }



    private Location getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
            List<String> providers = mLocationManager.getProviders(true);
            Location bestLocation = null;
            for (String provider : providers) {
                Location loc = mLocationManager.getLastKnownLocation(provider);
                if (loc == null) {
                    continue;
                }
                if (bestLocation == null || loc.getAccuracy() < bestLocation.getAccuracy()) {
                    // Found best last known location:
                    bestLocation = loc;
                }
            }
            return bestLocation;
        } else {
            return null;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // code for location updates
        currentLocation = location;

        if (currentLocation != null) { // if we have a location, then update the textView
            updateLocData();
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Toast.makeText(context, "status changes to: " + s, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(context, "provider " + provider + " enabled", Toast.LENGTH_LONG).show();
//        setPointer();
//        checkForPermission();
//        updateLocData();
    }

    @Override
    public void onProviderDisabled(String provider) {
        //ig gps is disabled then we show an alert dialog that asks the user to allow gps
        Toast.makeText(context, "provider " + provider + " disabled", Toast.LENGTH_LONG).show();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setCancelable(false);
        builder.setMessage("This app cannot work without location services, please allow location services");

        builder.setPositiveButton("Allow", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //if the user allow then we take him to the gps settings
                Intent gpsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(gpsIntent);
                dialog.cancel();
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //if the user do not allow then we take him back to the previous activity
                Intent toCompDetails = new Intent(context, CompleteDetailsActivity.class);
                startActivity(toCompDetails);
                dialog.cancel();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

}
