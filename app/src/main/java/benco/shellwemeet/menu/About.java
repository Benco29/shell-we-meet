package benco.shellwemeet.menu;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import benco.shellwemeet.R;

public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_item_about);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}