package de.glassroom.gst.wf;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

public final class WorkflowParser {
	private static SAXBuilder saxBuilder;
	static {
		saxBuilder = new SAXBuilder();
	}
	
	private WorkflowParser() {}
	
	/**
	 * Parses a workflow and returns a workflow instance.
	 * @param workflow Workflow in XML representation.
	 * @return Workflow manager instance.
	 * @throws IllegalArgumentException The given string is no valid workflow XML.
	 */
	public static Workflow parseWorkflow(String workflow) throws IllegalArgumentException {
		Element workflowElement;
		try {
			Document doc = saxBuilder.build(new StringReader(workflow));
			workflowElement = doc.getRootElement();
			if (!"workflow".equals(workflowElement.getName())) {
				throw new IllegalArgumentException("Invalid workflow: Route element does not match \"process\".");
			}
		} catch (JDOMException|IOException e) {
			throw new IllegalArgumentException("Failed to parse workflow.", e);
		}
		
		String beginWith = workflowElement.getAttributeValue("beginWith");
		if (beginWith == null) {
			throw new IllegalArgumentException("Missing [beginWith] attribute with slide identifier.");
		}
	
		Map<String, Slide> slides = new HashMap<>();
		
		for (Element slideElement : workflowElement.getChildren("slide", workflowElement.getNamespace())) {
			Slide slide = parseSlide(slideElement);
			if (slides.containsKey(slide.getId())) {
				throw new IllegalArgumentException("Two slides with same identifier: " + slide.getId());
			}
			slides.put(slide.getId(), slide);
		}
		
		if (!slides.containsKey(beginWith)) {
			throw new IllegalArgumentException("The slide " + beginWith + " does not exists. No slide available to start with.");
		}
		
		return new Workflow(slides, beginWith);
	}
	
	private static Slide parseSlide(Element slideElement) throws IllegalArgumentException {
		Namespace ns = slideElement.getNamespace();
		String id = slideElement.getAttributeValue("id");
		if (id == null) {
			throw new IllegalArgumentException("Missing required attribute for slide: id");
		}
		
		String typeString = slideElement.getAttributeValue("type", "<na>");
		SlideType type;
		try {
			type = SlideType.valueOf(typeString.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Missing or invalid slide type: " + typeString , e);
		}

        Map<String, Command> commands = new HashMap<>();
		Element commandsElement = slideElement.getChild("commands", ns);
		if (commandsElement != null) for (Element commandElement : commandsElement.getChildren("command", ns)) {
			Command command = parseCommand(commandElement);
			commands.put(command.getKey(), command);
		}

        Element propertiesElement = slideElement.getChild("properties", ns);

        Slide slide = new Slide(id, type, propertiesElement);

        Element voiceCommandsElement = slideElement.getChild("voiceCommands", ns);
        if (voiceCommandsElement != null) for (Element voiceCommandElement : voiceCommandsElement.getChildren("voiceCommand", ns)) {
            slide.addVoiceCommand(parseVoiceCommand(voiceCommandElement, commands));
        }

        Element buttonsElement = slideElement.getChild("buttons", ns);
        if (buttonsElement != null) for (Element buttonElement : buttonsElement.getChildren("button", ns)) {
            slide.addButton(parseButton(buttonElement, commands));
        }

        Element forwardElement = propertiesElement.getChild("forward", ns);
        if (forwardElement != null) {
            slide.setForwarding(parseForwarding(forwardElement));
        }

        Element performActionElement = propertiesElement.getChild("performAction", ns);
        if (performActionElement != null) {
            slide.setPerformAction(parsePerformAction(performActionElement));
        }

        return slide;
	}
	
	private static Command parseCommand(Element commandElement) throws IllegalArgumentException {
		String key = commandElement.getAttributeValue("key");
		String target = commandElement.getAttributeValue("target");
		String action = commandElement.getAttributeValue("action");
		if (key == null || (target == null && action == null)) {
			throw new IllegalArgumentException("A [key] and either a [target] or an [action] are required attributes of a command.");
		}

        return new Command(key, target, action);
	}

    private static VoiceCommand parseVoiceCommand(Element voiceCommandElement, Map<String, Command> availableCommands) throws IllegalArgumentException {
        String key = voiceCommandElement.getAttributeValue("command");
        Command command = availableCommands.get(key);
        if (command == null) {
            throw new IllegalArgumentException("No command defined for voice command with key " + key + ".");
        }
        String keywordsString = voiceCommandElement.getAttributeValue("keywords");
        if (keywordsString == null || keywordsString.trim().isEmpty()) {
            throw new IllegalArgumentException("No or empty [keywords] list for voice comamnd.");
        }
        List<String> keywords = new ArrayList<>(Arrays.asList(keywordsString.split(";")));
        return new VoiceCommand(command, keywords);
    }

    private static Button parseButton(Element buttonElement, Map<String, Command> availableCommands) throws IllegalArgumentException {
        String key = buttonElement.getAttributeValue("command");
        Command command = availableCommands.get(key);
        if (command == null) {
            throw new IllegalArgumentException("No command defined for button with key " + key + ".");
        }
        String label = buttonElement.getAttributeValue("label");
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("No label defined for button with key " + key + ".");
        }
        return new Button(command, label);
    }

    private static Slide.Forwarding parseForwarding(Element forwardElement) throws IllegalArgumentException {
        int delay;
        String target;
        try {
            delay = forwardElement.getAttribute("delay").getIntValue();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to forwardning command.", e);
        }
        target = forwardElement.getAttributeValue("target");
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing target in forward element.");
        }
        return new Slide.Forwarding(delay, target);
    }

	private static Slide.PerformAction parsePerformAction(Element performActionElement) throws IllegalArgumentException {
		int delay;
		String action;
		try {
			delay = performActionElement.getAttribute("delay").getIntValue();
		} catch (Exception e) {
			throw new IllegalArgumentException("Faile dto parse performAction command.", e);
		}
		action = performActionElement.getAttributeValue("action");
		if (action == null || action.trim().isEmpty()) {
			throw new IllegalArgumentException("Missing action in performAction element.");
		}
		return new Slide.PerformAction(delay, action);
	}
}
