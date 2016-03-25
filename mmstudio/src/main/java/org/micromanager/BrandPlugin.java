///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//-----------------------------------------------------------------------------
//
// AUTHOR:       Chris Weisiger, 2015
//
// COPYRIGHT:    University of California, San Francisco, 2015
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

package org.micromanager;

import java.net.URL;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

/**
 * BrandPlugins are used to assign branding information to the program. This
 * controls the appearance of certain dialogs, the name used in the UI
 * (the name "Micro-Manager" and its variants is trademarked), and a few other
 * minor behaviors.
 * Note that it is not allowed for more than one BrandPlugin to be installed
 * at a time.
 */
public abstract class BrandPlugin implements MMPlugin {
   /**
    * Provide the name of the "creator" of this instance of the program. This
    * may be inserted in some strings in the UI.
    */
   public String getOwnerName() {
      return "Generic Owner";
   }

   /**
    * Provide the contents of the intro dialog, except for the cancel/OK
    * buttons.
    * @param profileSelect A dropdown menu to select the user profile
    * @param profileDelete A button to delete the current profile
    * @param configSelect A dropdown menu to select the hardware config file
    * @param configBrowse A button to browse for a new config file
    */
   public abstract JPanel getIntroDialogPanel(JComboBox profileSelect,
         JButton profileDelete, JComboBox configSelect, JButton configBrowse);

   /**
    * Provide a JPanel that can be embedded into the main window of the
    * program. This should be small; it can't be more than about 300 pixels
    * wide without forcing the main window's width to increase.
    * @return a panel to embed into the main window.
    */
   public JPanel getMainWindowPanel() {
      return new JPanel();
   }

   /**
    * Provide a URL that Problem Reports may be uploaded to. If this method
    * returns null, then Problem Report uploading will be disabled.
    * @return URL to upload Problem Reports to. May be null.
    */
   public URL getProblemReportURL() {
      return null;
   }

   /**
    * Provide a URL that hardware config files may be uploaded to. If this
    * method returns null, then hardware config file uploading will be
    * disabled, and the user will not be prompted to upload their config files
    * after completing the Hardware Config Wizard.
    * @return URL to upload hardware config files to. May be null.
    */
   public URL getConfigFileURL() {
      return null;
   }

   /**
    * This callback is invoked as soon as possible after plugins are finished
    * loading, allowing the plugin to perform actions that should occur
    * prior to the intro dialog being displayed.
    */
   public void beforeLogin() {}

   /**
    * This callback is invoked after the user logs in, allowing the plugin to
    * perform whatever special actions it feels are appropriate.
    */
   public void afterLogin() {}

   /**
    * Provide a list of JMenuItems to insert into the Help menu. May be null.
    * @return list of Help menu items, or null.
    */
   public Collection<JMenuItem> getHelpMenuItems() {
      return null;
   }
}
