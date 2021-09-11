package com.simetriagrupo.fictab.activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.simetriagrupo.fictab.R;
import com.simetriagrupo.fictab.utils.DeviceUUIDFactory;
import com.simetriagrupo.fictab.utils.FileUploader;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.simetriagrupo.fictab.activities.FicTab.POST_KO;
import static com.simetriagrupo.fictab.activities.FicTab.POST_OK;

public class MainActivity extends AppCompatActivity {

    SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private FaceDetector detector;
    private CameraSource cameraSource;
    private String nombre;
    private int usuario;
    private ProgressBar loader;
    private FicTab ficTab;
    private int finishTime = 15; //15 secs
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ficTab = (FicTab) getApplicationContext();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setIcon(R.mipmap.logo_actionbar);
        actionBar.setDisplayShowTitleEnabled(false);
        loader = findViewById(R.id.main_loader);
        nombre = getIntent().getStringExtra("nombre");
        usuario = getIntent().getIntExtra("usuario", -1);
        findViewById(R.id.btn_fichar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loader.setVisibility(View.VISIBLE);
                handler.removeCallbacksAndMessages(null);
                clickImage();
            }
        });
        findViewById(R.id.btn_atras).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
//                    RingtoneManager.getRingtone(getApplicationContext(), Uri.parse("content://media/internal/audio/media/37")).play();
                    MediaPlayer.create(getApplicationContext(), R.raw.fich_nok).start();
//                    RingtoneManager manager = new RingtoneManager(getApplicationContext());
//                    manager.setType(RingtoneManager.TYPE_RINGTONE);
//                    Cursor cursor = manager.getCursor();
//                    while (cursor.moveToNext()) {
//                        String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
//                        Uri ringtoneURI = manager.getRingtoneUri(cursor.getPosition());
//                        // Do something with the title and the URI of ringtone
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                handler.removeCallbacksAndMessages(null);
                Intent intent = new Intent(MainActivity.this, FailActivity.class);
                intent.putExtra("nombre", nombre);
                startActivity(intent);
                finish();
            }
        });
        ((TextView) findViewById(R.id.nombre)).setText(nombre);
        surfaceView = findViewById(R.id.cam_preview);
        surfaceHolder = surfaceView.getHolder();
        detector = new FaceDetector.Builder(this)
                .setProminentFaceOnly(true)
                .setTrackingEnabled(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.FAST_MODE) // for one face this is OK
                .build();

        if (!detector.isOperational()) {
        } else {
            setupSurfaceHolder();
        }

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                MediaPlayer.create(getApplicationContext(), R.raw.fich_nok).start();
                Intent intent = new Intent(MainActivity.this, FailActivity.class);
                intent.putExtra("nombre", nombre);
                startActivity(intent);
                MainActivity.this.finish();
            }
        }, finishTime * 1000);
    }

    private void setupSurfaceHolder() {
        cameraSource = new CameraSource.Builder(this, detector)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(2.0f)
                .setAutoFocusEnabled(true)
                .build();

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @SuppressLint("MissingPermission")
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    cameraSource.start(surfaceHolder);
                    detector.setProcessor(new LargestFaceFocusingProcessor(detector,
                            new Tracker<Face>()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });
    }

    private void clickImage() {
        if (cameraSource != null) {
            cameraSource.takePicture(/*shutterCallback*/null, new CameraSource.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bytes) {
                    createImageFile();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    resizeImage(bitmap);
                }
            });
        }
    }

    String mCurrentPhotoPath;
    String imageName;
    private File createImageFile(){
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        imageName = imageFileName;
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void addMediaToGallery() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void resizeImage(Bitmap b) {
//        File imgFileOrig = new File(mCurrentPhotoPath);
//        Bitmap b = BitmapFactory.decodeFile(imgFileOrig.getAbsolutePath());
        // original measurements
        final int destWidth;
        final int destHeight;
        int origWidth = b.getWidth();
        int origHeight = b.getHeight();
        if (origWidth>=origHeight){
            destWidth = 1200;//or the width you need
            destHeight = origHeight / (origWidth / destWidth);
        }
        else{
            destHeight = 1200;
            destWidth = origWidth / (origHeight / destHeight);
        }

        if (origWidth > destWidth) {
            // picture is wider than we want it, we calculate its target height

            Bitmap b2 = getResizedBitmap(b,destHeight,destWidth);
            b2 = modifyOrientation(b2,mCurrentPhotoPath);
            // we create an scaled bitmap so it reduces the image, not just trim it
            //Bitmap b2 = Bitmap.createScaledBitmap(b, destWidth, destHeight, false);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            // compress to the format you want, JPEG, PNG...
            // 70 is the 0-100 quality percentage
            b2.compress(Bitmap.CompressFormat.JPEG, 95, outStream);
            // we save the file, at least until we have made use of it
            File f = new File(mCurrentPhotoPath);
            try {
                f.createNewFile();
                FileOutputStream fo = null;
                fo = new FileOutputStream(f);
                fo.write(outStream.toByteArray());
                fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            b = modifyOrientation(b,mCurrentPhotoPath);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            // compress to the format you want, JPEG, PNG...
            // 70 is the 0-100 quality percentage
            b.compress(Bitmap.CompressFormat.JPEG, 95, outStream);
            // we save the file, at least until we have made use of it
            File f = new File(mCurrentPhotoPath);
            try {
                f.createNewFile();
                FileOutputStream fo = null;
                fo = new FileOutputStream(f);
                fo.write(outStream.toByteArray());
                fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        addMediaToGallery();
        String[] params = {mCurrentPhotoPath};
        new Fichar().execute(params);
    }

    private Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth)
    {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);
        // recreate the new Bitmap
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }
    private Bitmap modifyOrientation(Bitmap bitmap, String image_absolute_path){
        ExifInterface ei = null;
        try {
            ei = new ExifInterface(image_absolute_path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotate(bitmap, 90);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotate(bitmap, 180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotate(bitmap, 270);

            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return flip(bitmap, true, false);

            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return flip(bitmap, false, true);
            case ExifInterface.ORIENTATION_UNDEFINED:
                return flip(bitmap, true, false);
            default:
                return bitmap;
        }
    }

    private static Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private class Fichar extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... param) {
            try
            {
                FileUploader fileUploader = new FileUploader();
                JsonObject params = new JsonObject();
                params.addProperty("trabajador_id", usuario);
                params.addProperty("device_id", ficTab.getDevice());
                params.addProperty("lat", String.valueOf(ficTab.getLatitud()));
                params.addProperty("lon", String.valueOf(ficTab.getLongitud()));
                params.addProperty("file",param[0]);
                String result = fileUploader.worker(FicTab.BASE_URL + "/fija/fichar", params.toString());
                JSONObject jsonObject = new JSONObject(result);
                if (jsonObject.getInt("status")==POST_OK || jsonObject.getInt("status")==POST_KO)
                    return jsonObject;
                return null;
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject res) {
            loader.setVisibility(View.GONE);
            if (res!=null) {
                try {
//                    Toast.makeText(MainActivity.this,res.getString("message"),Toast.LENGTH_SHORT).show();
                    if (res.getInt("status")==POST_OK){
                        MediaPlayer.create(getApplicationContext(), R.raw.fich_ok).start();
//                        RingtoneManager.getRingtone(getApplicationContext(), Uri.parse("content://media/internal/audio/media/21")).play();
                        Intent intent = new Intent(MainActivity.this, ExitActivity.class);
                        intent.putExtra("nombre", nombre);
                        startActivity(intent);
                        finish();
                    }
                    else{
                        Toast.makeText(MainActivity.this,"No se ha podido ejecutar la petición",Toast.LENGTH_SHORT).show();
//                        RingtoneManager.getRingtone(getApplicationContext(), Uri.parse("content://media/internal/audio/media/37")).play();
                        MediaPlayer.create(getApplicationContext(), R.raw.fich_nok).start();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
//                    RingtoneManager.getRingtone(getApplicationContext(), Uri.parse("content://media/internal/audio/media/37")).play();
                    MediaPlayer.create(getApplicationContext(), R.raw.fich_nok).start();
                    Toast.makeText(MainActivity.this,"No se ha podido ejecutar la petición",Toast.LENGTH_SHORT).show();
                }
            }
            else{
                Toast.makeText(MainActivity.this,"No se ha podido ejecutar la petición",Toast.LENGTH_SHORT).show();
//                RingtoneManager.getRingtone(getApplicationContext(), Uri.parse("content://media/internal/audio/media/37")).play();
                MediaPlayer.create(getApplicationContext(), R.raw.fich_nok).start();
            }
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
}