package com.lazerwars2563.Activitys;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.lazerwars2563.Class.UserDetails;
import com.lazerwars2563.R;
import com.lazerwars2563.util.UserClient;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {
    private static String TAG = "ProfileActivity";

    private static final int REQUEST_CODE = 221;
    static final int REQUEST_IMAGE_CAPTURE = 2;
    static final int REQUEST_IMAGE_GALLERY = 1;

    private UserDetails user;

    private TextView view_name_text;
    private ImageButton view_button_camera;
    private ImageButton view_button_upload;
    private ImageButton view_button_gallery;
    private ImageView view_img;
    private StorageReference mStorageRef;

    private Bitmap imgBitmap;
    private byte[] imgUplaodBites;
    public Uri imgUri;
    private double mProgress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        user = UserClient.getInstance().getUser();

        view_name_text = findViewById(R.id.view_name_text);
        view_name_text.setText(user.getUserName());
        view_button_gallery = findViewById(R.id.view_button_gallery);
        view_button_upload  = findViewById(R.id.view_button_uplaod);
        view_button_camera  = findViewById(R.id.view_button_picture_camera);
        view_img = findViewById(R.id.view_image_profile);

        mStorageRef = FirebaseStorage.getInstance().getReference("Images");
        try {
            LoadOldImage();
        }
        catch (IOException e)
        {
            Log.d(TAG,"couldn't load old image");
        }

        verifyPermissions();
    }

    private  File localFile;
    private void LoadOldImage() throws IOException {
        StorageReference islandRef = mStorageRef.child(user.getUserId() + ".jpg");
        localFile = File.createTempFile("images", "jpg");

        islandRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                view_img.setImageBitmap(myBitmap);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                view_img.setImageResource(R.drawable.ic_warning_black_24dp);
            }
        });
    }


    private void SetButtonsListeners() {
        view_button_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileChooser();
            }
        });

        view_button_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(imgBitmap == null)
                {
                    Toast.makeText(ProfileActivity.this,"Choose picture",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(mProgress != 0)
                {
                    Toast.makeText(ProfileActivity.this,"Uplaod in progress",Toast.LENGTH_SHORT).show();
                }
                else {
                    uploadNewImage();
                }
            }
        });

        view_button_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

    }

    private void uploadNewImage()
    {
        Log.d(TAG, "uploadNewPhoto: uploading a new image bitmap to storage");
        BackgroundImageResize resize = new BackgroundImageResize(imgBitmap);
        resize.execute();
    }

    private void FileUploader() {
        Toast.makeText(ProfileActivity.this,"Uploading image",Toast.LENGTH_SHORT).show();
        final StorageReference Ref = mStorageRef.child(user.getUserId() + ".jpg");
        mProgress = 0.1;//start upload

        UploadTask uploadTask = Ref.putBytes(imgUplaodBites);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(ProfileActivity.this,"image uploaded",Toast.LENGTH_SHORT).show();
                mProgress = 0;
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ProfileActivity.this,"could not upload image",Toast.LENGTH_SHORT).show();
                mProgress = 0;
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                double currentProgress = (100 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                if(currentProgress > mProgress + 15)
                {
                    mProgress = (100 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    Log.d(TAG, "onProgress: upload is "+ mProgress + "% done");
                    Toast.makeText(ProfileActivity.this,mProgress + "%",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public class BackgroundImageResize extends AsyncTask<Uri,Integer,byte[]>
    {
        Bitmap mBitmap;

        public BackgroundImageResize(Bitmap bitmap)
        {
            this.mBitmap = bitmap;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG,"BackgroundImageResize: compressing image");
        }

        @Override
        protected byte[] doInBackground(Uri... uris) {
            float size_in_MB = mBitmap.getByteCount() / 1000000;
            int quality = 100;

            if(size_in_MB > 1)
            {
               quality = (int) (quality/size_in_MB);
            }

            byte[] bytes;
            bytes = getBytesFromBitmap(mBitmap,quality);
            return bytes;
        }

        @Override
        protected void onPostExecute(byte[] bytes) {
            super.onPostExecute(bytes);
            imgUplaodBites = bytes;
            //uplaod task
            FileUploader();
        }
    }

    public static  byte[] getBytesFromBitmap(Bitmap bitmap, int quality)


    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_IMAGE_GALLERY && resultCode == RESULT_OK && data != null && data.getData() != null)//from gallery
        {
            Log.d(TAG, "placing from gallery");
            imgUri = data.getData();
            try {
                Bitmap dst = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgUri);
                view_img.setImageBitmap(CropToSquare(dst));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) { // from camera
            Log.d(TAG, "placing from camera");
            Bundle extras = data.getExtras();
            Bitmap dst = (Bitmap) extras.get("data");
            view_img.setImageBitmap(CropToSquare(dst));
        }
    }
    //crop to square shape
    private Bitmap CropToSquare(Bitmap bitmap) {
        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = (height > width) ? width : height;
        int newHeight = (height > width)? height - ( height - width) : height;
        int cropW = (width - height) / 2;
        cropW = (cropW < 0)? 0: cropW;
        int cropH = (height - width) / 2;
        cropH = (cropH < 0)? 0: cropH;
        imgBitmap = Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight);

        return imgBitmap;
    }

    private void FileChooser() {
        Log.d(TAG, "FileChooser: choosing picture");
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,REQUEST_IMAGE_GALLERY);
    }

    private void dispatchTakePictureIntent() {
        Log.d(TAG, "dispatchTakePictureIntent: taking picture");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void verifyPermissions()
    {
        Log.d(TAG, "verifyPermissions: ask user for permissions");
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA};
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[0]) == PackageManager.PERMISSION_GRANTED
        && ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[1]) == PackageManager.PERMISSION_GRANTED
        && ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[2]) == PackageManager.PERMISSION_GRANTED)
        {
            SetButtonsListeners();
        }
        else
        {
            ActivityCompat.requestPermissions(ProfileActivity.this, permissions,REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        verifyPermissions();
    }

}
