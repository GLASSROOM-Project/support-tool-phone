package de.glassroom.gst;

import android.util.Log;

import com.github.jknack.handlebars.Template;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import de.glassroom.gpe.Filter;
import de.glassroom.gpe.Guide;
import de.glassroom.gpe.annotations.MetadataAnnotation;
import de.glassroom.gst.wf.Command;
import de.glassroom.gst.wf.Slide;
import de.glassroom.gst.wf.VoiceCommand;
import de.glassroom.gst.wf.Workflow;

public class DefaultSlideHandler implements SlideHandler {
    private final Template template;
    private Workflow workflow;
    private Slide slide;
    private MainActivity activity;

    public DefaultSlideHandler(Template template) {
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

        TextRecognitionHandler textRecognitionHandler;

        switch (slide.getId()) {
            case "search":
                session.setFilter(null);
                session.setGuideSelection(null);
                textRecognitionHandler = new TextRecognitionHandler() {
                    @Override
                    public void textRecognized(final String text) {
                        Log.i("DefaultSlideHandler", "Recognized filter text: " + text);
                        Filter<Guide> guideFilter = new Filter<Guide>() {
                            @Override
                            public boolean accept(Guide guide) {
                                if (guide.getNodes().size() <= 2) {
                                    // drop empty guides
                                    return false;
                                }
                                StringBuilder guideTexts = new StringBuilder();
                                MetadataAnnotation metadata = guide.getMetadata();
                                if (metadata != null) {
                                    for (String title : metadata.getTitles().values()) {
                                        guideTexts.append(title.toLowerCase()).append("|");
                                    }
                                    for (String description : metadata.getDescriptions().values()) {
                                        guideTexts.append(description.toLowerCase()).append("|");
                                    }
                                }
                                return guideTexts.toString().indexOf(text.toLowerCase()) >= 0;
                            }

                            @Override
                            public String toString() {
                                return text;
                            }
                        };
                        List<Guide> relevantGuides = session.getGuideManager().getGuides(guideFilter, null);
                        if (relevantGuides.isEmpty()) {
                            Log.i("DefaultSlideHandler", "No match for filter.");
                            openSlide(session.getActiveSlide().getId());
                        } else {
                            Log.i("DefaultSlideHandler", "Found " + relevantGuides.size() + " matches for filter.");
                            session.setFilter(guideFilter);
                            openSlide("filtered-catalog");
                        }
                    }

                    @Override
                    public void onError() {
                        Log.i("DefaultSlideHandler", "Failed to recognize text for filtering guides.");
                        openSlide(session.getActiveSlide().getId());
                    }
                };
                break;
            default:
                textRecognitionHandler = null;
        }

        List<VoiceCommand> voiceCommands = slide.getVoiceCommands();
        Guide guide = session.getActiveGuide();
        if (guide != null) {
            data.put("guide", WorkflowHandler.extractGuideData(guide));
        }

        try {
            String html = template.apply(data);
            activity.getWebView().loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);
            if (!voiceCommands.isEmpty()) {
                activity.prepareSpeechRecognition(voiceCommands, textRecognitionHandler);
                activity.startSpeechRecognition();
            } else {
                Log.w("WorkflowHandler", "No command found to continue workflow.");
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
                case "openGuide":
                    session.setFilter(null);
                    session.setStepSelection(null);
                    session.setWarningSelection(null);
                    session.setHintSelection(null);
                    break;
                case "resetGuide":
                    session.setGuideSelection(null);
                    break;
            }
        }
        final String target = command.getTarget();
        if (target != null) {
            openSlide(target);
        }
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
