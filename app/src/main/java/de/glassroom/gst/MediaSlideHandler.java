package de.glassroom.gst;

import android.app.Activity;
import android.util.Log;

import com.github.jknack.handlebars.Template;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.glassroom.gpe.Guide;
import de.glassroom.gpe.Step;
import de.glassroom.gpe.content.ContentDescriptor;
import de.glassroom.gst.wf.Command;
import de.glassroom.gst.wf.Slide;
import de.glassroom.gst.wf.VoiceCommand;
import de.glassroom.gst.wf.Workflow;

public class MediaSlideHandler implements SlideHandler {
    private final Template template;
    private Workflow workflow;
    private Slide slide;
    private MainActivity activity;

    public MediaSlideHandler(Template template) {
        this.template = template;
    }

    @Override
    public void initialize(MainActivity activity, Workflow workflow, Slide slide) {
        this.workflow = workflow;
        this.slide = slide;
        this.activity = activity;
    }

    @Override
    public void renderSlide(Session session) {
        Map<String, Object> data = slide.extractData();

        Step step = session.getStepSelection().getCurrentSelection();
        data.put("step", WorkflowHandler.extractStepData(step));
        ContentDescriptor content = session.getActiveContentDescriptor();
        Map<String, Object> contentData = WorkflowHandler.extractContentData(content);
        if (content.getMediaPath() != null) {
            String fullMediaPath = PersistenceHandler.getURLForMediaFile(
                    step.getParentGuide().getId(),
                    content.getId(),
                    content.getMediaPath());
            contentData.put("fullMediaPath", fullMediaPath);
        }
        data.put("content", contentData);

        List<VoiceCommand> voiceCommands = slide.getVoiceCommands();
        Guide guide = session.getActiveGuide();
        if (guide != null) {
            data.put("guide", WorkflowHandler.extractGuideData(guide));
        }

        try {
            String html = template.apply(data);
            activity.getWebView().loadUrl("javascript:document.open();document.close();");
            activity.getWebView().loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);
            activity.prepareSpeechRecognition(voiceCommands, null);
            switch (content.getMimeType()) {
                case "video/mp4":
                    Timer t = new Timer();
                    t.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.getWebView().loadUrl("javascript:GST.playMedia();");
                                }
                            });
                        }
                    }, 500);
                    break;
                default:
                    activity.startSpeechRecognition();
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
                case "play":
                    // A reload will auto-play the video.
                    Runnable forwardRunnable = new Runnable() {
                        @Override
                        public void run() {
                            workflow.setActiveSlide(slide.getId());
                        }
                    };
                    activity.runOnUiThread(forwardRunnable);
                    break;
            }
        }
        final String target = command.getTarget();
        if (target != null) {
            openSlide(target);
        }
    }

    private void openSlide(final String slideId) {
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        workflow.setActiveSlide(slideId);
                    }
                });
            }
        }, 500);
    }
}
