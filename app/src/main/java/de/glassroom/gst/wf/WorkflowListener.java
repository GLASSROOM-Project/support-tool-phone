package de.glassroom.gst.wf;

/**
 * Listener for workflow execution events.
 * @author simon.schwantzer(at)im-c.de
 */
public interface WorkflowListener {
	/**
	 * Event called when a slide becomes active in a workflow.
	 * @param workflow Workflow the slade became active in. 
	 * @param activeSlide Slide which became active.
	 */
	public void slideChanged(Workflow workflow, Slide activeSlide);
}
