package de.glassroom.gst.wf;

public class Button {
    private final Command command;
    private final String label;

    public Button(Command command, String label) {
        this.command = command;
        this.label = label;
    }

    public Command getCommand() {
        return command;
    }

    public String getLabel() {
        return label;
    }
}
