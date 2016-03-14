///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//-----------------------------------------------------------------------------
//
// AUTHOR:       Chris Weisiger, 2016
//
// COPYRIGHT:    (c) 2016 Open Imaging Inc.
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

package com.openimaging.mmbrand;

import java.net.URL;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.micromanager.BrandPlugin;
import org.micromanager.Studio;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;


@Plugin(type=BrandPlugin.class)
public class MMBrandPlugin extends BrandPlugin implements SciJavaPlugin {
   private Studio studio_;

   @Override
   public void setContext(Studio studio) {
      studio_ = studio;
   }

   @Override
   public String getName() {
      return "Open Imaging branding plugin";
   }

   @Override
   public String getHelpText() {
      return "Provides Open Imaging logos and the Micro-Manager name.";
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
   public String getProgramName() {
      return "Micro-Manager";
   }

   @Override
   public JPanel getIntroDialogPanel(JComboBox profileSelect,
         JButton profileDelete, JComboBox configSelect, JButton configBrowse) {
      return new JPanel();
   }

   // TODO: add logo
   @Override
   public JPanel getMainWindowPanel() {
      return new JPanel();
   }

   // TODO: implement server-side upload script
   @Override
   public URL getProblemReportURL() {
      return null;
   }

   // TODO: implement server-side upload script
   @Override
   public URL getConfigFileURL() {
      return null;
   }

   // TODO: test for registration
   @Override
   public void afterLogin() {}
}
