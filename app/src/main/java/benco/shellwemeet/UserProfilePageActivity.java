package benco.shellwemeet;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.DataQueryBuilder;

import java.io.InputStream;
import java.util.List;

import benco.shellwemeet.menu.About;
import benco.shellwemeet.menu.Logout;
import benco.shellwemeet.menu.MyProfile;
import benco.shellwemeet.menu.Statistics;
import benco.shellwemeet.utils.DBUtil;
import benco.shellwemeet.utils.DateValidator;
import benco.shellwemeet.utils.LoaderDialog;

public class UserProfilePageActivity extends AppCompatActivity {

    private final String TAG = "UserProfilePageActivity";

    Context context;
    ImageView profileImg, dialogImg;
    TextView usernameTxt, isOnlineTxt, locationTxt, descriptionTxt;
    Button chatBtn;
    LoaderDialog loader;
    View chatView;
    DBUtil database;

    Intent prevIntent;

    //menu options intents
    @Override
    public boolean onCreateOptionsMenu(Menu menu) { // creating a menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { // menu's options

        Intent prevIntent = getIntent();
        String username = prevIntent.getStringExtra("username");

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
                Intent intent3 = new Intent(this, Logout.class);
                startActivity(intent3);
                return true;

            case R.id.myMatches:
                Intent intent4 = new Intent(this, MatchActivity.class);
                intent4.putExtra("username", username);
                startActivity(intent4);
                return true;

            case R.id.myProf:
                Intent intent5 = new Intent(this, MyProfile.class);
                intent5.putExtra("username", username);
                startActivity(intent5);
                return true;

            default:
                return onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile_page);

        setPointer();
    }


    public void setPointer(){

        context = this;

        database = new DBUtil(context);
        String loaderMsg = "Loading user info..";
        loader = new LoaderDialog(context, loaderMsg);

        chatView = findViewById(R.id.profilePageActivityForm);

        //the name of the user we clicked on at the MatchesActivity
        prevIntent = getIntent();
        final String currentUsername = prevIntent.getStringExtra("userName");

        dialogImg = new ImageView(context);

        chatBtn = findViewById(R.id.profPageBtn);
        chatBtn.setText("Send "+currentUsername+" a message");

        profileImg = findViewById(R.id.profPageProfImg);
        usernameTxt = findViewById(R.id.profilePageUserName);
        isOnlineTxt = findViewById(R.id.profilePageIsOnline);
        locationTxt = findViewById(R.id.profilePageLocation);
        descriptionTxt = findViewById(R.id.profilePageDescription);

        loader.showAlertDialogLoader(true);
        chatView.setVisibility(View.GONE);
        // get the user by its name from backendless
        DataQueryBuilder builder = DataQueryBuilder.create();
        builder.setWhereClause("name='"+currentUsername+"'");
        Backendless.Persistence.of(BackendlessUser.class).find(builder, new AsyncCallback<List<BackendlessUser>>() {
            @Override
            public void handleResponse(List<BackendlessUser> response) {
                if(response == null || response.isEmpty()){
                    Toast.makeText(context, "NO GOOOOOOOOOD", Toast.LENGTH_LONG).show();
                    return;
                }
                BackendlessUser user = response.get(0);
                usernameTxt.setText((String)user.getProperty("name"));
                isOnlineTxt.setText((Boolean)user.getProperty("isOnline")? "online": "offline");
                //getting the location property
                String loc = user.getProperty("location") == null ? "Unknown" : user.getProperty("location").toString();
                String newLoc = DateValidator.getNestedString(loc,"metadata={","}");
                locationTxt.setText(loc.equals("Unknown") ? "Unknown" : newLoc);
                descriptionTxt.setText((String)user.getProperty("description"));
                if (user.getProperty("profileImgThumbnail") == null || user.getProperty("profileImgThumbnail").equals("")) {
                    String imageUri = getURLForResource(R.drawable.profile);
                    Uri uri = Uri.parse(imageUri);
                    profileImg.setImageURI(uri);
                    dialogImg.setImageURI(uri);//the image that will be presented in the dialog upon clicking the profile image view
                } else {
                    //download thumb of profile picture and set the ImageView profile picture
                    String thumbnailStr = user.getProperty("profileImgThumbnail").toString();
                    new DownloadImageTask(profileImg).execute(thumbnailStr);
                    new DownloadImageTask(dialogImg).execute(thumbnailStr);

                }

                LoaderDialog.stopAlertDialog(loader);
                chatView.setVisibility(View.VISIBLE);
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Toast.makeText(context, "Error: "+fault.getMessage(), Toast.LENGTH_SHORT).show();
                Log.i(TAG, "handleFault: "+fault.toString());
                LoaderDialog.stopAlertDialog(loader);
                chatView.setVisibility(View.VISIBLE);
            }
        });


        profileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageViewDialog(dialogImg, currentUsername);
            }
        });

        chatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent toChatIntent = new Intent(context, ChatWithUserActivity.class);
                toChatIntent.putExtra("username", currentUsername);
                startActivity(toChatIntent);
            }
        });


    }

    public String getURLForResource (int resourceId) {
        return Uri.parse("android.resource://"+R.class.getPackage().getName()+"/" +resourceId).toString();
    }

    /**
     * an AsyncTask to download an image from string url
     */
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    private void showImageViewDialog(ImageView imageView, String username) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(username+" Profile Picture");

        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        if(imageView.getParent() != null) {
            ((ViewGroup) imageView.getParent()).removeView(imageView); // <- fix
        }
        builder.setView(imageView);

        AlertDialog imageDialog = builder.create();
        imageDialog.show();
    }


}
