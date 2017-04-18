package de.glassroom.gst;

import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.glassroom.gpe.Guide;
import de.glassroom.gpe.Step;
import de.glassroom.gpe.annotations.MetadataAnnotation;
import de.glassroom.gpe.content.ContentDescriptor;
import de.glassroom.gpe.content.Hint;
import de.glassroom.gpe.content.Warning;
import de.glassroom.gst.wf.Command;
import de.glassroom.gst.wf.Slide;
import de.glassroom.gst.wf.Workflow;
import de.glassroom.gst.wf.WorkflowListener;

public class WorkflowHandler implements WorkflowListener {
    private static final String lang = PersistenceHandler.getClientProperties().getProperty("lang", "de_DE");

    private final MainActivity activity;
    private final WebView webView;
    private final Handlebars handlebars;
    private final Map<String, Template> templates;
    private final Workflow workflow;
    private final Session session;

    private SlideHandler slideHandler;

    public WorkflowHandler(final MainActivity activity, final WebView webView, final Workflow workflow) {
        this.activity = activity;
        this.webView = webView;
        this.workflow = workflow;
        this.webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void executeCommand(String key) {
                Log.i("WorkflowHandler", "Received JS call to execute command: " + key);
                activity.stopSpeechRecognition();
                Slide activeSlide = session.getActiveSlide();
                if (activeSlide != null) {
                    Command command = activeSlide.getCommand(key);
                    execute(command);
                }
            }

            @JavascriptInterface
            public void log(String text) {
                Log.i("<WebView>", text);
            }

            @JavascriptInterface
            public void mediaPlaybackComplete() {
                Timer t = new Timer();
                t.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                workflow.setActiveSlide("guide-step");
                            }
                        });
                    }
                }, 500);
            }
        }, "JSInterface");

        this.templates = new HashMap<String, Template>();
        handlebars = new Handlebars();
        session = new Session();

        this.workflow.registerWorkflowListener(this);
    }

    public void initializeTemplates() {
        for (String templatePath : workflow.getTemplateIds()) {
            loadTemplate(templatePath);
        }
    }

    private Template loadTemplate(String templatePath) {
        Template template = templates.get(templatePath);
        if (template == null) {
            try {
                template = handlebars.compile(Uri.fromFile(new File("//assets/templates/" + lang + "/" + templatePath)).getPath());
                templates.put(templatePath, template);
            } catch (Exception e) {
                Log.e("WorkflowHandler", "Failed to load template: " + templatePath +". Reason: " + e.getMessage());
            }
        }
        return template;
    }

    @Override
    public void slideChanged(Workflow workflow, Slide slide) {
        Log.i("WorkflowHandler", "Slide " + slide.getId() + " is now active.");
        session.setActiveSlide(slide);

        String templatePath = slide.getTemplatePath();

        Template template = loadTemplate(templatePath);

        switch (slide.getType()) {
            case SPLASH:
                slideHandler = new SplashSlideHandler(this, template);
                break;
            case DEFAULT:
                slideHandler = new DefaultSlideHandler(template);
                break;
            case TITLED_SELECT:
            case PROGRESS_SELECT:
                slideHandler = new SelectionSlideHandler(template);
                break;
            case MEDIA:
                slideHandler = new MediaSlideHandler(template);
                break;
            default:
                Log.w("WorkflowHandler", "Unsupported slide type.");
                return;
        }
        slideHandler.initialize(activity, workflow, slide);
        slideHandler.renderSlide(session);
    }

    public void resetSlide() {
        slideHandler.renderSlide(session);
    }

    public static Map<String, Object> extractGuideData(Guide guide) {
        Map<String, Object> map = new LinkedHashMap<>();
        MetadataAnnotation metadata = guide.getMetadata();
        if (metadata != null) {
            map.put("title", metadata.getTitle());
            map.put("description", metadata.getDescription());
        }
        map.put("steps", guide.getNodes().size() - 2);
        long lastUpdate = guide.getLastUpdate().getTime();
        map.put("lastUpdate", DateUtils.getRelativeTimeSpanString(lastUpdate).toString());
        return map;
    }

    public static Map<String, Object> extractStepData(Step step) {
        Map<String, Object> data = new LinkedHashMap<>();
        return data;
    }

    public static Map<String, Object> extractContentData(ContentDescriptor content) {
        Map<String, Object> map = new LinkedHashMap<>();
        String info = content.getInfo();
        if (info != null) {
            map.put("info", content.getInfo());
        } else {
            map.put("info", "Die notwendigen Vorbereitungen werden im Video erklärt.");
        }
        if (content.getMediaPath() != null) {
            map.put("mimeType", content.getMimeType());
            map.put("mediaPath", content.getMediaPath());
        }
        List<Warning> warnings = content.getWarnings();
        if (!warnings.isEmpty()) {
            List<String> warningsList = new ArrayList<>();
            for (Warning warning : warnings) {
                warningsList.add(warning.getText());
            }
            map.put("warnings", warningsList);
            map.put("hasWarnings", true);
            map.put("numWarnings", content.getWarnings().size());
        } else {
            map.put("hasWarnings", false);
        }
        List<Hint> hints = content.getHints();
        if (!hints.isEmpty()) {
            List<String> hintsList = new ArrayList<>();
            for (Hint hint : hints) {
                hintsList.add(hint.getText());
            }
            map.put("hints", hintsList);
            map.put("hasHints", true);
            map.put("numHints", content.getWarnings().size());
        } else {
            map.put("hasWarnings", false);
        }
        return map;
    }

    public void execute(final Command command) {
        slideHandler.execute(command, session);
    }
}
