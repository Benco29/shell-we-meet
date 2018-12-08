package benco.shellwemeet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.DataQueryBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import benco.shellwemeet.adapters.UserAdapter;
import benco.shellwemeet.menu.About;
import benco.shellwemeet.menu.Logout;
import benco.shellwemeet.menu.MyMatches;
import benco.shellwemeet.menu.MyProfile;
import benco.shellwemeet.menu.Statistics;
import benco.shellwemeet.model.UserProfile;
import benco.shellwemeet.utils.Constants;
import benco.shellwemeet.utils.DBUtil;
import benco.shellwemeet.utils.FileConn;
import benco.shellwemeet.utils.LoaderDialog;

@SuppressLint("StaticFieldLeak")
public class MatchActivity extends AppCompatActivity {

    final String TAG = "MatchActivity";

    Context context;

    RecyclerView rvMatches;
    UserAdapter adapter;
    DBUtil database;
    List<BackendlessUser> bUsersList = new ArrayList<>();
    List<UserProfile> usersList = new ArrayList<>();

    LoaderDialog loader;
    FileConn fileConn;
    String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match);

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

        switch(item.getItemId()) {

            case R.id.stats:
                Intent intent1 = new Intent(this, Statistics.class);
                startActivity(intent1);
                return true;

            case R.id.about:
                Intent intent2 = new Intent(this, About.class);
                startActivity(intent2);
                return true;

            case R.id.logout:
                Intent intent3 = new Intent(this, Logout.class);
                startActivity(intent3);
                return true;

            case R.id.myMatches:
                Intent intent4 = new Intent(this, MyMatches.class);
                startActivity(intent4);
                return true;

            case R.id.myProf:
                Intent intent5 = new Intent(this, MyProfile.class);
                Intent prevIntent = getIntent();
                String username = prevIntent.getStringExtra("username");
                intent5.putExtra("username", username);
                startActivity(intent5);
                return true;

            default:
                return onOptionsItemSelected(item);

        }

    }

    private void setPointer() {

        context = this;
        database = new DBUtil(context);
        fileConn = new FileConn(context);
        rvMatches = findViewById(R.id.rvMatches);

        String msg = "loading...";
        loader = new LoaderDialog(context, msg);
        loader.showAlertDialogLoader(true);

        String fileRead;
        String[] fileReadArray;
        try {
            fileRead = fileConn.read();
            fileReadArray = fileRead.split("#");
            currentUsername = FileConn.getUsernameFromStringArray(fileReadArray);
        } catch (IOException exception) {
            exception.printStackTrace();
            Log.e(TAG, "setPointer: " + exception);
        }

        //gets the users that "matches" your profile
        AsyncCallback<List<BackendlessUser>> backendlessCallback = new AsyncCallback<List<BackendlessUser>>() {
            @Override
            public void handleResponse(List<BackendlessUser> response) {
                Log.i(TAG, "handleResponse: "+response.toString());
                bUsersList = response;
                database.publicUsersList = UserProfile.convertBackendlessList(response);
                usersList = database.publicUsersList;

                UserProfile myProfile = getMyProfileFromTheList(currentUsername, usersList);
                usersList.remove(myProfile);

                adapter = new UserAdapter(context, usersList);
                rvMatches.setAdapter(adapter);
                rvMatches.setLayoutManager(new GridLayoutManager(context, Constants.COLUMN_NUMBER/*, GridLayout.VERTICAL, false*/));
                LoaderDialog.stopAlertDialog(loader);
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Toast.makeText(context, "Error" + fault.getMessage(), Toast.LENGTH_SHORT).show();
                Log.i(TAG, "handleFault: "+fault.toString());
                LoaderDialog.stopAlertDialog(loader);
            }
        };
        int PAGESIZE = 80;
        DataQueryBuilder builder = DataQueryBuilder.create();
        builder.setPageSize(PAGESIZE);
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setPageSize(PAGESIZE);
        Backendless.Data.of(BackendlessUser.class).find( builder, backendlessCallback);

                /** old one */

//        Backendless.Persistence.of(BackendlessUser.class).find(new AsyncCallback<List<BackendlessUser>>() {
//            @Override
//            public void handleResponse(List<BackendlessUser> response) {
//
//                Log.i(TAG, "handleResponse: "+response.toString());
//                bUsersList = response;
//                database.publicUsersList = UserProfile.convertBackendlessList(response);
//                usersList = database.publicUsersList;
//                adapter = new UserAdapter(context, usersList);
//                rvMatches.setAdapter(adapter);
//                rvMatches.setLayoutManager(new GridLayoutManager(context, Constants.COLUMN_NUMBER/*, GridLayout.VERTICAL, false*/));
//                showProgress(false);
//            }
//
//            @Override
//            public void handleFault(BackendlessFault fault) {
//                Toast.makeText(context, "Error" + fault.getMessage(), Toast.LENGTH_SHORT).show();
//                Log.i(TAG, "handleFault: "+fault.toString());
//                showProgress(false);
//            }
//        });
    }

    private UserProfile getMyProfileFromTheList(String currentUsername, List<UserProfile> usersList) {

        for (UserProfile user : usersList) {
            if (user.getUsername().equals(currentUsername)) {
                return user;
            }
        }
        Log.e(TAG, "getMyProfileFromTheList: user wasn't on the list...");
        return new UserProfile();
    }


}//end of class
