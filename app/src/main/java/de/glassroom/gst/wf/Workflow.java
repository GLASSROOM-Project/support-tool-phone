package de.glassroom.gst.wf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Execution engine for a workflow.
 * @author simon.schwantzer(at)im-c.de
 */
public class Workflow {
	private final Map<String, Slide> slides;
	private final Set<WorkflowListener> workflowListeners;
	private Slide activeSlide;
		
	protected Workflow(Map<String, Slide> slides, String beginWith) {
		this.slides = slides;
		activeSlide = slides.get(beginWith);
		workflowListeners = new HashSet<WorkflowListener>();
	}
	
	/**
	 * Returns a slide.
	 * @param id Identifier of the slide to return. 
	 * @return Slide or <code>null</code> if no slide with the given identifier is registered.
	 */
	public Slide getSlide(String id) {
		return slides.get(id);
	}
	
	/**
	 * Returns the active slide.
	 * @return Active slide.
	 */
	public Slide getActiveSlide() {
		return activeSlide;
	}
	
	/**
	 * Sets the active slide.
	 * @param slideId Identifier of the slide to set.
	 * @throws IllegalArgumentException
	 */
	public void setActiveSlide(String slideId) throws IllegalArgumentException {
		if (!slides.containsKey(slideId)) {
			throw new IllegalArgumentException("No slide with id " + slideId + " found.");
		}
		activeSlide = slides.get(slideId);
		notifyListeners();
	}
	
	/**
	 * Registers an listener for workflow events. 
	 * @param listener Listener to register.
	 */
	public void registerWorkflowListener(WorkflowListener listener) {
		workflowListeners.add(listener);
		listener.slideChanged(this, activeSlide);
	}
	
	private void notifyListeners() {
		for (WorkflowListener listener : workflowListeners) {
			listener.slideChanged(this, activeSlide);
		}
	}

    /**
     * Returns all templates referenced in the slides.
     * @return List of template paths.
     */
	public Set<String> getTemplateIds() {
        Set<String> templateIds = new HashSet<>();
        for (Slide slide : slides.values()) {
            templateIds.add(slide.getTemplatePath());
        }
        return templateIds;
    }
	
	
}
