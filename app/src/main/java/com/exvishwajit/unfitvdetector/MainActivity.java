package com.exvishwajit.unfitvdetector;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Executor executor = Executors.newSingleThreadExecutor();
    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};

    PreviewView mPreviewView;
    Button captureImage;
    Button refresh;
    TextView numPlate;
    TextView date;
    TextView validity;
    TextView model;

    List<String> imgUriList = new ArrayList<String>();

    private FirebaseStorage storage;
    private StorageReference mStorageRef;

    FirebaseDatabase rootNode;
    DatabaseReference myRef;

    List<String> plateNo = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage = FirebaseStorage.getInstance();
        mStorageRef = storage.getReference();

        rootNode = FirebaseDatabase.getInstance();
        myRef = rootNode.getReference("Scans");

        Scan scan = new Scan("numberplate", "false", "date", "state", "model");

        myRef.setValue(scan);

        mPreviewView = findViewById(R.id.previewView);
        captureImage = findViewById(R.id.captureImg);
        refresh = findViewById(R.id.refresh);
        numPlate = findViewById(R.id.plateNumber);
        date = findViewById(R.id.date);
        validity = findViewById(R.id.valid);
        model = findViewById(R.id.model);

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readDatabase();
            }
        });

        // Read from the database





        getSupportActionBar().hide();

        if(allPermissionsGranted()){
            startCamera(); //start camera if permission has been granted by user
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    void readDatabase(){
        // Read from the database
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                plateNo.clear();

                Scan value = dataSnapshot.getValue(Scan.class);
                numPlate.setText(value.result);
                date.setText("Exp Date: "+value.date);
                validity.setText(value.state);
                model.setText(value.model);
                Log.d(TAG, "Value is: " + value.result + " " + value.open);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private void startCamera() {

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {

                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);

                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();

        //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);

        // Query if extension is available (optional).
        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            // Enable the extension if available.
            hdrImageCaptureExtender.enableExtension(cameraSelector);
        }

        final ImageCapture imageCapture = builder.setTargetResolution(new Size(1024, 768))
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();

        preview.setSurfaceProvider(mPreviewView.createSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis, imageCapture);

        captureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readDatabase();
                Scan scan = new Scan("numberplate", "true", "date", "state", "model");

                myRef.setValue(scan);
                captureImage.setText("....");

                Log.d(TAG, "onClick: start");
                int framecounter = 0;
                while(framecounter < 5){

                    Handler handler1 = new Handler();

                    int finalFramecounter = framecounter;
                    handler1.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //camcode1
                            SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                            String uri = mDateFormat.format(new Date())+ ".jpg";
                            File file = new File(getBatchDirectoryName(), uri);
                            Log.d(TAG, "bindPreview: " + finalFramecounter + getBatchDirectoryName());
                            Log.d(TAG, "Image saved: " + getBatchDirectoryName() + uri);
                            //imgUriList.add(uri);

                            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
                            imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback () {
                                @Override
                                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                               uploadPicture(file, ""+finalFramecounter);
                                               file.delete();

                                                if(finalFramecounter == 4){
                                                    captureImage.setText("Scan");
                                                }

                                            } catch (Exception e){
                                                Log.d(TAG, "ERROR"+e.toString());
                                            }

                                            Toast.makeText(MainActivity.this, "Processing", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                @Override
                                public void onError(@NonNull ImageCaptureException error) {
                                    error.printStackTrace();
                                }
                            });
                            // camcode2
                        }
                    }, (framecounter+1)*800);



                    framecounter++;
                    Log.d(TAG, "onClick: " + framecounter);
                }
            }
        });




    }

    void uploadPicture(File img_file, String name){

        //String randomKey = UUID.randomUUID().toString();

        Uri file = Uri.fromFile(img_file);
        StorageReference riversRef = mStorageRef.child("images/" + name);

        riversRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        // ...
                        Toast.makeText(MainActivity.this, "failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public String getBatchDirectoryName() {

        String app_folder_path = "";
        //app_folder_path = Environment.getExternalStorageDirectory().toString() + "/UnfitVehicleDetector";
        app_folder_path = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();
        File dir = new File(app_folder_path);
        if (!dir.exists() && !dir.mkdirs()) {

        }

        return app_folder_path;
    }

    private boolean allPermissionsGranted(){

        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                startCamera();
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }


}
