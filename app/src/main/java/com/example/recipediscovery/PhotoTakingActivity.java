package com.example.recipediscovery;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.Manifest;

public class PhotoTakingActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> ImageCaptureActivityResultLauncher;
    private ActivityResultLauncher<Intent> PickPhotoActivityResultLauncher;
    private Uri photoURI;
    private String currentPhotoPath;

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_taking);

        // action bar return button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        imageView = findViewById(R.id.imageView);
        // require camera permission
        ActivityResultLauncher<String []> cameraPermissionRequest =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result->{
                            Boolean cameraGranted = result.getOrDefault(
                                    Manifest.permission.CAMERA,false
                            );
                        }
                );
        cameraPermissionRequest.launch(new String[]{Manifest.permission.CAMERA});

        // camera intent

        ImageCaptureActivityResultLauncher= registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>(){
                    @Override
                    public void onActivityResult(ActivityResult result){
                        if (result.getResultCode() == Activity.RESULT_OK){
                            Bitmap imageBitmap = null;
                            try{
                                ImageDecoder.Source source =ImageDecoder.createSource(getContentResolver(), photoURI);
                                imageBitmap = ImageDecoder.decodeBitmap(source);
                            }
                            catch (IOException e){
                                e.printStackTrace();
                            }
                            Log.d("myTag", "This is my message");
                            Button continuebutton = findViewById(R.id.continuebutton);
                            if (continuebutton.getVisibility() != View.VISIBLE){
                                continuebutton.setVisibility(View.VISIBLE);
                            }
                            imageView.setImageBitmap(imageBitmap);

                        }
                    }
                }
        );

        // pick photo from album Intent
        PickPhotoActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>(){
                    @Override
                    public void onActivityResult(ActivityResult result){
                        if (result.getResultCode() == Activity.RESULT_OK){
                            photoURI = result.getData().getData();
                            Button continuebutton = findViewById(R.id.continuebutton);
                            if (continuebutton.getVisibility() != View.VISIBLE){
                                continuebutton.setVisibility(View.VISIBLE);
                            }
                            imageView.setImageURI(photoURI);
                        } else {
                        }
                    }
                }
        );
    }


    public void dispatchTakePictureIntent(View view){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex){

        }

        if(photoFile != null){
            photoURI = FileProvider.getUriForFile(this, "com.example.android.recipeDiscovery", photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
            ImageCaptureActivityResultLauncher.launch(takePictureIntent);
        }
    }


    private File createImageFile() throws IOException{
        String timeStamp = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    public void dispatchPickPhotoIntent(View view){
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        PickPhotoActivityResultLauncher.launch(pickPhotoIntent);
    }


    public void Continue(View v) {
        Intent intent = new Intent(this, PhotoProcessingActivity.class);
        intent.putExtra("PHOTO_URI", photoURI.toString());
        startActivity(intent);
    }




    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}