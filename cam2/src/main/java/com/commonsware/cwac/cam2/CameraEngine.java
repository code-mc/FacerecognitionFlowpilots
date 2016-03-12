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

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.Log;
import java.io.File;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import de.greenrobot.event.EventBus;

/**
 * Base class for camera engines, which abstract out camera
 * functionality for different APIs (e.g., android.hardware.Camera,
 * android.hardware.camera2.*).
 */
abstract public class CameraEngine {
  private static final int CORE_POOL_SIZE=1;
  private static final int MAX_POOL_SIZE=Runtime.getRuntime().availableProcessors();
  private static final int KEEP_ALIVE_SECONDS=60;
  private static volatile CameraEngine singleton=null;
  private EventBus bus=EventBus.getDefault();
  private boolean isDebug=false;
  private LinkedBlockingQueue<Runnable> queue=new LinkedBlockingQueue<Runnable>();
  private ThreadPoolExecutor pool;

  private static class CrashableEvent {
    /**
     * The exception that was raised when trying to process
     * the request, or null if no such exception was raised.
     */
    public final Exception exception;

    public CrashableEvent() {
      this(null);
    }

    public CrashableEvent(Exception e) {
      if (e!=null) {
        Log.e("CWAC-Cam2", "Exception in camera processing", e);
      }

      this.exception=e;
    }
  }

  /**
   * Event raised when camera descriptors are ready for use.
   * Subscribe to this event if you use loadCameraDescriptors()
   * to get the results. May include an exception if there was
   * an exception accessing the camera.
   *
   * Note that the descriptors will be in a ranked order based
   * on your requested CameraSelectionCriteria, with the best
   * match as the 0th element of the list.
   */
  public static class CameraDescriptorsEvent extends CrashableEvent {
    /**
     * The camera descriptors loaded in response to a call
     * to loadCameraDescriptors()
     */
    public final List<CameraDescriptor> descriptors;

    public CameraDescriptorsEvent(List<CameraDescriptor> descriptors) {
      this.descriptors=descriptors;
    }

    public CameraDescriptorsEvent(Exception exception) {
      super(exception);
      this.descriptors=null;
    }
  }

  /**
   * Event raised when the camera has been opened.
   * Subscribe to this event if you use open()
   * to to find out when the open has succeeded.
   * May include an exception if there was
   * an exception accessing the camera.
   */
  public static class OpenedEvent extends CrashableEvent {
    public OpenedEvent() {
      super();
    }

    public OpenedEvent(Exception exception) {
      super(exception);
    }
  }

  /**
   * Event raised when the camera has been closed.
   * Subscribe to this event if you use close()
   * to to find out when the close has completed.
   * May include an exception if there was
   * an exception closing the camera.
   */
  public static class ClosedEvent extends CrashableEvent {
    public ClosedEvent() {
      super();
    }

    public ClosedEvent(Exception exception) {
      super(exception);
    }
  }

  /**
   * Event raised when picture is taken, as a result of a
   * takePicture() call. May include an exception if there was
   * an exception accessing the camera.
   */
  public static class PictureTakenEvent extends CrashableEvent {
    private ImageContext imageContext=null;

    public PictureTakenEvent(ImageContext imageContext) {
      super();
      this.imageContext=imageContext;
    }

    public PictureTakenEvent(Exception exception) {
      super(exception);
    }

    public ImageContext getImageContext() {
      return(imageContext);
    }
  }

  /**
   * Event raised when picture is taken, as a result of a
   * takePicture() call. May include an exception if there was
   * an exception accessing the camera.
   */
  public static class VideoTakenEvent extends CrashableEvent {
    private VideoTransaction xact;

    public VideoTakenEvent(VideoTransaction xact) {
      super();
      this.xact=xact;
    }

    public VideoTakenEvent(Exception exception) {
      super(exception);
    }

    public VideoTransaction getVideoTransaction() {
      return(xact);
    }
  }

  /**
   * Create a CameraSession.Builder to build a CameraSession
   * for a given CameraDescriptor. On the Builder is where you
   * indicate your desired preview size, picture size, and
   * so forth.
   *
   * @param ctxt an Android Context for accessing system stuff
   * @param descriptor the CameraDescriptor for which we want
   *                   a session
   * @return a Builder to build that session
   */
  abstract public CameraSession.Builder buildSession(Context ctxt,
                                                     CameraDescriptor descriptor);

  /**
   * Loads a roster of the available cameras for this engine,
   * ranked based on the supplied criteria. Subscribe
   * to the CameraDescriptorsEvent to get the results of this
   * call asynchronously.
   *
   * @param criteria preferred camera capabilities, or
   *                 null for a default ranking
   */
  abstract public void loadCameraDescriptors(CameraSelectionCriteria criteria);

  /**
   * Open the requested camera and show a preview on the supplied
   * surface. Subscribe to the OpenEvent to find out when this
   * work is completed.
   *
   * @param session the session for the camera of interest
   * @param texture the preview surface
   */
  abstract public void open(CameraSession session,
                            SurfaceTexture texture);

  /**
   * Close the open camera. Subscribe to the ClosedEvent to
   * find out when this work is completed. Note that this
   * method may be synchronous or asynchronous.
   *
   * @param session the session for the camera of interest
   */
  abstract public void close(CameraSession session);

  /**
   * Take a picture, on the supplied camera, using the picture
   * configuration from the supplied transaction. Posts a
   * PictureTakenEvent when the request is completed, successfully
   * or unsuccessfully.
   *
   * @param session the session for the camera of interest
   * @param xact the configuration of the picture to take
   */
  abstract public void takePicture(CameraSession session,
                                   PictureTransaction xact);

  abstract public void recordVideo(CameraSession session,
      VideoTransaction xact) throws Exception;

  abstract public void stopVideoRecording(CameraSession session) throws Exception;

  /**
   * Builds a CameraEngine instance based on the device's
   * API level.
   *
   * @param ctxt any Context will do
   * @param forceClassic if true, always use ClassicCameraEngine
   * @return a new CameraEngine instance
   */
  synchronized public static CameraEngine buildInstance(Context ctxt,
                                                        boolean forceClassic) {
    if (singleton==null) {
      if (!forceClassic &&
          Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
        singleton=new CameraTwoEngine(ctxt);
      }
      else {
        singleton=new ClassicCameraEngine();
      }
    }

    return(singleton);
  }

  /**
   * Sets the event bus to use, where the default is the
   * default event bus supplied by the EventBus class.
   *
   * @param bus the bus to use for events
   */
  public void setBus(EventBus bus) {
    this.bus=bus;
  }

  /**
   * @return the bus to use for events
   */
  public EventBus getBus() {
    return(bus);
  }

  /**
   * Sets whether or not exceptions should be logged, in addition
   * to being included in relevant events. The default is false.
   *
   * @param isDebug true if exceptions should be logged, false otherwise
   */
  public void setDebug(boolean isDebug) {
    this.isDebug=isDebug;
  }

  /**
   * @return true if exceptions should be logged, false otherwise
   */
  public boolean isDebug() {
    return(isDebug);
  }

  public ThreadPoolExecutor getThreadPool() {
    if (pool==null) {
      pool=new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE,
          KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, queue);
    }

    return(pool);
  }

  public void setThreadPool(ThreadPoolExecutor pool) {
    this.pool=pool;
  }
}
