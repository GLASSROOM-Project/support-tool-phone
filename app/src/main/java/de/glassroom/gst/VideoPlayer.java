package de.glassroom.gst;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.VideoView;

public class VideoPlayer extends Activity implements MediaPlayer.OnCompletionListener, View.OnTouchListener {
    public static final int INTENT_ID = 2165;

    private VideoView mVV;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        setContentView(R.layout.video_player);

        String uri = getIntent().getExtras().getString("uri");

        mVV = (VideoView)findViewById(R.id.videoview);
        mVV.setOnCompletionListener(this);
        mVV.setOnTouchListener(this);

        mVV.setVideoURI(Uri.parse(uri));

        mVV.start();
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String uri = getIntent().getExtras().getString("uri");
        mVV.setVideoURI(Uri.parse(uri));
    }

    public void stopPlaying() {
        mVV.stopPlayback();
        setResult(RESULT_OK, new Intent());
        this.finish();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        finish();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        stopPlaying();
        return true;
    }
}
