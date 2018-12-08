package benco.shellwemeet;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;

import java.io.IOException;

import benco.shellwemeet.menu.About;
import benco.shellwemeet.menu.Statistics;
import benco.shellwemeet.utils.Constants;
import benco.shellwemeet.utils.DBUtil;
import benco.shellwemeet.utils.FileConn;
import benco.shellwemeet.utils.LoaderDialog;


public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    Context context;
    Button loginBtn, regBtn;
    EditText loginEmailField, loginPassField;
    CheckBox stayLoggedin;

    DBUtil database;
    FileConn fileConn;
    LoaderDialog loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setPointer();
    }

    //menu options intents
    @Override
    public boolean onCreateOptionsMenu(Menu menu) { // creating a menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { // menu's options

        switch (item.getItemId()) {
            case R.id.stats:
                Intent intent1 = new Intent(this, Statistics.class);
                startActivity(intent1);
                return true;


            case R.id.about:
                Intent intent2 = new Intent(this, About.class);
                startActivity(intent2);
                return true;

            case R.id.logout:
                Intent intent3 = new Intent(this, LoginActivity.class);
                startActivity(intent3);
                return true;

            default:
                return onOptionsItemSelected(item);
        }

    }

    private void setPointer() {
        context = this;

        stayLoggedin = findViewById(R.id.logRegCB);

        loginBtn = findViewById(R.id.loginBtn);
        regBtn = findViewById(R.id.registerBtn);

        loginEmailField = findViewById(R.id.mainLoginEmailField);
        loginPassField = findViewById(R.id.mainLoginPassField);

        database = new DBUtil(context);
        fileConn = new FileConn(context);
        try {
            fileConn.clear();//clear the file - so we won't have two usernames on it
        } catch (IOException e) {
            e.printStackTrace();
        }
        String loaderMsg = "Loading..Please wait..";
        loader = new LoaderDialog(context, loaderMsg);

        Intent prevIntent = getIntent();
        String userLocation = prevIntent.getStringExtra("location");
        String singleAddressLocation = "";
        if (userLocation == null || userLocation.isEmpty()) {
            singleAddressLocation = "Unknown";
        } else {
            singleAddressLocation = getStringAddress(userLocation);
        }
        final String finalLocation = singleAddressLocation;
        Log.e(TAG, "setPointer: " + userLocation);
        Log.e(TAG, "setPointer: " + singleAddressLocation);

        loginBtn.setOnClickListener(new View.OnClickListener() { //if the user exists and wants to login
            @Override
            public void onClick(View v) {
                loader.showAlertDialogLoader(true);
                if (loginEmailField.getText().toString().isEmpty() || loginPassField.getText().toString().isEmpty()) {
                    Toast.makeText(context, "Please fill out all the fields", Toast.LENGTH_SHORT).show();
                } else {
                    String inputEmail = loginEmailField.getText().toString();
                    String inputPass = loginPassField.getText().toString();
                    boolean isLoggedIn = stayLoggedin.isEnabled();
                    //writing to the file
                    try {
                        fileConn.write("Email: " + inputEmail + Constants.DELIMITER + "Password: " + inputPass + Constants.DELIMITER);
                    } catch (IOException exception) {
                        exception.printStackTrace();
                        Log.e(TAG, "onClick: " + exception);
                    }

                    //loggin the user
                    Backendless.UserService.login(inputEmail, inputPass, new AsyncCallback<BackendlessUser>() {
                        @Override
                        public void handleResponse(BackendlessUser response) {
                            Toast.makeText(context, "welcome back " + response.getProperty("name"), Toast.LENGTH_LONG).show();
                            response.setProperty("isOnline", true);
                            //updating the user in backendless server
                            BackendlessUser bUser = response;
                            Backendless.UserService.update(bUser, new AsyncCallback<BackendlessUser>() {
                                @Override
                                public void handleResponse(BackendlessUser response) {
                                    Log.i(TAG, "handleResponse: user property updated");
                                    LoaderDialog.stopAlertDialog(loader);
                                }

                                @Override
                                public void handleFault(BackendlessFault fault) {
                                    Log.e(TAG, "handleFault: " + fault);
                                    LoaderDialog.stopAlertDialog(loader);
                                }
                            });

                            try {
                                fileConn.write("Username: " + response.getProperty("name").toString() + Constants.DELIMITER);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Intent matchesIntent = new Intent(context, MatchActivity.class);
                            startActivity(matchesIntent);


                            LoaderDialog.stopAlertDialog(loader);
                        }


                        @Override
                        public void handleFault(BackendlessFault fault) {
                            String errString = "Couldn't Login, cause: " + fault.getCode() + " " + fault.getMessage();
                            Toast.makeText(context, errString, Toast.LENGTH_LONG).show();
                            Log.i(TAG, errString);
                            LoaderDialog.stopAlertDialog(loader);
                        }
                    }, isLoggedIn);
                }
            }
        });

        regBtn.setOnClickListener(new View.OnClickListener() { //if the user is new, then go to register activity
            @Override
            public void onClick(View v) {
                loader.showAlertDialogLoader(true);
                String inputEmail = loginEmailField.getText().toString();
                String inputPass = loginPassField.getText().toString();

                if (inputEmail.equals("")) {
                    Toast.makeText(context, "Email field cannot be empty", Toast.LENGTH_LONG).show();
                    LoaderDialog.stopAlertDialog(loader);
                    return;
                }
                if (inputPass.equals("")) {
                    Toast.makeText(context, "Password field cannot be empty", Toast.LENGTH_LONG).show();
                    LoaderDialog.stopAlertDialog(loader);
                    return;
                }

                Intent regIntent = new Intent(context, RegisterActivity.class);
                regIntent.putExtra("email", inputEmail);
                regIntent.putExtra("password", inputPass);

                regIntent.putExtra("location", finalLocation);
                regIntent.putExtra("Loggedin", stayLoggedin.isChecked());//letting the next activity know if the user wants to stay logged-in
                startActivity(regIntent);
                LoaderDialog.stopAlertDialog(loader);
            }
        });

    }//end of setPointer

    private String getStringAddress(String userLocation) {

        String[] splitString = userLocation.split("\n");
        String first = splitString[0];
        String returnedString = "";
        returnedString += first;
        for (int i = 1; i < splitString.length; i++) {
            if (splitString[i] == first) {
                break;
            }
            returnedString += "\n" + splitString[i];
        }
        return returnedString;
    }

}//end of class