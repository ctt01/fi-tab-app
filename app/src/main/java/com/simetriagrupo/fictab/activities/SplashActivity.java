package com.simetriagrupo.fictab.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.simetriagrupo.fictab.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FicTab ficTab = (FicTab) getApplicationContext();
        if (ficTab.getDevice()!=null){
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
        else{
            startActivity(new Intent(this, RegisterDevice.class));
            finish();
        }
    }
}