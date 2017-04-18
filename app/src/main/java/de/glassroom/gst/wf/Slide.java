package de.glassroom.gst.wf;

import org.jdom2.Element;
import org.jdom2.Namespace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface for slides.
 * 
 * @author simon.schwantzer(at)im-c.de
 */
public class Slide {
    public static class Forwarding {
        private final int delay;
        private final String target;

        public Forwarding(int delay, String target) {
            this.delay = delay;
            this.target = target;
        }

        public String getTarget() {
            return target;
        }

        public int getDelay() {
            return delay;
        }
    }

    public static class PerformAction {
        private final int delay;
        private final String action;

        public PerformAction(int delay, String action) {
            this.delay = delay;
            this.action = action;
        }

        public String getAction() {
            return action;
        }

        public int getDelay() {
            return delay;
        }
    }

	private final String id;
	private final SlideType type;
	protected final Element properties;
	private final Map<String, Command> commands;
    private final List<VoiceCommand> voiceCommands;
    private final List<Button> buttons;
    private Forwarding forwarding;
    private PerformAction performAction;

	public Slide(String id, SlideType type, Element properties) {
		this.id = id;
		this.type = type;
		this.properties = properties;
        commands = new HashMap<>();
        voiceCommands = new ArrayList<>();
        buttons = new ArrayList<>();
	}
	
	/**
	 * Returns the unique identifier for the slide.
	 * @return Unique identifier.
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Returns the type of the slide.
	 * @return Slide type.
	 */
	public SlideType getType() {
		return type;
	}
	
	/**
	 * Sets the template for this slide. 
	 * @param template Path of template.
	 * @return Reference for chaining.
	 */
	public Slide setTemplate(String template) {
		return this;
	}
	
	/**
	 * Returns the template of this slide.
	 * @return Path of template or <code>null</code>.
	 */
	public String getTemplatePath() {
		Element templateElement = properties.getChild("template", properties.getNamespace());
		return templateElement != null ? templateElement.getText() : null;
	}
	
	/**
	 * Returns the background of this slide.
	 * @return Path of the background image or <code>null</code>.
	 */
	public String getBackgroundPath() {
		Element backgroundElement = properties.getChild("background", properties.getNamespace());
		return backgroundElement != null ? backgroundElement.getText() : null;
	}
	
	public String getBody() {
        return properties.getChildText("body", properties.getNamespace());
    }

    public String getTitle() {
        return properties.getChildText("title", properties.getNamespace());
    }
	
	/**
	 * Returns the raw properties of the slide.
	 * @return Properties as XML element.
	 */
	public Element getRawProperties() {
		return properties;
	}

    public void addButton(Button button) {
        buttons.add(button);
        Command command = button.getCommand();
        commands.put(command.getKey(), command);
    }

    public void addVoiceCommand(VoiceCommand voiceCommand) {
        voiceCommands.add(voiceCommand);
        Command command = voiceCommand.getCommand();
        commands.put(command.getKey(), command);
    }

    public List<Button> getButtons() {
        return buttons;
    }

    public List<VoiceCommand> getVoiceCommands() {
        return voiceCommands;
    }

    public Command getCommand(String key) {
        return commands.get(key);
    }

    public void setForwarding(Forwarding forwarding) {
        this.forwarding = forwarding;
    }

    public Forwarding getForwarding() {
        return forwarding;
    }

    public void setPerformAction(PerformAction performAction) {
        this.performAction = performAction;
    }

    public PerformAction getPerformAction() {
        return performAction;
    }

    public Map<String, Object> extractData() {
        Namespace ns = properties.getNamespace();
        Map<String, Object> data = new LinkedHashMap<>();

        String title = getTitle();
        if (title != null) data.put("title", title);
        String body = getBody();
        if (body != null) data.put("body", body);
        String background = getBackgroundPath();
        if (background != null) data.put("background", background);

        List<Button> buttons = getButtons();
        if (buttons != null && !buttons.isEmpty()) {
            List<Object> buttonsArray = new ArrayList<>();
            for (int i = 0; i < buttons.size(); i++) {
                Button button = buttons.get(i);
                Map<String, Object> buttonObject = new LinkedHashMap<>();
                buttonObject.put("key", button.getCommand().getKey());
                buttonObject.put("label", button.getLabel());
                buttonObject.put("index", i + 1);
                buttonsArray.add(buttonObject);
            }
            data.put("buttons", buttonsArray);
        }

        if (!commands.containsKey("help")) {
            data.put("hideHelp", true);
        }

        if (!commands.containsKey("cancel")) {
            data.put("hideCancel", true);
        }

        return data;
    }
}
