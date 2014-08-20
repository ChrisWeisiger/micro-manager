package org.micromanager.data.test;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.awt.Panel;

import org.micromanager.api.data.Datastore;
import org.micromanager.api.data.NewImageEvent;

import org.micromanager.imagedisplay.DisplayWindow;
import org.micromanager.imagedisplay.HyperstackControls;
import org.micromanager.imagedisplay.MMCompositeImage;
import org.micromanager.imagedisplay.MMImagePlus;

import org.micromanager.utils.ReportingUtils;

public class TestDisplay {
   private Datastore store_;
   private DisplayWindow window_;
   private MMCompositeImage ijImage_;
   private MMImagePlus plus_;
   private MMVirtualStack stack_;
   private HyperstackControls controls_;
   private HistogramsPanel histograms_;

   private EventBus bus_;
   
   public TestDisplay(Datastore store) {
      store_ = store;
      store_.registerForEvents(this);
      bus_ = new EventBus();
      stack_ = new MMVirtualStack(store);
      plus_ = new MMImagePlus("foo", stack_, bus_);
      stack_.setImagePlus(plus_);
      ijImage_ = new MMCompositeImage(plus_, 1, "foo", bus_);
      ijImage_.updateImage();
//      controls_ = new HyperstackControls(bus_, false, false);
      // TODO: For now, stuffing the histograms into the display window. The 
      // display window takes a Panel but the histograms are a JPanel, hence
      // the wrapper.
      Panel temp = new Panel();
      HistogramsPanel histograms_ = new HistogramsPanel(store_, ijImage_, bus_);
      temp.add(histograms_);
      histograms_.setVisible(true);
      temp.validate();
      window_ = new DisplayWindow(ijImage_, temp, bus_);
   }
   
   /**
    * Datastore has received a new image.
    */
   @Subscribe
   public void onNewImage(NewImageEvent event) {
      ReportingUtils.logError("Display caught new image");
      try {
         plus_.getProcessor().setPixels(event.getImage().getRawPixels());
         ijImage_.getProcessor().setPixels(event.getImage().getRawPixels());

         ijImage_.reset();
         ijImage_.updateAndDraw();
      }
      catch (Exception e) {
         ReportingUtils.logError(e, "Couldn't set pixels");
      }
   }

   /**
    * Something on our display bus (i.e. not the Datastore bus) wants us to
    * redisplay.
    */
   @Subscribe
   public void onDrawEvent(DrawEvent event) {
      ijImage_.updateImage();
      ijImage_.drawWithoutUpdate();
      histograms_.calcAndDisplayHistAndStats(true);
   }
}