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

package org.micromanager.notifications.internal;

import com.google.common.io.ByteStreams;

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

import org.json.JSONException;
import org.json.JSONObject;

import org.micromanager.notifications.NotificationManager;

import org.micromanager.PropertyMap;
import org.micromanager.Studio;

import org.micromanager.internal.utils.MDUtils;


public class DefaultNotificationManager implements NotificationManager {
   private Studio studio_;

   private static final String DEFAULT_SERVER = "http://127.0.0.1:8000";
   private static final String CHARSET = "UTF-8";
   private static final String CRLF = "\r\n";

   private static final String SYSTEM_ID = "system ID for communicating with the notification server";
   private static final String AUTH_KEY = "authentication key for communicating with the notification server";
   private static final int DEFAULT_SYSTEM_ID = -1;
   private static final String DEFAULT_AUTH_KEY = "hello123";
   private static final String CONTACT_EMAIL = "email address to send notifications to";
   private static final String CONTACT_CELLPHONE = "cellphone to send notifications to";

   private static final Integer MONITOR_SLEEP_TIME = 3000;

   private boolean isEnabled_ = false;
   // Part of authentication to the server.
   private Integer systemId_;
   private String authKey_;
   private String macAddress_ = "";
   private String contactEmail_ = "";
   private String contactCellphone_ = "";
   // Threads that we are currently monitoring.
   private final HashSet<Thread> monitoredThreads_ = new HashSet<Thread>();
   // Queue of incoming heartbeats to process.
   private final LinkedBlockingQueue<Thread> heartbeats_ = new LinkedBlockingQueue<Thread>();
   private Thread threadMonitor_ = null;

   public DefaultNotificationManager(Studio studio) {
      studio_ = studio;
      systemId_ = studio_.profile().getInt(DefaultNotificationManager.class,
            SYSTEM_ID, DEFAULT_SYSTEM_ID);
      authKey_ = studio_.profile().getString(DefaultNotificationManager.class,
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
               isEnabled_ = setIDs(systemId_, authKey_);
               if (!isEnabled_ &&
                  (systemId_ != DEFAULT_SYSTEM_ID || authKey_ != DEFAULT_AUTH_KEY)) {
                  studio_.logs().showError("This system is unable to use notifications.");
                  storeSystemID(DEFAULT_SYSTEM_ID);
                  storeAuthKey(DEFAULT_AUTH_KEY);
               }
            }
         }).start();
      }
      contactEmail_ = studio_.profile().getString(
               DefaultNotificationManager.class, CONTACT_EMAIL, "");
      contactCellphone_ = studio_.profile().getString(
               DefaultNotificationManager.class, CONTACT_CELLPHONE, "");
   }

   // TODO: this should set the strings into the global profile.
   public boolean setIDs(int systemId, String authKey) {
      try {
         JSONObject params = new JSONObject();
         params.put("system", systemId);
         params.put("auth_key", authKey);
         params.put("mac_address", macAddress_);
         isEnabled_ = sendRequest("/notify/testKey", params);
         if (isEnabled_) {
            systemId_ = systemId;
            authKey_ = authKey;
            storeSystemID(systemId);
            storeAuthKey(authKey);
         }
      }
      catch (JSONException e) {
         // This should never happen!
         studio_.logs().logError(e, "Error martialling parameters to test IDs");
      }
      catch (Exception e) {
         // Assume the ID was invalid.
         return false;
      }
      return isEnabled_;
   }

   public void setContactCellphone(String cellphone) {
      contactCellphone_ = cellphone;
      studio_.profile().setString(DefaultNotificationManager.class,
            CONTACT_CELLPHONE, cellphone);
   }

   public void setContactEmail(String email) {
      contactEmail_ = email;
      studio_.profile().setString(DefaultNotificationManager.class,
            CONTACT_EMAIL, email);
   }

   private void storeSystemID(int systemId) {
      studio_.profile().setInt(DefaultNotificationManager.class,
            SYSTEM_ID, systemId);
   }

   private void storeAuthKey(String authKey) {
      studio_.profile().setString(DefaultNotificationManager.class,
            AUTH_KEY, authKey);
   }

   public String getMacAddress() {
      return macAddress_;
   }

   @Override
   public boolean getCanUseNotifications() {
      return isEnabled_;
   }

   @Override
   public void sendTextAlert(String text) throws IOException {
      studio_.logs().showError("Not yet implemented");
   }

   @Override
   public void startThreadHeartbeats(String text, int timeoutMinutes) throws IOException {
      if (timeoutMinutes < 2) {
         throw new IllegalArgumentException("Heartbeat timeout " + timeoutMinutes + " is too short");
      }
      Thread thread = Thread.currentThread();
      if (monitoredThreads_.contains(thread)) {
         throw new IllegalArgumentException("Thread " + thread + " is already being monitored");
      }
      synchronized(monitoredThreads_) {
         monitoredThreads_.add(thread);
      }
      sendRequest("/notify/startMonitor", martialParams(
            "monitor_id", Long.toString(thread.getId()),
            "failure_text", text, "email", contactEmail_,
            "cellphone", contactCellphone_,
            "timeout_minutes", Integer.toString(timeoutMinutes)));
      if (threadMonitor_ == null) {
         // Time to start monitoring.
         restartMonitor();
      }
   }

   @Override
   public void stopThreadHeartbeats() throws IOException {
      Thread thread = Thread.currentThread();
      if (!monitoredThreads_.contains(thread)) {
         throw new IllegalArgumentException("Thread " + thread + " is not currenty sending heartbeats.");
      }
      synchronized(monitoredThreads_) {
         monitoredThreads_.remove(thread);
         if (monitoredThreads_.isEmpty()) {
            // Stop general monitoring, and wait for the thread to exit.
            try {
               threadMonitor_.interrupt();
               threadMonitor_.join();
               threadMonitor_ = null;
            }
            catch (InterruptedException e) {
               studio_.logs().logError(e, "Interrupted while waiting for threadMonitor to end.");
            }
         }
      }
      sendRequest("/notify/stopMonitor", martialParams(
            "monitor_id", Long.toString(thread.getId())));
   }

   @Override
   public void sendThreadHeartbeat() {
      heartbeats_.offer(Thread.currentThread());
   }

   private void restartMonitor() {
      threadMonitor_ = new Thread(new Runnable() {
         @Override
         public void run() {
            monitorThreads();
         }
      }, "Heartbeat thread monitor");
      threadMonitor_.start();
   }

   /**
    * This method runs in a separate thread, and does two things:
    * - listen for heartbeats in the heartbeats_ queue, periodically sending
    *   them to the server
    * - check the monitored threads to see if any of them have terminated
    *   without having called stopThreadHeartbeats.
    */
   private void monitorThreads() {
      while (true) {
         try {
            Thread.sleep(MONITOR_SLEEP_TIME);
         }
         catch (InterruptedException e) {
            return;
         }
         // Eliminate redundant heartbeats.
         HashSet<Thread> uniquifiedHeartbeats = new HashSet<Thread>();
         while (!heartbeats_.isEmpty()) {
            Thread thread = heartbeats_.poll();
            uniquifiedHeartbeats.add(thread);
         }
         for (Thread thread : uniquifiedHeartbeats) {
            try {
               sendRequest("/notify/heartbeat", martialParams(
                     "monitor_id", Long.toString(thread.getId())));
            }
            catch (IOException e) {
               studio_.logs().logError(e, "Error sending heartbeat for " + thread.getId());
            }
         }
         // Check for threads that have died.
         synchronized(monitoredThreads_) {
            for (Thread thread : new ArrayList<Thread>(monitoredThreads_)) {
               if (!thread.isAlive()) {
                  try {
                     sendRequest("/notify/monitorFailure", martialParams(
                           "monitor_id", Long.toString(thread.getId())));
                     monitoredThreads_.remove(thread);
                  }
                  catch (IOException e) {
                     studio_.logs().logError("Error sending thread death for " + thread.getId());
                  }
               }
            }
         }
      }
   }

   /**
    * Internal utility function: generate a JSONObject containing the provided
    * list of parameters, as well as our server ID, auth key, and MAC address.
    */
   private JSONObject martialParams(String... argsArray) {
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
            // This one parameter is an int, not a string.
            params.put(URLEncoder.encode("system", CHARSET), systemId_);
         }
         for (int i = 0; i < args.size(); i += 2) {
            params.put(URLEncoder.encode(args.get(i), CHARSET),
                  URLEncoder.encode(args.get(i + 1), CHARSET));
         }
      }
      catch (UnsupportedEncodingException e) {
         studio_.logs().logError(e, "Invalid character encoding " + CHARSET);
         return null;
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
   private boolean sendRequest(String path, JSONObject params) throws IOException {
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
   public String uploadConfigFile(File file) {
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
   public String uploadProblemReport(File file) {
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
   private String uploadFile(URL url, File file, String fileParamName,
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
