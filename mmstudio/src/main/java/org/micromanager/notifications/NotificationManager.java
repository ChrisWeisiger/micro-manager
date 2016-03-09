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

import java.io.IOException;

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
    * @throws IOException if there was an error communicating with the server.
    */
   public void sendTextAlert(String text) throws IOException;

   /**
    * Enabled thread monitoring for the current thread, and allow sending
    * heartbeat notifications to the server, with the specified timeout. If the
    * thread stops without having called stopHeartbeats(), or the server fails
    * to detect a heartbeat notification within the given timeout, then it will
    * send an alert to the user.
    * @param text Text of the alert to send if heartbeat notifications fail.
    * @param timeoutMinutes Amount of time that must pass before the server
    *        decides that the thread has erroneously stopped sending
    *        heartbeats, in minutes. The minimum allowed value is 2.
    * @throws IllegalArgumentException If timeout is less than 2, or if the
    *        thread is already being monitored.
    * @throws IOException if there was an error communicating with the server.
    */
   public void startThreadHeartbeats(String text, int timeoutMinutes) throws IOException;

   /**
    * Stop sending heartbeat notifications to the server for the current
    * thread.
    * @throws IOException if there was an error communicating with the server.
    */
   public void stopThreadHeartbeats() throws IOException;

   /**
    * Send a heartbeat from the current thread. You may call this method freely
    * (as long as the thread has previously had startHeartbeats() called for
    * it), but Micro-Manager will throttle heartbeat notifications, so there is
    * no guarantee that the heartbeat will be sent to the server immediately.
    * @throws IllegalArgumentException if the current thread is not set up for
    *         heartbeats.
    * @throws IOException if there was an error communicating with the server.
    */
   public void sendThreadHeartbeat() throws IOException;
}
