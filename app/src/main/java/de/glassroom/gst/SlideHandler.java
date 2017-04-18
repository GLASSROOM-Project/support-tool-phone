package de.glassroom.gst;

import android.webkit.WebView;

import de.glassroom.gst.wf.Command;
import de.glassroom.gst.wf.Slide;
import de.glassroom.gst.wf.Workflow;

public interface SlideHandler {
    public void initialize(MainActivity activity, Workflow workflow, Slide slide);
    public void renderSlide(Session session);
    public void execute(Command command, Session session);
}
