package com.example.tvjam;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    public static String playerName;
    public static final String breakingBadWikia = "https://breakingbad.fandom.com";
    public static final String vikingsWikia = "https://vikings.fandom.com";


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK) {
            Long score = data.getLongExtra("score", -1);
            Toast.makeText(getApplicationContext(), "The score was: " + Long.toString(score), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ArrayList<String> breakingBadTags = new ArrayList<String>();

        breakingBadTags.add("Age");
        breakingBadTags.add("Aliases");
        breakingBadTags.add("Portrayed by");
        //breakingBadTags.add("Status");
        breakingBadTags.add("First Appearance");
        breakingBadTags.add("Last Appearance");

        ArrayList<String> vikingsTags = new ArrayList<String>();

        //vikingsTags.add("Age");
        vikingsTags.add("AKA");
        vikingsTags.add("actor");
        vikingsTags.add("age");
        //breakingBadTags.add("First Appearance");
        //breakingBadTags.add("Last Appearance");

        final SharedPreferences prefs = this.getSharedPreferences("settings", 0);
        final SharedPreferences.Editor editor = prefs.edit();

        playerName = prefs.getString("playerName", "name");
        final EditText txt_Name = findViewById(R.id.txt_Name);
        txt_Name.setText(playerName);
        int rounds = prefs.getInt("rounds", 10);
        int fails = prefs.getInt("fails", 5);

        int gameMode = prefs.getInt("gameMode", 1);

        switch(gameMode) {
            case 2:
                ((RadioButton)findViewById(R.id.radioButton2)).setChecked(true);
                break;
            case 3:
                ((RadioButton)findViewById(R.id.radioButton3)).setChecked(true);
                break;
            default:
                ((RadioButton)findViewById(R.id.radioButton)).setChecked(true);
                break;
        }

        String hostname = prefs.getString("hostname", "");
        String roomName = prefs.getString("roomName", "Room 1");

        ((EditText)findViewById(R.id.txt_hostname)).setText(hostname);
        ((EditText)findViewById(R.id.txt_roomName)).setText(roomName);


        ((EditText)findViewById(R.id.txt_rounds)).setText(Integer.toString(rounds));
        ((EditText)findViewById(R.id.txt_fails)).setText(Integer.toString(fails));



        Button btn_Play = findViewById(R.id.btn_Play);
        btn_Play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String hostname = ((EditText)findViewById(R.id.txt_hostname)).getText().toString();
                String roomName = ((EditText)findViewById(R.id.txt_roomName)).getText().toString();

                RadioButton rb2 = findViewById(R.id.radioButton2);
                RadioButton rb3 = findViewById(R.id.radioButton3);

                int gameMode = 1;

                if(rb2.isChecked()) {gameMode = 2;}
                else if(rb3.isChecked()) {gameMode = 3;}

                if((hostname.isEmpty() || roomName.isEmpty()) && gameMode != 1 ) {
                    return;
                }

                Intent myIntent = new Intent(view.getContext(), GameActivity.class);

                playerName = txt_Name.getText().toString();
                editor.putString("playerName", playerName);
                String rounds = ((TextView)findViewById(R.id.txt_rounds)).getText().toString();
                String fails = ((TextView)findViewById(R.id.txt_fails)).getText().toString();


                Spinner mySpinner = findViewById(R.id.spinner);

                if(mySpinner.getSelectedItemPosition() == 0) {
                    myIntent.putExtra("wikiaLink", breakingBadWikia);
                    myIntent.putStringArrayListExtra("tags", breakingBadTags);
                }
                else if(mySpinner.getSelectedItemPosition() == 1) {
                    myIntent.putExtra("wikiaLink", vikingsWikia);
                    myIntent.putStringArrayListExtra("tags", vikingsTags);

                }

                editor.putInt("gameMode", gameMode);

                editor.putString("roomName", roomName);
                editor.putString("hostname", hostname);




                Set<String> myTags = null;

                editor.putStringSet("tags", myTags);
                editor.putInt("rounds", Integer.parseInt(rounds));
                editor.putInt("fails", Integer.parseInt(fails));
                editor.apply();

                startActivityForResult(myIntent, 0);

            }
        });
        Button btn_Leaderboard = findViewById(R.id.btn_Leaderboard);
        btn_Leaderboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(view.getContext(), LeaderboardActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });
    }
}
