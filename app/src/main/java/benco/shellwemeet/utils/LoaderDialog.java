package benco.shellwemeet.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

public class LoaderDialog {

    private Context context;
    private ProgressBar progressBar;
    private String alertMessage;

    private AlertDialog alertLoader;

    public LoaderDialog(Context context, String alertMessage) {
        this.context = context;
        this.alertMessage = alertMessage;

        this.progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleLarge);
        this.progressBar.setIndeterminate(true);
        this.progressBar.setVisibility(View.VISIBLE);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        builder.setView(progressBar);
        builder.setMessage(alertMessage);

        alertLoader = builder.create();
    }

    public void showAlertDialogLoader(boolean show) {

//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setCancelable(false);
//        builder.setView(progressBar);
//        builder.setMessage(alertMessage);
//
//        alertLoader = builder.create();

        if (show) {
            alertLoader.show();
        } else { //show == false
            alertLoader.dismiss();
        }
    }

    public static void stopAlertDialog(LoaderDialog loader) {
        loader.alertLoader.dismiss();
    }

    public static void showAlertAgain(LoaderDialog loader) {
        loader.alertLoader.show();
    }



    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }
}
