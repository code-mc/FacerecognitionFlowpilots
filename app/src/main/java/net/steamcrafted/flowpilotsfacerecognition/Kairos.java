package net.steamcrafted.flowpilotsfacerecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Handler;
import android.os.Looper;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.Base64;
import com.loopj.android.http.DataAsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.message.BasicHeader;

/**
 * API wrapper for the Kairos API. Parts taken from the official Kairos library, but mostly modified
 * and improved to actually work reliably.
 */
public class Kairos {
    protected String my_app_id;
    protected String my_api_key;
    protected String my_host;
    protected Context my_context;
    protected Header[] mHeaders;

    public Kairos(){}

    /**
     * Set the different fields that are used for the various API calls.
     *
     * @param ctx The context used to make the requests.
     * @param app_id App Id from the Kairos developer dashboard.
     * @param api_key Api Key from the Kairos developer dashboard.
     */
    public void setAuthentication(Context ctx, String app_id, String api_key) {
        this.my_context = ctx;
        this.my_app_id = app_id;
        this.my_api_key = api_key;
        this.my_host = "http://api.kairos.com/";

        mHeaders = new Header[]{new BasicHeader("Content-Type", "application/json"), new BasicHeader("app_id", my_app_id), new BasicHeader("app_key", my_api_key)};
    }

    /**
     * List all the registered galleries.
     * @param callback
     */
    public void listGalleries(final KairosListener callback) {
        post("http://api.kairos.com/gallery/list_all", new JSONObject(), callback);
    }

    /**
     * Get all subjects that have been registered for a certain gallery.
     * @param galleryId Id of the gallery to check.
     * @param callback
     */
    public void listSubjectsForGallery(String galleryId, final KairosListener callback) {
        try {
            JSONObject params = new JSONObject();
            params.put("gallery_name", galleryId);
            post("http://api.kairos.com/gallery/view", params, callback);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onFail("");
        }
    }

    /**
     * Delete a certain gallery.
     * @param galleryId Id of the gallery to delete.
     * @param callback
     */
    public void deleteGallery(String galleryId, final KairosListener callback) {
        try {
            JSONObject params = new JSONObject();
            params.put("gallery_name", galleryId);
            post("http://api.kairos.com/gallery/remove", params, callback);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onFail("");
        }
    }

    /**
     * Delete a certain subject inside a certain gallery.
     * @param subjectId Id of the subject to delete.
     * @param galleryId Id of the gallery which contains the subject.
     * @param callback
     */
    public void deleteSubject(String subjectId, String galleryId, final KairosListener callback) {
        try {
            JSONObject params = new JSONObject();
            params.put("subject_id", subjectId);
            params.put("gallery_name", galleryId);
            post("http://api.kairos.com/gallery/remove_subject", params, callback);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onFail("");
        }
    }

    /**
     * Detect a face inside the passed image.
     * @param image Image encoded in Base64 encoding
     * @param selector Optional selector value
     * @param minHeadScale Optional minimum head size relative to the image
     * @param callback
     */
    public void detect(String image, String selector, String minHeadScale, final KairosListener callback) {
        try {
            JSONObject params = new JSONObject();
            params.put("image", image);
            if(selector != null) {
                params.put("selector", selector);
            }

            if(minHeadScale != null) {
                params.put("minHeadScale", minHeadScale);
            }

            post("http://api.kairos.com/detect", params, callback);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onFail("");
        }
    }

    /**
     * Detect a face inside the passed image.
     * @param image Bitmap image to use
     * @param selector Optional selector value
     * @param minHeadScale Optional minimum head size relative to the image
     * @param callback
     */
    public void detect(final Bitmap image, final String selector, final String minHeadScale, final KairosListener callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                detect(base64FromBitmap(image), selector, minHeadScale, callback);
            }
        }).start();
    }

    /**
     * Train the provided subject with the provided image.
     * @param image The image to use for training.
     * @param subjectId The subject to store this training data for.
     * @param galleryId Id for the gallery to store the info in.
     * @param selector Optional selector
     * @param multipleFaces Hint the algorithm if there are multiple faces
     * @param minHeadScale Minimum size of head relative to the image
     * @param callback
     */
    public void enroll(String image, String subjectId, String galleryId, String selector, String multipleFaces, String minHeadScale, final KairosListener callback) {
        try {
            JSONObject params = new JSONObject();
            params.put("subject_id", subjectId);
            params.put("gallery_name", galleryId);
            params.put("image", image);
            if(selector != null) {
                params.put("selector", selector);
            }
            if(minHeadScale != null) {
                params.put("minHeadScale", minHeadScale);
            }
            if(multipleFaces != null) {
                params.put("multiple_faces", multipleFaces);
            }
            post("http://api.kairos.com/enroll", params, callback);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onFail("");
        }
    }

    /**
     * Method to make an API call using a POST request. Most calls use the exact same client with the
     * same headers etc.
     *
     * @param url The url to post to
     * @param params The parameters to post
     * @param callback
     */
    private void post(String url, JSONObject params, KairosListener callback){
        new AsyncHttpClient().post(this.my_context, url, mHeaders, new StringEntity(params.toString(), "UTF-8"), "application/json", createResponseHandler(callback));
    }

    /**
     * Method to create a new DataAsyncHttpResponseHandler from a KairosListener. Once again, used
     * in many methods.
     *
     * @param callback
     * @return The DataAsyncHttpResponseHandler that calls the appropriate KairosListener methods
     */
    private AsyncHttpResponseHandler createResponseHandler(final KairosListener callback) {
        return new DataAsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if(responseBody != null && responseBody.length > 0){
                    callback.onSuccess(new String(responseBody));
                }else{
                    callback.onFail("");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                if(responseBody != null && responseBody.length > 0){
                    callback.onFail(new String(responseBody));
                }else{
                    callback.onFail("");
                }
            }
        };
    }

    /**
     * Train the provided subject with the provided image.
     * @param image The image to use for training.
     * @param subjectId The subject to store this training data for.
     * @param galleryId Id for the gallery to store the info in.
     * @param selector Optional selector
     * @param multipleFaces Hint the algorithm if there are multiple faces
     * @param minHeadScale Minimum size of head relative to the image
     * @param callback
     */
    public void enroll(final Bitmap image, final String subjectId, final String galleryId, final String selector, final String multipleFaces, final String minHeadScale, final KairosListener callback) {
        final Handler main = new Handler(Looper.getMainLooper());
        // Spawn a new thread to do the image -> Base64 conversion (Resource heavy operation)
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String base64 = base64FromBitmap(image);
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        enroll(base64, subjectId, galleryId, selector, multipleFaces, minHeadScale, callback);
                    }
                });
            }
        }).start();
    }

    /**
     * Check the provided image for a face and try to link the face to an earlier trained subject.
     * @param imageURL The image to analyze in Base64 encoding.
     * @param galleryId Id of the gallery to look inside for trained subjects.
     * @param selector Optional selector
     * @param threshold The minimum confidence level to appear as a result.
     * @param minHeadScale The minimum size of a head relative to the image
     * @param maxNumResults The maximum number of returned results
     * @param callback
     */
    public void recognize(String imageURL, String galleryId, String selector, String threshold, String minHeadScale, String maxNumResults, final KairosListener callback) {
        try {
            JSONObject params = new JSONObject();
            params.put("image", imageURL);
            params.put("gallery_name", galleryId);
            if(selector != null) {
                params.put("selector", selector);
            }
            if(minHeadScale != null) {
                params.put("minHeadScale", minHeadScale);
            }
            if(threshold != null) {
                params.put("threshold", threshold);
            }
            if(maxNumResults != null) {
                params.put("max_num_results", maxNumResults);
            }
            post("http://api.kairos.com/recognize", params, callback);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     /**
     * Check the provided image for a face and try to link the face to an earlier trained subject.
     * @param image The image to analyze.
     * @param galleryId Id of the gallery to look inside for trained subjects.
     * @param selector Optional selector
     * @param threshold The minimum confidence level to appear as a result.
     * @param minHeadScale The minimum size of a head relative to the image
     * @param maxNumResults The maximum number of returned results
     * @param callback
     */
    public void recognize(final Bitmap image, final String galleryId, final String selector, final String threshold, final String minHeadScale, final String maxNumResults, final KairosListener callback) {
        final Handler main = new Handler(Looper.getMainLooper());
        // Spawn a new thread to do the image -> Base64 conversion (Resource heavy operation)
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String base64 = base64FromBitmap(image);
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        recognize(base64, galleryId, selector, threshold, minHeadScale, maxNumResults, callback);
                    }
                });
            }
        }).start();
    }

    /**
     * Convert a bitmap object to a Base64 encoded string.
     * @param image The image to convert.
     * @return A String in Base64 containing the image data.
     */
    protected String base64FromBitmap(Bitmap image) {
        // Create a compressed byte array from the provided image. 80% JPEG seems to be enough
        // for accurate recognition
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        image.compress(CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String encoded = Base64.encodeToString(byteArray, 0);
        return encoded;
    }

    public interface KairosListener {
        void onSuccess(String var1);
        void onFail(String var1);
    }
}
