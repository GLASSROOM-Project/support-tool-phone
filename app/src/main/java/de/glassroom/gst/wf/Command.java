package de.glassroom.gst.wf;

import java.util.ArrayList;
import java.util.List;

/**
 * Single command for a slide.
 * 
 * @author simon.schwantzer(at)im-c.de
 */
public class Command {

    private final String key;
	private final String target;
	private final String action;

	public Command(String key, String target, String action) {
		this.key = key;
		this.target = target;
		this.action = action;
	}
	
	/**
	 * Returns the command key.
	 * @return Key unique within all commands of a controller.
	 */
	public String getKey() {
		return key;
	}
	
	/**
	 * Returns the action to be performed.
	 * @return Action identifier. May be <code>null</code>.
	 */
	public String getAction() {
		return action;
	}
	
	/**
	 * Returns the target slide. 
	 * @return Slide to be shown when the command is performed. May be <code>null</code>.
	 */
	public String getTarget() {
		return target;
	}
}