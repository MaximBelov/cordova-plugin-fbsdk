package org.apache.cordova.facebook;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookDialogException;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookRequestError;
import com.facebook.FacebookSdk;
import com.facebook.FacebookServiceException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.FacebookAuthorizationException;
import com.facebook.Profile;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.applinks.AppLinkData;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.Sharer;
import com.facebook.share.model.GameRequestContent;
import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.gamingservices.GameRequestDialog;
import com.facebook.share.widget.MessageDialog;
import com.facebook.share.widget.ShareDialog;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ConnectPlugin extends CordovaPlugin {

    private static final int INVALID_ERROR_CODE = -2; //-1 is FacebookRequestError.INVALID_ERROR_CODE
    @SuppressWarnings("serial")
    private static final Set<String> OTHER_PUBLISH_PERMISSIONS = new HashSet<String>() {
        {
            add("ads_management");
            add("create_event");
            add("rsvp_event");
        }
    };
    private final String TAG = "ConnectPlugin";

    private CallbackManager callbackManager;
    private AppEventsLogger logger;
    private CallbackContext loginContext = null;
    private CallbackContext reauthorizeContext = null;
    private CallbackContext showDialogContext = null;
    private CallbackContext lastGraphContext = null;
    private String lastGraphRequestMethod = null;
    private String graphPath;
    private ShareDialog shareDialog;
    private GameRequestDialog gameRequestDialog;
    private MessageDialog messageDialog;

    @Override
    protected void pluginInitialize() {
        FacebookSdk.sdkInitialize(cordova.getActivity().getApplicationContext());

        // create callbackManager
        callbackManager = CallbackManager.Factory.create();

        // create AppEventsLogger
        logger = AppEventsLogger.newLogger(cordova.getActivity().getApplicationContext());

        // augment web view to enable hybrid app events
        enableHybridAppEvents();

        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {
                GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject jsonObject, GraphResponse response) {
                        if (response.getError() != null) {
                            if (lastGraphContext != null) {
                                lastGraphContext.error(getFacebookRequestErrorResponse(response.getError()));
                            } else if (loginContext != null) {
                                loginContext.error(getFacebookRequestErrorResponse(response.getError()));
                            }
                            return;
                        }

                        // If this login comes after doing a new permission request
                        // make the outstanding graph call
                        if (lastGraphContext != null) {
                            makeGraphCall(lastGraphContext, lastGraphRequestMethod);
                            return;
                        }

                        if (loginContext != null) {
                            Log.d(TAG, "returning login object " + jsonObject.toString());
                            loginContext.success(getResponse());
                            loginContext = null;
                        }

                        if (reauthorizeContext != null) {
                            reauthorizeContext.success(getResponse());
                            reauthorizeContext = null;
                        }
                    }
                }).executeAsync();
            }

            @Override
            public void onCancel() {
                FacebookOperationCanceledException e = new FacebookOperationCanceledException();
                if (loginContext != null) {
                    handleError(e, loginContext);
                    loginContext = null;
                }
                if (reauthorizeContext != null) {
                    handleError(e, reauthorizeContext);
                    reauthorizeContext = null;
                }
            }

            @Override
            public void onError(FacebookException e) {
                Log.e("Activity", String.format("Error: %s", e.toString()));
                if (loginContext != null) {
                    handleError(e, loginContext);
                    loginContext = null;
                }
                if (reauthorizeContext != null) {
                    handleError(e, reauthorizeContext);
                    reauthorizeContext = null;
                }

                // Sign-out current instance in case token is still valid for previous user
                if (e instanceof FacebookAuthorizationException) {
                    if (AccessToken.getCurrentAccessToken() != null) {
                        LoginManager.getInstance().logOut();
                    }
                }
            }
        });

        shareDialog = new ShareDialog(cordova.getActivity());
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                if (showDialogContext != null) {
                    showDialogContext.success(result.getPostId());
                    showDialogContext = null;
                }
            }

            @Override
            public void onCancel() {
                FacebookOperationCanceledException e = new FacebookOperationCanceledException();
                handleError(e, showDialogContext);
            }

            @Override
            public void onError(FacebookException e) {
                Log.e("Activity", String.format("Error: %s", e.toString()));
                handleError(e, showDialogContext);
            }
        });

        messageDialog = new MessageDialog(cordova.getActivity());
        messageDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                if (showDialogContext != null) {
                    showDialogContext.success();
                    showDialogContext = null;
                }
            }

            @Override
            public void onCancel() {
                FacebookOperationCanceledException e = new FacebookOperationCanceledException();
                handleError(e, showDialogContext);
            }

            @Override
            public void onError(FacebookException e) {
                Log.e("Activity", String.format("Error: %s", e.toString()));
                handleError(e, showDialogContext);
            }
        });

        gameRequestDialog = new GameRequestDialog(cordova.getActivity());
        gameRequestDialog.registerCallback(callbackManager, new FacebookCallback<GameRequestDialog.Result>() {
            @Override
            public void onSuccess(GameRequestDialog.Result result) {
                if (showDialogContext != null) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("requestId", result.getRequestId());
                        json.put("recipientsIds", new JSONArray(result.getRequestRecipients()));
                        showDialogContext.success(json);
                        showDialogContext = null;
                    } catch (JSONException ex) {
                        showDialogContext.success();
                        showDialogContext = null;
                    }
                }
            }

            @Override
            public void onCancel() {
                FacebookOperationCanceledException e = new FacebookOperationCanceledException();
                handleError(e, showDialogContext);
            }

            @Override
            public void onError(FacebookException e) {
                Log.e("Activity", String.format("Error: %s", e.toString()));
                handleError(e, showDialogContext);
            }
        });
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        // Developers can observe how frequently users activate their app by logging an app activation event.
        AppEventsLogger.activateApp(cordova.getActivity().getApplication());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d(TAG, "activity result in plugin: requestCode(" + requestCode + "), resultCode(" + resultCode + ")");
        callbackManager.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("getApplicationId")) {
            callbackContext.success(FacebookSdk.getApplicationId());
            return true;

        } else if (action.equals("setApplicationId")) {
            executeSetApplicationId(args, callbackContext);
            return true;

        } else if (action.equals("getClientToken")) {
            callbackContext.success(FacebookSdk.getClientToken());
            return true;

        } else if (action.equals("setClientToken")) {
            executeSetClientToken(args, callbackContext);
            return true;

        } else if (action.equals("getApplicationName")) {
            callbackContext.success(FacebookSdk.getApplicationName());
            return true;

        } else if (action.equals("setApplicationName")) {
            executeSetApplicationName(args, callbackContext);
            return true;

        } else if (action.equals("login")) {
            executeLogin(args, callbackContext);
            return true;

        } else if (action.equals("checkHasCorrectPermissions")) {
            executeCheckHasCorrectPermissions(args, callbackContext);
            return true;

        } else if (action.equals("isDataAccessExpired")) {
            if (hasAccessToken()) {
                callbackContext.success(AccessToken.getCurrentAccessToken().isDataAccessExpired() ? "true" : "false");
            } else {
                callbackContext.error("Session not open.");
            }
            return true;

        } else if (action.equals("reauthorizeDataAccess")) {
            executeReauthorizeDataAccess(args, callbackContext);
            return true;

        } else if (action.equals("logout")) {
            if (hasAccessToken()) {
                LoginManager.getInstance().logOut();
            }
            callbackContext.success();
            return true;

        } else if (action.equals("getLoginStatus")) {
            executeGetLoginStatus(args, callbackContext);
            return true;

        } else if (action.equals("getAccessToken")) {
            if (hasAccessToken()) {
                callbackContext.success(AccessToken.getCurrentAccessToken().getToken());
            } else {
                // Session not open
                callbackContext.error("Session not open.");
            }
            return true;

        } else if(action.equals("setAutoLogAppEventsEnabled")) {
            executeSetAutoLogAppEventsEnabled(args, callbackContext);
            return true;

        } else if(action.equals("setAdvertiserIDCollectionEnabled")) {
            executeSetAdvertiserIDCollectionEnabled(args, callbackContext);
            return true;

        } else if(action.equals("setDataProcessingOptions")) {
            executeSetDataProcessingOptions(args, callbackContext);
            return true;

        } else if (action.equals("setUserData")) {
            executeSetUserData(args, callbackContext);
            return true;

        } else if (action.equals("clearUserData")) {
            executeClearUserData(args, callbackContext);
            return true;

        } else if (action.equals("logEvent")) {
            executeLogEvent(args, callbackContext);
            return true;

        } else if (action.equals("logPurchase")) {
            executeLogPurchase(args, callbackContext);
            return true;

        } else if (action.equals("showDialog")) {
            executeDialog(args, callbackContext);
            return true;

        } else if (action.equals("getCurrentProfile")) {
            executeGetCurrentProfile(args, callbackContext);

            return true;
        } else if (action.equals("graphApi")) {
            executeGraph(args, callbackContext);

            return true;
        } else if (action.equals("getDeferredApplink")) {
            executeGetDeferredApplink(args, callbackContext);
            return true;
        } else if (action.equals("activateApp")) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    AppEventsLogger.activateApp(cordova.getActivity().getApplication());
                    callbackContext.success();
                }
            });

            return true;
        }
        return false;
    }

    private void executeSetApplicationId(JSONArray args, CallbackContext callbackContext) {
        if (args.length() == 0) {
            // Not enough parameters
            callbackContext.error("Invalid arguments");
            return;
        }

        try {
            String appId = args.getString(0);
            FacebookSdk.setApplicationId(appId);
            callbackContext.success();
        } catch (JSONException e) {
            callbackContext.error("Error setting application ID");
        }
    }

    private void executeSetClientToken(JSONArray args, CallbackContext callbackContext) {
        if (args.length() == 0) {
            // Not enough parameters
            callbackContext.error("Invalid arguments");
            return;
        }

        try {
            String clientToken = args.getString(0);
            FacebookSdk.setClientToken(clientToken);
            callbackContext.success();
        } catch (JSONException e) {
            callbackContext.error("Error setting client token");
        }
    }

    private void executeSetApplicationName(JSONArray args, CallbackContext callbackContext) {
        if (args.length() == 0) {
            // Not enough parameters
            callbackContext.error("Invalid arguments");
            return;
        }

        try {
            String appName = args.getString(0);
            FacebookSdk.setApplicationName(appName);
            callbackContext.success();
        } catch (JSONException e) {
            callbackContext.error("Error setting application name");
        }
    }

    private void executeGetDeferredApplink(JSONArray args,
                                           final CallbackContext callbackContext) {
        AppLinkData.fetchDeferredAppLinkData(cordova.getActivity().getApplicationContext(),
                new AppLinkData.CompletionHandler() {
                    @Override
                    public void onDeferredAppLinkDataFetched(
                            AppLinkData appLinkData) {
                        PluginResult pr;
                        if (appLinkData == null) {
                            pr = new PluginResult(PluginResult.Status.OK, "");
                        } else {
                            pr = new PluginResult(PluginResult.Status.OK, appLinkData.getTargetUri().toString());
                        }

                        callbackContext.sendPluginResult(pr);
                        return;
                    }
                });
    }

    private void executeDialog(JSONArray args, CallbackContext callbackContext) throws JSONException {
        Map<String, String> params = new HashMap<String, String>();
        String method = null;
        JSONObject parameters;

        try {
            parameters = args.getJSONObject(0);
        } catch (JSONException e) {
            parameters = new JSONObject();
        }

        Iterator<String> iter = parameters.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            if (key.equals("method")) {
                try {
                    method = parameters.getString(key);
                } catch (JSONException e) {
                    Log.w(TAG, "Nonstring method parameter provided to dialog");
                }
            } else {
                try {
                    params.put(key, parameters.getString(key));
                } catch (JSONException e) {
                    // Need to handle JSON parameters
                    Log.w(TAG, "Non-string parameter provided to dialog discarded");
                }
            }
        }

        if (method == null) {
            callbackContext.error("No method provided");
        } else if (method.equalsIgnoreCase("apprequests")) {

            if (!GameRequestDialog.canShow()) {
                callbackContext.error("Cannot show dialog");
                return;
            }
            showDialogContext = callbackContext;
            PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
            pr.setKeepCallback(true);
            showDialogContext.sendPluginResult(pr);

            GameRequestContent.Builder builder = new GameRequestContent.Builder();
            if (params.containsKey("message"))
                builder.setMessage(params.get("message"));
            if (params.containsKey("to"))
                builder.setTo(params.get("to"));
            if (params.containsKey("data"))
                builder.setData(params.get("data"));
            if (params.containsKey("title"))
                builder.setTitle(params.get("title"));
            if (params.containsKey("objectId"))
                builder.setObjectId(params.get("objectId"));
            if (params.containsKey("actionType")) {
                try {
                    final GameRequestContent.ActionType actionType = GameRequestContent.ActionType.valueOf(params.get("actionType"));
                    builder.setActionType(actionType);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Discarding invalid argument actionType");
                }
            }
            if (params.containsKey("filters")) {
                try {
                    final GameRequestContent.Filters filters = GameRequestContent.Filters.valueOf(params.get("filters"));
                    builder.setFilters(filters);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Discarding invalid argument filters");
                }
            }

            // Set up the activity result callback to this class
            cordova.setActivityResultCallback(this);

            gameRequestDialog.show(builder.build());

        } else if (method.equalsIgnoreCase("share") || method.equalsIgnoreCase("feed")) {
            if ((params.containsKey("photo_image") && !ShareDialog.canShow(SharePhotoContent.class)) || (!params.containsKey("photo_image") && !ShareDialog.canShow(ShareLinkContent.class))) {
                callbackContext.error("Cannot show dialog");
                return;
            }
            showDialogContext = callbackContext;
            PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
            pr.setKeepCallback(true);
            showDialogContext.sendPluginResult(pr);

            // Set up the activity result callback to this class
            cordova.setActivityResultCallback(this);
            if (params.containsKey("photo_image")) {
                SharePhotoContent content = buildPhotoContent(params);
                shareDialog.show(content);
            } else {
                ShareLinkContent content = buildLinkContent(params);
                shareDialog.show(content);
            }

        } else if (method.equalsIgnoreCase("send")) {
            if (!MessageDialog.canShow(ShareLinkContent.class)) {
                callbackContext.error("Cannot show dialog");
                return;
            }
            showDialogContext = callbackContext;
            PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
            pr.setKeepCallback(true);
            showDialogContext.sendPluginResult(pr);

            ShareLinkContent.Builder builder = new ShareLinkContent.Builder();
            if(params.containsKey("link"))
                builder.setContentUrl(Uri.parse(params.get("link")));

            messageDialog.show(builder.build());

        } else {
            callbackContext.error("Unsupported dialog method.");
        }
    }

    private void executeGetCurrentProfile(JSONArray args, CallbackContext callbackContext) {
        if (Profile.getCurrentProfile() == null) {
            callbackContext.error("No current profile.");
        } else {
            callbackContext.success(getProfile());
        }
    }

    private void executeGraph(JSONArray args, CallbackContext callbackContext) throws JSONException {
        lastGraphContext = callbackContext;
        CallbackContext graphContext  = callbackContext;
        String requestMethod = null;
        if (args.length() < 3) {
            lastGraphRequestMethod = null;
        } else {
            lastGraphRequestMethod = args.getString(2);
            requestMethod = args.getString(2);
        }
        PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
        pr.setKeepCallback(true);
        graphContext.sendPluginResult(pr);

        graphPath = args.getString(0);
        JSONArray arr = args.getJSONArray(1);

        final Set<String> permissions = new HashSet<String>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            permissions.add(arr.getString(i));
        }

        if (permissions.size() == 0) {
            makeGraphCall(graphContext, requestMethod);
            return;
        }

        String declinedPermission = null;

        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken.getPermissions().containsAll(permissions)) {
            makeGraphCall(graphContext, requestMethod);
            return;
        }

        Set<String> declined = accessToken.getDeclinedPermissions();

        // Figure out if we have all permissions
        for (String permission : permissions) {
            if (declined.contains(permission)) {
                declinedPermission = permission;
                break;
            }
        }

        if (declinedPermission != null) {
            graphContext.error("This request needs declined permission: " + declinedPermission);
			return;
        }

        cordova.setActivityResultCallback(this);
        LoginManager loginManager = LoginManager.getInstance();
        loginManager.logIn(cordova.getActivity(), permissions);
    }

    private void executeSetAutoLogAppEventsEnabled(JSONArray args, CallbackContext callbackContext) {
        boolean enabled = args.optBoolean(0);
        FacebookSdk.setAutoLogAppEventsEnabled(enabled);
        callbackContext.success();
    }

    private void executeSetAdvertiserIDCollectionEnabled(JSONArray args, CallbackContext callbackContext) {
        boolean enabled = args.optBoolean(0);
        FacebookSdk.setAdvertiserIDCollectionEnabled(enabled);
        callbackContext.success();
    }

    private void executeSetDataProcessingOptions(JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (args.length() == 0) {
            // Not enough parameters
            callbackContext.error("Invalid arguments");
            return;
        }

        JSONArray arr = args.getJSONArray(0);
        String[] options = new String[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
            options[i + 1] = arr.getString(i);
        }

        if (args.length() == 1) {
            FacebookSdk.setDataProcessingOptions(options);
        } else {
            String country = args.getString(1);
            String state = args.getString(2);
            FacebookSdk.setDataProcessingOptions(options);
        }
        callbackContext.success();
    }

    private void executeSetUserData(JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (args.length() == 0) {
            // Not enough parameters
            callbackContext.error("Invalid arguments");
            return;
        }

        Map<String, String> params = new HashMap<String, String>();
        JSONObject parameters;

        try {
            parameters = args.getJSONObject(0);
        } catch (JSONException e) {
            callbackContext.error("userData must be an object");
            return;
        }

        Iterator<String> iter = parameters.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            try {
                params.put(key, parameters.getString(key));
            } catch (JSONException e) {
                Log.w(TAG, "Non-string parameter provided to setUserData discarded");
            }
        }

        logger.setUserData(params.get("em"), params.get("fn"), params.get("ln"), params.get("ph"), params.get("db"), params.get("ge"), params.get("ct"), params.get("st"), params.get("zp"), params.get("cn"));
        callbackContext.success();
    }

    private void executeClearUserData(JSONArray args, CallbackContext callbackContext) {
        logger.clearUserData();
        callbackContext.success();
    }

    private void executeLogEvent(JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (args.length() == 0) {
            // Not enough parameters
            callbackContext.error("Invalid arguments");
            return;
        }

        String eventName = args.getString(0);
        if (args.length() == 1) {
            logger.logEvent(eventName);
            callbackContext.success();
            return;
        }

        // Arguments is greater than 1
        JSONObject params = args.getJSONObject(1);
        Bundle parameters = new Bundle();
        Iterator<String> iter = params.keys();

        while (iter.hasNext()) {
            String key = iter.next();
            try {
                // Try get a String
                String value = params.getString(key);
                parameters.putString(key, value);
            } catch (JSONException e) {
                // Maybe it was an int
                Log.w(TAG, "Type in AppEvent parameters was not String for key: " + key);
                try {
                    int value = params.getInt(key);
                    parameters.putInt(key, value);
                } catch (JSONException e2) {
                    // Nope
                    Log.e(TAG, "Unsupported type in AppEvent parameters for key: " + key);
                }
            }
        }

        if (args.length() == 2) {
            logger.logEvent(eventName, parameters);
            callbackContext.success();
        }

        if (args.length() == 3) {
            double value = args.getDouble(2);
            logger.logEvent(eventName, value, parameters);
            callbackContext.success();
        }
    }

    private void executeLogPurchase(JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (args.length() < 2 || args.length() > 3) {
            callbackContext.error("Invalid arguments");
            return;
        }
        BigDecimal value = new BigDecimal(args.getString(0));
        String currency = args.getString(1);
        if (args.length() == 3 ) {
            JSONObject params = args.getJSONObject(2);
            Bundle parameters = new Bundle();
            Iterator<String> iter = params.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                try {
                    // Try get a String
                    String paramValue = params.getString(key);
                    parameters.putString(key, paramValue);
                } catch (JSONException e) {
                    // Maybe it was an int
                    Log.w(TAG, "Type in AppEvent parameters was not String for key: " + key);
                    try {
                        int paramValue = params.getInt(key);
                        parameters.putInt(key, paramValue);
                    } catch (JSONException e2) {
                        // Nope
                        Log.e(TAG, "Unsupported type in AppEvent parameters for key: " + key);
                    }
                }
            }
            logger.logPurchase(value, Currency.getInstance(currency), parameters);
        } else {
            logger.logPurchase(value, Currency.getInstance(currency));
        }
        callbackContext.success();
    }

    private void executeLogin(JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "login FB");

        // #568: Reset lastGraphContext in case it would still contains the last graphApi results of a previous session (login -> graphApi -> logout -> login)
        lastGraphContext = null;
        lastGraphRequestMethod = null;

        // Get the permissions
        Set<String> permissions = new HashSet<String>(args.length());

        for (int i = 0; i < args.length(); i++) {
            permissions.add(args.getString(i));
        }

        // Set a pending callback to cordova
        loginContext = callbackContext;
        PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
        pr.setKeepCallback(true);
        loginContext.sendPluginResult(pr);

        // Set up the activity result callback to this class
        cordova.setActivityResultCallback(this);
        LoginManager.getInstance().logIn(cordova.getActivity(), permissions);
    }

    private void executeCheckHasCorrectPermissions(JSONArray args, CallbackContext callbackContext) throws JSONException {
        Set<String> permissions = new HashSet<String>(args.length());

        for (int i = 0; i < args.length(); i++) {
            permissions.add(args.getString(i));
        }

        if (permissions.size() > 0) {
            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            if (!accessToken.getPermissions().containsAll(permissions)) {
                callbackContext.error("A permission has been denied");
                return;
            }
        }

        callbackContext.success("All permissions have been accepted");
    }

    private void executeReauthorizeDataAccess(JSONArray args, CallbackContext callbackContext) throws JSONException {
        lastGraphContext = null;
        lastGraphRequestMethod = null;

        reauthorizeContext = callbackContext;
        PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
        pr.setKeepCallback(true);
        reauthorizeContext.sendPluginResult(pr);

        cordova.setActivityResultCallback(this);
        LoginManager.getInstance().reauthorizeDataAccess(cordova.getActivity());
    }

    private void executeGetLoginStatus(JSONArray args, CallbackContext callbackContext) {
        boolean force = args.optBoolean(0);
        if (force) {
            AccessToken.refreshCurrentAccessTokenAsync(new AccessToken.AccessTokenRefreshCallback() {
                @Override
                public void OnTokenRefreshed(AccessToken accessToken) {
                    callbackContext.success(getResponse());
                }
                @Override
                public void OnTokenRefreshFailed(FacebookException exception) {
                    callbackContext.success(getResponse());
                }
            });
        } else {
            callbackContext.success(getResponse());
        }
    }

    private void enableHybridAppEvents() {
        try {
            Context appContext = cordova.getActivity().getApplicationContext();
            Resources res = appContext.getResources();
            int enableHybridAppEventsId = res.getIdentifier("fb_hybrid_app_events", "bool", appContext.getPackageName());
            boolean enableHybridAppEvents = enableHybridAppEventsId != 0 && res.getBoolean(enableHybridAppEventsId);
            if (enableHybridAppEvents) {
                AppEventsLogger.augmentWebView((WebView) this.webView.getView(), appContext);
                Log.d(TAG, "FB Hybrid app events are enabled");
            } else {
                Log.d(TAG, "FB Hybrid app events are not enabled");
            }
        } catch (Exception e) {
            Log.d(TAG, "FB Hybrid app events cannot be enabled");
        }
    }

    private SharePhotoContent buildPhotoContent(Map<String, String> paramBundle) {
        SharePhoto.Builder photoBuilder = new SharePhoto.Builder();
        if (!(paramBundle.get("photo_image") instanceof String)) {
            Log.d(TAG, "photo_image must be a string");
        } else {
            try {
                byte[] photoImageData = Base64.decode(paramBundle.get("photo_image"), Base64.DEFAULT);
                Bitmap image = BitmapFactory.decodeByteArray(photoImageData, 0, photoImageData.length); 
                photoBuilder.setBitmap(image).setUserGenerated(true);
            } catch (Exception e) {
                Log.d(TAG, "photo_image cannot be decoded");
            }
        }
        SharePhoto photo = photoBuilder.build();
        SharePhotoContent.Builder photoContentBuilder = new SharePhotoContent.Builder();
        photoContentBuilder.addPhoto(photo);

        return photoContentBuilder.build();
    }

    private ShareLinkContent buildLinkContent(Map<String, String> paramBundle) {
        ShareLinkContent.Builder builder = new ShareLinkContent.Builder();
        if (paramBundle.containsKey("href"))
            builder.setContentUrl(Uri.parse(paramBundle.get("href")));
        if (paramBundle.containsKey("link"))
            builder.setContentUrl(Uri.parse(paramBundle.get("link")));
        if (paramBundle.containsKey("quote"))
            builder.setQuote(paramBundle.get("quote"));
        if (paramBundle.containsKey("hashtag"))
            builder.setShareHashtag(new ShareHashtag.Builder().setHashtag(paramBundle.get("hashtag")).build());

        return builder.build();
    }

    // Simple active session check
    private boolean hasAccessToken() {
        AccessToken token = AccessToken.getCurrentAccessToken();

		if (token == null)
			return false;

		return !token.isExpired();
    }

    private void handleError(FacebookException exception, CallbackContext context) {
        if (exception.getMessage() != null) {
            Log.e(TAG, exception.toString());
        }
        String errMsg = "Facebook error: " + exception.getMessage();
        int errorCode = INVALID_ERROR_CODE;
        // User clicked "x"
        if (exception instanceof FacebookOperationCanceledException) {
            errMsg = "User cancelled dialog";
            errorCode = 4201;
        } else if (exception instanceof FacebookDialogException) {
            // Dialog error
            errMsg = "Dialog error: " + exception.getMessage();
        }

        if (context != null) {
            context.error(getErrorResponse(exception, errMsg, errorCode));
        } else {
            Log.e(TAG, "Error already sent so no context, msg: " + errMsg + ", code: " + errorCode);
        }
    }

    private void makeGraphCall(final CallbackContext graphContext, String requestMethod) {
        //If you're using the paging URLs they will be URLEncoded, let's decode them.
        try {
            graphPath = URLDecoder.decode(graphPath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String[] urlParts = graphPath.split("\\?");
        String graphAction = urlParts[0];
        GraphRequest graphRequest = GraphRequest.newGraphPathRequest(AccessToken.getCurrentAccessToken(), graphAction, new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                if (graphContext != null) {
                    if (response.getError() != null) {
                        graphContext.error(getFacebookRequestErrorResponse(response.getError()));
                    } else {
                        graphContext.success(response.getJSONObject());
                    }
                    graphPath = null;
                }
            }
        });

        if (requestMethod != null) {
            graphRequest.setHttpMethod(HttpMethod.valueOf(requestMethod));
        }

        Bundle params = graphRequest.getParameters();

        if (urlParts.length > 1) {
            String[] queries = urlParts[1].split("&");

            for (String query : queries) {
                int splitPoint = query.indexOf("=");
                if (splitPoint > 0) {
                    String key = query.substring(0, splitPoint);
                    String value = query.substring(splitPoint + 1, query.length());
                    params.putString(key, value);
                }
            }
        }

        graphRequest.setParameters(params);
        graphRequest.executeAsync();
    }

    /**
     * Create a Facebook Response object that matches the one for the Javascript SDK
     * @return JSONObject - the response object
     */
    public JSONObject getResponse() {
        String response;
        final AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (hasAccessToken()) {
            long dataAccessExpirationTimeInterval = accessToken.getDataAccessExpirationTime().getTime() / 1000L;
            Date today = new Date();
            long expiresTimeInterval = (accessToken.getExpires().getTime() - today.getTime()) / 1000L;
            response = "{"
                + "\"status\": \"connected\","
                + "\"authResponse\": {"
                + "\"accessToken\": \"" + accessToken.getToken() + "\","
                + "\"data_access_expiration_time\": \"" + Math.max(dataAccessExpirationTimeInterval, 0) + "\","
                + "\"expiresIn\": \"" + Math.max(expiresTimeInterval, 0) + "\","
                + "\"userID\": \"" + accessToken.getUserId() + "\""
                + "}"
                + "}";
        } else {
            response = "{"
                + "\"status\": \"unknown\""
                + "}";
        }
        try {
            return new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    public JSONObject getFacebookRequestErrorResponse(FacebookRequestError error) {

        String response = "{"
            + "\"errorCode\": \"" + error.getErrorCode() + "\","
            + "\"errorType\": \"" + error.getErrorType() + "\","
            + "\"errorMessage\": \"" + error.getErrorMessage() + "\"";

        if (error.getErrorUserMessage() != null) {
            response += ",\"errorUserMessage\": \"" + error.getErrorUserMessage() + "\"";
        }

        if (error.getErrorUserTitle() != null) {
            response += ",\"errorUserTitle\": \"" + error.getErrorUserTitle() + "\"";
        }

        response += "}";

        try {
            return new JSONObject(response);
        } catch (JSONException e) {

            e.printStackTrace();
        }
        return new JSONObject();
    }

    public JSONObject getErrorResponse(Exception error, String message, int errorCode) {
        if (error instanceof FacebookServiceException) {
            return getFacebookRequestErrorResponse(((FacebookServiceException) error).getRequestError());
        }

        String response = "{";

        if (error instanceof FacebookDialogException) {
            errorCode = ((FacebookDialogException) error).getErrorCode();
        }

        if (errorCode != INVALID_ERROR_CODE) {
            response += "\"errorCode\": \"" + errorCode + "\",";
        }

        if (message == null) {
            message = error.getMessage();
        }

        response += "\"errorMessage\": \"" + message + "\"}";

        try {
            return new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }
    
    public JSONObject getProfile() {
        String response;
        final Profile profile = Profile.getCurrentProfile();
        if (profile == null) {
            response = "{}";
        } else {
            response = "{"
                + "\"userID\": \"" + profile.getId() + "\","
                + "\"firstName\": \"" + profile.getFirstName() + "\","
                + "\"lastName\": \"" + profile.getLastName() + "\""
                + "}";
        }
        try {
            return new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }
}
