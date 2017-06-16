/*
 com.lampa.startapp
 https://github.com/lampaa/com.lampa.startapp

 Phonegap plugin for check or launch other application in android device (iOS support).
 bug tracker: https://github.com/lampaa/com.lampa.startapp/issues
 */
package com.lampa.startapp;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import java.util.Iterator;

import android.content.pm.ResolveInfo;
import android.net.Uri;
import java.lang.reflect.Field;
import java.util.List;

import android.content.ActivityNotFoundException;
import android.util.Log;
import android.os.Bundle;

public class startApp extends CordovaPlugin {

  public static final String TAG = "startApp";
  public startApp() { }
  private boolean NO_PARSE_INTENT_VALS = false;
  private CallbackContext savedCallback = null;

  /**
   * Executes the request and returns PluginResult.
   *
   * @param action            The action to execute.
   * @param args              JSONArray of arguments for the plugin.
   * @param callbackContext   The callback context used when calling back into JavaScript.
   * @return                  True when the action was valid, false otherwise.
   */
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("start")) {
      this.start(args, callbackContext);
    }
    else if(action.equals("go")) {
      this.start(args, callbackContext);
    }
    else if(action.equals("check")) {
      this.check(args, callbackContext);
    }
    else if(action.equals("getExtras")) {
      this.getExtras(callbackContext);
    }
    else if(action.equals("getExtra")) {
      this.getExtra(args, callbackContext);
    }

    return true;
  }


  /**
   * startApp
   */
  public void start(JSONArray args, final CallbackContext callback) {
    Intent LaunchIntent;
    final JSONObject params;
    JSONArray flags;
    JSONArray component;

    JSONObject extra;
    String key;

    int i;

    try {
      if (args.get(0) instanceof JSONObject) {
        params = args.getJSONObject(0);

        /*
         * disable parsing intent values
         */
        if(params.has("no_parse")) {
          NO_PARSE_INTENT_VALS = true;
        }

        /*
         * set application
         * http://developer.android.com/reference/android/content/Intent.html(java.lang.String)
         */
        if(params.has("application")) {
          PackageManager manager = cordova.getActivity().getApplicationContext().getPackageManager();
          LaunchIntent = manager.getLaunchIntentForPackage(params.getString("application"));

          if (LaunchIntent == null) {
            callback.error("Application \""+ params.getString("application") +"\" not found!");
            return;
          }
        }
        /*
         * set application
         * http://developer.android.com/reference/android/content/Intent.html (java.lang.String)
         */
        else if(params.has("intent")){
          LaunchIntent = new Intent( params.getString("intent") );
        }
        else {
          LaunchIntent = new Intent();
        }


        /*
         * set package
         * http://developer.android.com/reference/android/content/Intent.html#setPackage(java.lang.String)
         */
        if(params.has("package")) {
          LaunchIntent.setPackage(params.getString("package"));
        }

        /*
         * set action
         * http://developer.android.com/intl/ru/reference/android/content/Intent.html#setAction%28java.lang.String%29
         */
        if(params.has("action")) {
          LaunchIntent.setAction(getIntentValueString(params.getString("action")));
        }

        /*
         * set category
         * http://developer.android.com/intl/ru/reference/android/content/Intent.html#addCategory%28java.lang.String%29
         */
        if(params.has("category")) {
          LaunchIntent.addCategory(getIntentValueString(params.getString("category")));
        }

        /*
         * set type
         * http://developer.android.com/intl/ru/reference/android/content/Intent.html#setType%28java.lang.String%29
         */
        if(params.has("type")) {
          LaunchIntent.setType(params.getString("type"));
        }



        /*
         * set data (uri)
         * http://developer.android.com/intl/ru/reference/android/content/Intent.html#setData%28android.net.Uri%29
         */
        if(params.has("uri")) {
          LaunchIntent.setData(Uri.parse(params.getString("uri")));
        }

        /*
         * set flags
         * http://developer.android.com/intl/ru/reference/android/content/Intent.html#addFlags%28int%29
         */
        if(params.has("flags")) {
          flags = params.getJSONArray("flags");

          for(i=0; i < flags.length(); i++) {
            LaunchIntent.addFlags(getIntentValue(flags.getString(i)));
          }
        }

        /*
         * set component
         * http://developer.android.com/intl/ru/reference/android/content/Intent.html#setComponent%28android.content.ComponentName%29
         */
        if(params.has("component")) {
          component = params.getJSONArray("component");

          if(component.length() == 2) {
            LaunchIntent.setComponent(new ComponentName(component.getString(0), component.getString(1)));
          }
        }

        /*
         * set extra fields
         */
        if(!args.isNull(1)) {
          extra = args.getJSONObject(1);
          Iterator<String> iter = extra.keys();

          while (iter.hasNext()) {
            key = iter.next();

            String extraName = parseExtraName(key);
            JSONObject objWithType = extra.optJSONObject(key);
            Log.i(TAG, extraName);

            if( objWithType == null  ){
              Log.i(TAG, "is string");
              String strValue = extra.getString(key);
              LaunchIntent.putExtra(extraName, strValue);
            } else {
              String type = objWithType.getString("type");
              Log.i(TAG, type);
              if( type.equals("boolean") ){
                Log.i(TAG, "set bool");
                boolean boolVal = objWithType.getBoolean("value");
                LaunchIntent.putExtra(extraName, boolVal);
              } else if( type.equals("integer") ){
                Log.i(TAG, "set int");
                long intVal = objWithType.getLong("value");
                Log.i(TAG, Long.toString(intVal) );
                LaunchIntent.putExtra(extraName, intVal);
              } else {

                String strValue = objWithType.getString("value");
                LaunchIntent.putExtra(extraName, strValue);
              }
            }
          }
        }

        /*
         * launch intent
         */
        boolean deferCallback = false;


        if(params.has("intentstart") && "startActivityForResult".equals(params.getString("intentstart"))) {
          deferCallback = true;
          if( !isCallable( LaunchIntent ) ){
            callback.error("ActivityNotFoundException");
          } else{
            savedCallback = callback;
            cordova.startActivityForResult(this, LaunchIntent, 1000);
          }
        }
        else if(params.has("intentstart") && "sendBroadcast".equals(params.getString("intentstart"))) {
          cordova.getActivity().sendBroadcast(LaunchIntent);
        }
        else if(params.has("intentstart") && "sendOrderedBroadcast".equals(params.getString("intentstart"))) {
          deferCallback = true;
          cordova.getActivity().sendOrderedBroadcast(LaunchIntent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
              try {
                Bundle result = getResultExtras(true);

                if(params.has("results")) {
                  JSONObject resultObj = new JSONObject();
                  JSONArray results = params.getJSONArray("results");

                  for(int i=0; i < results.length(); i++) {
                    String key = results.getString(i);
                    Object val = result.get(key);
                    String strVal = "";
                    if( val != null ){
                      strVal = val.toString();
                    }
                    resultObj.put( key, strVal);
                  }

                  callback.success(resultObj);
                }  else {
                  callback.success();
                }

              }  catch (JSONException e) {
                callback.error("JSONException: " + e.getMessage());
                e.printStackTrace();
              }

            }
          }, null, 0, null, null);

        } else {
          cordova.getActivity().startActivity(LaunchIntent);
        }

        if( !deferCallback ){
          callback.success();
        }

      }
      else {
        callback.error("Incorrect params, array is not array object!");
      }
    }
    catch (JSONException e) {
      callback.error("JSONException: " + e.getMessage());
      e.printStackTrace();
    }
    catch (IllegalAccessException e) {
      callback.error("IllegalAccessException: " + e.getMessage());
      e.printStackTrace();
    }
    catch (NoSuchFieldException e) {
      callback.error("NoSuchFieldException: " + e.getMessage());
      e.printStackTrace();
    }
    catch (ActivityNotFoundException e) {
      callback.error("ActivityNotFoundException: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if( requestCode == 1000 ) {
      String strVal = Integer.toString(resultCode);
      savedCallback.success(strVal);
    }
  }

  /**
   * checkApp
   */
  public void check(JSONArray args, CallbackContext callback) {
    JSONObject params;

    try {
      if (args.get(0) instanceof JSONObject) {
        params = args.getJSONObject(0);


        if(params.has("package")) {
          PackageManager pm = cordova.getActivity().getApplicationContext().getPackageManager();

          /*
           * get package info
           */
          PackageInfo PackInfo = pm.getPackageInfo(params.getString("package"), PackageManager.GET_ACTIVITIES);

          /*
           * create json object
           */
          JSONObject info = new JSONObject();

          info.put("versionName", PackInfo.versionName);
          info.put("packageName", PackInfo.packageName);
          info.put("versionCode", PackInfo.versionCode);
          info.put("applicationInfo", PackInfo.applicationInfo);

          callback.success(info);
        }
        else {
          callback.error("Value \"package\" in null!");
        }
      }
      else {
        callback.error("Incorrect params, array is not array object!");
      }
    } catch (JSONException e) {
      callback.error("json: " + e.toString());
      e.printStackTrace();
    }
    catch (NameNotFoundException e) {
      callback.error("NameNotFoundException: " + e.toString());
      e.printStackTrace();
    }
  }

  /**
   * getExtras
   */
  private void getExtras(CallbackContext callback) {
    try {
      Bundle extras = cordova.getActivity().getIntent().getExtras();
      JSONObject info = new JSONObject();

      if (extras != null) {
        for (String key : extras.keySet()) {
          Object val = extras.get(key);
          info.put(key, val == null ? "" : val.toString());
        }
      }

      callback.success(info);
    }
    catch(JSONException e) {
      callback.error(e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * getExtra
   */
  private void getExtra(JSONArray args, CallbackContext callback) {
    try {
      String extraName = parseExtraName(args.getString(0));
      Intent extraIntent = cordova.getActivity().getIntent();

      if(extraIntent.hasExtra(extraName)) {
        String extraValue = extraIntent.getStringExtra(extraName);

        if (extraValue == null) {
          extraValue = extraIntent.getParcelableExtra(extraName).toString();
        }

        callback.success(extraValue);
      }
      else {
        callback.error("extra field not found");
      }
    }
    catch(JSONException e) {
      callback.error(e.getMessage());
      e.printStackTrace();
    }
  }


  private String parseExtraName(String extraName) {

    try {
      extraName = this.getIntentValueString(extraName);
      Log.i(TAG, extraName);
    }
    catch(NoSuchFieldException e) {
      e.printStackTrace();
    }
    catch(IllegalAccessException e) {
      e.printStackTrace();
    }

    Log.e(TAG, extraName);

    return extraName;
  }

  private  String getIntentValueString(String flag) throws NoSuchFieldException, IllegalAccessException {
    if(NO_PARSE_INTENT_VALS) {
      return flag;
    }

    Field field = Intent.class.getDeclaredField(flag);
    field.setAccessible(true);

    return (String) field.get(null);
  }

  private static int getIntentValue(String flag) throws NoSuchFieldException, IllegalAccessException {
    Field field = Intent.class.getDeclaredField(flag);
    field.setAccessible(true);

    return field.getInt(null);
  }

  private boolean isCallable(Intent intent) {
    List<ResolveInfo> list = this.cordova.getActivity().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
    return list.size() > 0;
  }
}
