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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.simetriagrupo.fictab.R;
import com.simetriagrupo.fictab.utils.DeviceUUIDFactory;
import com.simetriagrupo.fictab.utils.MyKeyboard;
import com.simetriagrupo.fictab.utils.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.simetriagrupo.fictab.activities.FicTab.BASE_URL;
import static com.simetriagrupo.fictab.activities.FicTab.POST_KO;
import static com.simetriagrupo.fictab.activities.FicTab.POST_OK;

public class LoginActivity extends AppCompatActivity implements MyKeyboard.MyEnterListener {
    ProgressBar loader;
    TextView txtCodigo;
    TextView txtVersion;
    Button btnLogin;
    boolean permissionsGranted = false;
    ColorStateList hintColor;
    String[] appPermissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final int PERMISSIONS_REQUEST_CODE = 1240;
    FicTab ficTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ficTab = (FicTab) getApplicationContext();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setIcon(R.mipmap.logo_actionbar);
        actionBar.setDisplayShowTitleEnabled(false);
        loader = findViewById(R.id.loader);
        txtCodigo = findViewById(R.id.codigo);
        hintColor = txtCodigo.getHintTextColors();
        btnLogin = findViewById(R.id.btn_login);
        ColorStateList oldColors =  ((TextView)findViewById(R.id.titulo)).getTextColors();
        ((TextView)findViewById(R.id.digitalClock)).setTextColor(oldColors);
        MyKeyboard keyboard = (MyKeyboard) findViewById(R.id.keyboard);
        keyboard.setListener(this);
        txtCodigo.setRawInputType(InputType.TYPE_CLASS_TEXT);
        txtCodigo.setTextIsSelectable(true);
        InputConnection ic = txtCodigo.onCreateInputConnection(new EditorInfo());
        keyboard.setInputConnection(ic);

        if (checkAndRequestPermissions()) {
            permissionsGranted = true;
        }
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
                if (validCode(txtCodigo.getText().toString()))
                    btnLogin.setEnabled(true);
                else
                    btnLogin.setEnabled(false);
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
                        txtCodigo.setError(getString(R.string.codigo_err));
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
                        txtCodigo.setError(getString(R.string.codigo_err));
                    }
                }
            }
        });
        findViewById(R.id.btn_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loader.setVisibility(View.VISIBLE);
                login();
            }
        });

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        txtVersion = findViewById(R.id.app_version);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            txtVersion.setText("v. "+pInfo.versionName+" / "+ ficTab.getDevice());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private boolean validCode(String codigo) {
        return (codigo.length()<=5 && codigo.length()>0);
    }

    @Override
    public void onClickEnter() {
        if (btnLogin.isEnabled()){
            loader.setVisibility(View.VISIBLE);
            login();
        }
        else if (txtCodigo.getText()==null || txtCodigo.getText().toString()==""){
            Toast.makeText(getApplicationContext(), R.string.introduzca_codigo, Toast.LENGTH_LONG).show();
        }
        else
            Toast.makeText(getApplicationContext(), R.string.ubicacion_desconocida, Toast.LENGTH_LONG).show();
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

    private void login(){
        RequestQueue queue = VolleySingleton.getInstance(getApplicationContext()).getRequestQueue();
        String uri = BASE_URL + "/fija/login";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, uri, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                loader.setVisibility(View.GONE);
                try {
                    JSONObject res = new JSONObject(response);
                    int status = res.getInt("status");
                    if (status==POST_OK) {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("nombre",res.getString("nombre"));
                        intent.putExtra("usuario", res.getInt("usuario"));
                        startActivity(intent);
                        txtCodigo.setHint(R.string.intro_cod);
                        txtCodigo.setHintTextColor(hintColor);
                        txtCodigo.setText("");
                    }
                    else if (status == POST_KO){
                        txtCodigo.setText("");
                        Toast.makeText(getApplicationContext(), res.getString("message"), Toast.LENGTH_LONG).show();
                        txtCodigo.setHint("Núm. control erróneo");
                        txtCodigo.setHintTextColor(getResources().getColor(android.R.color.holo_red_dark,null));
                    }
                    else{
                        txtCodigo.setText("");
                        Toast.makeText(getApplicationContext(), R.string.fallo_operacion, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    txtCodigo.setText("");
                    Toast.makeText(getApplicationContext(), R.string.fallo_operacion, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } catch (Exception e) {
                    txtCodigo.setText("");
                    Toast.makeText(getApplicationContext(), R.string.fallo_operacion, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }, errorListener) {
            @Override
            public Priority getPriority() {
                return Priority.IMMEDIATE;
            }

            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("usuario", txtCodigo.getText().toString());
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            loader.setVisibility(View.GONE);
            txtCodigo.setText("");
            if (error instanceof NetworkError) {
                Toast.makeText(getApplicationContext(),R.string.no_network, Toast.LENGTH_LONG).show();
            } else {
//                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
                Toast.makeText(getApplicationContext(),R.string.no_network, Toast.LENGTH_LONG).show();
            }
        }
    };
}