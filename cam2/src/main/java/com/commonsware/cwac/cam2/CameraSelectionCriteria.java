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

/**
 * Representation of criteria used to select a camera from
 * available cameras on the device. Used with CameraEngine
 * for finding a subset of interesting cameras.
 */
public class CameraSelectionCriteria {
  private AbstractCameraActivity.Facing facing;

  /**
   * Getter method for the facing value, indicating if we
   * want the camera to be front-facing, back-facing, or
   * any direction.
   *
   * @return the facing value
   */
  public AbstractCameraActivity.Facing getFacing() {
    return(facing);
  }

  /**
   * Class to create instances of CameraSelectionCriteria via
   * a fluent, builder-style API.
   */
  public static class Builder {
    final private CameraSelectionCriteria criteria=new CameraSelectionCriteria();

    /**
     * Setter for the facing value, indicating if we
     * want the camera to be front-facing, back-facing, or
     * any direction.
     *
     * @param facing the facing value
     * @return the builder, for chained calls
     */
    public Builder facing(AbstractCameraActivity.Facing facing) {
      criteria.facing=facing;

      return(this);
    }

    /**
     * Returns the fabricated criteria
     *
     * @return the criteria defined via calls on the builder
     */
    public CameraSelectionCriteria build() {
      return(criteria);
    }
  }

}
