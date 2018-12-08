package benco.shellwemeet.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.files.BackendlessFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import benco.shellwemeet.ChatWithUserActivity;
import benco.shellwemeet.R;
import benco.shellwemeet.UserProfilePageActivity;
import benco.shellwemeet.model.UserProfile;
import io.socket.client.Url;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {


    //this inner class holds the information for the cell (the model of the cell)
    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView userProfImage, onOff;
        TextView userNick;

        public ViewHolder(View itemView) {
            super(itemView);

            // setting the ui elements from the itemView (which will be R.layout.profile_image_cell)
            userProfImage = itemView.findViewById(R.id.profileImageCell);
            onOff = itemView.findViewById(R.id.isOnline);
            userNick = itemView.findViewById(R.id.userNicknameImgCell);
        }
    }

    Context context;
    List<UserProfile> usersList;


    public UserAdapter(Context context, List<UserProfile> usersList) {
        this.context = context;
        this.usersList = usersList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //called when a viewHolder is created

        //getting view holding our cell's data
        LayoutInflater inflater = LayoutInflater.from(context);
        View userView = inflater.inflate(R.layout.profile_image_cell, parent, false);

        //make a viewHolder from the cell's layout
        ViewHolder viewHolder = new ViewHolder(userView);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull UserAdapter.ViewHolder holder, int position) {
        //called when a viewHolder is bound to a data item (in our case - a userProfile)

        //get a single user from the list according to the current position
        UserProfile user = usersList.get(position);

        //Uri profileImage = Uri.parse(user.getProfilePicture());
        //Drawable pic = context.getResources().getDrawable(R.drawable.seasnail, null);
        Drawable online = context.getResources().getDrawable(R.drawable.online, null);
        Drawable offline = context.getResources().getDrawable(R.drawable.offline, null);
        if (user.isOnline()) {
            holder.onOff.setImageDrawable(online);
        } else { //user is offline
            holder.onOff.setImageDrawable(offline);
        }
        if (user.getProfilePictureThumb()== null || user.getProfilePictureThumb().isEmpty()) {
            String imageUri = getURLForResource(R.drawable.profile);
            Uri uri = Uri.parse(imageUri);
            holder.userProfImage.setImageURI(uri);
        } else {
            new DownloadImageTask(holder.userProfImage).execute(user.getProfilePictureThumb());
        }

        //holder.userProfImage.setImageURI(Uri.parse(user.getProfilePicture()));

        // holder.userProfImage.setImageURI(profileImage);
        holder.userNick.setText(user.getUsername());

        final String userName = user.getUsername();

        holder.userProfImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //intent to the profile of the user
                Intent userProfileIntent = new Intent(context, UserProfilePageActivity.class);
                userProfileIntent.putExtra("userName", userName);
                context.startActivity(userProfileIntent);
            }
        });
    }

    @Override
    public int getItemCount() {// the number of elements
        return usersList.size(); //gives a - java.lang.NullPointerException: Attempt to invoke interface method 'int java.util.List.size()' on a null object reference
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


}