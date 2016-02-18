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
import java.util.HashMap;

import org.micromanager.notifications.NotificationManager;

import org.micromanager.PropertyMap;
import org.micromanager.Studio;


public class DefaultNotificationManager implements NotificationManager {
   private Studio studio_;

   private static final String DEFAULT_SERVER = "http://www.example.com";
   private static final String CHARSET = "UTF-8";

   private static final String USER_ID = "user ID for communicating with the notification server";
   private static final String DEFAULT_USER_ID = "public";

   /**
    * This class is responsible for sending heartbeats to the server.
    */
   private class Heartbeater extends Thread {
      private Object key_;
      private int id_;
      private int delaySec_;
      public Heartbeater(Object key, int id, int delaySec) {
         key_ = key;
         id_ = id;
         delaySec_ = delaySec;
      }

      @Override
      public void run() {
         while(!Thread.interrupted()) {
            try {
               Thread.sleep(delaySec_ * 1000);
            }
            catch (InterruptedException e) {
               return;
            }
            sendRequest("heartbeat", Integer.toString(id_));
         }
      }

      public int getID() {
         return id_;
      }
   }

   private String userId_;
   // Auto-incrementing ID for heartbeat IDs.
   private int curId_ = 0;
   // Maps key objects to the associated heartbeat threads.
   private HashMap<Object, Heartbeater> heartbeats_;

   public DefaultNotificationManager(Studio studio) {
      studio_ = studio;
      userId_ = studio_.profile().getString(DefaultNotificationManager.class,
            USER_ID, DEFAULT_USER_ID);
      heartbeats_ = new HashMap<Object, Heartbeater>();
   }

   // TODO: this should set the string into the global profile.
   public void setUserID(String id) {
      userId_ = id;
      studio_.profile().setString(DefaultNotificationManager.class,
            USER_ID, id);
   }

   /**
    * Generate a request parameter string from the provided list of parameters
    * and send it to the server.
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

   @Override
   public void sendTextAlert(String text) {
      sendRequest("textAlert", text);
   }

   @Override
   public void startHeartbeats(Object key, String text, int delaySec,
         int timeout) {
      if (delaySec < 10) {
         throw new IllegalArgumentException("Heartbeat delay " + delaySec + " is too short; must be at least 10s.");
      }
      if (timeout < delaySec * 2) {
         throw new IllegalArgumentException("Heartbeat timeout " + timeout + " is too short; must be at least 2x delay of " + delaySec);
      }
      if (heartbeats_.containsKey(key)) {
         throw new IllegalArgumentException("Heartbeat key " + key + " is already in use");
      }
      Heartbeater beater = new Heartbeater(key, curId_++, delaySec);
      heartbeats_.put(key, beater);
      sendRequest("startHeartbeat", Integer.toString(beater.getID()),
            "text", text, "delay", Integer.toString(delaySec),
            "timeout", Integer.toString(timeout));
      beater.start();
   }

   @Override
   public void stopHeartbeats(Object key) {
      if (!heartbeats_.containsKey(key)) {
         throw new IllegalArgumentException("Heartbeat key " + key + " is not in use.");
      }
      sendRequest("stopHeartbeat",
            Integer.toString(heartbeats_.get(key).getID()));
      heartbeats_.get(key).interrupt();
      heartbeats_.remove(key);
   }
}
