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

import org.micromanager.notifications.NotificationManager;

import org.micromanager.PropertyMap;
import org.micromanager.Studio;


public class DefaultNotificationManager implements NotificationManager {
   private Studio studio_;

   public DefaultNotificationManager(Studio studio) {
      studio_ = studio;
   }

   @Override
   public void sendTextAlert(String text) {
      studio_.logs().logError("Sending text " + text);
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
      studio_.logs().logError(String.format("Starting heartbeats from %s with failure text [%s], delay %d, timeout %d", key, text, delaySec, timeout));
   }

   @Override
   public void stopHeartbeats(Object key) {
      studio_.logs().logError("Stopping heartbeats from " + key);
   }
}
