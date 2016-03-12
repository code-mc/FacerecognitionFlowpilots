/***
 Copyright (c) 2015 CommonsWare, LLC

 Licensed under the Apache License, Version 2.0 (the "License"); you may
 not use this file except in compliance with the License. You may obtain
 a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.commonsware.cwac.cam2;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import com.commonsware.cwac.cam2.util.Utils;
import java.io.File;
import java.util.ArrayList;
import de.greenrobot.event.EventBus;

/**
 * Base class for activities that integrate with CameraFragment
 * for taking pictures or recording video.
 */
abstract public class AbstractCameraActivity extends Activity {
  /**
   * @return true if the activity wants FEATURE_ACTION_BAR_OVERLAY,
   * false otherwise
   */
  abstract protected boolean needsOverlay();

  /**
   * @return false if we should hide the action bar outright
   * (ignored if needsOverlay() returns true)
   */
  abstract protected boolean needsActionBar();

  /**
   * @return true if we are recording a video, false if we are
   * taking a still picture
   */
  abstract protected boolean isVideo();

  /**
   * @return a CameraFragment for the given circumstances
   */
  abstract protected CameraFragment buildFragment();

  /**
   * @return array of the names of the permissions needed by
   * this activity
   */
  abstract protected String[] getNeededPermissions();

  /**
   * Extra name for indicating what facing rule for the
   * camera you wish to use. The value should be a
   * CameraSelectionCriteria.Facing instance.
   */
  public static final String EXTRA_FACING="cwac_cam2_facing";
  /**
   * Extra name for indicating whether extra diagnostic
   * information should be reported, particularly for errors.
   * Default is false.
   */
  public static final String EXTRA_DEBUG_ENABLED="cwac_cam2_debug";
  /**
   * Extra name for indicating if MediaStore should be updated
   * to reflect a newly-taken picture. Only relevant if
   * a file:// Uri is used. Default to false.
   */
  public static final String EXTRA_UPDATE_MEDIA_STORE=
      "cwac_cam2_update_media_store";
  /**
   * If set to true, forces the use of the ClassicCameraEngine
   * on Android 5.0+ devices. Has no net effect on Android 4.x
   * devices. Defaults to false.
   */
  public static final String EXTRA_FORCE_CLASSIC="cwac_cam2_force_classic";

  protected static final String TAG_CAMERA=CameraFragment.class.getCanonicalName();
  private static final int REQUEST_PERMS=13401;
  protected CameraFragment cameraFrag;

  /**
   * Standard lifecycle method, serving as the main entry
   * point of the activity.
   *
   * @param savedInstanceState the state of a previous instance
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Utils.validateEnvironment(this);

    if (needsOverlay()) {
      getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

      // the following is nasty stuff to get rid of the action
      // bar drop shadow, which still exists on some devices
      // despite going into overlay mode (Samsung Galaxy S3, I'm
      // looking at you)

      if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
        ActionBar ab=getActionBar();

        if (ab!=null) {
          getActionBar().setElevation(0);
        }
      }
      else if (Build.VERSION.SDK_INT<Build.VERSION_CODES.KITKAT) {
/*
        View v=findViewById(android.R.id.content);

        if (v instanceof FrameLayout) {
          v.setForeground(null);
        }
*/
      }
      else {
        View v=((ViewGroup)getWindow().getDecorView()).getChildAt(0);

        if (v!=null) {
          v.setWillNotDraw(true);
        }
      }

    }
    else if (!needsActionBar()) {
      ActionBar ab=getActionBar();

      if (ab!=null) {
        ab.hide();
      }
    }

    String[] perms=netPermissions(getNeededPermissions());

    if (perms.length==0) {
      init();
    }
    else {
      requestPermissions(perms, REQUEST_PERMS);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String[] permissions,
                                         int[] grantResults) {
    String[] perms=netPermissions(getNeededPermissions());

    if (perms.length==0) {
      init();
    }
    else {
      setResult(RESULT_CANCELED);
      finish();
    }
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

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (keyCode==KeyEvent.KEYCODE_CAMERA) {
      cameraFrag.performCameraAction();

      return(true);
    }

    return(super.onKeyUp(keyCode, event));
  }

  @SuppressWarnings("unused")
  public void onEventMainThread(CameraController.NoSuchCameraEvent event) {
    finish();
  }

  @SuppressWarnings("unused")
  public void onEventMainThread(CameraController.ControllerDestroyedEvent event) {
    finish();
  }

  protected Uri getOutputUri() {
    Uri output=null;

    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR1) {
      ClipData clipData=getIntent().getClipData();

      if (clipData!=null && clipData.getItemCount() > 0) {
        output=clipData.getItemAt(0).getUri();
      }
    }

    if (output==null) {
      output=getIntent().getParcelableExtra(MediaStore.EXTRA_OUTPUT);
    }

    return(output);
  }

  protected void init() {
    cameraFrag=(CameraFragment)getFragmentManager().findFragmentByTag(TAG_CAMERA);

    if (cameraFrag==null) {
      cameraFrag=buildFragment();

      CameraController ctrl=new CameraController();

      cameraFrag.setController(ctrl);

      Facing facing=
        (Facing)getIntent().getSerializableExtra(EXTRA_FACING);

      if (facing==null) {
        facing=Facing.BACK;
      }

      CameraSelectionCriteria criteria=
        new CameraSelectionCriteria.Builder().facing(facing).build();
      boolean forceClassic=getIntent().getBooleanExtra(EXTRA_FORCE_CLASSIC, false);

      ctrl.setEngine(CameraEngine.buildInstance(this, forceClassic), criteria);
      ctrl.getEngine().setDebug(getIntent().getBooleanExtra(EXTRA_DEBUG_ENABLED, false));
      getFragmentManager()
        .beginTransaction()
        .add(android.R.id.content, cameraFrag, TAG_CAMERA)
        .commit();
    }
  }

  private boolean hasPermission(String perm) {
    if (useRuntimePermissions()) {
      return(checkSelfPermission(perm)==PackageManager.PERMISSION_GRANTED);
    }

    return(true);
  }

  private boolean useRuntimePermissions() {
    return(Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP_MR1);
  }

  private String[] netPermissions(String[] wanted) {
    ArrayList<String> result=new ArrayList<String>();

    for (String perm : wanted) {
      if (!hasPermission(perm)) {
        result.add(perm);
      }
    }

    return(result.toArray(new String[result.size()]));
  }

  /**
   * Possible values for the facing property
   */
  public enum Facing {
    FRONT, BACK;

    boolean isFront() {
      return(this==FRONT);
    }
  }

  public static class IntentBuilder {
    protected final Intent result;

    /**
     * Standard constructor. May throw a runtime exception
     * if the environment is not set up properly (see
     * validateEnvironment() on Utils).
     *
     * @param ctxt any Context will do
     */
    public IntentBuilder(Context ctxt, Class clazz) {
      Utils.validateEnvironment(ctxt);
      result=new Intent(ctxt, clazz);
    }

    /**
     * Returns the Intent defined by the builder.
     *
     * @return the Intent to use to start the CameraActivity
     */
    public Intent build() {
      return(result);
    }

    /**
     * Indicates what camera should be used as the starting
     * point. Defaults to the rear-facing camera.
     *
     * @param facing which camera to use
     * @return the builder, for further configuration
     */
    public IntentBuilder facing(Facing facing) {
      result.putExtra(EXTRA_FACING, facing);

      return(this);
    }

    /**
     * Call if you want extra diagnostic information dumped to
     * LogCat. Not ideal for use in production.
     *
     * @return the builder, for further configuration
     */
    public IntentBuilder debug() {
      result.putExtra(EXTRA_DEBUG_ENABLED, true);

      return(this);
    }

    /**
     * Indicates where to write the picture to. Defaults to
     * returning a thumbnail bitmap in the "data" extra, as
     * with ACTION_IMAGE_CAPTURE. Note that you need to have
     * write access to the supplied file.
     *
     * @param f file in which to write the picture
     * @return the builder, for further configuration
     */
    public IntentBuilder to(File f) {
      return(to(Uri.fromFile(f)));
    }

    /**
     * Indicates where to write the picture to. Defaults to
     * returning a thumbnail bitmap in the "data" extra, as
     * with ACTION_IMAGE_CAPTURE. Note that you need to have
     * write access to the supplied Uri.
     *
     * @param output Uri to which to write the picture
     * @return the builder, for further configuration
     */
    public IntentBuilder to(Uri output) {
      result.putExtra(MediaStore.EXTRA_OUTPUT, output);

      return(this);
    }

    /**
     * Indicates that the picture that is taken should be
     * passed over to MediaStore for indexing. By default,
     * this does not happen automatically and is the responsibility
     * of your app, should the image be reachable by MediaStore
     * in the first place. This setting is only relevant for file://
     * Uri values.
     *
     * @return the builder, for further configuration
     */
    public IntentBuilder updateMediaStore() {
      result.putExtra(EXTRA_UPDATE_MEDIA_STORE, true);

      return(this);
    }

    /**
     * Forces the use of ClassicCameraEngine on Android 5.0+ devices.
     *
     * @return the builder, for further configuration
     */
    public IntentBuilder forceClassic() {
      result.putExtra(EXTRA_FORCE_CLASSIC, true);

      return(this);
    }
  }
}
