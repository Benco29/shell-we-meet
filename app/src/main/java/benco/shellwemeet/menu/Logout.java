package benco.shellwemeet.menu;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;

import java.io.IOException;

import benco.shellwemeet.LoginActivity;
import benco.shellwemeet.R;
import benco.shellwemeet.utils.Constants;
import benco.shellwemeet.utils.DBUtil;
import benco.shellwemeet.utils.FileConn;

public class Logout extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_item_logout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Context context = this;
        DBUtil database = new DBUtil(context);
        FileConn fileConn = new FileConn(context);

        String username = "";

        try {//getting the username from file
            String userDetails = fileConn.read();
            String[] userArray = userDetails.split(Constants.DELIMITER);
            username = FileConn.getUsernameFromStringArray(userArray);

        } catch (IOException e) {
            e.printStackTrace();
        }
        //setting the online property to false in the backendless database
        database.setUserLoginOffline(username);

        if (database.isUserLoggedIn()) { //logging the user out
            database.logoutUser(username);
        }

        try {//erasing the file with the users data
            fileConn.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Intent loginIntent = new Intent(context, LoginActivity.class);
        startActivity(loginIntent);

    }
}
