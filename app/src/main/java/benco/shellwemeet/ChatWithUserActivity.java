package benco.shellwemeet;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.messaging.MessageStatus;
import com.backendless.messaging.PublishMessageInfo;
import com.backendless.messaging.PublishOptions;
import com.backendless.persistence.DataQueryBuilder;
import com.backendless.rt.messaging.Channel;
import com.backendless.rt.messaging.MessageInfoCallback;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import benco.shellwemeet.adapters.MsgAdapter;
import benco.shellwemeet.menu.About;
import benco.shellwemeet.menu.Logout;
import benco.shellwemeet.menu.MyProfile;
import benco.shellwemeet.menu.Statistics;
import benco.shellwemeet.utils.DBUtil;


public class ChatWithUserActivity extends AppCompatActivity {

    private final String TAG = "ChatWithUserActivity";
    
    Context context;
    Intent prevIntent;
    EditText sentMsg;
    ImageView profileImg, sendImgBtn;
    TextView username, isOnlineTxt;

    RecyclerView mainMsging;

    View progressBar, chatFormView;
    TextView tvLoad;

    MsgAdapter adapter;
    List<PublishMessageInfo> msgList;

    DBUtil database;

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
        setContentView(R.layout.activity_chat_with_user);
        
        setPointer();
        
    }

    private void setPointer() {

        context = this;
        //the text we are sending to the user
        sentMsg = findViewById(R.id.chatActivityEditText);
        //the send button
        sendImgBtn = findViewById(R.id.chatActivitySendBtn);

        profileImg = findViewById(R.id.chatProfileImage);
        username = findViewById(R.id.chatActivityUsername);
        isOnlineTxt = findViewById(R.id.chatActivityIsOnlineTxt);
        //the scrollView layout
        mainMsging = findViewById(R.id.chatActivityRecyclerView);

        progressBar = findViewById(R.id.chatWithUserProgressBar);
        chatFormView = findViewById(R.id.chatWithUserForm);
        tvLoad = findViewById(R.id.chatWithUserTvLoad);

        prevIntent = getIntent();
        final String currentUsername = prevIntent.getStringExtra("username");

        msgList = new ArrayList<>();
        adapter = new MsgAdapter(context, msgList);
        mainMsging.setAdapter(adapter);
        mainMsging.setLayoutManager(new LinearLayoutManager(context));

        database = new DBUtil(context);

        //receiving text messages
        Channel channel = Backendless.Messaging.subscribe( "default" );
        channel.addMessageListener(new MessageInfoCallback() {
            @Override
            public void handleResponse(PublishMessageInfo response) {
                Log.i("MYAPP", "Received string message " + response);
                msgList.add(response);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.e( "MYAPP", "Error processing a message " + fault );
            }
        });
//        channel.addMessageListener( new AsyncCallback<Message>()
//        {
//            @Override
//            public void handleResponse( Message message ) {
//                Log.i("MYAPP", "Received string message " + message);
//
//                msgList.add(message);
//                adapter.notifyDataSetChanged();
//            }
//
//            @Override
//            public void handleFault( BackendlessFault fault ) {
//                Log.e( "MYAPP", "Error processing a message " + fault );
//            }
//        }, Message.class );

        showProgress(true);
        // get the user by its name
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
                if (user.getProperty("profileImgThumbnail") == null || user.getProperty("profileImgThumbnail").equals("")) {
                    String imageUri = database.getURLForResource(R.drawable.profile);
                    Uri uri = Uri.parse(imageUri);
                    profileImg.setImageURI(uri);
                } else {
                    //download thumb of profile picture and set the ImageView profile picture
                    String thumbnailStr = user.getProperty("profileImgThumbnail").toString();
                    new DownloadImageTask(profileImg).execute(thumbnailStr);
                }
//                profileImg.setImageURI(imgUri);
                username.setText((String)user.getProperty("name"));
                isOnlineTxt.setText((Boolean)user.getProperty("isOnline")? "online":"offline");
                showProgress(false);

            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Toast.makeText(context, "Error: "+fault.getMessage(), Toast.LENGTH_SHORT).show();
                Log.i(TAG, "handleFault: "+fault.toString());
                showProgress(false);

            }
        });

        sendImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //sending a message to the user, and displaying it on screen
                final String enteredTxt = sentMsg.getText().toString().trim();
                PublishOptions publishOptions = new PublishOptions(Backendless.UserService.loggedInUser());

                sentMsg.setText("");

                Backendless.Messaging.publish( "default",
                        enteredTxt, publishOptions,
                        new AsyncCallback<MessageStatus>()
                        {
                            @Override
                            public void handleResponse( MessageStatus response ) {

                                Log.i( "MYAPP", "Message has been published" );
                            }

                            @Override
                            public void handleFault( BackendlessFault fault ) {

                                Log.e( "MYAPP", "Error publishing the message" );
                            }
                        });
            }
        });



    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        //on HONEYCOMB_MR2 we have the ViewPropertyAnimator APIs, which allows
        //for very easy animations. If available, use these APIs to fade-in
        //the progress spinner
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            chatFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            chatFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    chatFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            progressBar.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });

            tvLoad.setVisibility(show ? View.VISIBLE : View.GONE);
            tvLoad.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    tvLoad.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            tvLoad.setVisibility(show ? View.VISIBLE : View.GONE);
            chatFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }

    }


    /**
     * an AsyncTask to download an image from string url
     */
    public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
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


}
