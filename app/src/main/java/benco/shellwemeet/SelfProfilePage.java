package benco.shellwemeet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.system.ErrnoException;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.DataQueryBuilder;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import benco.shellwemeet.menu.About;
import benco.shellwemeet.menu.Logout;
import benco.shellwemeet.menu.MyMatches;
import benco.shellwemeet.menu.MyProfile;
import benco.shellwemeet.menu.Statistics;
import benco.shellwemeet.utils.Constants;
import benco.shellwemeet.utils.DBUtil;
import benco.shellwemeet.utils.DateValidator;
import benco.shellwemeet.utils.FileConn;
import benco.shellwemeet.utils.LoaderDialog;

public class SelfProfilePage extends AppCompatActivity {

    private final String TAG = "SelfProfilePage";

    Context context;
    //ui elements
    FloatingActionButton saveBtn, enlargeImgBtn, cropBtn;
    TextView myUsername, myAge, myLocation, myEmail, myDOB, myGender, myInterests, myDescription;
    CropImageView myProfileImg, dialogImg;

    LinearLayout selfProfileForm; // our linear layout that contains all ui elements
    LoaderDialog loader; //the dialog loading while getting the details from the Backendless database

    DBUtil database;
    FileConn fileConn;
    Bitmap myBitmap;

    private Uri mCropImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_profile_page);

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

    private void setPointer() {

        context = this;
        database = new DBUtil(context);
        fileConn = new FileConn(context);

        dialogImg = new CropImageView(context);
        selfProfileForm = findViewById(R.id.selfProfileForm);
        selfProfileForm.setVisibility(View.GONE);//setting the layout visibility to gone, until loading is finished
        cropBtn = findViewById(R.id.fabBtnCropImgProfile);
        cropBtn.setEnabled(false);
        enlargeImgBtn = findViewById(R.id.fabBtnProfileImgEnlarge);

        String loaderMsg = "Loading, Please wait...";
        loader = new LoaderDialog(context, loaderMsg);

        saveBtn = findViewById(R.id.selfProfileFAB);
        myUsername = findViewById(R.id.myProfileUserName);
        myAge = findViewById(R.id.myProfileAge);
        myLocation = findViewById(R.id.myProfileLocation);
        myEmail = findViewById(R.id.myProfileEmail);
        myDOB = findViewById(R.id.myProfileBirthDate);
        myGender = findViewById(R.id.myProfileGender);
        myInterests = findViewById(R.id.myProfileInterestedIn);
        myDescription = findViewById(R.id.selfProfilePageDescription);
        myProfileImg = findViewById(R.id.myProfPageProfImg);

        Intent prevIntent = getIntent();
        final String currentUsername = prevIntent.getStringExtra("username");

        loader.showAlertDialogLoader(true);

        if (currentUsername == null || currentUsername.isEmpty()) {

            String fileRead;
            String[] fileReadArray;
            String currUsername = "";
            try {
                fileRead = fileConn.read();
                fileReadArray = fileRead.split("#");
                currUsername = FileConn.getUsernameFromStringArray(fileReadArray);
            } catch (IOException exception) {
                exception.printStackTrace();
                Log.e(TAG, "setPointer: " + exception);
            }

            DataQueryBuilder builder = DataQueryBuilder.create();
            builder.setWhereClause("name='" + currUsername + "'");

            Backendless.Persistence.of(BackendlessUser.class).find(builder, new AsyncCallback<List<BackendlessUser>>() {
                @Override
                public void handleResponse(List<BackendlessUser> response) {
                    if (response == null || response.isEmpty()) {
                        Toast.makeText(context, "User was not found...", Toast.LENGTH_LONG).show();
                        Intent matchesIntent = new Intent(context, MatchActivity.class);
                        startActivity(matchesIntent);
                        return;
                    }
                    BackendlessUser user = response.get(0); //get the found user at index 0

                    myUsername.setText(user.getProperty("name").toString());
                    myAge.setText(user.getProperty("age").toString());
                    //getting the location property
                    String loc = user.getProperty("location").toString();
                    String newLoc = DateValidator.getNestedString(loc,"metadata={",",");
                    myLocation.setText(user.getProperty("location") == null ? "Unknown" : newLoc);

                    myEmail.setText(user.getEmail());
                    myDOB.setText(user.getProperty("birthDate").toString());
                    myGender.setText(user.getProperty("gender").toString());
                    myInterests.setText(user.getProperty("interestedIn").toString());
                    myDescription.setText(user.getProperty("description").toString());
                    //myProfileImg.setImageURI(Uri.parse(user.getProperty("profileImg").toString()));
                    if (user.getProperty("profileImgThumbnail") == null || user.getProperty("profileImgThumbnail").equals("")) {
                        int imgRes = R.drawable.profile;
                        myProfileImg.setImageResource(imgRes);
                        dialogImg.setImageResource(imgRes);//the image that will be presented in the dialog upon clicking the profile image view

                    } else {
                        //download thumb of profile picture and set the ImageView profile picture
                        String thumbnailStr = user.getProperty("profileImgThumbnail").toString();

                        new DownloadImageTask(myProfileImg).execute(thumbnailStr);
                        new DownloadImageTask(dialogImg).execute(thumbnailStr);
                    }
                    selfProfileForm.setVisibility(View.VISIBLE);
                    LoaderDialog.stopAlertDialog(loader);
                }

                @Override
                public void handleFault(BackendlessFault fault) {
                    Log.e(TAG, "handleFault: " + fault.toString());
                    Toast.makeText(context, "Error: " + fault.getMessage(), Toast.LENGTH_LONG).show();
                    LoaderDialog.stopAlertDialog(loader);
                    selfProfileForm.setVisibility(View.VISIBLE);
                }
            });

        } else {
            DataQueryBuilder builder = DataQueryBuilder.create();
            builder.setWhereClause("name='" + currentUsername + "'");

            //**gets the user and update the ui*/
            Backendless.Persistence.of(BackendlessUser.class).find(builder, new AsyncCallback<List<BackendlessUser>>() {
                @Override
                public void handleResponse(List<BackendlessUser> response) {
                    if (response == null || response.isEmpty()) {
                        Toast.makeText(context, "NO GOOOOOOOOOD", Toast.LENGTH_LONG).show();
                        return;
                    }
                    BackendlessUser user = response.get(0);

                    myUsername.setText(user.getProperty("name").toString());
                    myAge.setText(user.getProperty("age").toString());
                    //getting the location property
                    String loc = user.getProperty("location").toString();
                    String newLoc = DateValidator.getNestedString(loc,"metadata={","}");
                    myLocation.setText(user.getProperty("location") == null ? "Unknown" : newLoc);
                    myEmail.setText(user.getEmail());
                    myDOB.setText(user.getProperty("birthDate").toString());
                    myGender.setText(user.getProperty("gender").toString());
                    myInterests.setText(user.getProperty("interestedIn").toString());
                    myDescription.setText(user.getProperty("description").toString());
                    //myProfileImg.setImageURI(Uri.parse(user.getProperty("profileImg").toString()));
                    if (user.getProperty("profileImgThumbnail") == null || user.getProperty("profileImgThumbnail").equals("")) {
                        int imgRes = R.drawable.profile;
                        myProfileImg.setImageResource(imgRes);
                        dialogImg.setImageResource(imgRes);
                        //dialogImg.setImageURI(uri);//the image that will be presented in the dialog upon clicking the profile image view
                    } else {
                        //download thumb of profile picture and set the ImageView profile picture
                        String thumbnailStr = user.getProperty("profileImgThumbnail").toString();
                        new DownloadImageTask(myProfileImg).execute(thumbnailStr);
                        new DownloadImageTask(dialogImg).execute(thumbnailStr);
                    }
                    LoaderDialog.stopAlertDialog(loader);
                    selfProfileForm.setVisibility(View.VISIBLE);
                }

                @Override
                public void handleFault(BackendlessFault fault) {
                    Log.e(TAG, "handleFault: " + fault.toString());
                    Toast.makeText(context, "Error: " + fault.getMessage(), Toast.LENGTH_LONG).show();
                    LoaderDialog.stopAlertDialog(loader);
                    selfProfileForm.setVisibility(View.VISIBLE);
                }
            });
        }
        //when the user clicks on the save button we uploade all the new information to backendless
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try { //uploading the image to the database
                    database.uploadPhotoThumbToDatabase(currentUsername, myBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                loader.setAlertMessage("Updating your details..");
                LoaderDialog.showAlertAgain(loader);

                final String updatedUsername = myUsername.getText().toString().trim();
                final String updatedEmail = myEmail.getText().toString().trim();
                final String updatedDOB = myDOB.getText().toString().trim();
                final int updatedAge = Integer.parseInt(myAge.getText().toString().trim());
                final String updatedGender = myGender.getText().toString().trim();
                final String updatedInterests = myInterests.getText().toString().trim();
                final String updatedDescription = myDescription.getText().toString().trim();

                //getting the current user
                DataQueryBuilder builder = DataQueryBuilder.create();
                builder.setWhereClause("name='" + currentUsername + "'");
                Backendless.Persistence.of(BackendlessUser.class).find(builder, new AsyncCallback<List<BackendlessUser>>() {
                    @Override
                    public void handleResponse(List<BackendlessUser> response) {
                        if (response == null || response.isEmpty()) {
                            Toast.makeText(context, "NO GOOOOOOOOOD", Toast.LENGTH_LONG).show();
                            return;
                        }
                        //updating the user with the changed values
                        BackendlessUser user = response.get(0);
                        user.setProperty("name", updatedUsername);
                        user.setProperty("email", updatedEmail);
                        user.setProperty("birthDate", updatedDOB);
                        user.setProperty("gender", updatedGender);
                        user.setProperty("interestedIn", updatedInterests);
                        user.setProperty("description", updatedDescription);
                        user.setProperty("age", updatedAge);

                        Backendless.UserService.update(user, new AsyncCallback<BackendlessUser>() {
                            @Override
                            public void handleResponse(BackendlessUser response) {
                                Toast.makeText(SelfProfilePage.this, "Details updated successfully!", Toast.LENGTH_LONG).show();
                                LoaderDialog.stopAlertDialog(loader);
                            }

                            @Override
                            public void handleFault(BackendlessFault fault) {
                                Log.e(TAG, "handleFault: " + fault.toString());
                                Toast.makeText(context, "Error: " + fault.getMessage(), Toast.LENGTH_LONG).show();
                                LoaderDialog.stopAlertDialog(loader);
                            }
                        });

                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        Log.e(TAG, "handleFault: " + fault.toString());
                        Toast.makeText(context, "Error: " + fault.getMessage(), Toast.LENGTH_LONG).show();
                        LoaderDialog.stopAlertDialog(loader);
                    }
                });
            }
        });

        myUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog(Constants.USERNAME_ID);
            }
        });

        myEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog(Constants.EMAIL_ID);
            }
        });

        myDOB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog(Constants.DOB_ID);
            }
        });

        myGender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog(Constants.GENDER_ID);
            }
        });

        myInterests.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog(Constants.INTERESTED_IN_ID);
            }
        });

        myDescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog(Constants.DESCRIPTION_ID);
            }
        });

        enlargeImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickerAlertDialog(dialogImg, currentUsername);
            }
        });

    }

    public void onCropImageClick(View view) {
        myBitmap = myProfileImg.getCroppedImage(500, 500);
        //if (cropped != null)
        if (myBitmap != null) {
            myProfileImg.setImageBitmap(myBitmap);
        }
    }

    public void onLoadNewImageClick(View view) {
        startActivityForResult(getPickImageChooserIntent(), 200);
        cropBtn.setEnabled(true);
    }


    private void showImagePickerAlertDialog(CropImageView imageView, String username) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(username + " Profile Picture");

        if (imageView.getParent() != null) {
            ((ViewGroup) imageView.getParent()).removeView(imageView); // <- fix
        }
        builder.setView(imageView);

        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog imageDialog = builder.create();

        imageDialog.show();

    }

    public void showAlertDialog(final int id) {
        //an alert dialog to edit the users details on clicking
        View alertView = LayoutInflater.from(context).inflate(R.layout.alert_dialog_cell, null);
        Button btnOk = alertView.findViewById(R.id.cellAlertOkBtn);
        Button btnCancel = alertView.findViewById(R.id.cellAlertCancelBtn);
        TextView alertMessageTxt = alertView.findViewById(R.id.cellAlertTxtBody);
        final EditText inputValue = alertView.findViewById(R.id.cellAlertEditText);

        //setting the message of the alert, according to the id :
        switch (id) {
            case Constants.USERNAME_ID:
                alertMessageTxt.setText(R.string.usernameAlertTxtMsg);
                break;
            case Constants.EMAIL_ID:
                alertMessageTxt.setText(R.string.emailAlertTxtMsg);
                inputValue.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                break;
            case Constants.DOB_ID:
                alertMessageTxt.setText(R.string.dobAlertTxtMsg);
                inputValue.setInputType(InputType.TYPE_DATETIME_VARIATION_DATE);
                break;
            case Constants.GENDER_ID:
                alertMessageTxt.setText(R.string.genderAlertTxtMsg);
                break;
            case Constants.INTERESTED_IN_ID:
                alertMessageTxt.setText(R.string.interestedAlertTxtMsg);
                break;
            case Constants.DESCRIPTION_ID:
                alertMessageTxt.setText(R.string.descAlertTxtMsg);
                break;
            default:
                alertMessageTxt.setText(R.string.alertTxtMsgDefault);
                break;
        }


        final AlertDialog alert = new AlertDialog.Builder(context).create();

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = inputValue.getText().toString().trim();
                //once clicking ok - we know which field to update according to the id
                switch (id) {
                    case Constants.USERNAME_ID:
                        myUsername.setText(value);
                        break;
                    case Constants.EMAIL_ID:
                        myEmail.setText(value);
                        break;
                    case Constants.DOB_ID:
                        DateValidator validator = new DateValidator();
                        if (!validator.isDateValid(value, Constants.SLASH_DATE_FORMAT) && !validator.isDateValid(value, Constants.DOT_DATE_FORMAT)) {
                            Toast.makeText(context, "Invalid date, please enter a correct birth date.", Toast.LENGTH_LONG).show();
                            inputValue.setText("");
                            return;
                        } else {
                            myDOB.setText(value);
                            int ageInt = validator.ageCalculator(myDOB.getText().toString().trim());
                            String age = ageInt +"";
                            myAge.setText(age);
                            alert.dismiss();
                        }
                        break;
                    case Constants.GENDER_ID:
                        myGender.setText(value);
                        break;
                    case Constants.INTERESTED_IN_ID:
                        myInterests.setText(value);
                        break;
                    case Constants.DESCRIPTION_ID:
                        myDescription.setText(value);
                        break;
                }

                alert.dismiss();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert.dismiss();
            }
        });

        alert.setView(alertView);
        alert.setCancelable(false);
        alert.show();

    }

    public String getURLForResource(int resourceId) {
        return Uri.parse("android.resource://" + R.class.getPackage().getName() + "/" + resourceId).toString();
    }

    /**
     * an AsyncTask to download an image from string url
     */
    @SuppressLint("StaticFieldLeak")
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        CropImageView bmImage;

        public DownloadImageTask(CropImageView bmImage) {
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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri imageUri = getPickImageResultUri(data);
            // For API >= 23 we need to check specifically that we have permissions to read external storage,
            // but we don't know if we need to for the URI so the simplest is to try open the stream and see if we get error.
            boolean requirePermissions = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                    isUriRequiresPermissions(imageUri)) {

                // request permissions and handle the result in onRequestPermissionsResult()
                requirePermissions = true;
                mCropImageUri = imageUri;

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }

            if (!requirePermissions) {
                myProfileImg.setImageUriAsync(imageUri);

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (mCropImageUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            myProfileImg.setImageUriAsync(mCropImageUri);
        } else {
            Toast.makeText(this, "Required permissions are not granted", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Create a chooser intent to select the  source to get image from.<br/>
     * The source can be camera's  (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br/>
     * All possible sources are added to the  intent chooser.
     */
    public Intent getPickImageChooserIntent() {

        // Determine Uri of camera image to  save.
        Uri outputFileUri = getCaptureImageOutputUri();

        List<Intent> allIntents = new ArrayList();
        PackageManager packageManager = getPackageManager();

        // collect all camera intents
        Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            }
            allIntents.add(intent);
        }

        // collect all gallery intents
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            allIntents.add(intent);
        }

        // the main intent is the last in the  list (fucking android) so pickup the useless one
        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (intent.getComponent().getClassName().equals("com.android.documentsui.DocumentsActivity")) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);

        // Create a chooser from the main  intent
        Intent chooserIntent = Intent.createChooser(mainIntent, "Select source");

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;
    }

    /**
     * Get URI to image received from capture  by camera.
     */
    private Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = getExternalCacheDir();
        if (getImage != null) {
            outputFileUri = Uri.fromFile(new File(getImage.getPath(), "pickImageResult.jpeg"));
        }
        return outputFileUri;
    }

    /**
     * Get the URI of the selected image from  {@link #getPickImageChooserIntent()}.<br/>
     * Will return the correct URI for camera  and gallery image.
     *
     * @param data the returned data of the  activity result
     */
    public Uri getPickImageResultUri(Intent data) {
        boolean isCamera = true;
        if (data != null && data.getData() != null) {
            String action = data.getAction();
            isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
        }
        return isCamera ? getCaptureImageOutputUri() : data.getData();
    }

    /**
     * Test if we can open the given Android URI to test if permission required error is thrown.<br>
     */
    public boolean isUriRequiresPermissions(Uri uri) {
        try {
            ContentResolver resolver = getContentResolver();
            InputStream stream = resolver.openInputStream(uri);
            stream.close();
            return false;
        } catch (FileNotFoundException e) {
            if (e.getCause() instanceof ErrnoException) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "isUriRequiresPermissions: "+e.getMessage());
        }
        return false;
    }

}
