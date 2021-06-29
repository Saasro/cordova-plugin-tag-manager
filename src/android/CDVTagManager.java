  /**
   * Copyright (c) 2014 Jared Dickson
   *
   * Permission is hereby granted, free of charge, to any person obtaining a copy
   * of this software and associated documentation files (the "Software"), to deal
   * in the Software without restriction, including without limitation the rights
   * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
   * copies of the Software, and to permit persons to whom the Software is
   * furnished to do so, subject to the following conditions:
   *
   * The above copyright notice and this permission notice shall be included in
   * all copies or substantial portions of the Software.
   *
   * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
   * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
   * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
   * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
   * THE SOFTWARE.
   */

  package com.jareddickson.cordova.tagmanager;

  import org.apache.cordova.CordovaWebView;
  import org.apache.cordova.CallbackContext;
  import org.apache.cordova.CordovaPlugin;
  import org.apache.cordova.CordovaInterface;

  import com.google.android.gms.analytics.GoogleAnalytics;
  import com.google.android.gms.common.api.PendingResult;
  import com.google.android.gms.common.api.ResultCallback;
  import com.google.android.gms.tagmanager.Container;
  import com.google.android.gms.tagmanager.ContainerHolder;
  import com.google.android.gms.tagmanager.DataLayer;
  import com.google.android.gms.tagmanager.TagManager;


  import org.json.JSONArray;
  import org.json.JSONException;
  import org.json.JSONObject;

  import java.util.Map;
  import java.util.HashMap;
  import java.util.Iterator;
  import java.util.concurrent.TimeUnit;

  /**
   * This class echoes a string called from JavaScript.
   */
  public class CDVTagManager extends CordovaPlugin {

      private Container mContainer;
      private boolean isOpeningContainer = false;
      private static GoogleAnalytics analytics;


    public CDVTagManager() {
      }

      public void initialize(CordovaInterface cordova, CordovaWebView webView) {
          super.initialize(cordova, webView);
      }

      @Override
      public boolean execute(String action, JSONArray args, CallbackContext callback) {
          if (action.equals("initGTM")) {
              try {
                  // Set the dispatch interval
                analytics = GoogleAnalytics.getInstance(cordova.getActivity());
                analytics.setLocalDispatchPeriod(args.getInt(1)); // Set the dispatch interval

                  TagManager tagManager = TagManager.getInstance(this.cordova.getActivity().getApplicationContext());
                  String containerId = args.getString(0);
                  int interval = args.getInt(1);

                PendingResult<ContainerHolder> pending = tagManager.loadContainerPreferFresh(containerId, -1);
                  pending.setResultCallback(new ResultCallback<ContainerHolder>() {
                  @Override
                  public void onResult(ContainerHolder containerHolder) {
                    ContainerHolderSingleton.setContainerHolder(containerHolder);
                    mContainer = containerHolder.getContainer();
                    if (containerHolder != null && containerHolder.getStatus().isSuccess()) {
                      callback.success("initGTM - id = " +containerId + "; interval = " + interval + " seconds");
                      isOpeningContainer = true;
                    } else {
                      callback.error("initGTM - id = " +containerId + "; interval = " + interval + " seconds");
                      isOpeningContainer = false;
                    }
                  }
                }, 2000, TimeUnit.MILLISECONDS);
                  return true;
              } catch (final Exception e) {
                  callback.error(e.getMessage());
              }
          } else if (action.equals("exitGTM")) {
              try {
                  isOpeningContainer = false;
                  callback.success("exitGTM");
                  return true;
              } catch (final Exception e) {
                  callback.error(e.getMessage());
              }
          } else if (action.equals("trackEvent")) {
              if (isOpeningContainer) {
                  try {
                      DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                      int value = 0;
                      try {
                          value = args.getInt(3);
                      } catch (Exception e) {
                      }
                      dataLayer.push(DataLayer.mapOf("event", "interaction", "target", args.getString(0), "action", args.getString(1), "target-properties", args.getString(2), "value", value));
                      callback.success("trackEvent - category = " + args.getString(0) + "; action = " + args.getString(1) + "; label = " + args.getString(2) + "; value = " + value);
                      TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).dispatch();
                      return true;
                  } catch (final Exception e) {
                      callback.error(e.getMessage());
                  }
              } else {
                  callback.error("trackEvent failed - not initialized");
              }
          } else if (action.equals("pushEvent")) {
              if (isOpeningContainer) {
                  try {
                      DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                      dataLayer.push(objectMap(args.getJSONObject(0)));
                      callback.success("pushEvent: " + dataLayer.toString());
                      return true;
                  } catch (final Exception e) {
                      callback.error(e.getMessage());
                  }
              } else {
                  callback.error("pushEvent failed - not initialized");
              }
          } else if (action.equals("trackPage")) {
              if (isOpeningContainer) {
                  try {
                      DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                      dataLayer.push(DataLayer.mapOf("event", "content-view", "content-name", args.get(0)));
                      callback.success("trackPage - url = " + args.getString(0));
                      return true;
                  } catch (final Exception e) {
                      callback.error(e.getMessage());
                  }
              } else {
                  callback.error("trackPage failed - not initialized");
              }
          } else if (action.equals("dispatch")) {
              if (isOpeningContainer) {
                  try {
                      //GAServiceManager.getInstance().dispatchLocalHits();
                      callback.success("dispatch sent");
                      return true;
                  } catch (final Exception e) {
                      callback.error(e.getMessage());
                  }
              } else {
                  callback.error("dispatch failed - not initialized");
              }
          }
          return false;
      }

      private Map<String, Object> objectMap(JSONObject o) throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>(o.length());

        if (o.length() == 0) {
              return map;
          }
          Iterator it = o.keys();
          String key;
          Object value;
          while (it.hasNext()) {
              key = (String) it.next();
              value = o.has(key.toString()) ? o.get(key.toString()): null;
              map.put(key, value);
          }
          return map;
      }

    public static class ContainerHolderSingleton {
      private static ContainerHolder containerHolder;

      /**
       * Utility class; don't instantiate.
       */
      private ContainerHolderSingleton() {
      }

      public static ContainerHolder getContainerHolder() {
        return containerHolder;
      }

      public static void setContainerHolder(ContainerHolder c) {
        containerHolder = c;
      }
    }

  }
