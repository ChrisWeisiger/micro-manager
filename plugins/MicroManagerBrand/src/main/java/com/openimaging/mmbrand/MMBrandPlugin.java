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

import com.bulenkov.iconloader.IconLoader;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

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
   public JPanel getIntroDialogPanel(JComboBox profileSelect,
         JButton profileDelete, JComboBox configSelect, JButton configBrowse) {
      return new IntroPanel(studio_, profileSelect, profileDelete,
            configSelect, configBrowse);
   }

   // TODO: add logo
   @Override
   public JPanel getMainWindowPanel() {
      JPanel result = new JPanel(new MigLayout("insets 0"));
      result.add(new JLabel(IconLoader.getIcon(
                  "/com/openimaging/brand/oi_logo_small.png")));
      return result;
   }

   // TODO: implement server-side upload script
   @Override
   public URL getProblemReportURL() {
      try {
         return new URL("http://127.0.0.1:8000/uploads/problem_reports");
      }
      catch (MalformedURLException e) {
         studio_.logs().logError(e, "Problem report URL is invalid");
         return null;
      }
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
