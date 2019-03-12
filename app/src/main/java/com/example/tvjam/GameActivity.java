package com.example.tvjam;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;


public class GameActivity extends AppCompatActivity {


    public static String SERVERURI = "http://192.168.188.21:12345";

    public static final long timeToAnswer = 20000;
    CountDownTimer countDownTimer;
    public static final long roundCooldown = 2000;
    CountDownTimer roundCooldownTimer;
    public long timeLeft;
    public long playerScore;
    public long jokerScore;
    public long skipScore;

    public int maxNumRounds;
    public int maxMistakes;
    public int currNumRounds;
    public int currMistakes;
    public Boolean roundActive;
    public int correctAnswer;
    public int nextCorrectAnswer;
    public boolean gameover;

    public static final int CRAWLAMOUNT = 25;
    public static String WIKIAURL = "";//https://breakingbad.fandom.com";

    public static int questionGeneratorReturn;

    private static int gameMode = 1; // 1 = local, 3 = host, 2 = join
    String roomName = "";


    Bitmap imgBit;

    ArrayList<String> possibleTags = new ArrayList<String>();
    String myGoodMainChar = "";
    String myGoodTag = "Aliases";

    ArrayList<String> answers = new ArrayList<String>();
    ArrayList<String> charsVisited = new ArrayList<String>();
    String constructedQuestion;

    ProgressDialog pd;

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(SERVERURI);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Emitter.Listener joinRoomInfo = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String createdBy;
                    String players;
                    try {
                        createdBy = data.getString("createdBy");
                        players = data.getString("players");
                    } catch (Exception e) {
                        return;
                    }

                }
            });
        }
    };

    private Emitter.Listener playerJoined = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pd.dismiss();
                    gameManager();
                }
            });
        }
    };

    private class DownloadCharImage extends AsyncTask<String, Integer, Long>  {

        @Override
        protected Long doInBackground(String... strings) {
            try{
                imgBit = BitmapFactory.decodeStream((InputStream)new URL(strings[0]).getContent());

            }catch(Exception e) {}

            return null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("score", playerScore);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
            if(countDownTimer != null) countDownTimer.cancel();
            if(roundCooldownTimer != null) roundCooldownTimer.cancel();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();
        mSocket.off("leaveGame");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        setTitle("Score: 0");

        pd = new ProgressDialog(this);

        Intent myIntent = this.getIntent();
        WIKIAURL = myIntent.getStringExtra("wikiaLink");
        possibleTags = myIntent.getStringArrayListExtra("tags");


        currNumRounds = 0;
        roundActive = false;

        final SharedPreferences prefs = this.getSharedPreferences("settings", 0);
        final SharedPreferences.Editor editor = prefs.edit();
        maxNumRounds = prefs.getInt("rounds", 10);
        maxMistakes = prefs.getInt("fails", 5);


        gameMode = prefs.getInt("gameMode", 1);

        String playerName = prefs.getString("playerName", "myName");

        if(gameMode == 2 || gameMode == 3) {
            SERVERURI = prefs.getString("hostname", "127.0.0.1");
            roomName = prefs.getString("roomName", "Room 1");

            if(mSocket.connected()) mSocket.disconnect();
            mSocket.connect();

            mSocket.on("playerJoined", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            pd.dismiss();
                            startLevelTimer();
                            gameManager();
                        }
                    });
                }
            });

            mSocket.on("result", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              pd.cancel();
                              JSONObject data = (JSONObject) args[0];
                              try{
                                  TextView lbl_Question = findViewById(R.id.lbl_Question);

                                  Integer res = data.getInt("result");

                                  Integer score = data.getInt("otherScore");


                                  if(res == 0) {
                                      lbl_Question.setText("You won! Opponent score: "  + score.toString());

                                  }
                                  else if(res == 1) {
                                      lbl_Question.setText("You lost! Opponent score: "  + score.toString());

                                  }
                                  else {
                                      lbl_Question.setText("Draw!");
                                  }

                                  pd.cancel();

                              }catch(Exception e) {

                              }

                          }
                    });
                }
            });

            mSocket.on("quizPack", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            JSONObject data = (JSONObject) args[0];
                            String question;
                            String imageURL;
                            answers.clear();
                            try {
                                imageURL = data.getString("imageURL");
                                nextCorrectAnswer = data.getInt("correctAnswer");
                                constructedQuestion = data.getString("question");


                                new DownloadCharImage().execute(imageURL);

                                Thread.sleep(500);

                                String toBeSplit = data.getString("answers");

                                toBeSplit = toBeSplit.substring(1, toBeSplit.length()-1);

                                String[] ans = toBeSplit.split(",");


                                for (int i = 0; i < ans.length; i++) {
                                    answers.add(ans[i]);
                                }



                                if (currNumRounds == 0)
                                {
                                    /*setNewQuestion(constructedQuestion, answers.get(0), answers.get(1), answers.get(2), answers.get(3));
                                    pd.dismiss();
                                    gameManager();*/

                                    correctAnswer = nextCorrectAnswer;
                                    ImageView i = findViewById(R.id.img_Image);
                                    i.setImageBitmap(imgBit);
                                    setNewQuestion(constructedQuestion, answers.get(0), answers.get(1), answers.get(2), answers.get(3));
                                    pd.dismiss();
                                    startLevelTimer();
                                    gameManager();
                                }
                                else                                 if(roundActive == false) gameManager();

                            } catch (Exception e) {
                                e.printStackTrace();
                                return;
                            }

                        }
                    });
                }
            });


            if(gameMode == 2){
                JSONObject myObj = new JSONObject();
                try{
                    myObj.put("roomName", roomName);
                    myObj.put("playerName", playerName);
                }catch (Exception e ) {e.printStackTrace();}

                mSocket.emit("connectRoom", myObj);
                pd = new ProgressDialog(this);
                pd.setMessage("Please Wait..");
                pd.show();
            }
            else if(gameMode == 3){
                try{
                    JSONObject myObj = new JSONObject();
                    myObj.put("createdBy", playerName);
                    myObj.put("roomName", roomName);

                    mSocket.emit("createRoom", myObj);
                }
                catch (Exception e) {e.printStackTrace();}
            }
        }

        Button btn_Joker = findViewById(R.id.btn_Joker);

        btn_Joker.setActivated(false);
        btn_Joker.setBackgroundTintList(btn_Joker.getResources().getColorStateList(R.color.disabled_background_color));
        btn_Joker.setTextColor(btn_Joker.getResources().getColorStateList(R.color.disabled_background_color));

        Button btn_Skip = findViewById(R.id.btn_Skip);

        btn_Skip.setActivated(false);
        btn_Skip.setBackgroundTintList(btn_Skip.getResources().getColorStateList(R.color.disabled_background_color));
        btn_Skip.setTextColor(btn_Skip.getResources().getColorStateList(R.color.disabled_background_color));

        if(gameMode != 2)
          new GetCharacterList().execute(WIKIAURL + "/api/v1/Articles/Top?expand=1&category=Characters&limit=" + CRAWLAMOUNT, "true");


        if(gameMode == 1) {
            gameManager();
        }
        else {
            pd.setMessage("Waiting for the Room...");
            pd.show();
        }

    }


    public void gameManager() {
        if(currNumRounds < maxNumRounds && !roundActive && currMistakes < maxMistakes) {
            if(jokerScore > 500) {
                Button btn_Joker = findViewById(R.id.btn_Joker);
                findViewById(R.id.btn_Joker).setActivated(true);
                btn_Joker.setBackgroundTintList(btn_Joker.getResources().getColorStateList(R.color.defaultGrey));
                btn_Joker.setTextColor(btn_Joker.getResources().getColorStateList(R.color.black));
            }
            if(skipScore > 600) {
                Button btn_Skip = findViewById(R.id.btn_Skip);
                findViewById(R.id.btn_Skip).setActivated(true);
                btn_Skip.setBackgroundTintList(btn_Skip.getResources().getColorStateList(R.color.defaultGrey));
                btn_Skip.setTextColor(btn_Skip.getResources().getColorStateList(R.color.black));
            }
            startNewRound();

        }
        else if(currMistakes >= maxMistakes || currNumRounds >= maxNumRounds) {
            displayGameover();
            gameover = true;

            final SharedPreferences prefs = this.getSharedPreferences("settings", 0);
            final SharedPreferences.Editor editor = prefs.edit();
            String playerName = prefs.getString("playerName", "name");


            Map<String, Long> leaderboard;

            leaderboard = readMap("leaderboard");

            long currTime = System.currentTimeMillis() / 1000L;

            leaderboard.put(playerName + Long.toString(currTime), playerScore);

            writeMap("leaderboard", leaderboard);
        }
    }

    public void writeMap(String filename, Map<String, Long> myObj){
        File directory = getFilesDir(); //or getExternalFilesDir(null); for external storage
        File file = new File(directory, filename);

        try {
            FileOutputStream fOut = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fOut);
            out.writeObject(myObj);
            out.close();
            fOut.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Long> readMap(String filename) {

        File directory = getFilesDir(); //or getExternalFilesDir(null); for external storage
        File file = new File(directory, filename);

        Map<String, Long> myObj = new HashMap();

        try {
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            myObj = (Map<String, Long>) in.readObject();
            in.close();
            fileIn.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return myObj;
    }

    public void startNewRound() {
        if(gameMode != 1 ) {
            pd.setMessage("Waiting for other client...");
            pd.show();
        }
        if(answers.size() != 4) { return; }
        pd.cancel();
        if(currNumRounds != 0) {
            setNewQuestion(constructedQuestion, answers.get(0), answers.get(1), answers.get(2), answers.get(3));
            answers.clear();
            correctAnswer = nextCorrectAnswer;
            startLevelTimer();
            if(gameMode == 1 || gameMode == 3)
              new GetCharacterList().execute(WIKIAURL + "/api/v1/Articles/Top?expand=1&category=Characters&limit=" + CRAWLAMOUNT, "false");

        }
        currNumRounds++;
        roundActive = true;
        resetButtons();

        Button btn_Answer1 = findViewById(R.id.btn_Answer1);
        Button btn_Answer2 = findViewById(R.id.btn_Answer2);
        Button btn_Answer3 = findViewById(R.id.btn_Answer3);
        Button btn_Answer4 = findViewById(R.id.btn_Answer4);
        Button btn_Joker = findViewById(R.id.btn_Joker);
        Button btn_Skip = findViewById(R.id.btn_Skip);


        btn_Skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(view.isActivated()) {
                    btn_Skip.setActivated(false);
                    btn_Skip.setBackgroundTintList(btn_Skip.getResources().getColorStateList(R.color.disabled_background_color));
                    btn_Skip.setTextColor(btn_Skip.getResources().getColorStateList(R.color.disabled_background_color));
                    skipScore = 0;
                    disableButtons();
                    startCooldownTimer();
                    countDownTimer.cancel();

                }
            }
        });

        btn_Joker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(view.isActivated()) {
                    btn_Joker.setActivated(false);
                    btn_Joker.setBackgroundTintList(btn_Joker.getResources().getColorStateList(R.color.disabled_background_color));
                    btn_Joker.setTextColor(btn_Joker.getResources().getColorStateList(R.color.disabled_background_color));
                    jokerScore = 0;
                    disableButtons();

                    int keepAlive;
                    do{
                        keepAlive = new Random().nextInt(4)+1;
                    }while(keepAlive == correctAnswer);

                    int reenable[] = {correctAnswer, keepAlive};
                    for(int i = 0; i <reenable.length; i++) {
                        switch (reenable[i]) {
                            case 1:
                                btn_Answer1.setEnabled(true);
                                break;
                            case 2:
                                btn_Answer2.setEnabled(true);
                                break;
                            case 3:
                                btn_Answer3.setEnabled(true);
                                break;
                            case 4:
                                btn_Answer4.setEnabled(true);
                                break;
                        }
                    }
                }
            }
        });

        btn_Answer1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(correctAnswer == 1) {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.green));
                    answeredCorrectly();
                }
                else {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.red));
                    answeredIncorrectly();
                }
            }
        });
        btn_Answer2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(correctAnswer == 2) {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.green));
                    answeredCorrectly();
                }
                else {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.red));
                    answeredIncorrectly();
                }
            }
        });
        btn_Answer3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(correctAnswer == 3) {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.green));
                    answeredCorrectly();
                }
                else {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.red));
                    answeredIncorrectly();
                }
            }
        });
        btn_Answer4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(correctAnswer == 4) {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.green));
                    answeredCorrectly();
                }
                else {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.red));
                    answeredIncorrectly();
                }
            }
        });
    }

    public void setNewQuestion(String question, String answer1, String answer2, String answer3, String answer4)
    {
        Button btn_Answer1 = findViewById(R.id.btn_Answer1);
        Button btn_Answer2 = findViewById(R.id.btn_Answer2);
        Button btn_Answer3 = findViewById(R.id.btn_Answer3);
        Button btn_Answer4 = findViewById(R.id.btn_Answer4);
        TextView lbl_Question = findViewById(R.id.lbl_Question);

        lbl_Question.setText(question);
        btn_Answer1.setText(answer1);
        btn_Answer2.setText(answer2);
        btn_Answer3.setText(answer3);
        btn_Answer4.setText(answer4);

        ImageView i = findViewById(R.id.img_Image);
        i.setImageBitmap(imgBit);

        myGoodMainChar = "";
    }

    public void answeredCorrectly() {
        countDownTimer.cancel();
        playerScore += timeLeft / 100;
        jokerScore += timeLeft / 100;
        skipScore += timeLeft / 100;
        setTitle("Score: " + Long.toString(playerScore));
        disableButtons();
        startCooldownTimer();
    }

    public void answeredIncorrectly() {
        //jokerScore = 0;
        currMistakes++;
        countDownTimer.cancel();
        showCorrectAnswer();
        disableButtons();
        startCooldownTimer();
    }

    public void enableButtons() {
        findViewById(R.id.btn_Answer1).setEnabled(true);
        findViewById(R.id.btn_Answer2).setEnabled(true);
        findViewById(R.id.btn_Answer3).setEnabled(true);
        findViewById(R.id.btn_Answer4).setEnabled(true);
    }

    public void disableButtons() {
        findViewById(R.id.btn_Answer1).setEnabled(false);
        findViewById(R.id.btn_Answer2).setEnabled(false);
        findViewById(R.id.btn_Answer3).setEnabled(false);
        findViewById(R.id.btn_Answer4).setEnabled(false);
    }

    public void displayGameover() {
        resetButtons();
        disableButtons();
        TextView lbl_Question = findViewById(R.id.lbl_Question);
        lbl_Question.setText("Game over !");
        lbl_Question.setTextSize(30.f);

        if(gameMode != 1) {
            pd.setMessage("Waiting for other player to finish!");
            pd.show();

            JSONObject myObj = new JSONObject();

            try{
                myObj.put("score", playerScore);
                myObj.put("roomName", roomName);
            }catch(Exception e) {e.printStackTrace();}


            mSocket.emit("gameover", myObj);
        }

    }

    public void resetButtons() {
        enableButtons();
        Button btn1 = findViewById(R.id.btn_Answer1);
        btn1.setBackgroundTintList(btn1.getResources().getColorStateList(R.color.defaultGrey));
        Button btn2 = findViewById(R.id.btn_Answer2);
        btn2.setBackgroundTintList(btn2.getResources().getColorStateList(R.color.defaultGrey));
        Button btn3 = findViewById(R.id.btn_Answer3);
        btn3.setBackgroundTintList(btn3.getResources().getColorStateList(R.color.defaultGrey));
        Button btn4 = findViewById(R.id.btn_Answer4);
        btn4.setBackgroundTintList(btn4.getResources().getColorStateList(R.color.defaultGrey));

    }

    public void showCorrectAnswer() {
        Button myBtn = null;
        switch(correctAnswer) {
        case 1:
            myBtn = findViewById(R.id.btn_Answer1);
            break;
        case 2:
            myBtn = findViewById(R.id.btn_Answer2);
            break;
        case 3:
            myBtn = findViewById(R.id.btn_Answer3);
            break;
        case 4:
            myBtn = findViewById(R.id.btn_Answer4);
            break;
        }
        if(myBtn != null)
            myBtn.setBackgroundTintList(myBtn.getResources().getColorStateList(R.color.green));
    }

    public void startLevelTimer() {
        countDownTimer = new CountDownTimer(timeToAnswer, 1000) {

            ProgressBar progressBar = findViewById(R.id.progressBar);

            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                TextView lbl_timeRemaining = findViewById(R.id.lbl_timeRemaining);
                lbl_timeRemaining.setText(Long.toString(millisUntilFinished / 1000));

                double remainingPercentage = ((double)timeToAnswer - millisUntilFinished) / timeToAnswer*100;
                progressBar.setProgress(100 - (int)remainingPercentage);
            }

            public void onFinish() {
                Log.d("Timer", "Timer ran out");
                progressBar.setProgress(0);
                answeredIncorrectly();
            }
        };

        countDownTimer.start();
    }

    public void startCooldownTimer() {
        roundCooldownTimer = new CountDownTimer(roundCooldown, 1000) {

            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                roundActive = false;
                gameManager();
            }
        };

        roundCooldownTimer.start();
    }

    private class GetCharacterList extends AsyncTask<String, Integer, Long> {
        Boolean firstTime = false;
        String imgurl;
        public String downloadAsString(String reqUrl) {
            StringBuilder sb = null;
            try{
                URL url = new URL(reqUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                // read the response
                InputStream in = new BufferedInputStream(conn.getInputStream());


                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                sb = new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }catch (Exception e) {e.printStackTrace();}
            return sb.toString();
        }

        public void largeLog(String tag, String content) {
            if (content.length() > 4000) {
                Log.d(tag, content.substring(0, 4000));
                largeLog(tag, content.substring(4000));
            } else {
                Log.d(tag, content);
            }
        }

        @Override
        protected Long doInBackground(String... params) {
            String response ="";
            firstTime = Boolean.parseBoolean(params[1]);
            try {
                String reqUrl = params[0];

                response = downloadAsString(reqUrl);

                JSONObject jsonObj = new JSONObject(response);
                JSONArray characters = jsonObj.getJSONArray("items");


                /*for(int i = 0; i < characters.length(); i++) {
                    JSONObject jj = characters.getJSONObject(i);
                    Log.d("item " + i, jj.toString());

                }*/

                myGoodMainChar = "";
                answers.clear();

                while(myGoodMainChar.isEmpty()) {

                    int randomChar = new Random().nextInt(characters.length());

                    JSONObject jj = characters.getJSONObject(randomChar);



                    try{
                        imgurl = jj.getString("thumbnail");
                        imgBit = BitmapFactory.decodeStream((InputStream)new URL(jj.getString("thumbnail")).getContent());
                    }catch(Exception e) {continue;}

                    String charName = jj.getString("title");
                    String charUrl = jj.getString("url");


                    String myResponse = downloadAsString(WIKIAURL + charUrl + "?action=raw");

                    myGoodTag = possibleTags.get(new Random().nextInt(possibleTags.size()));


                    String extractRes = extractTag(myGoodTag, myResponse);

                    //largeLog("extractRes", extractRes);
                    //Log.d("my Char : ", charName);


                    if(extractRes != ""  && !charsVisited.contains(charUrl)) {
                        myGoodMainChar = charName;

                        if((myGoodTag.equals("First Appearance") || myGoodTag.equals("Last Appearance")) && extractRes.charAt(1) != 'x') {
                            myResponse = downloadAsString(WIKIAURL + "/wiki/" + extractRes + "?action=raw");
                            String season = extractTag("season", myResponse);
                            String episode = extractTag("episode", myResponse);

                            Log.d("extraced episode:", season + "x" + episode);

                            String newTitle = season + "x" + episode;

                            if(newTitle.length() >= 3) {
                                answers.add(newTitle);
                            }
                            else  {
                                myGoodMainChar = "";
                                continue;
                            }

                        }
                        else {
                            answers.add(extractRes);
                        }

                        charsVisited.add(charUrl);

                    }
                }


                while(answers.size() < 4) {
                    int randomChar = new Random().nextInt(characters.length());

                    JSONObject jj = characters.getJSONObject(randomChar);

                    String charName = jj.getString("title");
                    String charUrl = jj.getString("url");

                    if(!charsVisited.contains(charUrl)) {
                        String myResponse = downloadAsString(WIKIAURL + charUrl + "?action=raw");

                        String extractRes = extractTag(myGoodTag, myResponse);

                        largeLog("extractRes", extractRes);
                        Log.d("my Char : ", charName);


                        if(extractRes != "" && extractRes != " " && extractRes != answers.get(0) && !answers.contains(extractRes)) {
                            answers.add(extractRes);
                            if((myGoodTag.equals("First Appearance") || myGoodTag.equals("Last Appearance")) && extractRes.charAt(1) != 'x') {
                                answers.remove(answers.size()-1);
                                myResponse = downloadAsString(WIKIAURL + "/wiki/" + extractRes + "?action=raw");
                                String season = extractTag("season", myResponse);
                                String episode = extractTag("episode", myResponse);

                                Log.d("extraced episode:", season + "x" + episode);

                                String newTitle = season + "x" + episode;

                                if(newTitle.length() >= 3 && !answers.contains(newTitle)) {
                                    answers.add(newTitle);
                                }
                                else continue;

                            }
                        }
                    }
                }

                Log.d("CharFinder", "My Good char is " + myGoodMainChar +  answers.toString() + "had to visit: " + charsVisited.toString());
                Log.d("All answers", answers.toString());

            }catch(Exception e) {
                largeLog("log", e.toString());
                e.printStackTrace();
                questionGeneratorReturn = -1;
            }

            return 1L;
        }


        public String extractTag(String tag, String input) {
            String s;
            s = input.substring(input.indexOf("|" + tag) + 1);
            s = s.substring(0, s.indexOf("\n"));


            Log.d("test1", s);

            //check if found actual result
            if(!input.substring(0, 5).equals(s.substring(0, 5)) && s.length() > tag.length() + " = ".length()) {
                s = s.substring(tag.length() + " = ".length());
                if(s.charAt(0) == '[') {
                    if(s.indexOf('|') != -1) {
                        s = s.substring(s.lastIndexOf('|') + 1);
                    }
                    else s = s.substring(s.lastIndexOf('[') + 1);
                    s = s.substring(0, s.indexOf(']'));
                }
                else if(s.charAt(0) == '{') {
                    if(s.indexOf('|') != -1) {
                        s = s.substring(s.lastIndexOf('|') + 1);
                    }
                    else s = s.substring(s.lastIndexOf('{') + 1);
                    s = s.substring(0, s.indexOf('}'));
                }
                else {
                    ArrayList<Integer> positions = new ArrayList<Integer>();
                    positions.add(s.indexOf("<"));
                    positions.add(s.indexOf("{"));
                    positions.add(s.indexOf("|"));

                    int smallest = Integer.MAX_VALUE;
                    if(positions.size() >= 1) {
                        for(int i = 0; i < positions.size(); i++) {
                            if(positions.get(i) < smallest && positions.get(i) != -1) {
                                smallest = positions.get(i);
                            }
                        }
                    }
                    if(smallest != Integer.MAX_VALUE)
                      s = s.substring(0, smallest);
                }
            }
            else {
                return "";
            }
            Log.d("test2", s);

            String myReturn = "";

            if(s.length() > 1) {
                myReturn = s;
                if(myReturn.charAt(myReturn.length()-1) == ' ') {
                    myReturn = myReturn.substring(0, myReturn.length()-1);
                }
            }
            else if (s.length() == 1 && (s.charAt(0) >= '0' && s.charAt(0) <= '9'))
                myReturn = s;

            Log.d("myReturn", myReturn);

            myReturn = myReturn.replace("\"", "");
            myReturn = myReturn.replace("{", "");
            myReturn = myReturn.replace("}", "");
            myReturn = myReturn.replace("[", "");
            myReturn = myReturn.replace("]", "");


            return myReturn;
        }

        public String constructQuestion() {
            String question = "";
            if(myGoodTag.equals("Aliases") || myGoodTag.equals("AKA")) {
                question = "What is an alias of " + myGoodMainChar + " ?";
            }
            else if(myGoodTag.equals("Age") || myGoodTag.equals("age")) {
                question = "What is the age of " + myGoodMainChar + " ?";
            }
            else if(myGoodTag.equals("Portrayed by") || myGoodTag.equals("actor")) {
                question = "By whom is " + myGoodMainChar + " portrayed?";
            }
            else if(myGoodTag.equals("First Appearance")) {
                question = "What is the first appearance of " + myGoodMainChar + " ?";
            }
            else if(myGoodTag.equals("Last Appearance")) {
                question = "What is the last appearance of " + myGoodMainChar + " ?";
            }
            else if(myGoodTag.equals("Status")) {
                question = "What is the status of " + myGoodMainChar + " ?";
            }
            return question;
        }

        protected void onPostExecute(Long result) {
            try{
                String myCorrectAnswer = answers.get(0);

                ArrayList<String> randList = (ArrayList<String>) answers.clone();
                Collections.shuffle(randList);

                answers = randList;

                nextCorrectAnswer = randList.indexOf(myCorrectAnswer)+1;

                Log.d("Correct answer is", Integer.toString(randList.indexOf(myCorrectAnswer)));

                constructedQuestion = constructQuestion();


                JSONObject myObj = new JSONObject();

                myObj.put("question", constructedQuestion);
                myObj.put("answers", answers);

                myObj.put("roomName", roomName);

                myObj.put("imageURL", imgurl);

                myObj.put("correctAnswer", nextCorrectAnswer);

                mSocket.emit("generatedQuiz", myObj);


                if(firstTime) {
                    correctAnswer = nextCorrectAnswer;
                    setNewQuestion(constructedQuestion, randList.get(0),randList.get(1),randList.get(2),randList.get(3));
                    if(gameMode == 1)
                       startLevelTimer();
                    new GetCharacterList().execute(WIKIAURL + "/api/v1/Articles/Top?expand=1&category=Characters&limit=" + CRAWLAMOUNT, "false");

                }
            }catch(Exception e) {
                new GetCharacterList().execute(WIKIAURL + "/api/v1/Articles/Top?expand=1&category=Characters&limit=" + CRAWLAMOUNT, "false");
            }
        }

    }
}
