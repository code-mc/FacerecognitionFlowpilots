package net.steamcrafted.flowpilotsfacerecognition.presenter;

import android.content.Context;
import android.graphics.Bitmap;

import net.steamcrafted.flowpilotsfacerecognition.Kairos;
import net.steamcrafted.flowpilotsfacerecognition.R;
import net.steamcrafted.flowpilotsfacerecognition.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Wannes2 on 12/03/2016.
 *
 * Presenter for the CameraActivity.
 */
public class CameraPresenter extends BasePresenter<CameraPresenter.CameraPresenterReactions>{

    public CameraPresenter(CameraPresenterReactions reactor) {
        super(reactor);
        mAlbumName = Utils.getDeviceId(reactor.getContext());
    }

    /**
     * The reaction interface to talk back to the view.
     */
    public interface CameraPresenterReactions
    {
        void updateProgress(int progress);
        void identifiedSubject(String subject);
        void showToast(String message);
        void showToast(int message_id);
        Context getContext();
    }

    private float ACCEPT_TRESHHOLD = .80f;

    private int mProgress = 0;
    private String mSubjectName = "";
    private String mAlbumName;

    /**
     * Increase the identify/training progress by one.
     */
    public void progressIncreased(){
        mProgress++;
        // Reflect this change in the view.
        mReact.updateProgress(mProgress);
    }

    /**
     * Decrease the identify/training progress by one.
     */
    public void progressDecreased(){
        if(mProgress > 0){
            mProgress--;
            // Reflect this change in the view.
            mReact.updateProgress(mProgress);
        }
    }

    /**
     * Set the subject name for training operations.
     * @param subject The name of the trained subject.
     */
    public void setSubject(String subject){
        mSubjectName = subject;
    }

    /**
     * Identifies the image and looks for trained subjects. When a subject is found with a high
     * enough confidence level the view is notified.
     *
     * @param k The Kairos API object.
     * @param image The image to identify.
     */
    public void identify(Kairos k, Bitmap image){
        // Increase the progress
        progressIncreased();

        final String galleryId = mAlbumName;
        final String selector = "FULL";
        final String minHeadScale = "0.25";

        // Do the API call
        k.recognize(image, galleryId, selector, null, minHeadScale, null, new Kairos.KairosListener() {
            @Override
            public void onSuccess(String s) {
                // Aaand decrease the progress again as we've gotten a result
                progressDecreased();
                // Log the result
                Utils.log(s);
                // Extract the needed data from the returned JSON
                try {
                    JSONObject root = new JSONObject(s);
                    JSONObject tran = root.getJSONArray("images").getJSONObject(0).getJSONObject("transaction");
                    if(tran.getString("status").equalsIgnoreCase("success") && Float.parseFloat(tran.getString("confidence")) > ACCEPT_TRESHHOLD){
                        String subject = tran.getString("subject");
                        // Reflect the successful identification inside the UI
                        mReact.identifiedSubject(subject);
                        return;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Notify the absence of a person to the view
                mReact.showToast(R.string.couldnt_identify_person);
            }

            @Override
            public void onFail(String s) {
                // Decrease the progress again as we've gotten a result, albeit a bad one
                progressDecreased();
                Utils.log(s);

                // Notify the absence of a person to the view
                mReact.showToast(R.string.couldnt_identify_person);
            }
        });
    }

    /**
     * Trains the current subject. Adds a reference for this image to the current subject if a face
     * is found inside the image.
     *
     * @param k The Kairos API object.
     * @param image The image to train.
     */
    public void train(Kairos k, Bitmap image){
        progressIncreased();

        final String galleryId = mAlbumName;
        final String selector = "FULL";
        final String multipleFaces = "false";
        final String minHeadScale = "0.25";

        k.enroll(image, mSubjectName, galleryId, selector, multipleFaces, minHeadScale, new Kairos.KairosListener() {
            @Override
            public void onSuccess(String s) {
                Utils.log(s);
                progressDecreased();

                try {
                    JSONObject root = new JSONObject(s);
                    JSONObject tran = root.getJSONArray("images").getJSONObject(0).getJSONObject("transaction");
                    if(tran.getString("status").equalsIgnoreCase("success")){
                        mReact.showToast(R.string.face_added);
                        return;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mReact.showToast(R.string.not_face_detected);
            }

            @Override
            public void onFail(String s) {
                Utils.log(s);
                progressDecreased();

                mReact.showToast(R.string.not_face_detected);
            }
        });
    }
}
