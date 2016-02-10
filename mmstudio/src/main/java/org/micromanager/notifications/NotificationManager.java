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

package org.micromanager.notifications;

import org.micromanager.PropertyMap;

/**
 * This class provides access to the notification system for sending alerts to
 * users. It can be accessed via Studio.notifier() or
 * Studio.getNotificationManager().
 */
public interface NotificationManager {
   /**
    * Send a text notification to the current user. This requires the user
    * to have provided appropriate contact information via the GUI.
    * @param text String of the text to send to the user.
    */
   public void sendTextAlert(String text);

   /**
    * Start sending heartbeat notifications to the server at the specified
    * frequency, and with the specified timeout. If the server fails to detect
    * a heartbeat notification within the given timeout, then it will send
    * an alert to the user.
    * @param key Unique key object used to identify the "owner" of these
    *        heartbeats, and also used by stopHeartbeats().
    * @param text Text of the alert to send if heartbeat notifications fail.
    * @param delaySec Time between heartbeat notifications, in seconds.
    * @param timeout Amount of time that must pass before the server decides
    *        that the program has erroneously stopped sending heartbeats.
    * @throws IllegalArgumentException If delaySec is less than 10 seconds,
    *         if timeout is less than 2 * delaySec, or if the key parameter
    *         is already being used for heartbeats.
    */
   public void startHeartbeats(Object key, String text, int delaySec,
         int timeout);

   /**
    * Stop sending heartbeat notifications to the server.
    * @param key Unique key object that was passed to startHeartbeats()
    *        previously. Once this method completes, the key may be re-used by
    *        startHeartbeats().
    */
   public void stopHeartbeats(Object key);
}
