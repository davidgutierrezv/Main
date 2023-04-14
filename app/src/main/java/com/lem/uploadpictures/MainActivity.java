package com.lem.uploadpictures;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private ImageView mImageView;
    private ActivityResultLauncher<Intent> mTakePictureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.imageView);

        Button captureButton = findViewById(R.id.button_capture);

        captureButton.setOnClickListener(view -> dispatchTakePictureIntent());

        Button uploadButton = findViewById(R.id.button_upload);

        uploadButton.setOnClickListener(view -> uploadImage());

        mTakePictureLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        try {
                            Bundle extras = result.getData().getExtras();
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            mImageView.setImageBitmap(imageBitmap);
                            saveImage(imageBitmap);
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            mTakePictureLauncher.launch(takePictureIntent);
        }
    }

    private void saveImage(Bitmap imageBitmap) {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File imageFile = null;
        try {
            imageFile = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadImage() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] files = storageDir.listFiles();
        if (files != null) {
            for (File file : files) {
                String base64Image = convertToBase64(file);
                sendToEndpoint(base64Image);
            }
        }
    }

    private String convertToBase64(File file) {
        byte[] bytes = new byte[(int) file.length()];
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytes);
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private void sendToEndpoint(final String base64Image) {
        String url = "https://2dc5-191-113-68-228.ngrok-free.app/Image";
        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject postData = new JSONObject();
        try {
            postData.put("image", base64Image);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, postData,
                response -> {
                    Log.d("MainActivity", "Response: " + response.toString());
                    Toast.makeText(MainActivity.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                    deleteImage();
                },
                error -> {
                    Log.e("MainActivity", "Error: " + error.toString());
                    Toast.makeText(MainActivity.this, "Error uploading image", Toast.LENGTH_SHORT).show();
                });

        queue.add(jsonObjectRequest);
    }

    private void deleteImage() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] files = storageDir.listFiles();
        if (files != null) {
            for (File file : files) {
                boolean deleted = file.delete();
                if (deleted) {
                    Log.d("MainActivity", "Image deleted successfully");
                } else {
                    Log.e("MainActivity", "Error deleting image");
                }
            }
        }
    }

}