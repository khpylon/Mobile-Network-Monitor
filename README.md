# Mobile Network Monitor

## Intro

The purpose of this app is to alert you when your phone goes in and out of service.  More specifically, if the phone is out of
service and then goes back into service, it will play a user-selectable ringtone to let you know this has happened.

## Requirements

This app was developed on Android 17 but targets Android 13 (API 33).  There is no guarantee it will work on any earlier version.

## Contributing Language Translations

If you would like to use the app in your preferred language but there is no translation for it, read [how to submit one.](https://github.com/khpylon/Mobile-Network-Monitor/blob/master/TRANSLATIONS.md)

## Bug Reports

The app will soon have the ability to detect an app crash and generate reports.  This happens whenever the app is run. It will
write a file named *mobnetman-<datetime>.txt* to the *Downloads* folder of your phone, and
display a brief message.  Google will also upload analytics when a crash occurs, but they often do not contain as much detail as the crash report. Since the app does not communicate with the Internet, you will need to manually upload the crash report file to ["Issues"](https://github.com/khpylon/Mobile-Network-Monitor/issues) on GitHub.  I'll then try to figure out why the app crashed and how to fix it.


## Disclaimer

I am NOT liable for any kind of damage (special, direct, indirect, consequential or whatsoever) resulting from the use of this app. 
