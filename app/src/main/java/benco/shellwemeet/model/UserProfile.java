package benco.shellwemeet.model;


import com.backendless.BackendlessUser;
import com.backendless.geo.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserProfile {
    private String username;
    private String password;
    private String email;
    private String description;
    private String profilePicture;// a url string of image location on server
    private String profilePictureThumb;// a url string of image location on server
    private String birthDate;
    private List<String> imagesList;
    private String gender;
    private String interestedIn;
    private String objectId;
    private GeoPoint location;
    private boolean isOnline;

    public UserProfile(String username, String password, String email, String description, String profilePicture, String profilePictureThumb, String videoVideo, List<String> imagesList, GeoPoint location, boolean isOnline) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.description = description;
        this.profilePicture = profilePicture;
        this.imagesList = imagesList;
        this.location = location;
        this.isOnline = isOnline;
        this.profilePictureThumb = profilePictureThumb;
    }

    public String getProfilePictureThumb() {
        return profilePictureThumb;
    }

    public void setProfilePictureThumb(String profilePictureThumb) {
        this.profilePictureThumb = profilePictureThumb;
    }

    public UserProfile(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public UserProfile(){}

    // a constructor that builds a UserProfile Object out of a Backendless user
    public UserProfile(BackendlessUser bUser){
        Map<String, Object> userMap =  bUser.getProperties();
        this.username = (String)userMap.get("name");
        //this.password = (String)userMap.get("password");
        this.email = bUser.getEmail();
        this.description = (String)userMap.get("description");
        this.profilePicture = (String)userMap.get("profileImg");
        this.profilePictureThumb = (String)userMap.get("profileImgThumbnail");
        this.birthDate = (String)userMap.get("birthDate");
        this.gender = (String)userMap.get("gender");
        this.interestedIn = (String)userMap.get("interestedIn");
        this.objectId = bUser.getObjectId();
        this.location = (GeoPoint) userMap.get("location");
        this.isOnline = (Boolean)userMap.get("isOnline");
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getInterestedIn() {
        return interestedIn;
    }

    public void setInterestedIn(String interestedIn) {
        this.interestedIn = interestedIn;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public List<String> getImagesList() {
        return imagesList;
    }

    public void setImagesList(List<String> imagesList) {
        this.imagesList = imagesList;
    }



    public GeoPoint getLocation() {
        return location;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                ", description='" + description + '\'' +
                ", profilePicture='" + profilePicture + '\'' +
                ", profilePictureThumb='" + profilePictureThumb + '\'' +
                ", birthDate='" + birthDate + '\'' +
                ", imagesList=" + imagesList +
                ", gender='" + gender + '\'' +
                ", interestedIn='" + interestedIn + '\'' +
                ", objectId='" + objectId + '\'' +
                ", location=" + location +
                ", isOnline=" + isOnline +
                '}';
    }
    // a "constructor" that builds a Backendless user Object out of a UserProfile object
    public BackendlessUser getBackendlessUser(){
        BackendlessUser bUser = new BackendlessUser();
        bUser.setProperty("name",this.username);
        bUser.setEmail(this.email);
        bUser.setPassword(this.password);
        bUser.setProperty("description", this.description);
        bUser.setProperty("profileImg", this.profilePicture);
        bUser.setProperty("profileImgThumbnail", this.profilePictureThumb);
        bUser.setProperty("birthDate", this.birthDate);
        bUser.setProperty("gender", this.gender);
        //bUser.setProperty("objectId",this.objectId);
        bUser.setProperty("interestedIn", this.interestedIn);
        bUser.setProperty("location", this.location);
        bUser.setProperty("isOnline", this.isOnline);
        return bUser;
    }


    public static List<UserProfile> convertBackendlessList(List<BackendlessUser> bUsers){
        ArrayList<UserProfile> list = new ArrayList<>();

        for(BackendlessUser bUser: bUsers){
            list.add(new UserProfile(bUser));
        }

        return list;
    }
}
