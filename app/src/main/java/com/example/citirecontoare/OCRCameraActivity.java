package com.example.citirecontoare;// OCRCameraActivity.java
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.TextureView;

public class OCRCameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private TextureView textureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_camera);

    }}