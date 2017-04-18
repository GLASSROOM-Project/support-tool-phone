package de.glassroom.gst;

public interface TextRecognitionHandler {
    public void textRecognized(String text);
    public void onError();
}
