# Journal App for Android

## Installation Instructions

* install JDK version 7 or higher
* install maven version 3.1.1 or higher
* install Android SDK

* make sure M2_HOME/bin folder has added to your PATH variable
* make sure ANDROID_HOME/platform-tools folder has added to your PATH variable

```sh
# To build app run command:
mvn clean install
```

To install app on your device run command:
```sh
adb install dummy-application/target/dummy-application.apk
```

## License

    Journal App for Android
    Copyright (C) 2019 John Wiley & Sons, Inc.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
