# Sample Face Login App

Example application for a face login using the Kairos API.

## Usage

### First launch

 1. Build the application.
 2. On first launch you will be asked to callibrate the camera. Snap a picture and rotate the result until it lines up correctly.
 
### Training

 1. First you have to train using some sample pictures. Tap the "Set Picture" button and select a name for which you want to add pictures.
 2. Snap a few pictures (3-5 seems enough) from slightly different angles. Make sure your whole face is visible (including both ears). Also, make sure it is evenly lit over the complete surface. So no dark halves etc, either stand face to a light or face away. Following these guidelines will result in the best recognition further down the line.
 3. Back out of the activity by pressing the back button.

### Recognition

 1. Once you finished training you can sign in using the "Sign In" button.
 2. Snap a selfie and wait a bit. If your face was successfully recognized it will redirect to the previous activty and give a little "Welcome back {your name}" toast. If not it will give an error message and you should just try again. Keep the previously mentioned ideal conditions in mind while snapping a picture for signing in!
 3. Once you are signed in there is not much left to do inside the app.
 
### Extras

 * Registered wrong training data, or bad data, open the overflow menu and tap the "Delete Training Data" option.
 * Messed up the calibration step? Do it again by tapping its option inside the overflow menu.
 
## License

Released under the GPLv3 [https://github.com/code-mc/FacerecognitionFlowpilots/blob/master/license.md](license).

>> FacerecognitionFlowpilots
>>
>> Copyright (C) 2016 code-mc
>>
>> This program is free software: you can redistribute it and/or modify
>> it under the terms of the GNU General Public License as published by
>> the Free Software Foundation, either version 3 of the License, or
>> (at your option) any later version.
>>
>> This program is distributed in the hope that it will be useful,
>> but WITHOUT ANY WARRANTY; without even the implied warranty of
>> MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
>> GNU General Public License for more details.
>>
>> You should have received a copy of the GNU General Public License
>> along with this program.  If not, see http://www.gnu.org/licenses .