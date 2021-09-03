package com.simetriagrupo.fictab.activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.simetriagrupo.fictab.R;

public class FailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fail);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setIcon(R.mipmap.logo_actionbar);
        actionBar.setDisplayShowTitleEnabled(false);

        ((TextView)findViewById(R.id.nombre)).setText(getIntent().getStringExtra("nombre"));
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                Intent i = new Intent(FailActivity.this, LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
        }, 2000);
    }
}