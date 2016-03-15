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

import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.micromanager.Studio;


public class IntroPanel extends JPanel {
   private Studio studio_;

   public IntroPanel(Studio studio, JComboBox profileSelect,
         JButton profileDelete, JComboBox configSelect, JButton configBrowse) {
      super(new MigLayout("insets 0"));
      studio_ = studio;
      JLabel ourIcon = new JLabel(
            IconLoader.getIcon("/com/openimaging/brand/splash_logo.png"));
      add(ourIcon, "span, wrap");
      add(new JLabel(IconLoader.getIcon(
                  "/com/openimaging/brand/sponsor_logos.png")), "span, wrap");

      Font stdFont = new Font("Arial", Font.PLAIN, 10);

      JLabel profileLabel = new JLabel("User Profile:");
      profileLabel.setFont(stdFont);
      add(profileLabel, "split, spanx");
      profileSelect.setFont(stdFont);
      add(profileSelect, "width 150!");
      profileDelete.setFont(stdFont);
      add(profileDelete);

      JLabel fileLabel = new JLabel("Configuration file:");
      fileLabel.setFont(stdFont);
      add(fileLabel, "gapleft push");
      configSelect.setFont(stdFont);
      add(configSelect, "width 250!");
      configBrowse.setFont(stdFont);
      add(configBrowse, "wrap");
   }
}
