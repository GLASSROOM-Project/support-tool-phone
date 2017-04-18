package de.glassroom.gst;

import android.util.Log;

import java.io.IOException;

import de.glassroom.gpe.Filter;
import de.glassroom.gpe.Guide;
import de.glassroom.gpe.GuideManager;
import de.glassroom.gpe.Step;
import de.glassroom.gpe.content.ContentDescriptor;
import de.glassroom.gpe.content.Hint;
import de.glassroom.gpe.content.Warning;
import de.glassroom.gst.wf.Slide;

/**
 * A container for all runtime information of a user session.
 */
public class Session {
    private static final String lang = PersistenceHandler.getClientProperties().getProperty("lang", "de_DE");

    private final GuideManager guideManager;
    private Slide previousSlide, activeSlide;
    private Selection<Guide> guideSelection;
    private Selection<Step> stepSelection;
    private Filter<Guide> guideFilter;
    private ContentDescriptor activeContent;
    private Selection<Warning> warningSelection;
    private Selection<Hint> hintSelection;

    public Session() {
        guideManager = new GuideManager();
        for (Guide guide : PersistenceHandler.importGuides()) {
            guideManager.addGuide(guide);
        }
    }

    public GuideManager getGuideManager() {
        return guideManager;
    }

    public void setActiveSlide(Slide slide) {
        if (activeSlide != slide) {
            previousSlide = activeSlide;
        }
        activeSlide = slide;
    }

    public Slide getActiveSlide() {
        return activeSlide;
    }

    public Slide getPreviousSlide() {
        return previousSlide;
    }

    public boolean hasPreviousSlide() {
        return previousSlide != null;
    }

    public Guide getActiveGuide() {
        return guideSelection != null ? guideSelection.getCurrentSelection() : null;
    }

    public Step getActiveStep() {
        return stepSelection != null ? stepSelection.getCurrentSelection() : null;
    }

    public ContentDescriptor getActiveContentDescriptor() {
        return activeContent;
    }

    public void setGuideSelection(Selection<Guide> selection) {
        guideSelection = selection;
        if (guideSelection != null) {
            guideSelection.addHandler(new SelectionHandler<Guide>() {
                @Override
                public void selectionChanged(Guide selectedEntry) {
                    setStepSelection(null);
                }
            });
        }
    }

    public Selection<Guide> getGuideSelection() {
        return guideSelection;
    }

    public void setStepSelection(Selection<Step> selection) {
        stepSelection = selection;
        if (stepSelection != null) {
            stepSelection.addHandler(new SelectionHandler<Step>() {
                @Override
                public void selectionChanged(Step step) {
                    try {
                        activeContent = PersistenceHandler.readContentDescriptor(step.getParentGuide().getId(), step.getContent().getContentPackage());
                    } catch (IOException e) {
                        Log.w("Session", "Failed to retrieve content for step: " + step.getId(), e);
                        activeContent = null;
                    }
                }
            });
        } else {
            activeContent = null;
            hintSelection = null;
            warningSelection = null;
        }
    }

    public Selection<Step> getStepSelection() {
        return stepSelection;
    }

    public void setFilter(Filter<Guide> filter) {
        this.guideFilter = filter;
    }

    public Filter<Guide> getFilter() {
        return guideFilter;
    }

    public void setWarningSelection(Selection<Warning> warningSelection) {
        this.warningSelection = warningSelection;
    }

    public Selection<Warning> getWarningSelection() {
        return warningSelection;
    }

    public void setHintSelection(Selection<Hint> hintSelection) {
        this.hintSelection = hintSelection;
    }

    public Selection<Hint> getHintSelection() {
        return hintSelection;
    }
}
