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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashMap;
import java.util.HashSet;

import org.micromanager.notifications.NotificationManager;

import org.micromanager.PropertyMap;
import org.micromanager.Studio;


public class DefaultNotificationManager implements NotificationManager {
   private Studio studio_;

   private static final String DEFAULT_SERVER = "http://derakon.mooo.com/~chriswei/cgi-bin/index.py";
   private static final String CHARSET = "UTF-8";

   private static final String USER_ID = "user ID for communicating with the notification server";
   private static final String AUTH_KEY = "authentication key for communicating with the notification server";
   private static final String DEFAULT_USER_ID = "public";
   private static final String DEFAULT_AUTH_KEY = "auth";

   private static final Integer MONITOR_SLEEP_TIME = 3000;

   // Part of authentication to the server.
   private String userId_;
   private String macAddress_ = "";
   // Threads that we are currently monitoring.
   private final HashSet<Thread> monitoredThreads_ = new HashSet<Thread>();
   // Queue of incoming heartbeats to process.
   private final LinkedBlockingQueue<Thread> heartbeats_ = new LinkedBlockingQueue<Thread>();
   private Thread threadMonitor_ = null;

   public DefaultNotificationManager(Studio studio) {
      studio_ = studio;
      userId_ = studio_.profile().getString(DefaultNotificationManager.class,
            USER_ID, DEFAULT_USER_ID);
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
   }

   // TODO: this should set the strings into the global profile.
   public void setIDs(String userID, String authKey) {
      userId_ = userID;
      authKey_ = authKey;
      studio_.profile().setString(DefaultNotificationManager.class,
            USER_ID, userID);
      studio_.profile().setString(DefaultNotificationManager.class,
            AUTH_KEY, authKey);
   }

   @Override
   public void sendTextAlert(String text) {
      sendRequest("action", "textAlert", "message", text);
   }

   @Override
   public void startThreadHeartbeats(String text, int timeoutMinutes) {
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
      sendRequest("action", "startHeartbeat",
            "notificationID", Long.toString(thread.getId()),
            "message", text, "timeout", Integer.toString(timeoutMinutes));
      if (threadMonitor_ == null) {
         // Time to start monitoring.
         restartMonitor();
      }
   }

   @Override
   public void stopThreadHeartbeats() {
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
      sendRequest("action", "stopHeartbeat",
            "notificationID", Long.toString(thread.getId()));
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
            sendRequest("action", "heartbeat",
                  "notificationID", Long.toString(thread.getId()));
         }
         // Check for threads that have died.
         synchronized(monitoredThreads_) {
            for (Thread thread : new ArrayList<Thread>(monitoredThreads_)) {
               if (!thread.isAlive()) {
                  sendRequest("action", "heartbeatFailure",
                        "notificationID", Long.toString(thread.getId()));
                  monitoredThreads_.remove(thread);
               }
            }
         }
      }
   }

   /**
    * Internal utility function: generate a request parameter string from the
    * provided list of parameters and send it to the server.
    */
   private void sendRequest(String... argsArray) {
      if (userId_ == null) {
         throw new RuntimeException("User ID not set");
      }
      if (argsArray.length % 2 != 0) {
         throw new IllegalArgumentException("Uneven parameter list");
      }
      ArrayList<String> args = new ArrayList<String>(Arrays.asList(argsArray));
      args.add("userID");
      args.add(userId_);
      args.add("systemID");
      args.add(macAddress_);
      args.add("authKey");
      args.add(authKey_);
      String params = "";
      try {
         for (int i = 0; i < args.size(); i += 2) {
            params += String.format("%s%s=%s", i != 0 ? "&" : "",
                  URLEncoder.encode(args.get(i), CHARSET),
                  URLEncoder.encode(args.get(i + 1), CHARSET));
         }
      }
      catch (UnsupportedEncodingException e) {
         studio_.logs().logError(e, "Invalid character encoding " + CHARSET);
         return;
      }
      String path = DEFAULT_SERVER + "?" + params;
      try {
         URL url = new URL(path);
         URLConnection connection = url.openConnection();
         connection.setRequestProperty("Accept-Charset", CHARSET);
         BufferedReader reader = new BufferedReader(
               new InputStreamReader(connection.getInputStream()));
         while (true) {
            String line = reader.readLine();
            if (line == null) {
               break;
            }
            studio_.logs().logError(line);
         }
      }
      catch (MalformedURLException e) {
         studio_.logs().logError(e, "Bad URL format " + path);
      }
      catch (IOException e) {
         studio_.logs().logError(e, "Error reading response from server");
      }
   }
}
