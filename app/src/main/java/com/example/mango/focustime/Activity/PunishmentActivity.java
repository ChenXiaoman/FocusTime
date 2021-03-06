package com.example.mango.focustime.Activity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.example.mango.focustime.processutil.Features;
import com.example.mango.focustime.R;
import com.example.mango.focustime.service.MyService;

//import static com.example.mango.focustime.StartButtonListener.context;

public class PunishmentActivity extends AppCompatActivity {
    private MediaPlayer mp = new MediaPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_punishment);
        MediaPlayer mp = MediaPlayer.create(this, R.raw.apple_pan_short);
        mp.start();

        // Stop detection service
        Features.showForeground = false;
        Intent i = new Intent(this, MyService.class);
        this.stopService(i);

        //finish playing music and go back to mainActivity
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                Intent i = new Intent(PunishmentActivity.this, FocusModeActivity.class);
                startActivity(i);
            }
        });
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(getApplicationContext(), R.string.cannot_exit, Toast.LENGTH_SHORT).show();
    }

}
