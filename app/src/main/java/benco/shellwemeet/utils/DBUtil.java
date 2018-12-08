package benco.shellwemeet.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.IDataStore;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.files.BackendlessFile;
import com.backendless.geo.GeoPoint;
import com.backendless.persistence.DataQueryBuilder;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import benco.shellwemeet.AddLocationActivity;
import benco.shellwemeet.CompleteDetailsActivity;
import benco.shellwemeet.R;
import benco.shellwemeet.listeners.OnUserListLoadedListener;
import benco.shellwemeet.model.UserProfile;

public class DBUtil {

    private static final String TAG = "Backendless";
    public static String currentUserLogged = "";

    private Context context;
    public static List<UserProfile> publicUsersList;

    public DBUtil(Context context) {
        this.context = context;
        Backendless.setUrl(Constants.SERVER_URL);
        //initializing the the Backendless Application
        Backendless.initApp(context, Constants.APPLICATION_ID, Constants.API_KEY);

    }

    //checks if the username is already exist in the database
    public void doesExists(String username, AsyncCallback<List<BackendlessUser>> callback) {

        final String name = username;

        String whereClause = "name = '" + name + "'";
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause(whereClause);

        Backendless.Persistence.of(BackendlessUser.class).find(queryBuilder, callback);
    }

    public String getURLForResource(int resourceId) {
        return Uri.parse("android.resource://" + R.class.getPackage().getName() + "/" + resourceId).toString();
    }

    public void setUserLoginOnline(String username) {

        String whereClause = "name = '" + username + "'";
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause(whereClause);
        Backendless.Persistence.of(BackendlessUser.class).find(queryBuilder, new AsyncCallback<List<BackendlessUser>>() {
            @Override
            public void handleResponse(List<BackendlessUser> response) {
                if (response == null || response.isEmpty()) {
                    Log.e(TAG, "handleResponse: user could not be found ");
                    return;
                }
                BackendlessUser user = response.get(0);
                user.setProperty("isOnline", true);

                Backendless.UserService.update(user, new AsyncCallback<BackendlessUser>() {
                    @Override
                    public void handleResponse(BackendlessUser response) {
                        Log.i(TAG, "handleResponse: " + response.toString());
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        Log.e(TAG, "handleFault: " + fault);
                    }
                });
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.e(TAG, "handleFault: " + fault);
            }
        });

    }

    public void setUserLoginOffline(String username) {
        String whereClause = "name = '" + username + "'";
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause(whereClause);
        Backendless.Persistence.of(BackendlessUser.class).find(queryBuilder, new AsyncCallback<List<BackendlessUser>>() {
            @Override
            public void handleResponse(List<BackendlessUser> response) {
                if (response == null || response.isEmpty()) {
                    Log.e(TAG, "handleResponse: user could not be found ");
                    return;
                }
                BackendlessUser user = response.get(0);
                user.setProperty("isOnline", false);

                Backendless.UserService.update(user, new AsyncCallback<BackendlessUser>() {
                    @Override
                    public void handleResponse(BackendlessUser response) {
                        Log.i(TAG, "handleResponse: " + response.toString());
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        Log.e(TAG, "handleFault: " + fault);
                    }
                });
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.e(TAG, "handleFault: " + fault);
            }
        });

    }

    public void uploadGeoPointRelation(String currentUsername, final GeoPoint geoPoint) {

        String whereClause = "name = '" + currentUsername + "'";
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause(whereClause);
        //finding user in table
        Backendless.Persistence.of(BackendlessUser.class).find(queryBuilder, new AsyncCallback<List<BackendlessUser>>() {
            @Override
            public void handleResponse(List<BackendlessUser> response) {
                if (response == null || response.isEmpty()) {
                    Log.e(TAG, "handleResponse: user could not be found ");
                    return;
                }
                final BackendlessUser bUser = response.get(0);
                //saving geoPoint to backendless geolocation table
                Backendless.Geo.savePoint(geoPoint, new AsyncCallback<GeoPoint>() {
                    @Override
                    public void handleResponse(GeoPoint response) {
                        Log.i(TAG, "saving geoPoint worked!!" + response);
                        //Adding the geo point as a relation to the user
                        Backendless.Persistence.of(BackendlessUser.class).addRelation(bUser, "location", "latitude=" + geoPoint.getLatitude() + " AND longitude=" + geoPoint.getLongitude(), new AsyncCallback<Integer>() {
                            @Override
                            public void handleResponse(Integer response) {
                                Log.i(TAG, "adding geoPoint relation worked!!" + response);
                            }

                            @Override
                            public void handleFault(BackendlessFault fault) {
                                Log.e(TAG, "adding geoPoint relation failed... " + fault);
                            }
                        });
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        Log.e(TAG, "saving geoPoint failed... " + fault);
                    }
                });
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.e(TAG, "handleFault: "+fault);
            }
        });


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


    public void registerDeviceForMessaging() {

        Backendless.Messaging.registerDevice(Constants.SENDER_ID, "default", new AsyncCallback<Void>() {
            @Override
            public void handleResponse(Void response) {
                Log.i(TAG, "handleResponse: "+response);
                Toast.makeText(context, "Registered for messaging!!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.e(TAG, "handleFault: "+fault );
                Toast.makeText(context, "Error: "+fault.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }


    public void UpdateUserOnDatabase(BackendlessUser user) {


        Backendless.UserService.update(user, new AsyncCallback<BackendlessUser>() {
            @Override
            public void handleResponse(BackendlessUser response) {
                Toast.makeText(context, "User info updated", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Toast.makeText(context, "Error: " + fault.getMessage(), Toast.LENGTH_SHORT).show();
                Log.i(TAG, "handleFault: " + fault.toString());
            }
        });

    }

    public void getUserByName(String username) {
        DataQueryBuilder builder = DataQueryBuilder.create();
        builder.setWhereClause("name='" + username + "'");
        Backendless.Persistence.of(BackendlessUser.class).find(builder, new AsyncCallback<List<BackendlessUser>>() {
            @Override
            public void handleResponse(List<BackendlessUser> response) {
                if (response == null || response.isEmpty()) {
                    Toast.makeText(context, "NO GOOOOOOOOOD", Toast.LENGTH_LONG).show();
                    return;
                }
                final BackendlessUser user = response.get(0);
                final String currentUserName = user.getProperty("name").toString();
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Toast.makeText(context, "Error: " + fault.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "handleFault: " + fault.toString());
            }
        });
    }


//    public void getAllUsers(final OnUserListLoadedListener loadedListener) {
//        // we can get an argument of an object that implements OnTaskListLoadedListener
//        // once we finished loading the task we will call its onTaskListLoaded with
//        // the results to notify it
//
//
//
//        Backendless.Persistence.of(BackendlessUser.class).find(new AsyncCallback<List<BackendlessUser>>() {
//            @Override
//            public void handleResponse(List<BackendlessUser> response) {
//                //if successful
//                Toast.makeText(context, "User found successfully", Toast.LENGTH_SHORT).show();
//                Log.i(TAG, "User found successfully"+response.toString());
//
//                // notify the listening object that we have finished the loading process,
//                // and send it the list of UserProfile objects
//                loadedListener.onUserListLoaded(new UserProfile(response));
//            }
//
//            @Override
//            public void handleFault(BackendlessFault fault) {
//                //an error has occurred, the error code can be retrieved with fault.getCode()
//                String errString = "User find FAILED code " + fault.getCode()+" , " +fault.getMessage();
//                Toast.makeText(context, errString, Toast.LENGTH_SHORT).show();
//                Log.i(TAG, errString);
//
//                // notify the listening object that we have finished the loading process,
//                // and send it null since there was an error
//                loadedListener.onUserListLoaded(null);
//            }
//        });
//    }

    public void saveUser(UserProfile userProfile) {
        Backendless.Data.of(BackendlessUser.class).save(userProfile.getBackendlessUser(), new AsyncCallback<BackendlessUser>() {
            @Override
            public void handleResponse(BackendlessUser response) {
                //if successful
                Toast.makeText(context, "User found successfully", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "User found successfully" + response.toString());
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                //an error has occurred, the error code can be retrieved with fault.getCode()
                String errString = "User find FAILED code " + fault.getCode() + " , " + fault.getMessage();
                Toast.makeText(context, errString, Toast.LENGTH_SHORT).show();
                Log.i(TAG, errString);
            }
        });
    }

    /**
     * loginUser method is used to login an existing user to the app(the Backendless database)
     *
     * @param username is the username used to login the user
     * @param password is the password used to login the user
     */
    public void loginUser(String username, String password, boolean stayLoggedIn) {

        Backendless.UserService.login(username, password, new AsyncCallback<BackendlessUser>() {
            @Override
            public void handleResponse(BackendlessUser response) {
                Toast.makeText(context, "welcome back " + response.getProperty("name"), Toast.LENGTH_SHORT).show();


            }

            @Override
            public void handleFault(BackendlessFault fault) {
                String errString = "Couldn't Login, cause: " + fault.getCode() + " " + fault.getMessage();
                Toast.makeText(context, errString, Toast.LENGTH_SHORT).show();
                Log.i(TAG, errString);
            }
        }, stayLoggedIn);

    }


    public void registerUser(UserProfile userProfile) {

        //getting the UserProfile properties
        String username = userProfile.getUsername();
        String password = userProfile.getPassword();
        String email = userProfile.getEmail();
        //creating a Backendless user to register
        BackendlessUser backendlessUser = new BackendlessUser();
        backendlessUser.setPassword(password);
        backendlessUser.setEmail(email);
        backendlessUser.setProperty("name", username);


        Backendless.UserService.register(backendlessUser, new AsyncCallback<BackendlessUser>() {
            @Override
            public void handleResponse(BackendlessUser response) {
                Toast.makeText(context, "Register of " + response.getProperty("name") + " was successful!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                String errString = "Couldn't register, cause: " + fault.getCode() + fault.getMessage();
                Toast.makeText(context, errString, Toast.LENGTH_LONG).show();
                Log.i(TAG, errString);
            }
        });

    }

    public boolean isUserLoggedIn() {
        Log.i(TAG, Backendless.UserService.loggedInUser());
        return !Backendless.UserService.loggedInUser().equals("");
    }


    public void logoutUser(final String username) {

        Backendless.UserService.logout(new AsyncCallback<Void>() {
            @Override
            public void handleResponse(Void response) {
                Log.i(TAG, "handleResponse: user: " + username + " has logged-out successfully");
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.e(TAG, "handleFault: " + fault);
            }
        });


    }

    public void uploadPhotoToDatabase(final String currentUsername, Uri imageUri) throws Exception {

        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(imageUri, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();
        Log.e(TAG, "onActivityResult: Bitmap returning null start:");
        Bitmap currBitmap = BitmapFactory.decodeFile(picturePath);

        String timeStamp = new SimpleDateFormat("ddMMyyyy").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_.jpg";

        Backendless.Files.Android.upload(currBitmap, Bitmap.CompressFormat.JPEG, Constants.JPEG_QUALITY,
                imageFileName, "sentPics", new AsyncCallback<BackendlessFile>() {
                    @Override
                    public void handleResponse(BackendlessFile response) {
                        Log.e(TAG, "handleResponse: " + "photo saved to backendless");
                        //getting the current user
                        final String profilePicUrl = response.getFileURL();
                        DataQueryBuilder builder = DataQueryBuilder.create();
                        builder.setWhereClause("name='" + currentUsername + "'");
                        Backendless.Persistence.of(BackendlessUser.class).find(builder, new AsyncCallback<List<BackendlessUser>>() {
                            @Override
                            public void handleResponse(List<BackendlessUser> response) {
                                if (response == null || response.isEmpty()) {
                                    Toast.makeText(context, "NO GOOOOOOOOOD", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                BackendlessUser user = response.get(0);
                                user.setProperty("profileImg", profilePicUrl);
                                //updating the Backendless database
                                Backendless.UserService.update(user, new AsyncCallback<BackendlessUser>() {
                                    @Override
                                    public void handleResponse(BackendlessUser response) {
                                        Log.i(TAG, "handleResponse: " + response.toString());
                                        Toast.makeText(context, "Profile image property updated", Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void handleFault(BackendlessFault fault) {
                                        Log.e(TAG, "handleFault: " + fault.toString());
                                        Toast.makeText(context, "Error: " + fault.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }

                            @Override
                            public void handleFault(BackendlessFault fault) {
                                Log.e(TAG, "handleFault: " + fault.toString());
                                Toast.makeText(context, "Error: " + fault.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        Log.e(TAG, "handleFault: " + fault.toString());
                    }
                });
    }


    public void uploadPhotoThumbToDatabase(final String currentUsername, Bitmap imageBitmap) throws Exception {

//        String[] filePathColumn = {MediaStore.Images.Media.DATA};
//        Cursor cursor = context.getContentResolver().query(imageUri, filePathColumn, null, null, null);
//        cursor.moveToFirst();
//        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//        String picturePath = cursor.getString(columnIndex);
//        cursor.close();
//        Log.e(TAG, "onActivityResult: Bitmap returning null start:");
//        Bitmap currBitmap = BitmapFactory.decodeFile(picturePath);


        String timeStamp = new SimpleDateFormat("ddMMyyyy").format(new Date());
        String imageFileName = "JPEG_" + currentUsername + "_" + timeStamp + "_.jpg";

        Backendless.Files.Android.upload(imageBitmap, Bitmap.CompressFormat.JPEG, Constants.JPEG_QUALITY_THUMB,
                imageFileName, "sentPics", true, new AsyncCallback<BackendlessFile>() {
                    @Override
                    public void handleResponse(BackendlessFile response) {
                        Log.e(TAG, "handleResponse: " + "photo saved to backendless");
                        //getting the current user
                        final String profilePicUrl = response.getFileURL();
                        DataQueryBuilder builder = DataQueryBuilder.create();
                        builder.setWhereClause("name='" + currentUsername + "'");
                        Backendless.Persistence.of(BackendlessUser.class).find(builder, new AsyncCallback<List<BackendlessUser>>() {
                            @Override
                            public void handleResponse(List<BackendlessUser> response) {
                                if (response == null || response.isEmpty()) {
                                    Toast.makeText(context, "NO GOOOOOOOOOD", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                BackendlessUser user = response.get(0);
                                user.setProperty("profileImgThumbnail", profilePicUrl);
                                //updating the Backendless database
                                Backendless.UserService.update(user, new AsyncCallback<BackendlessUser>() {
                                    @Override
                                    public void handleResponse(BackendlessUser response) {
                                        Log.i(TAG, "handleResponse: " + response.toString());
                                        Toast.makeText(context, "Profile image property updated", Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void handleFault(BackendlessFault fault) {
                                        Log.e(TAG, "handleFault: " + fault.toString());
                                        Toast.makeText(context, "Error: " + fault.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }

                            @Override
                            public void handleFault(BackendlessFault fault) {
                                Log.e(TAG, "handleFault: " + fault.toString());
                                Toast.makeText(context, "Error: " + fault.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        Log.e(TAG, "handleFault: " + fault.toString());
                    }
                });


    }


}//end of class
