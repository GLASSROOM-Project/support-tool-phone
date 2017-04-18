package de.glassroom.gst.wf;

import java.util.List;

public class VoiceCommand {
    private final List<String> keywords;
    private final Command command;

    public VoiceCommand(Command command, List<String> keywords) {
        this.command = command;
        this.keywords = keywords;
    }

    public Command getCommand() {
        return command;
    }

    public List<String> getKeywords() {
        return keywords;
    }
}
