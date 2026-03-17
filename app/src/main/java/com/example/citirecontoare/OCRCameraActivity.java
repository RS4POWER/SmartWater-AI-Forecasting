package com.example.citirecontoare;// OCRCameraActivity.java
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.provider.MediaStore;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class OCRCameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private TextureView textureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocrcamera);

    }}