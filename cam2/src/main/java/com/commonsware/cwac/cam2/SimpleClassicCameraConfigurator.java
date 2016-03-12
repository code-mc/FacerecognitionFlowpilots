package com.commonsware.cwac.cam2;

import android.hardware.Camera;

/**
 * Stub no-op implementation of ClassicCameraConfigurator,
 * to simplify implementing the interface. Just extend this class,
 * then override the particular methods that you need in the plugin.
 */
public class SimpleClassicCameraConfigurator implements ClassicCameraConfigurator {
  @Override
  public Camera.Parameters configure(Camera.CameraInfo info,
                                     Camera camera, Camera.Parameters params) {
    return null;
  }
}
