package benco.shellwemeet.menu;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;

import benco.shellwemeet.R;
import benco.shellwemeet.SelfProfilePage;
import benco.shellwemeet.utils.FileConn;

public class MyProfile extends AppCompatActivity {

    private static final String TAG = "MyProfile";

    Context context;
    FileConn fileConn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_item_my_profile);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        context = this;
        fileConn = new FileConn(context);

        //this returns null
//        BackendlessUser bUser = Backendless.UserService.CurrentUser();
//        Log.e(TAG, "onCreate: " +bUser );


        //getting the username from the file
        String fileRead;
        String[] fileReadArray;
        String usernameFromFile = "";
        try {
            fileRead = fileConn.read();
            fileReadArray = fileRead.split("#");
            usernameFromFile = FileConn.getUsernameFromStringArray(fileReadArray);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "onCreate: " +e);
        }

        Log.i(TAG, "onCreate: "+usernameFromFile);

        Intent myProfileIntent = new Intent(context, SelfProfilePage.class);
        myProfileIntent.putExtra("username", usernameFromFile);
        startActivity(myProfileIntent);
    }
}