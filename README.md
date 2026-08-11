# cordova-plugin-fbsdk

> Use Facebook SDK in Cordova projects

## Why this fork exists

Forked from [cordova-plugin-facebook-connect](https://github.com/cordova-plugin-facebook-connect/cordova-plugin-facebook-connect), which has published no release since `3.2.0` in April 2022. Its `master` still pins Facebook SDK 11.x, seven major versions behind, and Apple and Google requirements have moved several times since. This fork exists to keep a maintained plugin available for projects already depending on that integration, rather than to change its API — it stays a drop-in replacement.

Published as [cordova-plugin-fbsdk](https://www.npmjs.com/package/cordova-plugin-fbsdk).

Changes from upstream:

- Tracks current Facebook SDK releases. Upstream is on Android 11.3.0 / iOS 11.1.0 / browser JS SDK v11.0; this fork is on Android 18.3.0 / iOS 18.1.0 / v22.0.
- Follows current Cordova platform releases — the build configuration and project layout that recent cordova-android and cordova-ios versions generate, which is where most of the breakage in a plugin this old comes from.
- Bug reports get triaged and fixed here. Upstream has 43 open issues and no commits since June 2024.

## Table of contents

- [Why this fork exists](#why-this-fork-exists)
- [Installation](#installation)
- [Usage](#usage)
- [Demo](#sample-repo)
- [Compatibility](#compatibility)
- [Facebook SDK](#facebook-sdk)
- [API](#api)

## Installation

See npm package for versions - https://www.npmjs.com/package/cordova-plugin-fbsdk

Make sure you've registered your Facebook app with Facebook and have an `APP_ID` [https://developers.facebook.com/apps](https://developers.facebook.com/apps).

```bash
$ cordova plugin add cordova-plugin-fbsdk --save --variable APP_ID="123456789" --variable APP_NAME="myApplication" --variable CLIENT_TOKEN="abcd1234"
```

As the `APP_NAME` is used as a string in XML files, if your app name contains any special characters like "&", make sure you escape them, e.g. "&amp;".

If you need to change your `APP_ID` after installation, it's recommended that you remove and then re-add the plugin as above. Note that changes to the `APP_ID` value in your `config.xml` file will *not* be propagated to the individual platform builds.

### Installation Guides

- [iOS Guide](docs/ios/README.md)

- [Android Guide](docs/android/README.md)

- [Browser Guide](docs/browser/README.md)

- [Troubleshooting Guide | F.A.Q.](docs/TROUBLESHOOTING.md)

## Usage

This is a fork of the [official plugin for Facebook](https://github.com/Wizcorp/phonegap-facebook-plugin/) in Apache Cordova that implements the latest Facebook SDK. Unless noted, this is a drop-in replacement. You don't have to replace your client code.

The Facebook plugin for [Apache Cordova](http://cordova.apache.org/) allows you to use the same JavaScript code in your Cordova application as you use in your web application.

## Demo

If you are looking to test the plugin, would like to reproduce a bug or build issues, there is a demo project for such purpose: [demo](demo).

## Compatibility

  * Cordova >= 5.0.0
  * cordova-android >= 9.0.0
  * cordova-ios >= 6.0.0
  * cordova-browser >= 3.6

## Facebook SDK

This plugin use the SDKs provided by Facebook. More information about these in their documentation for [iOS](https://developers.facebook.com/docs/ios/) or [Android](https://developers.facebook.com/docs/android/)

### Facebook SDK version

This plugin will always be released for iOS and for Android with a synchronized usage of the Facebook SDKs.

### Graph API version

Please note that this plugin itself does not specify which Graph API version is used. The Graph API version is set by the Facebook SDK for iOS and Android (see [Facebook documentation about versioning](https://developers.facebook.com/docs/apps/versions/))

## API

### Get Application ID and Name

`facebookConnectPlugin.getApplicationId(Function success)`

Success function returns the current application ID.

`facebookConnectPlugin.getApplicationName(Function success)`

Success function returns the current application name.

### Set Application ID and Name

By default, the APP_ID and APP_NAME provided when the plugin is added are used. If you instead need to set the application ID and name in code, you can do so. (You must still include an APP_ID and APP_NAME when adding the plugin, as the values are required for the Android manifest and *-Info.plist files.)

`facebookConnectPlugin.setApplicationId(String id, Function success)`

Success function indicates the application ID has been updated.

`facebookConnectPlugin.setApplicationName(String name, Function success)`

Success function indicates the application name has been updated.

Note that in order to dynamically switch between multiple app IDs on iOS, you must use the *OTHER_APP_SCHEMES* variable and specify each additional app ID you will use with `setApplicationId` separated by a comma, e.g.

```bash
$ cordova plugin add cordova-plugin-fbsdk --save --variable APP_ID="123456789" --variable APP_NAME="myApplication" --variable CLIENT_TOKEN="abcd1234" --variable OTHER_APP_SCHEMES="fb987654321,fb876543210,fb765432109"
```

### Get Client Token

`facebookConnectPlugin.getClientToken(Function success)`

Success function returns the current Client Token.

### Set Client Token

By default, the CLIENT_TOKEN provided when the plugin is added is used. If you instead need to set the Client Token in code, you can do so. (You must still include a CLIENT_TOKEN when adding the plugin.)

`facebookConnectPlugin.setClientToken(String token, Function success, Function failure)`

Success function indicates the Client Token has been updated.

Failure function returns an error String.

### Login

`facebookConnectPlugin.login(Array strings of permissions, Function success, Function failure)`

Success function returns an Object like:

	{
		status: "connected",
		authResponse: {
			accessToken: "<long string>",
			data_access_expiration_time: "1623680244",
			expiresIn: "5183979",
			userID: "634565435"
		}
	}

  Failure function returns an Object like:

  	{
  		errorCode: "4201",
  		errorMessage: "User cancelled"
  	}

### Limited Login (iOS Only)

`facebookConnectPlugin.loginWithLimitedTracking(Array strings of permissions, String nonce, Function success, Function failure)`

Success function returns an Object like:

	{
		status: "connected",
		authResponse: {
			authenticationToken: "<long string>",
			nonce: "foo",
			userID: "634565435"
		}
	}

Failure function returns an Object like:

	{
		errorCode: "4201",
		errorMessage: "User cancelled"
	}

See the [Facebook Developer documentation](https://developers.facebook.com/docs/facebook-login/limited-login/ios/) for more details.

### Logout

`facebookConnectPlugin.logout(Function success, Function failure)`

### Get Current Profile

`facebookConnectPlugin.getCurrentProfile(Function success, Function failure)`

Success function returns an Object like:

	{
		userID: "634565435",
		firstName: "Woodrow",
		lastName: "Derenberger"
	}

**Note: The profile object contains a different set of properties when using Limited Login on iOS.**

Failure function returns an error String.

### Check permissions

`facebookConnectPlugin.checkHasCorrectPermissions(Array strings of permissions, Function success, Function failure)`

Success function returns a success string if all passed permissions are granted.

Failure function returns an error String if any passed permissions are not granted.

### Get Status

`facebookConnectPlugin.getLoginStatus(Boolean force, Function success, Function failure)`

Setting the force parameter to true clears any previously cached status and fetches fresh data from Facebook.

Success function returns an Object like:

```
{
	authResponse: {
		accessToken: "kgkh3g42kh4g23kh4g2kh34g2kg4k2h4gkh3g4k2h4gk23h4gk2h34gk234gk2h34AndSoOn",
		data_access_expiration_time: "1623680244",
		expiresIn: "5183738",
		userID: "12345678912345"
	},
	status: "connected"
}
```

For more information see: [Facebook Documentation](https://developers.facebook.com/docs/reference/javascript/FB.getLoginStatus)

### Check if data access is expired

`facebookConnectPlugin.isDataAccessExpired(Function success, Function failure)`

Success function returns a String indicating if data access is expired.

Failure function returns an error String.

For more information see: [Facebook Documentation](https://developers.facebook.com/docs/facebook-login/auth-vs-data/#testing-when-access-to-user-data-expires)

### Reauthorize data access

`facebookConnectPlugin.reauthorizeDataAccess(Function success, Function failure)`

Success function returns an Object like:

	{
		status: "connected",
		authResponse: {
			accessToken: "<long string>",
			data_access_expiration_time: "1623680244",
			expiresIn: "5183979",
			userID: "634565435"
		}
	}

Failure function returns an error String.

For more information see: [Facebook Documentation](https://developers.facebook.com/docs/facebook-login/auth-vs-data/#data-access-expiration)

### Show a Dialog

`facebookConnectPlugin.showDialog(Object options, Function success, Function failure)`

Example options -
Share Dialog:

	{
		method: "share",
		href: "http://example.com",
		hashtag: '#myHashtag',
		share_feedWeb: true, // iOS only
	}

#### iOS

The default dialog mode is [`FBSDKShareDialogModeAutomatic`](https://developers.facebook.com/docs/reference/ios/current/constants/FBSDKShareDialogMode/). You can share that by adding a specific dialog mode parameter. The available share dialog modes are: `share_sheet`, `share_feedBrowser`, `share_native` and `share_feedWeb`. [Read more about share dialog modes](https://developers.facebook.com/docs/reference/ios/current/constants/FBSDKShareDialogMode/)

Share Photo Dialog:

	{
		method: "share",
		photo_image: "/9j/4TIERXhpZgAATU0AKgAAAA..."
	}

*photo_image* must be a Base64-encoded string, such as a value returned by [cordova-plugin-camera](https://www.npmjs.com/package/cordova-plugin-camera) or [cordova-plugin-file](https://www.npmjs.com/package/cordova-plugin-file). Note that you must provide only the Base64 data, so if you have a data URL returned by something like `FileReader` that looks like "data:image/jpeg;base64,/9j/4TIERXhpZgAATU0AKgAAAA...", you should split on ";base64,", e.g. `myDataUrl.split(';base64,')[1]`.

Here's a basic example using the camera plugin:

```js
navigator.camera.getPicture(function(dataUrl) {
  facebookConnectPlugin.showDialog({
    method: 'share', 
    photo_image: dataUrl
  }, function() {
    console.log('share success');
  }, function(e) {
    console.log('share error', e);
  });
}, function(e) {
  console.log('camera error', e);
}, {
  quality: 100, 
  sourceType: Camera.PictureSourceType.CAMERA, 
  destinationType: Camera.DestinationType.DATA_URL
});
```

Game request:

	{
		method: "apprequests",
		message: "Come on man, check out my application.",
		data: data,
		title: title,
		actionType: 'askfor',
		objectID: 'YOUR_OBJECT_ID', 
		filters: 'app_non_users'
	}

Send Dialog:

	{
		method: "send",
		link: "http://example.com"
	}


For options information see: [Facebook share dialog documentation](https://developers.facebook.com/docs/sharing/reference/share-dialog) [Facebook send dialog documentation](https://developers.facebook.com/docs/sharing/reference/send-dialog)

Success function returns an Object or `from` and `to` information when doing `apprequest`.

Failure function returns an error String.

### The Graph API

`facebookConnectPlugin.api(String requestPath, Array permissions, String httpMethod, Function success, Function failure)`

Allows access to the Facebook Graph API. This API allows for additional permission because, unlike login, the Graph API can accept multiple permissions.

Example permissions:

	["public_profile", "user_birthday"]

`httpMethod` is optional and defaults to "GET".

Success function returns an Object.

Failure function returns an error String.

**Note: "In order to make calls to the Graph API on behalf of a user, the user has to be logged into your app using Facebook login, and you must include the access_token parameter in your requestPath. "**

For more information see:

- Calling the Graph API - [https://developers.facebook.com/docs/ios/graph](https://developers.facebook.com/docs/ios/graph)
- Graph Explorer - [https://developers.facebook.com/tools/explorer](https://developers.facebook.com/tools/explorer)
- Graph API - [https://developers.facebook.com/docs/graph-api/](https://developers.facebook.com/docs/graph-api/)
- Access Levels - [https://developers.facebook.com/docs/graph-api/overview/access-levels/](https://developers.facebook.com/docs/graph-api/overview/access-levels/)

### Events

App events allow you to understand the makeup of users engaging with your app, measure the performance of your Facebook mobile app ads, and reach specific sets of your users with Facebook mobile app ads.

- [iOS] [https://developers.facebook.com/docs/ios/app-events](https://developers.facebook.com/docs/ios/app-events)
- [Android] [https://developers.facebook.com/docs/android/app-events](https://developers.facebook.com/docs/android/app-events)
- [JS] Does not have an Events API, so the plugin functions are empty and will return an automatic success

Activation events are automatically tracked for you in the plugin.

Events are listed on the [insights page](https://www.facebook.com/insights/)

#### Log an Event

`logEvent(String name, Object params, Number valueToSum, Function success, Function failure)`

- **name**, name of the event
- **params**, extra data to log with the event (is optional)
- **valueToSum**, a property which is an arbitrary number that can represent any value (e.g., a price or a quantity). When reported, all of the valueToSum properties will be summed together. For example, if 10 people each purchased one item that cost $10 (and passed in valueToSum) then they would be summed to report a number of $100. (is optional)

#### Log a Purchase

`logPurchase(Number value, String currency, Object params, Function success, Function failure)`

**NOTE:** Both `value` and `currency` are required. The currency specification is expected to be an [ISO 4217 currency code](http://en.wikipedia.org/wiki/ISO_4217). `params` is optional.

#### Manually log activation events

`activateApp(Function success, Function failure)`

#### Data Processing Options

This plugin allows developers to set Data Processing Options as part of compliance with the California Consumer Privacy Act (CCPA).

`setDataProcessingOptions(Array strings of options, String country, String state, Function success, Function failure)`

To explicitly not enable Limited Data Use (LDU) mode, use:

```js
facebookConnectPlugin.setDataProcessingOptions([], null, null, function() {
  console.log('setDataProcessingOptions success');
}, function() {
  console.error('setDataProcessingOptions failure');
});
```

To enable LDU with geolocation, use:

```js
facebookConnectPlugin.setDataProcessingOptions(["LDU"], 0, 0, function() {
  console.log('setDataProcessingOptions success');
}, function() {
  console.error('setDataProcessingOptions failure');
});
```

To enable LDU for users and specify user geography, use:

```js
facebookConnectPlugin.setDataProcessingOptions(["LDU"], 1, 1000, function() {
  console.log('setDataProcessingOptions success');
}, function() {
  console.error('setDataProcessingOptions failure');
});
```

For more information see: [Facebook Documentation](https://developers.facebook.com/docs/app-events/guides/ccpa)

#### Advanced Matching

With [Advanced Matching](https://developers.facebook.com/docs/app-events/advanced-matching/), Facebook can match conversion events to your customers to optimize your ads and build larger re-marketing audiences.

`setUserData(Object userData, Function success, Function failure)`

- **userData**, an object containing the user data to use for matching

Example user data object:

	{
		"em": "jsmith@example.com", //email
		"fn": "john", //first name
		"ln": "smith", //last name
		"ph", "16505554444", //phone number
		"db": "19910526", //birthdate
		"ge": "f", //gender
		"ct": "menlopark", //city
		"st": "ca", //state
		"zp": "94025", //zip code
		"cn": "us" //country
	}

Success function indicates the user data has been set.

Failure function returns an error String.

`clearUserData(Function success, Function failure)`

Success function indicates the user data has been cleared.

Failure function returns an error String.

### Login

In your `onDeviceReady` event add the following

```js
var fbLoginSuccess = function (userData) {
  console.log("UserInfo: ", userData);
}

facebookConnectPlugin.login(["public_profile"], fbLoginSuccess,
  function loginError (error) {
    console.error(error)
  }
);
```

### Get Access Token

If you need the Facebook access token (for example, for validating the login on server side), do:
```js
var fbLoginSuccess = function (userData) {
  console.log("UserInfo: ", userData);
  facebookConnectPlugin.getAccessToken(function(token) {
    console.log("Token: " + token);
  });
}

facebookConnectPlugin.login(["public_profile"], fbLoginSuccess,
  function (error) {
    console.error(error)
  }
);
```

### Get Status and Post-to-wall

For a more instructive example change the above `fbLoginSuccess` to;

```js
var fbLoginSuccess = function (userData) {
  console.log("UserInfo: ", userData);
  facebookConnectPlugin.getLoginStatus(false, function onLoginStatus (status) {
    console.log("current status: ", status);
    facebookConnectPlugin.showDialog({
      method: "share"
    }, function onShareSuccess () {
      console.log("Posted.");
    });
  });
};
```

### Getting a User's Birthday

Using the graph api this is a very simple task:

```js
facebookConnectPlugin.api("me/?fields=id,birthday&access_token=" + myAccessToken, ["user_birthday"],
  function onSuccess (result) {
    console.log("Result: ", result);
    /* logs:
      {
        "id": "000000123456789",
        "birthday": "01/01/1985"
      }
    */
  }, function onError (error) {
    console.error("Failed: ", error);
  }
);
```

## Hybrid Mobile App Events

Starting from Facebook SDK v4.34 for both iOS and Android, there is a new way of converting pixel events into mobile app events. For more information: [https://developers.facebook.com/docs/app-events/hybrid-app-events/](https://developers.facebook.com/docs/app-events/hybrid-app-events/)

In order to enable this feature in your Cordova app, please set the *FACEBOOK_HYBRID_APP_EVENTS* variable to "true" (default is false):

```bash
$ cordova plugin add cordova-plugin-fbsdk --save --variable APP_ID="123456789" --variable APP_NAME="myApplication" --variable CLIENT_TOKEN="abcd1234" --variable FACEBOOK_HYBRID_APP_EVENTS="true"
```

Please check [this repo](https://github.com/msencer/fb_hybrid_app_events_sample) for an example app using this feature.

## GDPR Compliance

This plugin supports Facebook's [GDPR Compliance](https://developers.facebook.com/docs/app-events/gdpr-compliance/) **Delaying Automatic Event Collection**.

In order to enable this feature in your Cordova app, please set the *FACEBOOK_AUTO_LOG_APP_EVENTS* variable to "false" (default is true).

```bash
$ cordova plugin add cordova-plugin-fbsdk --save --variable APP_ID="123456789" --variable APP_NAME="myApplication" --variable CLIENT_TOKEN="abcd1234" --variable FACEBOOK_AUTO_LOG_APP_EVENTS="false"
```

Then, re-enable auto-logging after an end User provides consent by calling the `setAutoLogAppEventsEnabled` method and set it to true.

```js
facebookConnectPlugin.setAutoLogAppEventsEnabled(true, function() {
  console.log('setAutoLogAppEventsEnabled success');
}, function() {
  console.error('setAutoLogAppEventsEnabled failure');
});
```

## Collection of Advertiser IDs

To disable collection of `advertiser-id`, please set the *FACEBOOK_ADVERTISER_ID_COLLECTION* variable to "false" (default is true).

```bash
$ cordova plugin add cordova-plugin-fbsdk --save --variable APP_ID="123456789" --variable APP_NAME="myApplication" --variable CLIENT_TOKEN="abcd1234" --variable FACEBOOK_ADVERTISER_ID_COLLECTION="false"
```

Then, re-enable collection by calling the `setAdvertiserIDCollectionEnabled` method and set it to true.

```js
facebookConnectPlugin.setAdvertiserIDCollectionEnabled(true, function() {
  console.log('setAdvertiserIDCollectionEnabled success');
}, function() {
  console.error('setAdvertiserIDCollectionEnabled failure');
});
```

## Advertiser Tracking Enabled (iOS Only)

To enable advertiser tracking, call the `setAdvertiserTrackingEnabled` method.

```js
facebookConnectPlugin.setAdvertiserTrackingEnabled(true, function() {
  console.log('setAdvertiserTrackingEnabled success');
}, function() {
  console.error('setAdvertiserTrackingEnabled failure');
});
```

See the [Facebook Developer documentation](https://developers.facebook.com/docs/app-events/guides/advertising-tracking-enabled/) for more details.

## App Ads and Deep Links

`getDeferredApplink(Function success, Function failure)`

Success function returns the deep link if one is defined.

Failure function returns an error String.

Note that on iOS, you must use a plugin such as [cordova-plugin-idfa](https://www.npmjs.com/package/cordova-plugin-idfa) to first request tracking permission from the user, then call the `setAdvertiserTrackingEnabled` method to enable advertiser tracking. Attempting to call `getDeferredApplink` without doing so will result in an empty string being returned.

```js
cordova.plugins.idfa.requestPermission().then(function() {
  facebookConnectPlugin.setAdvertiserTrackingEnabled(true);
  facebookConnectPlugin.getDeferredApplink(function(url) {
    console.log('url = ' + url);
  });
});
```

See the [Facebook Developer documentation](https://developers.facebook.com/docs/app-ads/deep-linking/) for more details.

## URL Suffixes for Multiple Apps

When using the same Facebook app with multiple iOS apps, use the *FACEBOOK_URL_SCHEME_SUFFIX* variable to set a unique URL Suffix for each app. This ensures that Facebook redirects back to the correct app after closing the login window.

```bash
$ cordova plugin add cordova-plugin-fbsdk --save --variable APP_ID="123456789" --variable APP_NAME="myApplication" --variable CLIENT_TOKEN="abcd1234" --variable FACEBOOK_URL_SCHEME_SUFFIX="mysecondapp"
```
