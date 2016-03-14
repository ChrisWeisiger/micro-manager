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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Window;

import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.micromanager.internal.utils.GUIUtils;
import org.micromanager.Studio;

/**
 * Shows a dialog allowing the user to set the system ID and authentication key
 * for using notifications.
 */
public class NotificationConfigDialog {
   public static void show(Window parent, Studio studio) {
      DefaultNotificationManager notifier = (DefaultNotificationManager) (studio.notifier());
      JPanel panel = new JPanel(new MigLayout());
      JLabel siteLabel = new JLabel(
            "<html><a href=\"http://open-imaging.com\">http://open-imaging.com</a></html>");
      siteLabel.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            new Thread(GUIUtils.makeURLRunnable("https://open-imaging.com")).start();
         }
      });
      if (studio.notifier().getCanUseNotifications()) {
         panel.add(new JLabel(
"<html>Notifications are already enabled. If you want to stop using this<br>" +
"system for notifications, you can release its authentication key by<br>" +
"logging into your account at<br>"), "span, wrap");
         panel.add(siteLabel);
         JOptionPane.showMessageDialog(parent, panel);
         return;
      }
      panel.add(new JLabel(
"<html>Notifications are only available for users who have an active<br>" +
"subscription with Open Imaging. If you have a subscription, you may<br>" +
"access your notification settings by logging into your account at</html>"),
         "span, wrap");
      panel.add(siteLabel, "span, wrap");
      panel.add(new JLabel(
"<html>To enable notifications for this system, copy this text into the<br>" +
"\"Assigned System\" text on the website:</html>"), "span");
      JTextField macText = new JTextField(notifier.getMacAddress());
      macText.setEditable(false);
      panel.add(macText, "wrap");
      panel.add(new JLabel(
"<html>Click the \"Assign\" button, then copy the \"Authorization Key\"<br>" +
"text on the website into this field:"), "span");
      JTextField keyText = new JTextField(10);
      panel.add(keyText, "wrap");

      int response = JOptionPane.showConfirmDialog(parent, panel,
            "Input Notification Settings", JOptionPane.OK_CANCEL_OPTION);
      if (response != JOptionPane.OK_OPTION) {
         // User cancelled.
         return;
      }
      boolean succeeded = false;
      try {
         Integer system = Integer.parseInt(keyText.getText().split(":", 2)[0]);
         String authKey = keyText.getText().split(":", 2)[1];
         succeeded = ((DefaultNotificationManager) studio.notifier()).setIDs(
               system, authKey);
      }
      catch (NumberFormatException e) {
         studio.logs().showError("The input system ID is not valid.");
         return;
      }
      catch (ArrayIndexOutOfBoundsException e) {
         studio.logs().showError("The authorization key was not valid.");
      }
      String message = succeeded ?
         "Success! Thank you for supporting \u00b5Manager!" :
         "Sorry, that was not a valid server ID / key combination.";
      JOptionPane.showMessageDialog(parent, message);
   }
}
