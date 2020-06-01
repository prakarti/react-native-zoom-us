package ch.milosz.reactnative;

import android.util.Log;
import android.text.TextUtils;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.LifecycleEventListener;

import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomSDKInitParams;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDKInitializeListener;
import us.zoom.sdk.InviteOptions;
import us.zoom.sdk.MeetingViewsOptions;

import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.MeetingError;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.MeetingServiceListener;

import us.zoom.sdk.StartMeetingOptions;
import us.zoom.sdk.StartMeetingParamsWithoutLogin;

import us.zoom.sdk.JoinMeetingOptions;
import us.zoom.sdk.JoinMeetingParams;

public class RNZoomUsModule extends ReactContextBaseJavaModule implements ZoomSDKInitializeListener, MeetingServiceListener, LifecycleEventListener {

  private final static String TAG = "RNZoomUs";
  private final ReactApplicationContext reactContext;

  private Boolean isInitialized = false;
  private Promise initializePromise;
  private Promise meetingPromise;

  public RNZoomUsModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    reactContext.addLifecycleEventListener(this);
  }

  @Override
  public String getName() {
    return "RNZoomUs";
  }

  @ReactMethod
  public void initialize(final String appKey, final String appSecret, final Promise promise) {
    if (isInitialized) {
      promise.resolve("Already initialize Zoom SDK successfully.");
      return;
    }

    isInitialized = true;

    try {
      initializePromise = promise;

      reactContext.getCurrentActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            ZoomSDK zoomSDK = ZoomSDK.getInstance();
            ZoomSDKInitParams params = new ZoomSDKInitParams();
            params.appKey = appKey;
            params.appSecret = appSecret;
            zoomSDK.initialize(reactContext.getCurrentActivity(), RNZoomUsModule.this, params);
          }
      });
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @ReactMethod
  public void startMeeting(
    final String displayName,
    final String meetingNo,
    final String userId,
    final int userType,
    final String zoomAccessToken,
    final String zoomToken,
    Promise promise
  ) {
    try {
      meetingPromise = promise;

      ZoomSDK zoomSDK = ZoomSDK.getInstance();
      if(!zoomSDK.isInitialized()) {
        promise.reject("ERR_ZOOM_START", "ZoomSDK has not been initialized successfully");
        return;
      }

      final MeetingService meetingService = zoomSDK.getMeetingService();
      if(meetingService.getMeetingStatus() != MeetingStatus.MEETING_STATUS_IDLE) {
        long lMeetingNo = 0;
        try {
          lMeetingNo = Long.parseLong(meetingNo);
        } catch (NumberFormatException e) {
          promise.reject("ERR_ZOOM_START", "Invalid meeting number: " + meetingNo);
          return;
        }

        if(meetingService.getCurrentRtcMeetingNumber() == lMeetingNo) {
          meetingService.returnToMeeting(reactContext.getCurrentActivity());
          promise.resolve("Already joined zoom meeting");
          return;
        }
      }

      StartMeetingOptions opts = new StartMeetingOptions();
      StartMeetingParamsWithoutLogin params = new StartMeetingParamsWithoutLogin();
      params.displayName = displayName;
      params.meetingNo = meetingNo;
      params.userId = userId;
      params.userType = userType;
      params.zoomAccessToken = zoomAccessToken;
      params.zoomToken = zoomToken;

      int startMeetingResult = meetingService.startMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
      Log.i(TAG, "startMeeting, startMeetingResult=" + startMeetingResult);

      if (startMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
        promise.reject("ERR_ZOOM_START", "startMeeting, errorCode=" + startMeetingResult);
      }
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @ReactMethod
  public void joinMeeting(
    ReadableMap joinMeetingParams,
    ReadableMap joinMeetingOptions,
    Promise promise
  ) {
    try {
      System.out.println("joinMeetingOptions" + joinMeetingOptions.toString());
      System.out.println("joinMeetingParams" + joinMeetingParams.toString());

      meetingPromise = promise;

      ZoomSDK zoomSDK = ZoomSDK.getInstance();
      if(!zoomSDK.isInitialized()) {
        promise.reject("ERR_ZOOM_JOIN", "ZoomSDK has not been initialized successfully");
        return;
      }

      final MeetingService meetingService = zoomSDK.getMeetingService();

      JoinMeetingOptions opts = new JoinMeetingOptions();
      opts.no_driving_mode = true;                             // true - for disable zoom meeting ui driving mode
      opts.no_invite = true;                                   // true - for hide invite button on participant view
      opts.no_meeting_end_message = false;                     // true - for disable to show meeting end dialog when meeting is end.
      opts.no_titlebar = false;                                // true - for hide title bar on zoom meeting ui
      opts.no_bottom_toolbar = false;                          // true - for hide bottom bar on zoom meeting ui
      opts.no_audio = false;                                   // true - if you would like to hide “Audio” button
      opts.no_video = false;                                   // true - if you would like to hide “Video” button
      opts.no_disconnect_audio = true;                        // true - if you would like to hide “Disconnect audio” button
      opts.no_dial_in_via_phone = true;                        // true - if you would like to hide “Call in by phone” button
      opts.no_dial_out_to_phone = true;                        // true - if you would like to hide “Call out” button
      opts.no_share = true;
      opts.invite_options = InviteOptions.INVITE_DISABLE_ALL;
      opts.meeting_views_options = MeetingViewsOptions.NO_BUTTON_SWITCH_CAMERA
              + MeetingViewsOptions.NO_TEXT_PASSWORD
              + MeetingViewsOptions.NO_TEXT_MEETING_ID;
      
      JoinMeetingParams params = new JoinMeetingParams();
      params.displayName = joinMeetingParams.getString("displayName");
      params.meetingNo = joinMeetingParams.getString("meetingNo");
      String password = joinMeetingParams.getString("password");
      if(!TextUtils.isEmpty(password)){
        params.password = password;
      }

      int joinMeetingResult = meetingService.joinMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
      Log.i(TAG, "joinMeeting, joinMeetingResult=" + joinMeetingResult);

      if (joinMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
        promise.reject("ERR_ZOOM_JOIN", "joinMeeting, errorCode=" + joinMeetingResult);
      }
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  // @ReactMethod
  // public void joinMeetingWithPassword(
  //   final String displayName,
  //   final String meetingNo,
  //   final String password,
  //   Promise promise
  // ) {
  //   try {
  //     meetingPromise = promise;

  //     ZoomSDK zoomSDK = ZoomSDK.getInstance();
  //     if(!zoomSDK.isInitialized()) {
  //       promise.reject("ERR_ZOOM_JOIN", "ZoomSDK has not been initialized successfully");
  //       return;
  //     }

  //     final MeetingService meetingService = zoomSDK.getMeetingService();

  //     JoinMeetingOptions opts = new JoinMeetingOptions();
  //     JoinMeetingParams params = new JoinMeetingParams();
  //     params.displayName = displayName;
  //     params.meetingNo = meetingNo;
  //     params.password = password;

  //     int joinMeetingResult = meetingService.joinMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
  //     Log.i(TAG, "joinMeeting, joinMeetingResult=" + joinMeetingResult);

  //     if (joinMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
  //       promise.reject("ERR_ZOOM_JOIN", "joinMeeting, errorCode=" + joinMeetingResult);
  //     }
  //   } catch (Exception ex) {
  //     promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
  //   }
  // }

  @Override
  public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
    Log.i(TAG, "onZoomSDKInitializeResult, errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);
    if(errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
      initializePromise.reject(
              "ERR_ZOOM_INITIALIZATION",
              "Error: " + errorCode + ", internalErrorCode=" + internalErrorCode
      );
    } else {
      registerListener();
      initializePromise.resolve("Initialize Zoom SDK successfully.");
    }
  }

  @Override
  public void onZoomAuthIdentityExpired() {
  }

  @Override
  public void onMeetingStatusChanged(MeetingStatus meetingStatus, int errorCode, int internalErrorCode) {
    Log.i(TAG, "onMeetingStatusChanged, meetingStatus=" + meetingStatus + ", errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);

    if (meetingPromise == null) {
      return;
    }

    if(meetingStatus == MeetingStatus.MEETING_STATUS_FAILED) {
      meetingPromise.reject(
              "ERR_ZOOM_MEETING",
              "Error: " + errorCode + ", internalErrorCode=" + internalErrorCode
      );
      meetingPromise = null;
    } else if (meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING) {
      meetingPromise.resolve("Connected to zoom meeting");
      meetingPromise = null;
    }
  }

  private void registerListener() {
    Log.i(TAG, "registerListener");
    ZoomSDK zoomSDK = ZoomSDK.getInstance();
    MeetingService meetingService = zoomSDK.getMeetingService();
    if(meetingService != null) {
      meetingService.addListener(this);
    }
  }

  private void unregisterListener() {
    Log.i(TAG, "unregisterListener");
    ZoomSDK zoomSDK = ZoomSDK.getInstance();
    if(zoomSDK.isInitialized()) {
      MeetingService meetingService = zoomSDK.getMeetingService();
      meetingService.removeListener(this);
    }
  }

  @Override
  public void onCatalystInstanceDestroy() {
    unregisterListener();
  }

  // React LifeCycle
  @Override
  public void onHostDestroy() {
    unregisterListener();
  }
  @Override
  public void onHostPause() {}
  @Override
  public void onHostResume() {}
}
