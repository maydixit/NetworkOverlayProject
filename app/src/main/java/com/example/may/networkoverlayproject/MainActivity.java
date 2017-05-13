package com.example.may.networkoverlayproject;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ToggleButton;


import com.example.may.networkoverlayproject.service.OceanVPNService;
import com.example.may.networkoverlayproject.service.ServiceFragment;
import com.jakewharton.rxbinding2.view.RxView;



public class MainActivity extends AppCompatActivity {

    ServiceFragment serviceFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceFragment = (ServiceFragment) getFragmentManager().findFragmentById(R.id.service_fragment);

        ToggleButton inputToggleButton = (ToggleButton) findViewById(R.id.inputToggleButton);
        RxView.clicks(inputToggleButton).subscribe(click -> {
            Log.d("tag","Time to toggle!" + click);
            serviceFragment.toggleService();
        }); // think about creating a subscriber and unsubscribing later?
        // Note: what happens if one of the toggles is missed, the application might do opposite things!



    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }

}
