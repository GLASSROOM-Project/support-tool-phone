package de.glassroom.gst;

import android.util.Log;

import com.github.jknack.handlebars.Template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import de.glassroom.gpe.Filter;
import de.glassroom.gpe.Guide;
import de.glassroom.gpe.Step;
import de.glassroom.gpe.content.ContentDescriptor;
import de.glassroom.gpe.content.Hint;
import de.glassroom.gpe.content.Warning;
import de.glassroom.gst.wf.Command;
import de.glassroom.gst.wf.Slide;
import de.glassroom.gst.wf.VoiceCommand;
import de.glassroom.gst.wf.Workflow;

public class SelectionSlideHandler implements SlideHandler {
    private final Template template;
    private Workflow workflow;
    private Slide slide;
    private MainActivity activity;
    private String fullMediaPath;

    public SelectionSlideHandler(Template template) {
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
        List<VoiceCommand> voiceCommands = slide.getVoiceCommands();


        switch (slide.getId()) {
            case "filtered-catalog":
                Filter<Guide> filter = session.getFilter();
                if (filter != null) {
                    data.put("filter", filter.toString());
                }
            case "catalog":
                Selection<Guide> guideSelection = session.getGuideSelection();
                if (guideSelection == null) {
                    guideSelection = initializeGuideSelection(session, session.getFilter());
                    if (guideSelection == null) {
                        openSlide("no-guides");
                        return;
                    }
                    session.setGuideSelection(guideSelection);
                }
                Guide selectedGuide = guideSelection.getCurrentSelection();
                data.put("guide", WorkflowHandler.extractGuideData(selectedGuide));
                data.put("isFirst", guideSelection.isFirst());
                data.put("isLast", guideSelection.isLast());
                data.put("entryNo", guideSelection.getCursor() + 1);
                data.put("entryTotal", guideSelection.size());
                break;
            case "guide-step":
                Selection<Step> stepSelection = session.getStepSelection();
                if (stepSelection == null) {
                    stepSelection = initializeStepSelection(session);
                    session.setStepSelection(stepSelection);
                }
                Step selectedStep = stepSelection.getCurrentSelection();
                data.put("step", WorkflowHandler.extractStepData(selectedStep));
                ContentDescriptor content = session.getActiveContentDescriptor();
                if (content != null) {
                    Map<String, Object> contentData = WorkflowHandler.extractContentData(content);
                    if (content.getMediaPath() != null) {
                        fullMediaPath = PersistenceHandler.getURLForMediaFile(
                                selectedStep.getParentGuide().getId(),
                                content.getId(),
                                content.getMediaPath());
                        contentData.put("fullMediaPath", fullMediaPath);
                    }
                    data.put("content", contentData);

                    List<Object> buttons = (List<Object>) data.get("buttons");
                    if (content.getMediaPath() == null) {
                        buttons = removeButtonWithKey("show", buttons);
                    }
                    if (content.getWarnings().isEmpty()) {
                        buttons = removeButtonWithKey("warnings", buttons);
                    }
                    if (content.getHints().isEmpty()) {
                        buttons = removeButtonWithKey("hints", buttons);
                    }
                    data.put("buttons", buttons);
                }
                data.put("isFirst", stepSelection.isFirst());
                data.put("isLast", stepSelection.isLast());
                data.put("entryNo", stepSelection.getCursor() + 1);
                data.put("entryTotal", stepSelection.size());
                data.put("progress", (int) Math.ceil(stepSelection.getProgress() * 100));
                break;
            case "step-warnings":
                Selection<Warning> warningSelection = session.getWarningSelection();
                if (warningSelection == null) {
                    warningSelection = initializeWarningSelection(session);
                    session.setWarningSelection(warningSelection);
                }
                data.put("warning", warningSelection.getCurrentSelection().getText());
                data.put("isFirst", warningSelection.isFirst());
                data.put("isLast", warningSelection.isLast());
                data.put("entryNo", warningSelection.getCursor() + 1);
                data.put("entryTotal", warningSelection.size());
                data.put("progress", (int) Math.ceil(warningSelection.getProgress() * 100));
                break;
            case "step-hints":
                Selection<Hint> hintSelection = session.getHintSelection();
                if (hintSelection == null) {
                    hintSelection = initializeHintSelection(session);
                    session.setHintSelection(hintSelection);
                }
                data.put("hint", hintSelection.getCurrentSelection().getText());
                data.put("isFirst", hintSelection.isFirst());
                data.put("isLast", hintSelection.isLast());
                data.put("entryNo", hintSelection.getCursor() + 1);
                data.put("entryTotal", hintSelection.size());
                data.put("progress", (int) Math.ceil(hintSelection.getProgress() * 100));
                break;
        }

        try {
            String html = template.apply(data);
            activity.getWebView().loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);
            if (!voiceCommands.isEmpty()) {
                activity.prepareSpeechRecognition(voiceCommands, null);
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

    private Selection<Guide> initializeGuideSelection(final Session session, Filter<Guide> filter) {
        Comparator<Guide> comparator = new Comparator<Guide>() {
            @Override
            public int compare(Guide lhs, Guide rhs) {
                String t1 = lhs.getMetadata().getTitle();
                String t2 = rhs.getMetadata().getTitle();
                if (t1 == null) {
                    return t2 == null ? 0 : -1;
                }
                return t1.compareTo(t2);
            }
        };
        if (filter == null) {
            filter = new Filter<Guide>() {
                @Override
                public boolean accept(Guide guide) {
                    // Drop empty guides.
                    return guide.getNodes().size() > 2;
                }
            };
        }
        List guides = session.getGuideManager().getGuides(filter, comparator);
        return !guides.isEmpty() ? new Selection<>(guides) : null;
    }

    private Selection<Step> initializeStepSelection(Session session) throws IllegalStateException {
        Guide guide = session.getActiveGuide();
        if (guide == null) {
            throw new IllegalStateException("No guide active to retrieve steps from.");
        }
        List<Step> serializedGuide = session.getGuideManager().serializeGuide(guide.getId());
        return new Selection<>(serializedGuide);
    }

    private Selection<Warning> initializeWarningSelection(Session session) throws IllegalStateException {
        ContentDescriptor content = session.getActiveContentDescriptor();
        if (content == null) {
            throw new IllegalStateException("No content descriptor active to retrieve warning from.");
        }
        return new Selection<>(content.getWarnings());
    }

    private Selection<Hint> initializeHintSelection(Session session) throws IllegalStateException {
        ContentDescriptor content = session.getActiveContentDescriptor();
        if (content == null) {
            throw new IllegalStateException("No content descriptor active to retrieve hints from.");
        }
        return new Selection<>(content.getHints());
    }

    private static List<Object> removeButtonWithKey(String key, List<Object> buttons) {
        List<Object> newList = new ArrayList<>();
        for (Object button : buttons) {
            Map<String, Object> obj = (Map<String, Object>) button;
            String buttonKey = (String) obj.get("key");
            if (!key.equals(buttonKey)) {
                newList.add(obj);
            }
        }
        return newList;
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
            }

            // Specific actions.
            switch (slide.getId()) {
                case "catalog":
                case "filtered-catalog":
                    handleActionForCatalog(action, session);
                    break;
                case "guide-step":
                    handleActionForGuideStep(action, session);
                    break;
                case "step-warnings":
                    handleActionForStepWarnings(action, session);
                    break;
                case "step-hints":
                    handleActionForStepHints(action, session);
                    break;
            }
        }
        final String target = command.getTarget();
        if (target != null) {
            openSlide(target);
        }
    }

    private void handleActionForCatalog(String action, Session session) {
        Selection<Guide> guideSelection =  session.getGuideSelection();
        switch (action) {
            case "previous":
                if (!guideSelection.isFirst()) {
                    guideSelection.previous();
                }
                openSlide(slide.getId());
                break;
            case "next":
                if (!guideSelection.isLast()) {
                    guideSelection.next();
                }
                openSlide(slide.getId());
                break;
            case "selectGuide":
                break;
            case "resetGuide":
                session.setFilter(null);
                session.setGuideSelection(null);
                break;
        }
    }

    private void handleActionForGuideStep(String action, Session session) {
        Selection<Step> stepSelection =  session.getStepSelection();
        switch (action) {
            case "previous":
                if (!stepSelection.isFirst()) {
                    stepSelection.previous();
                }
                openSlide(slide.getId());
                break;
            case "next":
                if (!stepSelection.isLast()) {
                    stepSelection.next();
                    openSlide(slide.getId());
                } else {
                    openSlide("guide-complete");
                }
                break;
            case "showMedia":
                ContentDescriptor content = session.getActiveContentDescriptor();
                String mimeType = content.getMimeType();
                if (mimeType != null) switch (mimeType) {
                    case "video/mp4":
                        // openSlide("media-video");
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activity.playVideo(fullMediaPath);
                            }
                        });
                        break;
                    case "image/jpeg":
                        openSlide("media-image");
                        break;
                } else {
                    // Reload current slide.
                    openSlide(slide.getId());
                }
                break;
        }
    }

    private void handleActionForStepWarnings(String action, Session session) {
        Selection<Warning> warningSelection =  session.getWarningSelection();
        switch (action) {
            case "previous":
                if (!warningSelection.isFirst()) {
                    warningSelection.previous();
                }
                openSlide(slide.getId());
                break;
            case "next":
                if (!warningSelection.isLast()) {
                    warningSelection.next();
                }
                openSlide(slide.getId());
                break;
        }
    }

    private void handleActionForStepHints(String action, Session session) {
        Selection<Hint> hintSelection = session.getHintSelection();
        switch (action) {
            case "previous":
                if (!hintSelection.isFirst()) {
                    hintSelection.previous();
                }
                openSlide(slide.getId());
                break;
            case "next":
                if (!hintSelection.isLast()) {
                    hintSelection.next();
                }
                openSlide(slide.getId());
                break;
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
