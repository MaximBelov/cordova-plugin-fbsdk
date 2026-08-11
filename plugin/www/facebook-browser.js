/* globals */
var __fbSdkReady = false;
var __fbCallbacks = [];
/* */

exports.getLoginStatus = function getLoginStatus(force, s, f) {
  if (typeof force === 'function') {
    s = force;
    f = s;
    force = false;
  }
  if (!__fbSdkReady) {
    return __fbCallbacks.push(function () {
      getLoginStatus(force, s, f);
    });
  }

  FB.getLoginStatus(function (response) {
    if (s) s(response);
  }, force);
};

exports.showDialog = function showDialog(options, s, f) {
  if (!__fbSdkReady) {
    return __fbCallbacks.push(function () {
      showDialog(options, s, f);
    });
  }

  options.name = options.name || '';
  options.message = options.message || '';
  options.href = options.href || '';
  options.quote = options.quote || '';
  if (options.actionType) {
    options.action_type = options.actionType;
  }
  if (options.objectID) {
    options.object_id = options.objectID;
  }
  if (options.filters && !Array.isArray(options.filters)) {
    options.filters = [options.filters];
  }

  FB.ui(options, function (response) {
    if (response && (response.request || !response.error_code)) {
      if (s) s(response);
      return;
    }
    if (f) f(response.message);
  });
};
// Attach this to a UI element, this requires user interaction.
exports.login = function login(permissions, s, f) {
  if (!__fbSdkReady) {
    return __fbCallbacks.push(function () {
      login(permissions, s, f);
    });
  }
  // JS SDK takes an object here but the native SDKs use array.
  var options = {};
  if (permissions && permissions.length > 0) {
    var index = permissions.indexOf('rerequest');
    if (index > -1) {
      permissions.splice(index, 1);
      options.auth_type = 'rerequest';
    }
    options.scope = permissions.join(',');
  }

  /**
   * Functions that resolves or rejects a Promise depending on response.
   *
   * Cases:
   * 1. Resolve/Success: If authResponse exists in response, that means that login is successful.
   *    In that case resolve (success) function will be invoked with authResponse value.
   * 2. Reject/Failure: In any other case (no response or response.authResponse) reject (failure) is invoked.
   *  a. response exists and response.status exists, rejected with response.status.message.
   *  b. response exists and response.status does not exist, rejected with response.
   *  c. response does not exist, rejected with 'no response' message.
   */
  FB.login(function (response) {
    if (response.authResponse) {
      if (s) s(response);
    } else if (response) {
      // Previously this was just an else statement.
      if (response.status) {
        // When status is undefined this would throw an error, and rejection function would never be invoked.
        if (f) f(response.status.message);
      } else {
        if (f) f(response);
      }
    } else {
      // In case that no response is available (e.g. popup dismissed)
      if (f) f('No response');
    }
  }, options);
};

exports.checkHasCorrectPermissions = function checkHasCorrectPermissions(permissions, s, f) {
  if (!__fbSdkReady) {
    return __fbCallbacks.push(function () {
      checkHasCorrectPermissions(permissions, s, f);
    });
  }

  if (!permissions || permissions.length === 0) {
    if (s) s('All permissions have been accepted');
  } else {
    FB.api('me/permissions', function (response) {
      if (response.error || !response.data) {
        if (f) f("There was an error getting the list of the user's permissions.");
      } else {
        var userPermissions = response.data,
          grantedPermissions = [],
          declinedPermissionsFound = false;
        for (var x = 0; x < userPermissions.length; x++) {
          if (userPermissions[x].status == 'granted') {
            grantedPermissions.push(userPermissions[x].permission);
          }
        }
        for (var x = 0; x < permissions.length; x++) {
          if (grantedPermissions.indexOf(permissions[x]) < 0) {
            declinedPermissionsFound = true;
          }
        }
        if (declinedPermissionsFound) {
          if (f) f('A permission has been denied');
        } else {
          if (s) s('All permissions have been accepted');
        }
      }
    });
  }
};

exports.isDataAccessExpired = function isDataAccessExpired(s, f) {
  if (!__fbSdkReady) {
    return __fbCallbacks.push(function () {
      isDataAccessExpired(s, f);
    });
  }

  var accessToken = FB.getAccessToken();
  if (accessToken) {
    FB.getLoginStatus(function (response) {
      if (!response.authResponse || !response.authResponse.data_access_expiration_time) {
        if (f) f('Data access expiration time not available.');
      } else {
        var isExpired = response.authResponse.data_access_expiration_time < new Date().getTime() / 1000;
        if (s) s(isExpired ? 'true' : 'false');
      }
    });
  } else {
    if (f) f('Session not open.');
  }
};

exports.reauthorizeDataAccess = function reauthorizeDataAccess(s, f) {
  if (!__fbSdkReady) {
    return __fbCallbacks.push(function () {
      reauthorizeDataAccess(s, f);
    });
  }

  FB.login(
    function (response) {
      if (response.authResponse) {
        if (s) s(response);
      } else if (response) {
        if (response.status) {
          if (f) f(response.status.message);
        } else {
          if (f) f(response);
        }
      } else {
        if (f) f('No response');
      }
    },
    { auth_type: 'reauthorize' },
  );
};

exports.getAccessToken = function getAccessToken(s, f) {
  if (!__fbSdkReady) {
    return __fbCallbacks.push(function () {
      getAccessToken(s, f);
    });
  }

  var response = FB.getAccessToken();
  if (response) {
    if (s) s(response);
    return;
  }
  if (f) f('Session not open.');
};

exports.logEvent = function logEvent(eventName, params, valueToSum, s, f) {
  if (!__fbSdkReady) {
    return __fbCallbacks.push(function () {
      logEvent(eventName, params, valueToSum, s, f);
    });
  }

  FB.AppEvents.logEvent(eventName, valueToSum, params);

  if (s) s();
};

exports.logPurchase = function logPurchase(value, currency, params, s, f) {
  if (typeof params === 'function') {
    s = params;
    f = s;
    params = undefined;
  }
  if (!__fbSdkReady) {
    return __fbCallbacks.push(function () {
      logPurchase(value, currency, params, s, f);
    });
  }

  FB.AppEvents.logPurchase(value, currency, params);

  if (s) s();
};

exports.logout = function logout(s, f) {
  if (!__fbSdkReady) {
    return __fbCallbacks.push(function () {
      logout(s, f);
    });
  }

  FB.logout(function (response) {
    if (s) s(response);
  });
};

exports.getCurrentProfile = function getCurrentProfile(s, f) {
  if (!__fbSdkReady) {
    return __fbCallbacks.push(function () {
      getCurrentProfile(s, f);
    });
  }

  var accessToken = FB.getAccessToken();
  if (accessToken) {
    FB.api('/me', { fields: 'id,first_name,last_name' }, function (response) {
      var profileObject = {
        userID: response.id || '',
        firstName: response.first_name || '',
        lastName: response.last_name || '',
      };
      if (s) s(profileObject);
    });
  } else {
    if (f) f('No current profile.');
  }
};

exports.api = function api(graphPath, permissions, httpMethod, s, f) {
  if (typeof httpMethod === 'function') {
    s = httpMethod;
    f = s;
    httpMethod = undefined;
  }
  if (httpMethod) {
    httpMethod = httpMethod.toLowerCase();
    if (httpMethod != 'post' && httpMethod != 'delete') {
      httpMethod = undefined;
    }
  }
  httpMethod = httpMethod || 'get';
  if (!__fbSdkReady) {
    return __fbCallbacks.push(function () {
      api(graphPath, permissions, httpMethod, s, f);
    });
  }

  // JS API does not take additional permissions
  FB.api(graphPath, httpMethod, function (response) {
    if (response.error) {
      if (f) f(response);
    } else {
      if (s) s(response);
    }
  });
};

exports.activateApp = function logEvent(s, f) {
  if (!__fbSdkReady) {
    return __fbCallbacks.push(function () {
      activateApp(s, f);
    });
  }

  FB.AppEvents.activateApp();

  if (s) s();
};

if (window.location.protocol === 'file:') {
  console.warn('Facebook JS SDK is not supported when using file:// protocol');
} else {
  window.fbAsyncInit = function () {
    FB.init({
      appId: APP_ID, // populated by the cordova after_prepare hook
      xfbml: true,
      version: FACEBOOK_BROWSER_SDK_VERSION, // populated by the cordova after_prepare hook
    });

    __fbSdkReady = true;

    for (var i = 0; i < __fbCallbacks.length; i++) {
      __fbCallbacks[i]();
    }
  };

  (function (d, s, id) {
    var js,
      fjs = d.getElementsByTagName(s)[0];
    if (d.getElementById(id)) {
      return;
    }
    js = d.createElement(s);
    js.id = id;
    js.src = '//connect.facebook.net/en_US/sdk.js';
    fjs.parentNode.insertBefore(js, fjs);
  })(document, 'script', 'facebook-jssdk');
}
