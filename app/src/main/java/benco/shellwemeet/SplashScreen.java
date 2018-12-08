package benco.shellwemeet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.backendless.geo.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import benco.shellwemeet.listeners.OnGeocoderFinishedListener;
import benco.shellwemeet.utils.Constants;
import benco.shellwemeet.utils.DBUtil;
import benco.shellwemeet.utils.FileConn;
import benco.shellwemeet.utils.LoaderDialog;

public class SplashScreen extends AppCompatActivity implements LocationListener {

    Context context;
    ProgressBar progBar;
    DBUtil database;

    Location currentLocation;
    LocationManager locationManager, mLocationManager;

    GeoPoint geoPoint;
    String locationString, currentUsername;
    LoaderDialog loader;

    FileConn fileConn;
    boolean isGpsOn;

    final String TAG = "SplashScreen";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_splash_screen);

        context = this;
        database = new DBUtil(context);

        geoPoint = new GeoPoint(); //geopoint to upload to the backendless database
        fileConn = new FileConn(context);//the file we will save the location and user details on

        //showing loader dialog until the location is found
        String loaderMessage = "Finding your location...Please wait...";
        loader = new LoaderDialog(context, loaderMessage);
        //loader.showAlertDialogLoader(true);
        //setting up the location manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (database.isUserLoggedIn()) { // the user is already logged in
            //get the location and upload to the database
            try {
                String[] userData = fileConn.read().split(Constants.DELIMITER);
                currentUsername = FileConn.getUsernameFromStringArray(userData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Intent matchesIntent = new Intent(context, MatchActivity.class);
            if (currentUsername != null && !currentUsername.isEmpty()) {
                matchesIntent.putExtra("username", currentUsername);
            }
            startActivity(matchesIntent);
            //loader.showAlertDialogLoader(false);

        } else { //if there is no user logged in, then continue finding location and to the login activity
            setPointer();
            checkForPermission();
            updateLocData();

        }

    }

    private void setPointer() {
        progBar = findViewById(R.id.splashProgBar);
        //setting the progress bar color to dark gray
        Drawable progressDrawable = progBar.getIndeterminateDrawable().mutate();
        //progressDrawable.setColorFilter(Color.DKGRAY, android.graphics.PorterDuff.Mode.SRC_IN);
        progBar.setProgressDrawable(progressDrawable);

        database.registerDeviceForMessaging();
    }

    private void checkForPermission() {
        Log.i(TAG, "checkForPermission: started");
        //creating a list that holds all the permissions we need to ask
        List<String> permList = new ArrayList();

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

        final String latLong = latitude + "\n" + longitude;

        geoPoint.setLatitude(latitude);
        geoPoint.setLongitude(longitude);
        Log.e(TAG, "updateLocData: continued after intent finished.................. ");
        if (Geocoder.isPresent()) {
            //getting the address from latitude and longitude
            getCityName(lastKnownLocation, new OnGeocoderFinishedListener() {
                @Override
                public void onFinished(List<Address> results) {
                    Address address = results.size() == 0 ? null : results.get(0);

                    if (address == null) {
                        locationString += latLong;
                    } else {
                        locationString += address.getCountryName() + "\n";
                        locationString += address.getAdminArea() + "\n";
                        locationString += address.getThoroughfare() + "\n";
                        locationString += address.getLatitude() + "\n";
                        locationString += address.getLongitude() + "\n";
                        locationString += address.getSubThoroughfare();

                        geoPoint.addMetadata("Country", address.getCountryName());
                        geoPoint.addMetadata("City", address.getAdminArea() == null ? "Unknown" : address.getAdminArea());
                        Log.e(TAG, "updateLocData: continued after intent finished.................. ");

                    }
                    /*location found including address*/
                    Toast.makeText(context, "Location found", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(context, LoginActivity.class);
                    //writing the location string to the file
                    try {
                        if (fileConn.read().isEmpty()) {
                            locationString = " " + locationString + Constants.DELIMITER;
                        }
                        fileConn.write("Location:" + locationString);
                    } catch (IOException exception) {
                        exception.printStackTrace();
                        Log.e(TAG, "onFinished: " + exception);
                    }

                    //Saving geo point to backendless and updating the user schema column "location"
                    database.uploadGeoPointRelation(currentUsername, geoPoint);

                    loginIntent.putExtra("location", locationString);
                    startActivity(loginIntent);
                    Log.e(TAG, "onFinished: found location finally!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " );
                    LoaderDialog.stopAlertDialog(loader);
                }
            });
        } else {
            Toast.makeText(context, "Location: " + geoPoint.toString(), Toast.LENGTH_LONG).show();
            //loader.showAlertDialogLoader(false);
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
                    Log.e(TAG, "doInBackground: " + e.toString());
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

        Location myLocation = getLastKnownLocation();

        if (myLocation != null) {
            return myLocation;
        } else {

            Location locationGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            long gpsLocationTime = locationGps != null ? locationGps.getTime() : 0;
            long netLocationTime = locationNet != null ? locationNet.getTime() : 0;
            Log.i(TAG, "05.7 getLocation: returning location");
            return gpsLocationTime > netLocationTime ? locationGps : locationNet;
        }

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

        if (currentUsername != null && !currentUsername.isEmpty() && geoPoint != null) {
            database.uploadGeoPointRelation(currentUsername, geoPoint);
        }
        Toast.makeText(context, "Location has changed " + location.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Toast.makeText(context, "status changes to: " + s, Toast.LENGTH_LONG).show();

    }

    @Override
    public void onProviderEnabled(String provider) {
        LoaderDialog.showAlertAgain(loader);
        //once gps is allowed then we find the location
        Toast.makeText(context, "provider " + provider + " enabled", Toast.LENGTH_LONG).show();
        locationString = "";

    }

    @Override
    public void onProviderDisabled(String provider) {
        isGpsOn = false;
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
                Intent toLoginScreen = new Intent(context, LoginActivity.class);
                startActivity(toLoginScreen);
                dialog.cancel();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

}//end of class
