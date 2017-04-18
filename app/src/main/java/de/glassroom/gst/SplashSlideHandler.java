package de.glassroom.gst;

import android.app.Activity;
import android.util.Log;

import com.github.jknack.handlebars.Template;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.glassroom.gpe.Filter;
import de.glassroom.gpe.Guide;
import de.glassroom.gpe.annotations.MetadataAnnotation;
import de.glassroom.gst.MainActivity;
import de.glassroom.gst.Session;
import de.glassroom.gst.SlideHandler;
import de.glassroom.gst.TextRecognitionHandler;
import de.glassroom.gst.WorkflowHandler;
import de.glassroom.gst.wf.Command;
import de.glassroom.gst.wf.Slide;
import de.glassroom.gst.wf.Workflow;

public class SplashSlideHandler implements SlideHandler {
    private final Template template;
    private final WorkflowHandler workflowHandler;
    private Workflow workflow;
    private Slide slide;
    private MainActivity activity;

    public SplashSlideHandler(WorkflowHandler workflowHandler, Template template) {
        this.workflowHandler = workflowHandler;
        this.template = template;
    }

    @Override
    public void initialize(MainActivity activity, Workflow workflow, Slide slide) {
        this.workflow = workflow;
        this.slide = slide;
        this.activity = activity;
    }

    @Override
    public void renderSlide(final Session session) {
        Map<String, Object> data = slide.extractData();
        Slide.Forwarding forwarding = slide.getForwarding();
        Slide.PerformAction performAction = slide.getPerformAction();

        Guide guide = session.getActiveGuide();
        if (guide != null) {
            data.put("guide", WorkflowHandler.extractGuideData(guide));
        }

        try {
            String html = template.apply(data);
            activity.getWebView().loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);
            if (forwarding != null) {
                forward(activity, forwarding.getDelay(), forwarding.getTarget());
            } else if (performAction != null){
                performAction(activity, performAction.getDelay(), performAction.getAction());
            }
        } catch (IOException e) {
            Log.e("WorkflowHandler", "Failed to apply template: " + slide.getTemplatePath());
        } catch (IllegalStateException e) {
            Log.w("WorkflowHandler", "Failed to initialize voice commands: " + e.getMessage());
        }
    }

    @Override
    public void execute(Command command, Session session) {
        String action = command.getAction();
        if (action != null) {
            // Common actions.
            switch (action) {
                case "help":
                    openSlide("help");
                    break;
                case "previous":
                    openSlide(session.getPreviousSlide().getId());
                    break;
            }
        }
        final String target = command.getTarget();
        if (target != null) {
            openSlide(target);
        }
    }

    private void forward(final Activity activity, int delayInSeconds, final String target) {
        final Runnable forwardRunnable = new Runnable() {
            @Override
            public void run() {
                workflow.setActiveSlide(target);
            }
        };
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                activity.runOnUiThread(forwardRunnable);
            }
        }, delayInSeconds * 1000);
    }

    private void performAction(final MainActivity activity, int delayInSeconds, final String action) {
        final Runnable forwardRunnable = new Runnable() {
            @Override
            public void run() {
                switch (action) {
                    case "init":
                        workflowHandler.initializeTemplates();
                        openSlide("catalog");
                        break;
                    case "exit":
                        activity.finish();
                        // activity.onDestroy();
                        break;
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                activity.runOnUiThread(forwardRunnable);
            }
        }, delayInSeconds * 1000);
    }

    private void openSlide(final String slideId) {
        final Runnable forwardRunnable = new Runnable() {
            @Override
            public void run() {
                workflow.setActiveSlide(slideId);
            }
        };
        activity.runOnUiThread(forwardRunnable);
    }
}
