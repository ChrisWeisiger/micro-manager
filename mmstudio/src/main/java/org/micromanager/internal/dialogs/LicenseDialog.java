///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     mmstudio
//-----------------------------------------------------------------------------
//
// AUTHOR:       Chris Weisiger, 2016
//
// COPYRIGHT:    (c) 2016 Open Imaging, Inc.
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

package org.micromanager.internal.dialogs;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.border.EtchedBorder;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.micromanager.internal.utils.ReportingUtils;

/**
 * This dialog shows licensing information for the program and all its
 * dependencies.
 */
public class LicenseDialog extends JDialog {
   public static void show(Window parent) {
      new LicenseDialog(parent);
   }

   private static final String HOWTO =
"This dialog shows license and copyright information for \u00b5Manager and " +
"various libraries and other components that it relies on. Click on a name " +
"to see its information.";

   private LicenseDialog(Window parent) {
      super(parent, "License Information", Dialog.ModalityType.MODELESS);
      setLayout(new MigLayout());
      JPanel contents = new JPanel(new MigLayout("fill", "[grow]"));
      Font textFont = new Font("Arial", Font.PLAIN, 10);
      JTextArea howto = new JTextArea(HOWTO);
      howto.setEditable(false);
      howto.setFocusable(false);
      howto.setLineWrap(true);
      howto.setWrapStyleWord(true);
      howto.setFont(textFont);
      howto.setBorder(new EtchedBorder());
      contents.add(howto, "growx, span, wrap");

      contents.add(createLicenseBlock(), "growx, span, wrap");

      JButton okButton = new JButton("OK");
      okButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            dispose();
         }
      });
      getRootPane().setDefaultButton(okButton);
      contents.add(okButton, "span, alignx right, wrap");

      add(contents, "width 600::, height 300::600");
      pack();
      setLocationRelativeTo(parent);
      setVisible(true);
   }

   private JPanel createLicenseBlock() {
      JPanel result = new JPanel(new MigLayout("insets 0, fill"));
      // Table of contents; contains clickable links to show the license for
      // the corresponding item.
      JPanel toc = new JPanel(new MigLayout("flowy"));
      JScrollPane tocScroller = new JScrollPane(toc);
      final JEditorPane licenseInfo = new JEditorPane();
      licenseInfo.setEditable(false);
      licenseInfo.setFocusable(false);
      final JScrollPane licenseScroller = new JScrollPane(licenseInfo);
      // This method of scanning the jar's contents adapted from
      // http://stackoverflow.com/questions/11012819/how-can-i-get-a-resource-folder-from-inside-my-jar-file
      try {
         File file = new File(
                  getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
         final JarFile jar = new JarFile(file);
         final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
         while(entries.hasMoreElements()) {
            final String name = entries.nextElement().getName();
            if (name.startsWith("org/micromanager/licenses") &&
                  name.endsWith(".license")) {
               String projectName = name.replace(
                     "org/micromanager/licenses/", "").replace(
                     ".license", "");
               // Without the href, we don't get underline/coloration.
               JLabel linkLabel = new JLabel("<html><a href=\"#\">" + projectName + "</a></html>");
               linkLabel.addMouseListener(new MouseAdapter() {
                  @Override
                  public void mousePressed(MouseEvent e) {
                     licenseInfo.setText(loadText("/" + name));
                     SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                           licenseScroller.getVerticalScrollBar().setValue(0);
                        }
                     });
                  }
               });
               toc.add(linkLabel);
            }
         }
         jar.close();
      }
      catch (IOException e) {
         ReportingUtils.logError(e, "Unable to get license information");
         licenseInfo.setText("Unable to load license information");
      }
      result.add(tocScroller, "growx, span, wrap, height 150!");
      result.add(licenseScroller, "growx, span, wrap, height 300:");
      return result;
   }

   /**
    * Load a text file from within the jar.
    */
   private String loadText(String path) {
      InputStream in = getClass().getResourceAsStream(path);
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      StringBuffer buf = new StringBuffer();
      while (true) {
         try {
            String line = reader.readLine();
            if (line == null) {
               break;
            }
            buf.append(line + "\n");
         }
         catch (IOException e) {
            ReportingUtils.logError(e, "Unable to read text at " + path);
            return "";
         }
      }
      return buf.toString();
   }
}
