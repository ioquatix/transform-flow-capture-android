# Video Stream Capture for Android

Video Stream Capture for Android is a tool for capturing and logging sensor data and video frames on Android devices. It uses CSV for storing data.

## Build and Install

This project has an Eclipse project file for building and running the capture tool.

To run this project, you must have the following libraries in your Eclipse-workspace and linked to the project:
- google-play-services_lib (for GPS access, ...) (see http://developer.android.com/google/play-services/setup.html)
- OpenCV for Android (download from http://opencv.org/downloads.html with more infos http://opencv.org/platforms/android.html)
-> Download Android SDK (~90MB)
-> Extract and copy/import the project from OpenCV-2.4.6-android-sdk\sdk\java into you workspace
-> Link it to the application

For convenience, these two libraries are also provided in the repository (with the state of September 2013).

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request

## License

Released under the MIT license.

Copyright, 2013, by [Alexander Pacha](http://my-it.at), Carolin Reichherzer and [Samuel G. D. Williams](http://www.codeotaku.com/samuel-williams).

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.