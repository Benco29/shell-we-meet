package benco.shellwemeet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.DataQueryBuilder;

import java.util.List;

import benco.shellwemeet.utils.LoaderDialog;

public class CompleteDetailsActivity extends AppCompatActivity {

    private final String TAG = "CompleteDetailsActivity";

    View compDetailsForm, progress;
    TextView tvLoad;

    Context context;
    CheckBox cbMan, cbWoman, cbOther, cbIntMen, cbIntWomen, cbBoth, cbIntOther;
    EditText iamOtherField, intOtherField, etDescription;
    Button continueBtn;
    Intent prevIntent;
    LoaderDialog loader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_details);

        setPointer();
    }

    private void setPointer() {

        context = this;

        //getting the user's name from the previous activity
        prevIntent = getIntent();
        final String currentUsername = prevIntent.getStringExtra("username");

        String msg = "loading..please wait";
        loader = new LoaderDialog(context, msg);

        //setting the loading elements
        progress = findViewById(R.id.compDetailsProgBar);
        compDetailsForm = findViewById(R.id.compDetailsForm);
        tvLoad = findViewById(R.id.compDetailsTvLoad);
        //setting the layout's checkboxes & editTexts
        cbMan = findViewById(R.id.cbMan);
        cbWoman = findViewById(R.id.cbWoman);
        cbOther = findViewById(R.id.cbOther);

        iamOtherField = findViewById(R.id.editTxtOther);

        cbIntMen = findViewById(R.id.intCBMen);
        cbIntWomen = findViewById(R.id.intCBWomen);
        cbBoth = findViewById(R.id.intCBBoth);
        cbIntOther = findViewById(R.id.intCBOther);

        intOtherField = findViewById(R.id.intEditTxtOther);

        etDescription = findViewById(R.id.compDetailsAboutMe);

        continueBtn = findViewById(R.id.CompDetContinueBtn);

        //setting an onclick to every checkBox so that you can only pick one...
        cbMan.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                iamOtherField.setEnabled(false);
                cbWoman.setChecked(false);
                cbOther.setChecked(false);
                validityBtnCheck();
            }
        });
        cbWoman.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                iamOtherField.setEnabled(false);
                cbMan.setChecked(false);
                cbOther.setChecked(false);
                validityBtnCheck();
            }
        });
        cbOther.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                iamOtherField.setEnabled(true);
                cbMan.setChecked(false);
                cbWoman.setChecked(false);
                validityBtnCheck();
            }
        });

        cbIntMen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                intOtherField.setEnabled(false);
                cbIntOther.setChecked(false);
                cbIntWomen.setChecked(false);
                cbBoth.setChecked(false);
                validityBtnCheck();
            }
        });
        cbIntWomen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                intOtherField.setEnabled(false);
                cbIntMen.setChecked(false);
                cbIntOther.setChecked(false);
                cbBoth.setChecked(false);
                validityBtnCheck();
            }
        });
        cbBoth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                intOtherField.setEnabled(false);
                cbIntMen.setChecked(false);
                cbIntWomen.setChecked(false);
                cbIntOther.setChecked(false);
                validityBtnCheck();
            }
        });
        cbIntOther.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                intOtherField.setEnabled(true);
                cbIntMen.setChecked(false);
                cbIntWomen.setChecked(false);
                cbBoth.setChecked(false);
                validityBtnCheck();
            }
        });


        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String otherTxt = iamOtherField.getText().toString().trim(); //saving this field in a final
                // string so that we can update the user in the database
                final String intOtherTxt = intOtherField.getText().toString().trim();//saving this field in a final
                // string so that we can update the user in the database
                final String description = etDescription.getText().toString().trim();//saving this field in a final
                // string so that we can update the user in the database

                loader.showAlertDialogLoader(true);
                //getting the current user
                DataQueryBuilder builder = DataQueryBuilder.create();
                builder.setWhereClause("name='"+currentUsername+"'");
                Backendless.Persistence.of(BackendlessUser.class).find(builder, new AsyncCallback<List<BackendlessUser>>() {
                    @Override
                    public void handleResponse(List<BackendlessUser> response) {
                        if(response == null || response.isEmpty()){
                            Toast.makeText(context, "NO GOOOOOOOOOD", Toast.LENGTH_LONG).show();
                            return;
                        }
                        boolean gender = false, interested = false, desc = false;

                                BackendlessUser user = response.get(0);
                        Log.i(TAG, "handleResponse: "+user.toString());
                                //setting the gender property
                                if (cbMan.isChecked()) {
                                    user.setProperty("gender","male");
                                    gender = true;
                                } else if (cbWoman.isChecked()) {
                                    user.setProperty("gender","female");
                                    gender = true;
                                } else if (cbOther.isChecked() && !otherTxt.isEmpty()){
                                    user.setProperty("gender",otherTxt);
                                    gender = true;
                                } else {
                                    Toast.makeText(context, "You must choose one Gender property", Toast.LENGTH_LONG).show();
                                }
                                //setting the interestedIn property
                                if (cbIntMen.isChecked()) {
                                    user.setProperty("interestedIn","Men");
                                    interested = true;
                                } else if (cbIntWomen.isChecked()) {
                                    user.setProperty("interestedIn","Women");
                                    interested = true;
                                } else if(cbBoth.isChecked()) {
                                    user.setProperty("interestedIn","Both");
                                    interested = true;
                                } else if (cbIntOther.isChecked() && !intOtherTxt.isEmpty()){
                                    user.setProperty("interestedIn",intOtherTxt);
                                    interested = true;
                                } else {
                                    Toast.makeText(context, "You must choose one Interested-In property", Toast.LENGTH_LONG).show();
                                    LoaderDialog.stopAlertDialog(loader);
                                }

                                //setting the description property
                                if (!description.isEmpty()) {
                                    user.setProperty("description", description);
                                    desc = true;
                                }

                                if (gender && interested && desc) {
                                    //updating the Backendless database
                                    Backendless.UserService.update(user, new AsyncCallback<BackendlessUser>() {
                                        @Override
                                        public void handleResponse(BackendlessUser response) {
                                            Toast.makeText(CompleteDetailsActivity.this, "User info updated", Toast.LENGTH_SHORT).show();
                                            Intent addProfileImg = new Intent(context, AddLocationActivity.class);
                                            addProfileImg.putExtra("username", currentUsername);
                                            startActivity(addProfileImg);
                                        }

                                        @Override
                                        public void handleFault(BackendlessFault fault) {
                                            Toast.makeText(context, "Error: "+fault.getMessage(), Toast.LENGTH_SHORT).show();
                                            Log.i(TAG, "handleFault: "+fault.toString());
                                        }
                                    });

                                } else {
                                    Toast.makeText(context, "Cannot continue without all Details", Toast.LENGTH_LONG).show();
                                    LoaderDialog.stopAlertDialog(loader);
                                }

                            }

                            @Override
                            public void handleFault(BackendlessFault fault) {
                                Log.i(TAG, "handleFault: "+fault.toString());
                                Toast.makeText(context, "Error: "+fault.getMessage(), Toast.LENGTH_SHORT).show();
                                LoaderDialog.stopAlertDialog(loader);
                            }
                        });


            }
        });



    }


    private void validityBtnCheck(){
        if ((cbMan.isChecked() || cbWoman.isChecked() || cbOther.isChecked()) &&
                (cbIntMen.isChecked() || cbIntWomen.isChecked() || cbBoth.isChecked() || cbIntOther.isChecked())) {
            continueBtn.setEnabled(true);
        } else {
            continueBtn.setEnabled(false);
        }
    }




}
