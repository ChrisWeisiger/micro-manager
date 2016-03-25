///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//-----------------------------------------------------------------------------
//
// AUTHOR:       Chris Weisiger, 2016
//
// COPYRIGHT:    (c) 2016 Open Imaging, Inc.
// COPYRIGHT:    (c) 2006 Regents of the University of California
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
//

package com.openimaging.mmbrand;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.micromanager.Studio;
import org.micromanager.internal.ServerComms;
import org.micromanager.internal.utils.DaytimeNighttime;
import org.micromanager.internal.utils.GUIUtils;
import org.micromanager.internal.utils.DefaultUserProfile;
import org.micromanager.internal.utils.ReportingUtils;

/**
 * This dialog shows a prompt to the user to register their copy of µManager.
 */
public class RegistrationDialog extends JDialog {
   private static final long serialVersionUID = 1L;
   public static final String HAVE_REGISTERED = "this user has registered with Micro-Manager";
   public static final String SHOULD_NEVER_REGISTER = "this user never wants to be prompted to register";

//   if anyone has ever regged with us, do not show
//   ask for first name, last name, org, email
//   checkbox for send me email, default checked
//   optional Open Imaging system ID field (marked optional; all other fields required)
//   optional system nickname?
//   link to privacy policy
//   send info up to server
//   add ip address as column to DB for config files, problem reports
   /**
    * Display the registration dialog, if the user has not registered and not
    * opted out of registering.
    */
   public static void showIfNecessary(Studio studio) {
      if (getHaveRegistered() || getShouldNeverRegister()) {
         return;
      }
      new RegistrationDialog(studio).setVisible(true);
   }

   public RegistrationDialog(final Studio studio) {
      super();

      setModal(true);
      setUndecorated(true);
      setTitle("Register Your Copy of \u00b5Manager");

      Font labelFont = new Font("Arial", Font.PLAIN, 12);

      JPanel contents = new JPanel(new MigLayout("flowx"));
      JLabel welcomeLabel = new JLabel(
            "<html>Welcome to \u00b5Manager!<br>" +
            "Please take a minute to let us know that you are using this " +
            "software.<br>We use this information to track how many people " +
            "are using \u00b5Manager<br>and to help support and fund " +
            "development of the program.<br>" +
            "Please <a href=\"\">see our privacy policy</a> for more " +
            "information.</html>");
      welcomeLabel.setBorder(BorderFactory.createLoweredBevelBorder());
      // HACK: convert to Color, because the ColorUIResource that
      // DaytimeNighttime returns here gets overridden as soon as setVisible()
      // is called, for unknown reasons.
      welcomeLabel.setBackground(new Color(
            DaytimeNighttime.getLightBackgroundColor().getRGB()));
      welcomeLabel.setFocusable(false);
      welcomeLabel.setFont(labelFont);
      welcomeLabel.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseReleased(MouseEvent e) {
            new Thread(GUIUtils.makeURLRunnable("https://www.iubenda.com/privacy-policy/661096")).start();
         }
      });
      contents.add(welcomeLabel, "alignx center, span, wrap");

      JLabel nameLabel = new JLabel("First Name");
      contents.add(nameLabel);
      final JTextField firstName = new JTextField(10);
      contents.add(firstName);

      nameLabel = new JLabel("Last Name");
      contents.add(nameLabel);
      final JTextField lastName = new JTextField(10);
      contents.add(lastName, "wrap");

      final JLabel orgLabel = new JLabel("Organization");
      contents.add(orgLabel);
      final JTextField org = new JTextField(10);
      contents.add(org, "wrap");

      final JLabel emailLabel = new JLabel("Email");
      contents.add(emailLabel);
      final JTextField email = new JTextField(10);
      contents.add(email);

      final JCheckBox optInEmail = new JCheckBox("Send me occasional emails");
      optInEmail.setSelected(true);
      contents.add(optInEmail, "spanx, wrap");

      JLabel subscriber = new JLabel(
               "<html>If you already have a subscription with " +
               "Open Imaging, you can provide<br>your system ID " +
               "and system nickname below. This will make it easier<br>" +
               "for us to support your system.</html>");
      subscriber.setBorder(BorderFactory.createLoweredBevelBorder());
      subscriber.setFont(labelFont);
      contents.add(subscriber, "alignx center, spanx, wrap");

      JLabel systemLabel = new JLabel("System ID");
      contents.add(systemLabel, "split 4, spanx");
      final JTextField system = new JTextField(10);
      contents.add(system);

      JLabel nicknameLabel = new JLabel("System Nickname");
      contents.add(nicknameLabel);
      final JTextField nickname = new JTextField(10);
      contents.add(nickname, "wrap");

      final JButton okButton = new JButton("OK");
      okButton.setFont(new Font("", Font.BOLD, 12));
      okButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ignored) {
            if (firstName.getText().equals("")) {
               JOptionPane.showMessageDialog(RegistrationDialog.this,
                  "Please provide your first name.");
               return;
            }
            if (lastName.getText().equals("")) {
               JOptionPane.showMessageDialog(RegistrationDialog.this,
                  "Please provide your last name.");
               return;
            }
            if (org.getText().equals("")) {
               JOptionPane.showMessageDialog(RegistrationDialog.this,
                  "Please provide your organization.");
               return;
            }
            if (email.getText().equals("") && optInEmail.isSelected()) {
               JOptionPane.showMessageDialog(RegistrationDialog.this,
                  "Please provide your email address.");
               return;
            }

            try {
               ServerComms.sendRequest("/register",
                     ServerComms.martialParams(
                        "first_name", firstName.getText(),
                        "last_name", lastName.getText(),
                        "organization", org.getText(),
                        "email", email.getText(),
                        "can_contact", Boolean.toString(optInEmail.isSelected()),
                        "system", system.getText(),
                        "system_nickname", nickname.getText()));
            }
            catch (IOException e) {
               studio.logs().logError(e.getMessage());
               // TODO: need a better way to determine that this field was
               // invalid.
               if (e.getMessage().equals("email")) {
                  studio.logs().showError("Sorry, that email address was not valid.");
               }
               else {
                  studio.logs().showError("There was an error communicating with the registration server. Please try again later.");
                  dispose();
               }
               return;
            }
            // save to profile
            setHaveRegistered(true);
            dispose();
         }
      });
      contents.add(okButton, "split 3, span, alignx center");

      final JButton skipButton = new JButton("Later");
      skipButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent arg0) {
            JOptionPane.showMessageDialog(RegistrationDialog.this, "You choose to postpone registration.\n" +
                  "This prompt will appear again next time you start the application.");
            setShouldNeverRegister(false);
            dispose();
         }
      });
      contents.add(skipButton);

      final JButton neverButton = new JButton("Never");
      neverButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent arg0) {
            JOptionPane.showMessageDialog(RegistrationDialog.this, "You have chosen never to register. \n" +
                  "If you change your mind in the future, please\nchoose the \"Register\" option in the Help menu.");
            setShouldNeverRegister(true);
            dispose();
         }
      });
      contents.add(neverButton);

      add(contents);
      pack();
      Dimension winSize = getSize();
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      setLocation(screenSize.width/2 - (winSize.width/2), screenSize.height/2 - (winSize.height/2));
   }

   public static boolean getHaveRegistered() {
      return DefaultUserProfile.getInstance().getBoolean(
            RegistrationDialog.class, HAVE_REGISTERED, false);
   }

   public static void setHaveRegistered(boolean haveRegistered) {
      DefaultUserProfile.getInstance().setBoolean(
            RegistrationDialog.class, HAVE_REGISTERED, haveRegistered);
   }

   public static boolean getShouldNeverRegister() {
      return DefaultUserProfile.getInstance().getBoolean(
            RegistrationDialog.class, SHOULD_NEVER_REGISTER, false);
   }

   public static void setShouldNeverRegister(boolean haveRegistered) {
      DefaultUserProfile.getInstance().setBoolean(
            RegistrationDialog.class, SHOULD_NEVER_REGISTER, haveRegistered);
   }
}
