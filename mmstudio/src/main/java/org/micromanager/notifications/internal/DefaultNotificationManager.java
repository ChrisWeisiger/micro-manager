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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
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


public class DefaultNotificationManager implements NotificationManager {
   private Studio studio_;

   private static final String DEFAULT_SERVER = "http://127.0.0.1:8000";
   private static final String CHARSET = "UTF-8";

   private static final String SYSTEM_ID = "system ID for communicating with the notification server";
   private static final String AUTH_KEY = "authentication key for communicating with the notification server";
   private static final int DEFAULT_SYSTEM_ID = -1;
   private static final String DEFAULT_AUTH_KEY = "hello123";
   private static final String CONTACT_EMAIL = "email address to send notifications to";
   private static final String CONTACT_CELLPHONE = "cellphone to send notifications to";

   private static final Integer MONITOR_SLEEP_TIME = 3000;

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
      contactEmail_ = studio_.profile().getString(
               DefaultNotificationManager.class, CONTACT_EMAIL, "");
      contactCellphone_ = studio_.profile().getString(
               DefaultNotificationManager.class, CONTACT_CELLPHONE, "");
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
   }

   // TODO: this should set the strings into the global profile.
   public boolean setIDs(int systemId, String authKey) throws IOException {
      boolean result = sendRequest("/notify/acquireKey", "auth_key", authKey);
      if (result) {
         systemId_ = systemId;
         authKey_ = authKey;
         studio_.profile().setInt(DefaultNotificationManager.class,
               SYSTEM_ID, systemId);
         studio_.profile().setString(DefaultNotificationManager.class,
               AUTH_KEY, authKey);
      }
      return result;
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
      sendRequest("/notify/startMonitor",
            "monitor_id", Long.toString(thread.getId()),
            "failure_text", text, "email", contactEmail_,
            "cellphone", contactCellphone_,
            "timeout_minutes", Integer.toString(timeoutMinutes));
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
      sendRequest("/notify/stopMonitor",
            "monitor_id", Long.toString(thread.getId()));
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
               sendRequest("/notify/heartbeat",
                     "monitor_id", Long.toString(thread.getId()));
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
                     sendRequest("/notify/monitorFailure",
                           "monitor_id", Long.toString(thread.getId()));
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
    * Internal utility function: generate a request parameter string from the
    * provided list of parameters and send it to the server.
    */
   private boolean sendRequest(String path, String... argsArray) throws IOException {
      if (systemId_ == null) {
         throw new RuntimeException("System ID not set");
      }
      if (argsArray.length % 2 != 0) {
         throw new IllegalArgumentException("Uneven parameter list");
      }
      JSONObject params = new JSONObject();
      ArrayList<String> args = new ArrayList<String>(Arrays.asList(argsArray));
      args.addAll(new ArrayList<String>(Arrays.asList(new String[] {
         "auth_key", authKey_, "mac_address", macAddress_,
      })));
      try {
         // This one parameter is an int, not a string.
         params.put(URLEncoder.encode("system", CHARSET), systemId_);
         for (int i = 0; i < args.size(); i += 2) {
            params.put(URLEncoder.encode(args.get(i), CHARSET),
                  URLEncoder.encode(args.get(i + 1), CHARSET));
         }
      }
      catch (UnsupportedEncodingException e) {
         studio_.logs().logError(e, "Invalid character encoding " + CHARSET);
         return false;
      }
      catch (JSONException e) {
         studio_.logs().logError(e, "Error creating JSON parameters list");
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
         connection.getResponseCode();
         InputStream stream = connection.getErrorStream();
         if (stream == null) {
            stream = connection.getInputStream();
         }
         BufferedReader reader = new BufferedReader(
               new InputStreamReader(stream));
         while (true) {
            String line = reader.readLine();
            if (line == null) {
               break;
            }
            studio_.logs().logError(line);
         }
         return true;
      }
      catch (MalformedURLException e) {
         studio_.logs().logError(e, "Bad URL format " + path);
         return false;
      }
   }
}
