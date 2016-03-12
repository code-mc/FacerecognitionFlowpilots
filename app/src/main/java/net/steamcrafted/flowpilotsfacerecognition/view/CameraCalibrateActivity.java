package net.steamcrafted.flowpilotsfacerecognition.view;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import net.steamcrafted.flowpilotsfacerecognition.R;
import net.steamcrafted.flowpilotsfacerecognition.model.CalibrateModel;
import net.steamcrafted.materialiconlib.MaterialDrawableBuilder;

public class CameraCalibrateActivity extends AppCompatActivity {

    private static final int RESULT_SAMPLE = 1;
    private View mCaptureBtn;
    private View mRotateLeft;
    private View mRotateRight;
    private ImageView mPreview;
    private Bitmap cache;

    private int mRotation = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_calibrate);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set the navigation icon to a check icon, left arrow gives the wrong UX impression
        toolbar.setNavigationIcon(
                MaterialDrawableBuilder.with(getApplicationContext())
                        .setIcon(MaterialDrawableBuilder.IconValue.CHECK)
                        .setColor(Color.WHITE)
                        .setToActionbarSize()
                        .build()
        );
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Store the selected rotation when we deliberately touch the "check" icon
                CalibrateModel.store(getApplicationContext(), mRotation);
                onBackPressed();
            }
        });

        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(getApplicationContext()));

        mCaptureBtn  = findViewById(R.id.calibrate_capture);
        mRotateLeft  = findViewById(R.id.calibrate_rotate_left);
        mRotateRight = findViewById(R.id.calibrate_rotate_right);
        mPreview = (ImageView) findViewById(R.id.calibrate_image);

        mCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start a CameraActivity in the SAMPLE mode to capture the preview selfie
                Intent camera = new Intent(CameraCalibrateActivity.this, CameraActivity.class);
                camera.putExtra(CameraActivity.EXTRA_MODE, CameraActivity.MODE_SAMPLE);
                startActivityForResult(camera, RESULT_SAMPLE);
            }
        });

        mRotateRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRotation += 90;
                CalibrateModel.store(getApplicationContext(), mRotation);
                updatePreview();
            }
        });

        mRotateLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRotation -= 90;
                CalibrateModel.store(getApplicationContext(), mRotation);
                updatePreview();
            }
        });
    }

    /**
     * Displays the currently cached bitmap in the provided rotation.
     */
    private void updatePreview() {
        if(cache != null){
            if(mRotation == 0){
                // Don't rotate the bitmap if rotation is set to 0...
                mPreview.setImageBitmap(cache);
            }else{
                // Rotate the bitmap and update the ImageView
                Matrix m = new Matrix();
                m.postRotate(mRotation);

                Bitmap rotated = Bitmap.createBitmap(cache, 0, 0, cache.getWidth(), cache.getHeight(), m, true);
                mPreview.setImageBitmap(rotated);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == RESULT_SAMPLE){
                // We successfully snapped a selfie (wooo), now we display it inside the actvity
                // so the user can rotate it in the correct position to calibrate the camera.

                // Get the image path from the passed intent
                String path = data.getExtras().getString(CameraActivity.EXTRA_RESULT_PATH);

                // Display options that ensure the image is loaded in the smallest possible memory
                // footprint. It's just a preview, we don't need a 10MB image in memory...
                DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .imageScaleType(ImageScaleType.EXACTLY)
                .build();

                // Load the image and save the bitmap so we can use it later to rotate
                ImageLoader.getInstance().displayImage("file://" + path, mPreview, options, new SimpleImageLoadingListener(){
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        super.onLoadingComplete(imageUri, view, loadedImage);

                        cache = loadedImage;
                    }
                });

                // Hide the button to capture a selfie, we just took one!
                mCaptureBtn.setVisibility(View.GONE);
            }
        }
    }
}
