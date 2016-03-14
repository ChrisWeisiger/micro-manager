///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     mmstudio
//-----------------------------------------------------------------------------
//AUTHOR:        Chris Weisiger, 2016
//COPYRIGHT:     (c) 2016 Open Imaging, Inc
//LICENSE:       This file is distributed under the BSD license.
//               License text is included with the source distribution.
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
package org.micromanager.internal.pluginmanagement;

import org.micromanager.BrandPlugin;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.micromanager.Studio;

/**
 * This class provides a default implementation of BrandPlugin for situations
 * where no real BrandPlugin is installed. It is intentionally bare-bones.
 */
public class DefaultBrandPlugin extends BrandPlugin {
   @Override
   public void setContext(Studio studio) {}

   @Override
   public String getName() {
      return "Default brand plugin";
   }

   @Override
   public String getHelpText() {
      return "Stand-in \"null\" branding plugin";
   }

   @Override
   public String getCopyright() {
      return "Copyright (c) 2016 Open Imaging Inc.";
   }

   @Override
   public String getVersion() {
      return "1.0";
   }

   @Override
   public JPanel getIntroDialogPanel(JComboBox profileSelect,
         JButton profileDelete, JComboBox configSelect, JButton configBrowse) {
      JPanel result = new JPanel(new MigLayout());

      result.add(new JLabel("Profile:"));
      result.add(profileSelect, "width 320!");
      result.add(profileDelete, "wrap");

      result.add(new JLabel("Config:"));
      result.add(configSelect, "width 320!");
      result.add(configBrowse, "wrap");

      return result;
   }
}
