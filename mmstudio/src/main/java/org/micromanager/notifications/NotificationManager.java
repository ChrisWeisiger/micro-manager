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
import java.net.ConnectException;

import org.micromanager.PropertyMap;

/**
 * This class provides access to the notification system for sending alerts to
 * users. It can be accessed via Studio.notifier() or
 * Studio.getNotificationManager(). Note that access to the notification
 * system requires notifications to be enabled and for the notification server
 * to accept your authentication information, which can be set via the
 * Multi-D Acquisition dialog's "Enable Notifications" button.
 * If your system is not authorized, then attempts to use methods in this
 * class will throw IOExceptions.
 */
public interface NotificationManager {
   /**
    * Return whether or not this system is capable of performing notifications.
    * To be able to use notifications, the system must have been previously
    * configured with a valid system ID and authentication key via the
    * Options panel, and the user must have enabled the "Notifications"
    * checkbox in the Multi-Dimensional Acquisition dialog.
    * @returns Whether or not notifications are possible.
    */
   public boolean getCanUseNotifications();

   /**
    * Send a notification to the current user. This requires the user
    * to have provided appropriate contact information via the GUI, in the
    * Multi-Dimensional Acquisition dialog.
    * @param text String of the text to send to the user. For emails, the
    *        first line (up to a newline character) or first 80 characters,
    *        whichever comes first, will be used as the email subject. For
    *        SMS messages, the text will be truncated to 140 characters.
    *        The special text "{system}" will be replaced by the name of this
    *        microscope system.
    * @throws ConnectException if the server was not reachable. Note that
    *         ConnectException is a subclass of IOException.
    * @throws IOException if there was an error communicating with the server.
    * @throws NotificationsDisabledException if the user has not enabled
    *         notifications.
    */
   public void sendNotification(String text) throws IOException, ConnectException, NotificationsDisabledException;

   /**
    * Return the remaining number of SMS text messages that may be sent to
    * the user's cellphone. Each subscription has a cap on SMS messages, with
    * a 30-day rolling window; if the cap is exceeded, then attempts to send
    * SMS messages (e.g. via sendNotification()) will throw an IOException.
    * @return Remaining number of text messages that may be sent before the cap
    *         is exceeded.
    * @throws ConnectException if the server was not reachable. Note that
    *         ConnectException is a subclass of IOException.
    * @throws IOException if there was an error communicating with the server.
    */
   public int getRemainingSMSMessages() throws IOException, ConnectException;

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
    * @throws ConnectException if the server was not reachable. Note that
    *         ConnectException is a subclass of IOException.
    * @throws IOException if there was an error communicating with the server.
    * @throws NotificationsDisabledException if the user has not enabled
    *         notifications.
    */
   public void startThreadHeartbeats(String text, int timeoutMinutes) throws IOException, ConnectException, NotificationsDisabledException;

   /**
    * Stop sending heartbeat notifications to the server for the current
    * thread.
    * @throws ConnectException if the server was not reachable. Note that
    *         ConnectException is a subclass of IOException.
    * @throws IOException if there was an error communicating with the server.
    */
   public void stopThreadHeartbeats() throws IOException, ConnectException;

   /**
    * Send a heartbeat from the current thread. You may call this method freely
    * (as long as the thread has previously had startHeartbeats() called for
    * it), but Micro-Manager will throttle heartbeat notifications, so there is
    * no guarantee that the heartbeat will be sent to the server immediately.
    * @throws IllegalArgumentException if the current thread is not set up for
    *         heartbeats.
    * @throws ConnectException if the server was not reachable. Note that
    *         ConnectException is a subclass of IOException.
    * @throws IOException if there was an error communicating with the server.
    */
   public void sendThreadHeartbeat() throws IOException, ConnectException;
}
