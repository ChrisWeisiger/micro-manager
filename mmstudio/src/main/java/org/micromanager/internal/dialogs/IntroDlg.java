///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     mmstudio
//-----------------------------------------------------------------------------
//
// AUTHOR:       Nenad Amodaj, nenad@amodaj.com, Dec 1, 2005
//
// COPYRIGHT:    University of California, San Francisco, 2006
// COPYRIGHT:    University of California, San Francisco, 2006
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
// CVS:          $Id$

package org.micromanager.internal.dialogs;

import ij.IJ;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.border.EtchedBorder;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;

import org.micromanager.BrandPlugin;
import org.micromanager.Studio;

import org.micromanager.internal.MMStudio;
import org.micromanager.internal.utils.DefaultUserProfile;
import org.micromanager.internal.utils.FileDialogs;
import org.micromanager.internal.utils.GUIUtils;
import org.micromanager.internal.utils.JavaUtils;
import org.micromanager.internal.utils.ReportingUtils;

/**
 * Splash screen and introduction dialog. 
 * Opens up at startup and allows selection of the configuration file.
 */
public class IntroDlg extends JDialog {
   private static final long serialVersionUID = 1L;
   private static final String USERNAME_NEW = "Create new profile";
   private static final String RECENTLY_USED_CONFIGS = "recently-used config files";
   private static final String GLOBAL_CONFIGS = "config files supplied from a central authority";
   private static final String SHOULD_ASK_FOR_CONFIG = "whether or not the intro dialog should include a prompt for the config file";
   private static final String DEMO_CONFIG_FILE_NAME = "MMConfig_demo.cfg";

   private static final int MAX_RECENT_CONFIGS = 6;

   private Studio studio_;
   private BrandPlugin plugin_ = null;

   private boolean okFlag_ = true;

   private JComboBox configSelect_;
   private JButton fileBrowseButton_;
   private JComboBox profileSelect_;
   private JButton profileDeleteButton_;

   public IntroDlg(Studio studio, String sysConfigFile, String versionStr) {
      super();
      studio_ = studio;

      createProfileControls();
      createConfigFileControls();

      // Select a plugin to use to customize the dialog.
      plugin_ = studio_.plugins().getBrandPlugin();

      setLayout(new MigLayout("insets 0"));

      Font textFont = new Font("Arial", Font.PLAIN, 10);
      setFont(textFont);
      setTitle("Micro-Manager Startup");
      setName("Intro");
      setResizable(false);
      setModal(true);
      setUndecorated(true);
      if (!IJ.isMacOSX()) {
        ((JPanel) getContentPane()).setBorder(BorderFactory.createLineBorder(Color.GRAY));
      }

      add(plugin_.getIntroDialogPanel(profileSelect_, profileDeleteButton_,
            configSelect_, fileBrowseButton_), "spanx, wrap");

      final JButton licenseButton = new JButton("License Info");
      licenseButton.setFont(textFont);
      licenseButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            LicenseDialog.show(IntroDlg.this);
         }
      });
      add(licenseButton, "gapright push");

      final JButton okButton = new JButton("OK");
      okButton.setFont(textFont);
      okButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            okFlag_ = true;
            setVisible(false);
         }
      });
      getRootPane().setDefaultButton(okButton);
      okButton.requestFocusInWindow();

      final JButton cancelButton = new JButton("Cancel");
      cancelButton.setFont(textFont);
      cancelButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            okFlag_ = false;
            setVisible(false);
         }
      });
      if (JavaUtils.isMac()) {
         add(cancelButton, "split 2, align right, flowx");
         add(okButton);
      }
      else {
         add(okButton, "split 2, align right, flowx");
         add(cancelButton);
      }

      pack();

      Dimension winSize = getPreferredSize();
      GraphicsConfiguration config = GUIUtils.getGraphicsConfigurationContaining(0, 0);
      Rectangle bounds = config.getBounds();
      setLocation(bounds.x + bounds.width / 2 - winSize.width / 2,
            bounds.y + bounds.height / 2 - winSize.height / 2);
      setConfigFile(sysConfigFile);
      toFront();
      setVisible(true);
   }

   /**
    * Create profileSelect_ and profileDeleteButton_.
    */
   private void createProfileControls() {
      final DefaultUserProfile profile = DefaultUserProfile.getInstance();
      Set<String> profiles = profile.getProfileNames();
      final ArrayList<String> profilesAsList = new ArrayList<String>(profiles);
      // HACK: put the "new" and "default" options first in the list.
      profilesAsList.remove(DefaultUserProfile.DEFAULT_USER);
      profilesAsList.add(0, DefaultUserProfile.DEFAULT_USER);
      profilesAsList.add(0, USERNAME_NEW);
      profileSelect_ = new JComboBox();
      profileSelect_.setToolTipText("The profile contains saved settings like window positions and acquisition parameters.");
      for (String profileName : profilesAsList) {
         profileSelect_.addItem(profileName);
      }
      profileSelect_.setSelectedItem(DefaultUserProfile.DEFAULT_USER);

      profileSelect_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            String profileName = (String) profileSelect_.getSelectedItem();
            if (profileName.contentEquals(USERNAME_NEW)) {
               profileSelect_.hidePopup();
               // Prompt the user for the new profile name.
               profileName = JOptionPane.showInputDialog("Please input the new profile name:");
               if (profileName == null || profileName.equals("")) {
                  // User declined to provide a name; do nothing.
                  profileSelect_.setSelectedItem(
                     DefaultUserProfile.DEFAULT_USER);
                  return;
               }
               else if (profile.hasUser(profileName)) {
                  ReportingUtils.showError("That profile name is already in use.");
               }
               else {
                  profilesAsList.add(profileName);
                  profileSelect_.addItem(profileName);
                  profile.addProfile(profileName);
               }
               // TODO: will this re-invoke our listener, causing us to call
               // setConfigFile twice?
               profileSelect_.setSelectedItem(profileName);
            }
            // Set the current active profile.
            profile.setCurrentProfile(profileName);
            // Enable/disable the "delete profile" button.
            profileDeleteButton_.setEnabled(!(profileName.equals(USERNAME_NEW) ||
                     profileName.equals(DefaultUserProfile.DEFAULT_USER)));
            // Update the list of hardware config files.
            setConfigFile(null);
         }
      });

      profileDeleteButton_ = new JButton("Delete");
      profileDeleteButton_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            String curName = (String) profileSelect_.getSelectedItem();
            if (JOptionPane.showConfirmDialog(null,
                  "Are you sure you want to delete the \"" + curName +
                  "\" profile?", "Confirm Profile Deletion",
                  JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
               // User backed out.
               return;
            }
            DefaultUserProfile.getInstance().deleteProfile(curName);
            profileSelect_.removeItem(curName);
            profileSelect_.setSelectedItem(DefaultUserProfile.DEFAULT_USER);
         }
      });
      profileDeleteButton_.setEnabled(false);
   }

   /**
    * Create configSelect_ and fileBrowseButton_
    */
   private void createConfigFileControls() {
      configSelect_ = new JComboBox();
      configSelect_.setEnabled(getShouldAskForConfigFile());
      fileBrowseButton_ = new JButton();
      fileBrowseButton_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            loadConfigFile();
         }
      });
      fileBrowseButton_.setText("...");
   }

   public boolean okChosen() {
      return okFlag_;
   }

   // Add a new config file to the dropdown menu.
   public void setConfigFile(String path) {
      configSelect_.removeAllItems();
      DefaultUserProfile profile = DefaultUserProfile.getInstance();
      ArrayList<String> configs = new ArrayList<String>(
            Arrays.asList(getRecentlyUsedConfigs()));
      Boolean doesExist = false;
      if (path != null) {
         doesExist = new File(path).exists();
      }
      if (doesExist) {
         addRecentlyUsedConfig(path);
         configs = new ArrayList<String>(
               Arrays.asList(getRecentlyUsedConfigs()));
      }

      // Add on global default configs.
      configs.addAll(Arrays.asList(profile.getStringArray(IntroDlg.class,
              GLOBAL_CONFIGS,
              new String[] {new File(DEMO_CONFIG_FILE_NAME).getAbsolutePath()})));
      // Remove duplicates, and then sort alphabetically.
      configs = new ArrayList<String>(new HashSet<String>(configs));
      Collections.sort(configs);
      for (String config : configs) {
         configSelect_.addItem(config);
      }
      configSelect_.addItem("(none)");

      if (doesExist) {
         configSelect_.setSelectedItem(path);
      }
      else {
         String[] recentlyUsed = getRecentlyUsedConfigs();
         configSelect_.setSelectedItem(recentlyUsed[recentlyUsed.length - 1]);
      }
   }

   public String getConfigFile() {
       String tvalue = configSelect_.getSelectedItem().toString();
       String nvalue = "(none)";
       if( nvalue.equals(tvalue))
           tvalue = "";

      return tvalue;
   }

   // User wants to use a file browser to select a hardware config file.
   protected void loadConfigFile() {
      File f = FileDialogs.openFile(this, "Choose a config file", FileDialogs.MM_CONFIG_FILE);
      if (f != null) {
         setConfigFile(f.getAbsolutePath());
      }
   }

   public static boolean getShouldAskForConfigFile() {
      return DefaultUserProfile.getInstance().getBoolean(IntroDlg.class,
            SHOULD_ASK_FOR_CONFIG, true);
   }

   public static void setShouldAskForConfigFile(boolean shouldAsk) {
      DefaultUserProfile.getInstance().setBoolean(IntroDlg.class,
            SHOULD_ASK_FOR_CONFIG, shouldAsk);
   }

   /**
    * Return the array of recently-used config files for the current user.
    * They are sorted based on how recently they were used, from oldest to
    * newest. As a special case, if we are unable to find any config files
    * and we're on the "default user", then we look in the Java Preferences
    * to see if we can import old config files used by a 1.4 version of
    * Micro-Manager.
    */
   public static String[] getRecentlyUsedConfigs() {
      // If there are no recently-used configs, supply the demo file.
      String[] result = DefaultUserProfile.getInstance().getStringArray(
            IntroDlg.class, RECENTLY_USED_CONFIGS, null);
      if (result == null) {
         if (DefaultUserProfile.getInstance().getIsDefaultUser()) {
            ReportingUtils.logDebugMessage("Attempting to load recently-used config files from 1.4 Preferences");
            result = loadRecentlyUsedConfigsFromPreferences();
         }
         if (result == null) {
            // No good; just use the demo config.
            result = new String[] {new File(DEMO_CONFIG_FILE_NAME).getAbsolutePath()};
         }
      }
      return result;
   }

   /**
    * Look up recently-used configs in the Preferences. This is a little
    * aggravating as we must use specifically org.micromanager.MMStudio as
    * the class for the Preferences, and that class no longer exists.
    * Consequently we are required to manually traverse the Preferences
    * tree.
    */
   private static String[] loadRecentlyUsedConfigsFromPreferences() {
      Preferences root = DefaultUserProfile.getLegacyUserPreferences14();
      if (root == null) {
         return null;
      }
      HashSet<String> keys;
      try {
         keys = new HashSet<String>(Arrays.asList(root.keys()));
      }
      catch (BackingStoreException e) {
         ReportingUtils.logError(e, "Error accessing old user preferences");
         return null;
      }
      // The actual config file names are stored with procedurally-generated
      // keys, as Preferences is unable to store String arrays.
      ArrayList<String> result = new ArrayList<String>();
      for (Integer i = 0; i < 5; ++i) { // 5 is old hardcoded max
         String key = "CFGFileEntry" + i.toString();
         if (keys.contains(key)) {
            result.add(root.get(key, ""));
         }
      }
      // 1.4 stored configs in the opposite order from how we store them now.
      Collections.reverse(result);
      if (result.size() > 0) {
         return result.toArray(new String[] {});
      }
      return null;
   }

   /**
    * This is a convenience method to save callers from having to know that
    * configs are ordered from oldest to newest.
    */
   public static String getMostRecentlyUsedConfig() {
      String[] configs = getRecentlyUsedConfigs();
      return configs[configs.length - 1];
   }

   // Add the provided config file onto the list of recently-used configs.
   // We impose a cap of 6 on this list. The last element of the array is
   // the most-recently used config file (and the first is the oldest).
   public static void addRecentlyUsedConfig(String config) {
      ArrayList<String> configs = new ArrayList<String>(
            Arrays.asList(getRecentlyUsedConfigs()));
      if (!configs.contains(config)) {
         configs.add(config);
      }
      else {
         // Move it to the end of the array.
         while (configs.contains(config)) {
            configs.remove(config);
         }
         configs.add(config);
      }
      if (configs.size() > MAX_RECENT_CONFIGS) {
         configs.remove(configs.get(0));
      }
      DefaultUserProfile.getInstance().setStringArray(IntroDlg.class,
            RECENTLY_USED_CONFIGS, configs.toArray(new String[0]));
   }
}
