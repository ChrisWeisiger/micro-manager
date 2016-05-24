///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//-----------------------------------------------------------------------------
//
// AUTHOR:       Chris Weisiger, 2015
//
// COPYRIGHT:    Open Imaging, Inc. 2015
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

package org.micromanager.internal;

import com.google.common.io.ByteStreams;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.json.JSONException;
import org.json.JSONObject;

import org.micromanager.notifications.NotificationManager;

import org.micromanager.PropertyMap;
import org.micromanager.Studio;

import org.micromanager.internal.utils.DefaultUserProfile;
import org.micromanager.internal.utils.MDUtils;


/**
 * This class is used to communicate with the server, assuming one is
 * available.
 */
public class ServerComms {
   private static final String DEFAULT_SERVER = "http://127.0.0.1:8000";
   private static final String CHARSET = "UTF-8";
   private static final String CRLF = "\r\n";

   private static final String SYSTEM_ID = "system ID for communicating with the server";
   private static final String AUTH_KEY = "authentication key for communicating with the server";
   private static final String SUPPRESS_AUTH_WARNINGS = "should suppress warnings about authenticating with the server";
   private static final String SUPPRESS_CONNECT_WARNINGS = "should suppress warnings about connecting to the server";
   private static final int DEFAULT_SYSTEM_ID = -1;
   private static final String DEFAULT_AUTH_KEY = "invalid auth key";

   private static Studio studio_;
   private static boolean isEnabled_ = false;
   // Part of authentication to the server.
   private static Integer systemId_;
   private static String authKey_;
   private static String macAddress_ = "";

   public static void initialize(Studio studio) {
      studio_ = studio;
      systemId_ = studio_.profile().getInt(ServerComms.class,
            SYSTEM_ID, DEFAULT_SYSTEM_ID);
      authKey_ = studio_.profile().getString(ServerComms.class,
            AUTH_KEY, DEFAULT_AUTH_KEY);

      mmcorej.StrVector addrs = studio_.core().getMACAddresses();
      if (addrs.size() > 0) {
         String addr = addrs.get(0);
         if (addr.length() > 0) {
            macAddress_ = addr;
         }
      }
      if (macAddress_.equals("")) {
         studio_.logs().logError("Unable to determine MAC address.");
      }

      // TODO: do we really want to communicate with the server on every
      // launch?
      if (systemId_ != DEFAULT_SYSTEM_ID ||
            !authKey_.equals(DEFAULT_AUTH_KEY)) {
         new Thread(new Runnable() {
            @Override
            public void run() {
               try {
                  isEnabled_ = setIDs(systemId_, authKey_);
               }
               catch (ConnectException e) {
                  showDialog("Communication with the server failed. Services that depend on the server will not be available.",
                        SUPPRESS_CONNECT_WARNINGS);
                  return;
               }
               if (!isEnabled_) {
                  showDialog("This system was unable to authenticate with the server. Some services will not be available.",
                        SUPPRESS_AUTH_WARNINGS);
               }
            }
         }).start();
      }
   }

   /**
    * Shows the specified error, with a "Do not show me again" checkbox, whose
    * state is stored in the specified profile key.
    */
   private static void showDialog(String error, String suppressKey) {
      if (studio_.profile().getBoolean(ServerComms.class,
               suppressKey, false)) {
         return;
      }
      if (GraphicsEnvironment.isHeadless()) {
         studio_.logs().logError(error);
         return;
      }
      JPanel panel = new JPanel(new MigLayout());
      panel.add(new JLabel(error), "span, wrap");
      JCheckBox checkbox = new JCheckBox("Do not show me this again.");
      panel.add(checkbox);
      JOptionPane.showMessageDialog(null, panel);
      studio_.profile().setBoolean(ServerComms.class,
            suppressKey, checkbox.isSelected());
   }

   public static void clearIDs() {
      storeSystemID(DEFAULT_SYSTEM_ID);
      storeAuthKey(DEFAULT_AUTH_KEY);
      isEnabled_ = false;
      try {
         ((DefaultUserProfile) studio_.profile()).saveGlobalProfile();
      }
      catch (IOException e) {
         studio_.logs().logError(e, "Error saving global profile.");
      }
   }

   public static boolean setIDs(int systemId, String authKey) throws ConnectException {
      int oldId = studio_.profile().getInt(ServerComms.class,
            SYSTEM_ID, DEFAULT_SYSTEM_ID);
      String oldKey = studio_.profile().getString(ServerComms.class,
            AUTH_KEY, DEFAULT_AUTH_KEY);
      if (systemId != oldId && !authKey.contentEquals(oldKey)) {
         // Setting new connection parameters, so we should allow errors
         // to be shown again.
         studio_.profile().setBoolean(ServerComms.class,
               SUPPRESS_AUTH_WARNINGS, false);
         studio_.profile().setBoolean(ServerComms.class,
               SUPPRESS_CONNECT_WARNINGS, false);
      }
      try {
         JSONObject params = new JSONObject();
         params.put("system", systemId);
         params.put("auth_key", authKey);
         params.put("mac_address", macAddress_);
         try {
            isEnabled_ = sendRequest("/notify/testKey", params);
            if (isEnabled_) {
               systemId_ = systemId;
               authKey_ = authKey;
               storeSystemID(systemId);
               storeAuthKey(authKey);
               try {
                  ((DefaultUserProfile) studio_.profile()).saveGlobalProfile();
               }
               catch (IOException e) {
                  studio_.logs().logError(e, "Error saving global profile");
               }
            }
         }
         catch (IOException e) {
            if (!(e instanceof ConnectException)) {
               // This should only ever happen if the keys are invalid.
               studio_.logs().logError(e, "Error testing authentication keys");
               return false;
            }
            throw((ConnectException) e);
         }
      }
      catch (JSONException e) {
         // This should never happen!
         studio_.logs().logError(e, "Error martialling parameters to test IDs");
      }
      return isEnabled_;
   }

   public static boolean isEnabled() {
      return isEnabled_;
   }

   /**
    * Return system ID and auth key as a colon-delimited string.
    */
   public static String getIDString() {
      if (systemId_ != DEFAULT_SYSTEM_ID &&
            !authKey_.contentEquals(DEFAULT_AUTH_KEY)) {
         return String.format("%d:%s", systemId_, authKey_);
      }
      return "";
   }

   /**
    * Set ID and auth key via a string formatted as "id:key". As we parse
    * a string's contents, errors are to be expected. Returns true if keys
    * are valid (i.e. can authenticate with the server).
    */
   public static boolean setIDString(String keyText) throws NumberFormatException, ArrayIndexOutOfBoundsException, ConnectException {
      Integer system = Integer.parseInt(keyText.split(":", 2)[0]);
      String authKey = keyText.split(":", 2)[1];
      return setIDs(system, authKey);
   }

   private static void storeSystemID(int systemId) {
      ((DefaultUserProfile) studio_.profile()).setGlobalInt(
            ServerComms.class, SYSTEM_ID, systemId);
   }

   private static void storeAuthKey(String authKey) {
      ((DefaultUserProfile) studio_.profile()).setGlobalString(
            ServerComms.class, AUTH_KEY, authKey);
   }

   public static String getMacAddress() {
      return macAddress_;
   }

   /**
    * Internal utility function: generate a JSONObject containing the provided
    * list of parameters, as well as our server ID, auth key, and MAC address.
    */
   public static JSONObject martialParams(String... argsArray) {
      if (argsArray.length % 2 != 0) {
         throw new IllegalArgumentException("Uneven parameter list");
      }
      JSONObject params = new JSONObject();
      ArrayList<String> args = new ArrayList<String>(Arrays.asList(argsArray));
      if (authKey_ != null) {
         args.add("auth_key");
         args.add(authKey_);
      }
      args.add("mac_address");
      args.add(macAddress_);
      try {
         if (systemId_ != null) {
            params.put("system", systemId_);
         }
         for (int i = 0; i < args.size(); i += 2) {
            params.put(args.get(i), args.get(i + 1));
         }
      }
      catch (JSONException e) {
         studio_.logs().logError(e, "Error creating JSON parameters list");
         return null;
      }
      return params;
   }

   /**
    * Internal utility function: send a request to the server.
    */
   public static boolean sendRequest(String path, JSONObject params) throws IOException, ConnectException {
      if (params == null) {
         // HACK: this check is because martialParams, above, returns null when
         // implausible things go wrong.
         return false;
      }
      try {
         URL url = new URL(DEFAULT_SERVER + path);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
         connection.setRequestProperty("Content-Type", "application/json");
         connection.setRequestProperty("Accept-Charset", CHARSET);
         connection.setDoOutput(true);
         OutputStream out = connection.getOutputStream();
         out.write(params.toString().getBytes(CHARSET));
         // Actually perform the post.
         int responseCode = connection.getResponseCode();
         if (responseCode >= 200 && responseCode <= 299) {
            return true;
         }
         // Read any error message from the server and throw an IOException
         // with the error as the contents.
         InputStream stream = connection.getErrorStream();
         if (stream == null) {
            stream = connection.getInputStream();
         }
         BufferedReader reader = new BufferedReader(
               new InputStreamReader(stream));
         String error = "";
         while (true) {
            String line = reader.readLine();
            if (line == null) {
               break;
            }
            error += line;
         }
         // Some of our errors can be JSONObjects; convert and pretty-print if
         // possible.
         try {
            error = new JSONObject(error).toString(2);
         }
         catch (JSONException ignored) {}
         throw new IOException(error);
      }
      catch (MalformedURLException e) {
         studio_.logs().logError(e, "Bad URL format " + path);
         return false;
      }
   }

   /**
    * Sends a problem report file to the server, including auth information if
    * it is available (but we allow anonymous problem reports as well). Returns
    * any error string if something went wrong.
    * TODO: it's kind of weird that this and uploadProblemReport are part of
    * the "notifications" logic.
    */
   public static String uploadConfigFile(File file) {
      URL url = studio_.plugins().getBrandPlugin().getConfigFileURL();
      if (url == null) {
         return "No valid upload URL";
      }
      return uploadFile(url, file, "config", "Config file accepted");
   }

   /**
    * Sends a problem report file to the server, including auth information if
    * it is available (but we allow anonymous problem reports as well). Returns
    * any error string if something went wrong.
    * TODO: it's kind of weird that this and uploadConfigFile are part of
    * the "notifications" logic.
    */
   public static String uploadProblemReport(File file) {
      URL url = studio_.plugins().getBrandPlugin().getProblemReportURL();
      if (url == null) {
         return "No valid upload URL";
      }
      return uploadFile(url, file, "report", "Problem report accepted");
   }

   /**
    * Adapted from
    * http://stackoverflow.com/questions/2469451/upload-files-from-java-client-to-a-http-server
    */
   private static String uploadFile(URL url, File file, String fileParamName,
         String successString) {
      try {
         String boundary = String.format(
               "-------------------MMBoundary%d", System.currentTimeMillis());
         URLConnection connection = url.openConnection();
         connection.setDoOutput(true);
         connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
         OutputStream output = connection.getOutputStream();
         PrintWriter writer = new PrintWriter(
               new OutputStreamWriter(output, CHARSET), true);

         // Send POST data.
         JSONObject params = martialParams("mac_address", macAddress_);
         for (String key : MDUtils.getKeys(params)) {
            writer.append("--" + boundary).append(CRLF);
            writer.append(String.format(
                  "Content-Disposition: form-data; name=\"%s\"%s",
                  key, CRLF));
            writer.append(String.format(
                     "Content-Type: text/plain; charset=%s%s", CHARSET, CRLF));
            writer.append(CRLF).append(params.getString(key)).append(CRLF);
            writer.flush();
         }

         // Send text file.
         writer.append("--" + boundary).append(CRLF);
         writer.append(String.format(
                  "Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"%s", fileParamName, file.getName(), CRLF));
         writer.append(String.format(
                  "Content-Type: text/plain; charset=%s%s", CHARSET, CRLF));
         writer.append(CRLF).flush();
         ByteStreams.copy(new FileInputStream(file), output);
         output.flush(); // Important before continuing with writer!
         writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

         // End of multipart/form-data.
         writer.append("--" + boundary + "--").append(CRLF).flush();

         // Read response.
         InputStream stream = connection.getInputStream();
         BufferedReader reader = new BufferedReader(
               new InputStreamReader(stream));
         String response = "";
         while (true) {
            String line = reader.readLine();
            if (line == null) {
               break;
            }
            response += line;
         }
         if (!response.equals(successString)) {
            return response;
         }
         return null;
      }
      catch (IOException e) {
         studio_.logs().logError(e, "Error uploading file to server");
         return "Error communicating with server";
      }
      catch (JSONException e) {
         studio_.logs().logError(e, "Error inserting JSON params when uploading file");
         return "File upload failed";
      }
      catch (Exception e) {
         studio_.logs().logError(e);
         return e.toString();
      }
   }
}
