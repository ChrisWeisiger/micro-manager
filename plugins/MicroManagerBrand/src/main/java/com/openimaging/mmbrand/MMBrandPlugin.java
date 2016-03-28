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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.Font;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.micromanager.BrandPlugin;
import org.micromanager.Studio;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

import org.micromanager.internal.ServerComms;
import org.micromanager.internal.utils.GUIUtils;

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
   public String getOwnerName() {
      return "Open Imaging";
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
      JPanel result = new JPanel(new MigLayout("fill, insets 0"));
      JLabel link;
      if (ServerComms.isEnabled()) {
         link = new JLabel("<html>Thank you for supporting \u00b5Manager development!</html>");
      }
      else {
         link = new JLabel("<html><a href=\"\">Get Support</a> for \u00b5Manager!</html>");
         link.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
               new Thread(GUIUtils.makeURLRunnable("https://open-imaging.com/services")).start();
            }
         });
      }
      link.setFont(new Font("Arial", Font.PLAIN, 10));
      result.add(link);
      return result;
   }

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

   @Override
   public URL getConfigFileURL() {
      try {
         return new URL("http://127.0.0.1:8000/uploads/config_files");
      }
      catch (MalformedURLException e) {
         studio_.logs().logError(e, "Config file URL is invalid");
         return null;
      }
   }

   @Override
   public void beforeLogin() {}

   @Override
   public void afterLogin() {
      RegistrationDialog.showIfNecessary(studio_);
   }

   @Override
   public List<JMenuItem> getHelpMenuItems() {
      ArrayList<JMenuItem> result = new ArrayList<JMenuItem>();
      JMenuItem reg = new JMenuItem("Register \u00b5Manager");
      reg.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            new RegistrationDialog(studio_).setVisible(true);
         }
      });
      result.add(reg);
      return result;
   }
}
