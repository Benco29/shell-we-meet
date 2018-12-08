package benco.shellwemeet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import benco.shellwemeet.menu.About;
import benco.shellwemeet.menu.Statistics;
import benco.shellwemeet.model.UserProfile;
import benco.shellwemeet.utils.Constants;
import benco.shellwemeet.utils.DBUtil;
import benco.shellwemeet.utils.FileConn;
import benco.shellwemeet.utils.LoaderDialog;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    Context context;

    EditText regUsername, regEmail, inputRegPass, inputRegConPass;
    Button regBtn;

    View registerView, progressBar;
    TextView tvLoad;
    LoaderDialog loader;

    DBUtil database;
    FileConn fileConn;
    UserProfile newUser;

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    public static final Pattern VALID_USERNAME_REGEX =
            Pattern.compile("^[a-zA-Z_0-9._%+@#$>^:;<*&!%-]{6,16}", Pattern.CASE_INSENSITIVE);
    public static final Pattern VALID_PASSWORD_REGEX =
            Pattern.compile("^[a-zA-Z_0-9._%+@#$><*&!%-]{8,16}$", Pattern.CASE_INSENSITIVE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

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

        regBtn = findViewById(R.id.registerBtn);

        tvLoad = findViewById(R.id.registerActivityTvLoad);
        progressBar = findViewById(R.id.registerActivityProgress);
        registerView = findViewById(R.id.registerActivityForm);
        //stayLog = findViewById(R.id.regCheckBox);

        String loaderMsg = "Loading..Please wait";
        loader = new LoaderDialog(context, loaderMsg);

        regUsername = findViewById(R.id.regUsernameField);
        regEmail = findViewById(R.id.regEmailField);
        inputRegPass = findViewById(R.id.regPasswordField);
        inputRegConPass = findViewById(R.id.regConfirmPasswordField);

        database = new DBUtil(context);
        fileConn = new FileConn(context);
        //getting the email and password text from previous activity
        Intent prevIntent = getIntent();
        String logEmail = prevIntent.getStringExtra("email");//get the username from the login activity
        String logPassword = prevIntent.getStringExtra("password");//get the password from the login activity

        regEmail.setText(logEmail); // the email we got from the login
        inputRegPass.setText(logPassword); //the password we got from the login
        final String userLocation = prevIntent.getStringExtra("location"); //the users location from the splash activity

        regBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loader.showAlertDialogLoader(true);

                final String username = regUsername.getText().toString();
                final String email = regEmail.getText().toString();
                final String password = inputRegPass.getText().toString();
                final String conPassword = inputRegConPass.getText().toString();

                //checking the input parameters
                if (!(password.equals(conPassword))) {
                    Toast.makeText(context, "Passwords does not match", Toast.LENGTH_SHORT).show();
                    inputRegPass.setText("");
                    inputRegConPass.setText("");
                    loader.showAlertDialogLoader(false);
                    return;
                }
                if (!(validatePassword(password))) {
                    Toast.makeText(context, "Invalid password. Password Must be 8 letters or longer", Toast.LENGTH_LONG).show();
                    loader.showAlertDialogLoader(false);
                    return;
                }
                //checking if the chosen username is already exists
                doesExist(username, new AsyncCallback<List<BackendlessUser>>() {
                    @Override
                    public void handleResponse(List<BackendlessUser> response) {

                        for (BackendlessUser user : response) {
                            if (user.getProperty("name").equals(username)) {
                                // username does exist
                                Toast.makeText(context, "Username already exists, try a different one", Toast.LENGTH_LONG).show();
                                loader.showAlertDialogLoader(false);
                                return;
                            }
                        }

                        // if we never got inside the if - then the user doesn't exist
                        if (!validateUsername(username)) {
                            Toast.makeText(context, "Username is invalid. Must be 8 letters or longer", Toast.LENGTH_LONG).show();
                            loader.showAlertDialogLoader(false);
                            return;
                        }
                        if (!validateEmail(email)) {
                            Toast.makeText(context, "Email is invalid, try again", Toast.LENGTH_SHORT).show();
                            loader.showAlertDialogLoader(false);
                            return;
                        }
                        if ((password.equals(conPassword)) && validatePassword(password) && validateUsername(username) && validateEmail(email)) {
                            //writing the details to the file
                            try {
                                fileConn.write("Username: "+username + Constants.DELIMITER
                                        + "Password: "+password + Constants.DELIMITER
                                        + "Email: "+email + Constants.DELIMITER);
                            } catch (IOException exception) {
                                exception.printStackTrace();
                                Log.e(TAG, "handleResponse: "+exception);
                            }
                            newUser = new UserProfile(username, password, email); //creating a new userProfile object
                            DBUtil.currentUserLogged = username;
                            //creating the new Table in backendless
                            Backendless.Persistence.save(newUser, new AsyncCallback<UserProfile>() {
                                @Override
                                public void handleResponse(UserProfile response) {
                                    //Toast.makeText(context, "New contact saved successfully!", Toast.LENGTH_LONG).show();
                                    database.registerUser(newUser); // register the user to the backendless database

                                    Intent completeDetailsIntent = new Intent(context, CompleteDetailsActivity.class);
                                    completeDetailsIntent.putExtra("username", newUser.getUsername()); //to find the user in the next activity inside the database and update it's details
                                    startActivity(completeDetailsIntent);
                                    loader.showAlertDialogLoader(false);
                                }

                                @Override
                                public void handleFault(BackendlessFault fault) {
                                    Log.i(TAG, "handleFault: " + fault.toString());
                                    Toast.makeText(context, "Error: " + fault.getMessage(), Toast.LENGTH_LONG).show();
                                    loader.showAlertDialogLoader(false);
                                }
                            });
                        }
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        // fault - error
                        Toast.makeText(context, "Couldn't register user. cause: " + fault.getMessage(), Toast.LENGTH_LONG).show();
                        loader.showAlertDialogLoader(false);
                    }
                });
            }
        });

    } // end of setPointer

    private void doesExist(String username, AsyncCallback<List<BackendlessUser>> callback) {
        database.doesExists(username, callback);
    }

    public static boolean validateEmail(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
        return matcher.find();
    }

    public static boolean validateUsername(String username) {
        Matcher matcher = VALID_USERNAME_REGEX.matcher(username);
        return matcher.find();
    }

    public static boolean validatePassword(String password) {
        Matcher matcher = VALID_PASSWORD_REGEX.matcher(password);
        return matcher.find();
    }


}//end of class
