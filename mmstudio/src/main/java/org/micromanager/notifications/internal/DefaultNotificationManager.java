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

import org.json.JSONException;
import org.json.JSONObject;

import org.micromanager.notifications.NotificationManager;

import org.micromanager.PropertyMap;
import org.micromanager.Studio;

import org.micromanager.internal.ServerComms;
import org.micromanager.internal.utils.MDUtils;


public class DefaultNotificationManager implements NotificationManager {
   private static final String CONTACT_EMAIL = "email address to send notifications to";
   private static final String CONTACT_CELLPHONE = "cellphone to send notifications to";

   private static final Integer MONITOR_SLEEP_TIME = 3000;

   private Studio studio_;
   private String contactEmail_ = "";
   private String contactCellphone_ = "";
   // Threads that we are currently monitoring.
   private final HashSet<Thread> monitoredThreads_ = new HashSet<Thread>();
   // Queue of incoming heartbeats to process.
   private final LinkedBlockingQueue<Thread> heartbeats_ = new LinkedBlockingQueue<Thread>();
   private Thread threadMonitor_ = null;

   public DefaultNotificationManager(Studio studio) {
      studio_ = studio;
      contactEmail_ = studio_.profile().getString(
               DefaultNotificationManager.class, CONTACT_EMAIL, "");
      contactCellphone_ = studio_.profile().getString(
               DefaultNotificationManager.class, CONTACT_CELLPHONE, "");
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
   public boolean getCanUseNotifications() {
      return ServerComms.isEnabled();
   }

   @Override
   public void sendTextAlert(String text) throws IOException, ConnectException {
      studio_.logs().showError("sendTextAlert is not yet implemented");
   }

   @Override
   public void startThreadHeartbeats(String text, int timeoutMinutes) throws IOException, ConnectException {
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
      ServerComms.sendRequest("/notify/startMonitor",
            ServerComms.martialParams(
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
   public void stopThreadHeartbeats() throws IOException, ConnectException {
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
      ServerComms.sendRequest("/notify/stopMonitor", ServerComms.martialParams(
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
               ServerComms.sendRequest("/notify/heartbeat",
                     ServerComms.martialParams(
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
                     ServerComms.sendRequest("/notify/monitorFailure",
                           ServerComms.martialParams(
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
}
