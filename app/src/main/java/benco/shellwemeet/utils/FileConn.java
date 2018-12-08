package benco.shellwemeet.utils;

import android.app.Activity;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class FileConn {

    private static final String FILE_NAME = "shell-we-meet-user.txt";
    private static final int READ_BLOCK_SIZE = 1024;

    private Context context;
    private FileInputStream fIn; //getting data drom the file
    private FileOutputStream fOut; //editing the data in the file

    public FileConn(Context context) {
        this.context = context;
    }

    public void write(String data) throws IOException {

        fOut = context.openFileOutput(FILE_NAME, Context.MODE_APPEND);

        OutputStreamWriter outWriter = new OutputStreamWriter(fOut);

        outWriter.write(data);

        outWriter.flush();
        outWriter.close();

        fOut.close();

    }

    public String read() throws IOException {

        fIn = context.openFileInput(FILE_NAME);

        InputStreamReader inReader = new InputStreamReader(fIn);

        char[] buffer = new char[READ_BLOCK_SIZE];
        String returnData = "";

        int amountRead;

        while ((amountRead = inReader.read(buffer))>0) {

            returnData += String.copyValueOf(buffer, 0, amountRead);

            buffer = new char[READ_BLOCK_SIZE];
        }

        inReader.close();

        fIn.close();

        return returnData;
    }

    public void clear() throws IOException {

        fOut = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
        fOut.close();

    }

    //finding the username String on the file
    public static String getUsernameFromStringArray (String[] stringArray) {
        for (String item : stringArray) {
            if (item.contains("Username: ")) {
                String result = item.substring(10);
                return result;
            }
        }
        //the username isn't found in the string array
        return null;
    }

    public static String getLocationFromStringArray(String[] userDetails) {
        for (String item : userDetails) {
            if (item.contains("Location: ")) {
                return item.substring(10);
            }
        }
        return null;
    }
}
