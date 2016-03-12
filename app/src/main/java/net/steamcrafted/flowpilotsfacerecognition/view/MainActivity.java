package net.steamcrafted.flowpilotsfacerecognition.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import net.steamcrafted.flowpilotsfacerecognition.Kairos;
import net.steamcrafted.flowpilotsfacerecognition.Networking;
import net.steamcrafted.flowpilotsfacerecognition.R;
import net.steamcrafted.flowpilotsfacerecognition.model.CalibrateModel;
import net.steamcrafted.loadtoast.LoadToast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int RESULT_ADD_PICTURE = 1;
    private static final int RESULT_IDENTIFY    = 2;

    private ImageView mImagePreview;
    private View mSelectPicture;
    private View mSignIn;
    private boolean mTrained = false;
    private LoadToast mLoadToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = ((Toolbar) findViewById(R.id.toolbar));
        toolbar.setTitle(R.string.title_activity_main);
        setSupportActionBar(toolbar);

        mLoadToast = new LoadToast(this).setTranslationY(toolbar.getHeight())
                .setProgressColor(getResources().getColor(R.color.color_accent));

        mImagePreview = (ImageView) findViewById(R.id.main_image_preview);
        mSelectPicture = findViewById(R.id.main_select_picture);
        mSignIn = findViewById(R.id.main_sign_in);

        mSelectPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disableButtons();

                // Start the camera activity in the STUDY modus which means we want to teach it
                // about a person
                Intent cameraIntent = new Intent(getApplicationContext(), CameraActivity.class);
                cameraIntent.putExtra(CameraActivity.EXTRA_MODE, CameraActivity.MODE_STUDY);
                cameraIntent.putExtra(CameraActivity.EXTRA_IMAGE_NAME, "face_" + System.currentTimeMillis());
                startActivityForResult(cameraIntent, RESULT_ADD_PICTURE);
            }
        });

        mSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disableButtons();

                // Start the camera activity in the IDENTIFY modus
                Intent cameraIntent = new Intent(getApplicationContext(), CameraActivity.class);
                cameraIntent.putExtra(CameraActivity.EXTRA_MODE, CameraActivity.MODE_IDENTIFY);
                startActivityForResult(cameraIntent, RESULT_IDENTIFY);
            }
        });

        mSignIn.setVisibility(View.GONE);
    }

    private void disableButtons() {
        mSignIn.setEnabled(false);
        mSelectPicture.setEnabled(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Enable the buttons again now we return from an activity
        enableButtons();

        switch(requestCode) {
            case RESULT_IDENTIFY:
                if(resultCode == RESULT_OK){
                    // A user has been signed in successfully, greet him/her with a toast message
                    String subject = data.getExtras().getString(CameraActivity.EXTRA_RESULT_SUBJECT);
                    Toast.makeText(getApplicationContext(), getString(R.string.greet_message, subject), Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // onResume seems like a good time to check the current training data state
        refreshSignInButton();

        // Start the calibrate activity if the camera hasn't been calibrated yet (i.e. on first launch of the app)
        if(!CalibrateModel.exists(getApplicationContext())){
            calibrateCamera();
        }
    }

    /**
     * Buttons are disabled to prevent starting an activity twice. Therefor we need to re-enable them
     * once the started activity ends.
     */
    private void enableButtons(){
        mSignIn.setEnabled(true);
        mSelectPicture.setEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id == R.id.action_delete_training_data){
            showDeleteTrainingDataDialog();
            return true;
        } else if(id == R.id.action_calibrate_camera){
            calibrateCamera();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Launches the activity to calibrate the camera. This ensures snapped pictures are rotated
     * accordingly. Correct rotation is needed for a successful face recognition.
     */
    private void calibrateCamera() {
        startActivity(new Intent(this, CameraCalibrateActivity.class));
    }

    /**
     * Shows the dialog to delete a single training entry or all entries at once.
     */
    private void showDeleteTrainingDataDialog() {
        final ArrayAdapter<String> subjects = new ArrayAdapter<>(getApplicationContext(),
                android.R.layout.simple_list_item_1, new ArrayList<String>());

        final Kairos.KairosListener deleteCallback = new Kairos.KairosListener() {
            @Override
            public void onSuccess(String s) {
                Toast.makeText(getApplicationContext(), getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
                mLoadToast.success();
                refreshSignInButton();
            }

            @Override
            public void onFail(String s) {
                Toast.makeText(getApplicationContext(), getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                mLoadToast.error();
            }
        };

        new MaterialDialog.Builder(MainActivity.this)
                .title(R.string.action_delete_training_data)
                .positiveText(R.string.delete_all)
                .negativeText(android.R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // Delete all entries
                        Networking.clearTrainingData(getApplicationContext(), deleteCallback);
                        // Show a LoadToast notifying the user data is being deleted
                        mLoadToast.setText(getString(R.string.delete_all_message)).show();
                        super.onPositive(dialog);
                    }
                })
                .adapter(subjects, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                        // Delete the selected entry
                        Networking.clearTrainingSubject(getApplicationContext(), subjects.getItem(which), deleteCallback);
                        // Show a LoadToast notifying the user data is being deleted
                        mLoadToast.setText(getString(R.string.delete_all_message)).show();
                        dialog.dismiss();
                    }
                }).build()
                .show();

        Networking.getAllTrainedSubjects(getApplicationContext(), new Networking.SimpleDataCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> data) {
                subjects.addAll(data);
            }

            @Override
            public void onFail() {

            }
        });
    }

    /**
     * Checks the API endpoint if there are any subjects that have been trained for the current
     * device. If so the sign in button is shown, otherwise it is hidden as there is nobody
     * to sign in.
     */
    private void refreshSignInButton() {
        Networking.getAllTrainedSubjects(getApplicationContext(), new Networking.SimpleDataCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> data) {
                mTrained = true;
                mSignIn.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFail() {
                mTrained = false;
                mSignIn.setVisibility(View.GONE);
            }
        });
    }
}
