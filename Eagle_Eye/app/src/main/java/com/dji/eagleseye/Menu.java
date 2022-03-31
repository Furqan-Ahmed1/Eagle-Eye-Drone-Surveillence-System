package com.dji.eagleseye;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import dji.sdk.sdkmanager.DJISDKManager;

public class Menu extends AppCompatActivity implements View.OnClickListener {

    private Button ml_button;
    private Button ux_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        initUI();
    }

    private void initUI() {


        ml_button = (Button) findViewById(R.id.ML_Camera);
        ml_button.setOnClickListener(this);
        ml_button.setEnabled(true);

        ux_button = (Button) findViewById(R.id.UX_Camera);
        ux_button.setOnClickListener(this);
        ux_button.setEnabled(true);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.ML_Camera:{
                Intent intent = new Intent(this, CameraDisplay.class);
                startActivity(intent);

                break;
            }
            case R.id.UX_Camera:{
                Intent intent = new Intent(this,FlyDrone.class);
                startActivity(intent);
                break;
            }
            default:
                break;
        }

    }
}