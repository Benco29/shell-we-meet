package benco.shellwemeet;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.system.ErrnoException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import benco.shellwemeet.utils.Constants;
import benco.shellwemeet.utils.DBUtil;

public class AddProfileActivity extends AppCompatActivity {

    private static final String TAG = "AddProfileActivity";

        private CropImageView cropImageView;
        private Uri mCropImageUri;
        Bitmap myBitmap;
        DBUtil database;
        Context context;
        Intent prevIntent;
        String currentUsername = "";
        Button continueBtn;
        FloatingActionButton cropBtn;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_add_profile);

            context = this;
            cropBtn = findViewById(R.id.fabBtnCropImg);
            cropBtn.setEnabled(false);//setting the
            continueBtn = findViewById(R.id.addProfContinueBtn);

            database = new DBUtil(context);
            cropImageView = findViewById(R.id.profImgView);
            prevIntent = getIntent();

            currentUsername = prevIntent.getStringExtra("username");

            checkForPermissions();

        }

        /**
         * On load image button click, start pick  image chooser activity.
         */
        public void onLoadImageClick(View view) {
            startActivityForResult(getPickImageChooserIntent(), 200);
            cropBtn.setEnabled(true);
            continueBtn.setEnabled(true);
        }

        /**
         * Crop the image and set it back to the  cropping view.
         */
        public void onCropImageClick(View view) {
            //Bitmap cropped =  mCropImageView.getCroppedImage(500, 500);
            myBitmap = cropImageView.getCroppedImage(500, 500);
            //if (cropped != null)
            if (myBitmap != null)
                cropImageView.setImageBitmap(myBitmap);
            //mCropImageView.setImageBitmap(cropped);
        }

        public void onContinueBtnClick(View view) {
            /*upload image to database and continue*/

            try {
                database.uploadPhotoThumbToDatabase(currentUsername, myBitmap);
            } catch (Exception exception) {
                exception.printStackTrace();
                Log.e(TAG, "onContinueBtnClick: "+exception );
                Toast.makeText(context, "Error: could not upload image. Please try again", Toast.LENGTH_LONG).show();
                return;
            }

            Intent toMatchesIntent = new Intent(context, MatchActivity.class);
            String username = prevIntent.getStringExtra("username");
            toMatchesIntent.putExtra("username", username);
            database.setUserLoginOnline(username);
            startActivity(toMatchesIntent);

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
                    cropImageView.setImageUriAsync(imageUri);
                }
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
            if (mCropImageUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cropImageView.setImageUriAsync(mCropImageUri);
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

            List<Intent> allIntents = new ArrayList<>();
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
            }
            return false;
        }


        private void checkForPermissions() {

            Log.i(TAG, "checkForPermission: started");
            //creating a list that holds all the permissions we need to ask
            List<String> permList = new ArrayList<>();

            //checking if we already have permission to use READ_EXTERNAL_STORAGE, if not then add it to the list of permissions to ask for
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            //checking if we already have permission to use WRITE_EXTERNAL_STORAGE, if not then add it to the list of permissions to ask for
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            //checking if we already have permission to use CAMERA, if not then add it to the list of permissions to ask for
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permList.add(Manifest.permission.CAMERA);
            }
            //if permList isn't empty then we have at least one permission to ask for
            if (!permList.isEmpty()) {
                //asks for permissions for all the items on the list
                ActivityCompat.requestPermissions(this, permList.toArray(new String[permList.size()]), Constants.READ_WRITE_PERM_REQUEST_CODE);
            } else {//we already have all the permissions we need
                Log.i(TAG, "checkForPermissions: we have all permissions needed");
            }
            Log.i(TAG, "checkForPermission: ended");
        }



}//end of class
