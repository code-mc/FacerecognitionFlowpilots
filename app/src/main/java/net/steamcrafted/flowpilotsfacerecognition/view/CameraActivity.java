package net.steamcrafted.flowpilotsfacerecognition.view;

import android.animation.Animator;
import android.app.ActionBar;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.commonsware.cwac.cam2.AbstractCameraActivity;
import com.commonsware.cwac.cam2.CameraController;
import com.commonsware.cwac.cam2.CameraEngine;
import com.commonsware.cwac.cam2.CameraFragment;
import com.commonsware.cwac.cam2.CameraSelectionCriteria;
import com.commonsware.cwac.cam2.util.Utils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import net.steamcrafted.flowpilotsfacerecognition.Kairos;
import net.steamcrafted.flowpilotsfacerecognition.Networking;
import net.steamcrafted.flowpilotsfacerecognition.R;
import net.steamcrafted.flowpilotsfacerecognition.model.CalibrateModel;
import net.steamcrafted.flowpilotsfacerecognition.model.UserModel;
import net.steamcrafted.flowpilotsfacerecognition.presenter.CameraPresenter;
import net.steamcrafted.loadtoast.LoadToast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class CameraActivity extends AppCompatActivity implements CameraPresenter.CameraPresenterReactions {

    public static final String EXTRA_MODE           = "EXTRA_MODE";
    public static final String EXTRA_IMAGE_NAME     = "EXTRA_IMAGE_NAME";
    public static final String EXTRA_RESULT_SUBJECT = "EXTRA_RESULT_SUBJECT";
    public static final String EXTRA_RESULT_PATH    = "EXTRA_RESULT_PATH";

    protected static final String TAG_CAMERA        = CameraFragment.class.getCanonicalName();

    public static final int MODE_STUDY      = 1;
    public static final int MODE_IDENTIFY   = 2;
    public static final int MODE_SAMPLE     = 3;
    private static final int REQUEST_PERMS  = 13401;

    private CameraPresenter mPresenter;

    private Kairos mKairos;
    private int mProgressText;
    private int mMode = MODE_STUDY;
    private float mRotation = 0f;
    private LoadToast mLoadToast;
    private Toast mToast;
    private String mImageName = "face_" + System.currentTimeMillis();
    private boolean mDestroyed = false;

    protected CameraFragment cameraFrag;

    /**
     * Create a new CameraFragment
     * @return CameraFragment
     */
    protected CameraFragment buildFragment() {
        CameraFragment f = CameraFragment.newPictureInstance(getOutputUri(), false);
        return f;
    }

    /**
     * Get the Uri for the location of the captured image
     *
     * @return Uri containing the location of the saved image.
     */
    private Uri getOutputUri() {
        return Uri.fromFile(new File(getFilesDir(), mImageName));
    }

    @Override
    protected void onResume(){
        super.onResume();

        // Hide the camera viewfinder camera switch button
        View b1 = findViewById(R.id.cwac_cam2_picture);
        View b2 = findViewById(R.id.cwac_cam2_settings);
        if(b1 != null) b1.setVisibility(View.VISIBLE);
        if(b2 != null) b2.setVisibility(View.GONE);

        // Show the viewfinder fragment
        showCamera();
    }

    /**
     * Show an animation of the passed image that rotates and zooms out while fading to alpha 0.
     *
     * @param bmp The image to use for the effect
     */
    private void polaroidEffect(Bitmap bmp){
        final ImageView flashView = new ImageView(CameraActivity.this);
        flashView.setImageBitmap(bmp);

        final ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        root.addView(flashView);

        float rotate = (float) (Math.random() * 60) - 30;

        flashView.animate()
                .alpha(0)
                .rotationBy(rotate)
                .scaleX(.5f).scaleY(.5f)
                .setInterpolator(new AccelerateInterpolator())
                .setDuration(900)
        .setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                root.removeView(flashView);
                findViewById(R.id.cwac_cam2_picture).setEnabled(true);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                root.removeView(flashView);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
                root.removeView(flashView);
            }
        });
    }

    /**
     * Show the viewfinder fragment.
     */
    public void showCamera(){
        if(cameraFrag != null){
            getFragmentManager()
                    .beginTransaction()
                    .show(cameraFrag)
                    .commit();
        }
    }

    /**
     * Manually call finish when back button is pressed. This ensures the only way an activity
     * is stopped is via a finish() call so we can properly detach the camera.
     */
    @Override
    public void onBackPressed() {
        finish();
    }

    /**
     * Standard lifecycle method, serving as the main entry
     * point of the activity.
     *
     * @param savedInstanceState the state of a previous instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the presenter object
        mPresenter = new CameraPresenter(this);

        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));

        final Intent intent = getIntent();
        if(intent.getExtras() != null){
            if(intent.getExtras().containsKey(EXTRA_MODE)){
                mMode = intent.getExtras().getInt(EXTRA_MODE);
            }
            if(intent.getExtras().containsKey(EXTRA_IMAGE_NAME) && mMode == MODE_STUDY){
                mImageName = intent.getExtras().getString(EXTRA_IMAGE_NAME);
                showWhosDialog();
            }
        }

        if(mMode == MODE_STUDY)
            mProgressText = R.string.toast_upload_train;
        else if(mMode == MODE_IDENTIFY)
            mProgressText = R.string.toast_upload_identify;

        mLoadToast = new LoadToast(CameraActivity.this);
        mLoadToast.setProgressColor(getResources().getColor(R.color.color_accent));
        mToast = Toast.makeText(getContext(), "", Toast.LENGTH_SHORT);
        mKairos = Networking.initKairos(getApplicationContext());
        mRotation = CalibrateModel.get(getApplicationContext());

        Utils.validateEnvironment(this);

        ActionBar ab = getActionBar();

        if (ab != null) {
            ab.hide();
        }

        // Handle permissions, needed on Marshmallow+
        String[] perms = netPermissions(getNeededPermissions());

        if (perms.length == 0) {
            init();
        } else {
            requestPermissions(perms, REQUEST_PERMS);
        }
    }

    /**
     * Initialize the camera fragment that shows the viewfinder and has the CameraController
     * bound to its lifecycle.
     */
    protected void init() {
        cameraFrag = (CameraFragment) getFragmentManager().findFragmentByTag(TAG_CAMERA);

        if (cameraFrag == null) {
            cameraFrag = buildFragment();

            CameraController ctrl = new CameraController();

            cameraFrag.setController(ctrl);

            AbstractCameraActivity.Facing facing = AbstractCameraActivity.Facing.FRONT;

            CameraSelectionCriteria criteria =
                    new CameraSelectionCriteria.Builder().facing(facing).build();
            boolean forceClassic = false;

            ctrl.setEngine(CameraEngine.buildInstance(this, forceClassic), criteria);
            ctrl.getEngine().setDebug(false);
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, cameraFrag, TAG_CAMERA)
                    .commit();
        }
    }

    /**
     * Intercept the finish call to correctly detach the camera fragment
     */
    @Override
    public void finish() {
        if(mDestroyed) return;

        mDestroyed = true;
        if(cameraFrag != null){
            // Manually remove the camera framgent, otherwise activity lifecycle is triggered with
            // a considerable delay. (we're talking 10 seconds delay)
            getFragmentManager().beginTransaction().remove(cameraFrag).commit();
        }
        super.finish();
    }

    /**
     * Shows a dialog to select the current user for whom pictures
     * are getting added.
     */
    private void showWhosDialog() {
        final List<String> arr = UserModel.get(getApplicationContext());
        final ArrayAdapter<String> adap = new ArrayAdapter<>(getApplicationContext(),
                android.R.layout.simple_list_item_1, new ArrayList(arr));

        new MaterialDialog.Builder(CameraActivity.this)
                .cancelable(false)
                .title(R.string.whos_this)
                .negativeText(android.R.string.cancel)
                .neutralText(R.string.add_user)
                .adapter(adap, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                        // Update the Presenter with the selected subject
                        mPresenter.setSubject(adap.getItem(which));
                        dialog.dismiss();
                    }
                })
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onNeutral(final MaterialDialog parentDialog) {
                        // Spawn a new dialog to create a new subject
                        new MaterialDialog.Builder(CameraActivity.this)
                                .title(R.string.add_user)
                                .input(R.string.new_name_hint, 0, false, new MaterialDialog.InputCallback() {
                                    @Override
                                    public void onInput(MaterialDialog dialog, CharSequence input) {

                                    }
                                })
                                .positiveText(android.R.string.ok)
                            .negativeText(android.R.string.cancel)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    // Add the new subject to the list and update the UserModel
                                    arr.add(dialog.getInputEditText().getText().toString());
                                    adap.add(dialog.getInputEditText().getText().toString());
                                    UserModel.store(getApplicationContext(), arr);
                                    super.onPositive(dialog);
                                }
                            })
                            .dismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    parentDialog.show();
                                }
                            }).build()
                        .show();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        // Finish the activity when we don't select anything, can't train a "John Doe"
                        finish();
                    }
                })
                .build()
        .show();
    }

    /**
     * Standard lifecycle method, for when the fragment moves into
     * the started state. Passed along to the CameraController.
     */
    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    /**
     * Standard lifecycle method, for when the fragment moves into
     * the stopped state. Passed along to the CameraController.
     */
    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    /**
     * Eventbus methods
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(CameraController.NoSuchCameraEvent event) {
        finish();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final CameraEngine.PictureTakenEvent event) {
        // Get the bitmap from the camera
        Bitmap bmp = event.getImageContext().getBitmap(true);
        if(mMode == MODE_SAMPLE){
            // In sample mode we just want the image for usage inside a different activity, so we
            // safe it do disk and pass the path inside the result intent
            try {
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, new FileOutputStream(getOutputUri().getPath()));
                Intent in = getIntent();
                in.putExtra(EXTRA_RESULT_PATH, getOutputUri().getPath());
                setResult(RESULT_OK, in);
            } catch (FileNotFoundException e) {
                setResult(RESULT_CANCELED);
                e.printStackTrace();
            }
            finish();
        } else {
            // In the other modes we either want to analyze the image or use it for training

            // Rotate the bitmap using the calibrated rotation
            Matrix m = new Matrix();
            if(bmp.getWidth() > bmp.getHeight()){
                m.setRotate(mRotation);
            }

            // Create the rotated bitmap
            final Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            // Recycle the non-rotated bitmap
            bmp.recycle();
            // Do the image-zooms-out-while-rotating-a-little animation to signal the user a picture
            // was taken
            polaroidEffect(rotated);

            // Depending on the mode either train or identify using the bitmap
            if(mMode == MODE_STUDY){
                mPresenter.train(mKairos, rotated);
            }else if(mMode == MODE_IDENTIFY){
                mPresenter.identify(mKairos, rotated);
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CameraController.ControllerDestroyedEvent event) {
        finish();
    }

    /**
     * Camera permission methods
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        String[] perms = netPermissions(getNeededPermissions());

        if(perms.length == 0) {
            init();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    protected String[] getNeededPermissions() {
        return new String[0];
    }

    private boolean hasPermission(String perm) {
        if (useRuntimePermissions()) {
            return checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean useRuntimePermissions() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    private String[] netPermissions(String[] wanted) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result.toArray(new String[result.size()]);
    }

    /**
     * Reaction methods from CameraPresenter.CameraPresenterReactions
     */
    private boolean mToastShown = false;
    @Override
    public void updateProgress(int progress) {
        mLoadToast.setText(getString(mProgressText, progress));
        if(progress == 0){
            mToastShown = false;
            mLoadToast.success();
        }else if(!mToastShown){
            mToastShown = true;
            mLoadToast.show();
        }
    }

    @Override
    public void identifiedSubject(String subject) {
        Intent t = getIntent();
        t.putExtra(EXTRA_RESULT_SUBJECT, subject);
        setResult(RESULT_OK, t);
        finish();
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    @Override
    public void showToast(String message) {
        mToast.setText(message);
        mToast.show();
    }

    @Override
    public void showToast(int message_id) {
        showToast(getString(message_id));
    }
}

