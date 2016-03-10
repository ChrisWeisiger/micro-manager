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
"<html>Please input the system ID and corresponding authentication key for<br>" + 
"an unused system.</html>"), "span, wrap");
      panel.add(new JLabel("System ID:"), "alignx label");
      JTextField system = new JTextField(10);
      panel.add(system);
      panel.add(new JLabel("Authentication key:"), "alignx label");
      JTextField authKey = new JTextField(10);
      panel.add(authKey);

      int response = JOptionPane.showConfirmDialog(parent, panel,
            "Input Notification Settings", JOptionPane.OK_CANCEL_OPTION);
      if (response != JOptionPane.OK_OPTION) {
         // User cancelled.
         return;
      }
      boolean succeeded = false;
      try {
         succeeded = ((DefaultNotificationManager) studio.notifier()).setIDs(
               Integer.parseInt(system.getText()), authKey.getText());
      }
      catch (NumberFormatException e) {
         studio.logs().showError("The input system ID is not valid.");
         return;
      }
      catch (IOException e) {
         studio.logs().showError(e, "Sorry, we were unable to set your notification keys.");
         return;
      }
      String message = succeeded ?
         "Success! Thank you for supporting \u00b5Manager!" :
         "Sorry, that was not a valid server ID / key combination.";
      JOptionPane.showMessageDialog(parent, message);
   }
}
