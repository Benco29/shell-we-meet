package benco.shellwemeet.listeners;

import java.util.List;

import benco.shellwemeet.model.UserProfile;

// we want to know when the loading of the tasks it finished.
// for this we will have an object that implements this interface.
// such that when the loading is finished,
// the onTaskListLoaded method is called (with the response - list of Task objects)

// this interface is similar to View.OnClickListener
public interface OnUserListLoadedListener {
    void onUserListLoaded(List<UserProfile> userProfileList);// what we want to do once loading the tasks has finished
    // this method is similar to onClick(View view) of View.OnClickListener
}
