package com.simetriagrupo.fictab.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.simetriagrupo.fictab.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterDevice extends AppCompatActivity {

    TextView txtCodigo;
    Button btnGuardar;
    FicTab ficTab;
    MyLocationListener locationListener;
    LocationManager locationManager;
    Location myLocation = null;
    ProgressBar loader;
    String[] appPermissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final int PERMISSIONS_REQUEST_CODE = 1240;
    boolean permissionsGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_device);
        ficTab = (FicTab) getApplicationContext();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setIcon(R.mipmap.logo_actionbar);
        actionBar.setDisplayShowTitleEnabled(false);
        loader = findViewById(R.id.loader);
        txtCodigo = findViewById(R.id.cod_disp);
        btnGuardar = findViewById(R.id.btn_codigo);

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                dataChanged(txtCodigo.getText().toString());
            }
        };
        txtCodigo.addTextChangedListener(afterTextChangedListener);
        txtCodigo.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_NEXT){
                    if (txtCodigo.getText().toString()==null || txtCodigo.getText().toString().equals(""))
                        txtCodigo.setError(getString(R.string.codigo_blank));
                    if (!validCode(txtCodigo.getText().toString())) {
                        txtCodigo.setError(getString(R.string.codigo_invalido));
                    }
                }
                return false;
            }
        });
        txtCodigo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focus) {
                if (!focus){
                    if (!validCode(txtCodigo.getText().toString())) {
                        txtCodigo.setError(getString(R.string.codigo_invalido));
                    }
                }
            }
        });

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                guardarDatos();
            }
        });

        locationListener = new MyLocationListener();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this, new String[] {  Manifest.permission.ACCESS_FINE_LOCATION  },
                    1024);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }

        if ( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            buildAlertMessageNoGps();
        }

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String provider = locationManager.getBestProvider(criteria, true);
        if (provider!=null){
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                myLocation = location;
                loader.setVisibility(View.GONE);
                if (validCode(txtCodigo.getText().toString()))
                    btnGuardar.setEnabled(true);
            }
        }
    }

    private void dataChanged(String codigo){
        if ((codigo!=null && !codigo.equals("")) && validCode(codigo))
            btnGuardar.setEnabled(true);
        else
            btnGuardar.setEnabled(false);
    }

    private boolean validCode(String codigo){
        return (codigo.length()==3);
    }

    private void guardarDatos(){
        ficTab.setDevice(txtCodigo.getText().toString());
        ficTab.setLatitud((float) myLocation.getLatitude());
        ficTab.setLongitud((float) myLocation.getLongitude());
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationListener = new RegisterDevice.MyLocationListener();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this, new String[] {  Manifest.permission.ACCESS_FINE_LOCATION  },
                    1024);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
    }

    public class MyLocationListener implements LocationListener {

        public void onLocationChanged(Location location) {
            myLocation = location;
            loader.setVisibility(View.GONE);
            if (validCode(txtCodigo.getText().toString()))
                btnGuardar.setEnabled(true);
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.gps_deshabilitado)
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog,final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean checkAndRequestPermissions()
    {
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm : appPermissions)
        {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
            {
                listPermissionsNeeded.add(perm);
            }
        }
        if (!listPermissionsNeeded.isEmpty())
        {
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    PERMISSIONS_REQUEST_CODE
            );
            return false;
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE)
        {
            HashMap<String, Integer> permissionResults = new HashMap<>();
            int deniedCount = 0;

            for (int i=0; i<grantResults.length; i++)
            {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                {
                    permissionResults.put(permissions[i], grantResults[i]);
                    deniedCount++;
                }
            }

            if (deniedCount == 0)
            {
                permissionsGranted = true;
            }
            else
            {
                for (Map.Entry<String, Integer> entry : permissionResults.entrySet())
                {
                    String permName = entry.getKey();

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permName))
                    {
                        showDialog("", "La aplicación necesita todos los permisos para funcionar correctamente.",
                                "Sí, conceder permisos",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        checkAndRequestPermissions();
                                    }
                                },
                                "No, salir", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        finish();
                                    }
                                }, false);
                    }
                    else
                    {
                        showDialog("", "Ha denegado algunos permisos a la aplicación. Por favor, añada los permisos en [Ajustes] > [Permisos]",
                                "Acceda a Ajustes",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        // Go to app settings
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", getPackageName(), null));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    }
                                },
                                "No, salir", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        finish();
                                    }
                                }, false);
                        break;
                    }
                }
            }
        }
    }
    public AlertDialog showDialog(String title, String msg, String positiveLabel,
                                  DialogInterface.OnClickListener positiveOnClick,
                                  String negativeLabel, DialogInterface.OnClickListener negativeOnClick,
                                  boolean isCancelAble)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setCancelable(isCancelAble);
        builder.setMessage(msg);
        builder.setPositiveButton(positiveLabel, positiveOnClick);
        builder.setNegativeButton(negativeLabel, negativeOnClick);

        AlertDialog alert = builder.create();
        alert.show();
        return alert;
    }
}