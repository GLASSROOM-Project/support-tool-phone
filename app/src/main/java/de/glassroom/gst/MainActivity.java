package de.glassroom.gst;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import de.glassroom.gst.wf.VoiceCommand;
import de.glassroom.gst.wf.Workflow;
import de.glassroom.gst.wf.WorkflowParser;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {
    private static final String lang = PersistenceHandler.getClientProperties().getProperty("lang", "de_DE");

    private WebView webView;
    private WorkflowHandler workflowHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.webView);
        webView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        webView.getSettings().setJavaScriptEnabled(true);
        // webView.getSettings().setBuiltInZoomControls(true);
        // webView.getSettings().setSupportZoom(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("https://www.google.de/");

//        mainHandler = new Handler(getApplicationContext().getMainLooper());

//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
//        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000);
//        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
//        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
//        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplication().getPackageName());
//        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang.replace("_", "-"));
//        speechRecognizer.setRecognitionListener(prepareRegnitionListener());

//        isListening = false;
//        recognitionRetry = 0;

        Workflow workflow = loadWorkflow(getResources().getAssets());
        workflowHandler = new WorkflowHandler(this, webView, workflow);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

//    private RecognitionListener prepareRegnitionListener() {
//        return new RecognitionListener() {
//            @Override
//            public void onReadyForSpeech(Bundle params) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        webView.loadUrl("javascript:GST.onVoiceReady();");
//                    }
//                });
//            }
//
//            @Override
//            public void onBeginningOfSpeech() {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        webView.loadUrl("javascript:GST.onVoiceActive();");
//                    }
//                });
//            }
//
//            @Override
//            public void onRmsChanged(float rmsdB) {
//                // Do nothing.
//            }
//
//            @Override
//            public void onBufferReceived(byte[] buffer) {
//                // Do nothing.
//            }
//
//            @Override
//            public void onEndOfSpeech() {
//                // Do nothing.
//            }
//
//            @Override
//            public void onError(int error) {
//                String errorText;
//                isListening = false;
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        webView.loadUrl("javascript:GST.onVoiceError();");
//                    }
//                });
//                boolean throwError = false;
//                switch (error) {
//                    case SpeechRecognizer.ERROR_AUDIO:
//                        if (recognitionRetry < 5) {
//                            Log.e("MainActivity", "Failed to recognize speech: Audio recording error. Retrying ...");
//                            recognitionRetry++;
//                            startListening(2000);
//                        } else {
//                            Log.e("MainActivity", "Failed to recognize speech: Audio recording error. Aborting.");
//                            throwError = true;
//                        }
//                        break;
//                    case SpeechRecognizer.ERROR_CLIENT:
//                        Log.e("MainActivity", "Failed to recognize speech: Other client side errors. Aborting.");
//                        throwError = true;
//                        break;
//                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
//                        Log.e("MainActivity", "Failed to recognize speech: Insufficient permissions. Aborting.");
//                        throwError = true;
//                        break;
//                    case SpeechRecognizer.ERROR_NETWORK:
//                        Log.w("MainActivity", "Failed to recognize speech: Other network related errors. Aborting.");
//                        throwError = true;
//                        break;
//                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
//                        if (recognitionRetry < 5) {
//                            Log.w("MainActivity", "Failed to recognize speech: Network operation timed out. Retrying ...");
//                            recognitionRetry++;
//                            startListening(2000);
//                        } else {
//                            Log.w("MainActivity", "Failed to recognize speech: Network operation timed out. Aborting.");
//                            throwError = true;
//                        }
//                        break;
//                    case SpeechRecognizer.ERROR_NO_MATCH:
//                        Log.d("MainActivity", "Failed to recognize speech: No recognition results matched. Retrying ...");
//                        // startListening(0);
//                        break;
//                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
//                        Log.i("MainActivity", "Failed to recognize speech: Recognition service busy. Retrying ...");
//                        if (recognitionRetry < 5) {
//                            recognitionRetry++;
//                            startListening(1000);
//                        } else {
//                            throwError = true;
//                        }
//                        break;
//                    case SpeechRecognizer.ERROR_SERVER:
//                        Log.w("MainActivity", "Failed to recognize speech: Server sends error status.");
//                        throwError = true;
//                        break;
//                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
//                        Log.d("MainActivity", "Failed to recognize speech: No speech input. Retrying ...");
//                        startListening(0);
//                        break;
//                    default:
//                        Log.e("MainActivity", "Failed to recognize speech: Unknown error.");
//                        throwError = true;
//                }
//                if (throwError && textRecognizedHandler != null) {
//                    textRecognizedHandler.onError();
//                }
//            }
//
//            @Override
//            public void onResults(Bundle results) {
//                isListening = false;
//                recognitionRetry = 0;
//                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
//                Log.d("MainActivity", "Completed speech recognition. Result: " + matches);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        webView.loadUrl("javascript:GST.onVoiceInactive();");
//                    }
//                });
//
//                if (matches.isEmpty()) {
//                    startListening(0);
//                    return;
//                }
//
//                boolean isValidCommand = false;
//                if (validVoiceCommands != null) {
//                    for (VoiceCommand command : validVoiceCommands) {
//                        for (String keyword : command.getKeywords()) {
//                            for (String match : matches) {
//                                if (match.equalsIgnoreCase(keyword)) {
//                                    isValidCommand = true;
//                                    workflowHandler.execute(command.getCommand());
//                                }
//                                if (isValidCommand) break;
//                            }
//                            if (isValidCommand) break;
//                        }
//                        if (isValidCommand) break;
//                    }
//                }
//
//                if (isValidCommand) {
//                    return;
//                } else {
//                    if (textRecognizedHandler != null) {
//                        String match = matches.get(0);
//                        Log.i("MainActivity", "Recognized text: " + match);
//                        textRecognizedHandler.textRecognized(match.substring(0,1).toUpperCase() + match.substring(1));
//                        // textRecognizedHandler = null;
//                    } else {
//                        Log.d("MainActivity", "No matching voice command. Retrying ...");
//                        startListening(0);
//                    }
//                }
//            }
//
//            @Override
//            public void onPartialResults(Bundle partialResults) {
//                // Do nothing.
//            }
//
//            @Override
//            public void onEvent(int eventType, Bundle params) {
//                // Do nothing.
//            }
//        };
//    }

    private static Workflow loadWorkflow(AssetManager assetManager) {
        String line;
        Workflow wf = null;

        try {
            InputStream is = assetManager.open("workflow/workflow." + lang + ".xml", AssetManager.ACCESS_STREAMING);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String workflowXML = "";
            while ((line = br.readLine()) != null) {
                workflowXML += line;
            }
            wf = WorkflowParser.parseWorkflow(workflowXML);
            Log.i("MainActivity", "Workflow initialized.");
        } catch (IOException e) {
            Log.e("MainActivity", "Failed to open workflow.en_GB.xml file.", e);
        }

        return wf;
    }

    @Override
    public void onDestroy() {
        //Log.d("Lifecycle", "onDestroy()");
//        speechRecognizer.destroy();
        super.onDestroy();
        finish();
    }

    public void prepareSpeechRecognition(List<VoiceCommand> validVoiceCommands, TextRecognitionHandler handler) {
//        if (isListening) {
//            throw new IllegalStateException("A recognition task is currently running. Preparation aborted.");
//        }
//        Log.d("MainActivity", "Preparing speech recognition.");
//        this.validVoiceCommands = validVoiceCommands;
//        this.textRecognizedHandler = handler;
    }

    public void startSpeechRecognition() throws IllegalStateException {
//        if (isListening) {
//            throw new IllegalStateException("A recognition task is already running. Start aborted.");
//        }
//        startListening(0);
    }

    public void reinitializeVoiceCommands() {
//        mainHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                Log.d("MainActivity", "Reinitializing voice listener.");
//                if (isListening) {
//                    speechRecognizer.cancel();
//                    isListening = false;
//                }
//                speechRecognizer.startListening(speechRecognizerIntent);
//            }
//        });
    }

    public void stopSpeechRecognition() {
//        if (isListening) mainHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                Log.d("MainActivity", "Stop listing to speech.");
//                speechRecognizer.cancel();
//                isListening = false;
//                validVoiceCommands = null;
//                textRecognizedHandler = null;
//            }
//        });
    }

    private void startListening(int delay) {
//        isListening = true;
//        recognitionRetry = 0;
//        if (delay > 0) {
//            Timer t = new Timer();
//            t.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    mainHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            speechRecognizer.startListening(speechRecognizerIntent);
//                            Log.d("MainActivity", "Start delayed listening to speech.");
//                        }
//                    });
//                }
//            }, delay);
//        } else {
//            speechRecognizer.startListening(speechRecognizerIntent);
//            Log.d("MainActivity", "Start instant listening to speech.");
//        }
    }

    public WebView getWebView() {
        return webView;
    }

    public void playVideo(String uri) {
        Intent videoPlaybackActivity = new Intent(this, VideoPlayer.class);
        videoPlaybackActivity.putExtra("uri", uri);
        startActivityForResult(videoPlaybackActivity, VideoPlayer.INTENT_ID);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case VideoPlayer.INTENT_ID:
                workflowHandler.resetSlide();
                break;
            default:
                Log.e("MainActivity", "Unknown intent request code: " + requestCode);
                workflowHandler.resetSlide();
        }
    }
}
