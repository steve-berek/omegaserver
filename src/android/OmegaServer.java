package com.steveberek.cordova.plugin;


import android.os.AsyncTask;
public class OmegaServer extends CordovaPlugin {


     private String TAG = "OMEGA_HTTP_SERVER";
     private CallbackContext globalCallbackContext;
     private String DEFAULT_DOMAIN = "localhost";
     private String rootPath = "";
     private int port = 80;

  @Override
  public boolean execute(String action, JSONArray args,
    final CallbackContext callbackContext) {
      globalCallbackContext = callbackContext;
      // Verify that the user sent a 'show' action
      if (!action.equals("startServer") && !action.equals("stopServer")) {
          callbackContext.error("\"" + action + "\" is not a recognized action.");
          return false;
      }
      if (action.equals("startServer")) {
          try {
              JSONObject options = args.getJSONObject(0);
              String root = options.getString("root");
              String port = options.getString("port");
              Log.d(TAG, "ROOTPATH_VALUE -> " + root);
              Log.d(TAG, "PORT_VALUE -> " + port);
              if (!root.equals("") && !port.equals("")) {
                  this.rootPath = root;
                  this.port = Integer.valueof(port);
              cordova.getActivity().runOnUiThread(new Runnable() {
                  public void run() {
                               new TinyServerTask().execute();
                  }
              });
              } else {
                  callbackContext.error("ROOT PATH OR PORT IS EMPTY");
                  return false;
              }
            
          } catch (Exception e) {
              callbackContext.error("Error encountered: " + e.getMessage());
              return false;
          }
      }
    }

    public void startServer() {
        TinyWebServer.startServer( this.DEFAULT_DOMAIN , this.portValue, this.rootPath);
    }

    public void stopServer() {
        TinyWebServer.stopServer();
    }
       class TinyServerTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... urls) {
            startServer();
            return null;
        }
    }
}