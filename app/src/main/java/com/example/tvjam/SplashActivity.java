package com.example.tvjam;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Intent intent = this.getIntent();

        GetCharacterList g = new GetCharacterList(this);
        g.WIKIAURL = intent.getStringExtra("WIKIAURL");
        g.execute();

    }

    private class GetCharacterList extends AsyncTask<String, Integer, Long> {
        Bitmap imgBit;

        ArrayList<String> possibleTags = new ArrayList<String>();
        String myGoodMainChar = "";
        String myGoodTag = "Aliases";

        ArrayList<String> answers = new ArrayList<String>();
        ArrayList<String> charsVisited = new ArrayList<String>();

        private Context context;

        String WIKIAURL;

        public GetCharacterList(Context context){
            this.context=context;
        }

        public String downloadAsString(String reqUrl) {
            StringBuilder sb = null;
            try {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            String response = "";
            try {
                String reqUrl = params[0];

                response = downloadAsString(reqUrl);

                JSONObject jsonObj = new JSONObject(response);
                JSONArray characters = jsonObj.getJSONArray("items");


                /*for(int i = 0; i < characters.length(); i++) {
                    JSONObject jj = characters.getJSONObject(i);
                    Log.d("item " + i, jj.toString());

                }*/


                while (myGoodMainChar.isEmpty()) {

                    int randomChar = new Random().nextInt(characters.length());

                    JSONObject jj = characters.getJSONObject(randomChar);

                    imgBit = BitmapFactory.decodeStream((InputStream) new URL(jj.getString("thumbnail")).getContent());

                    String charName = jj.getString("title");
                    String charUrl = jj.getString("url");


                    String myResponse = downloadAsString(WIKIAURL + charUrl + "?action=raw");


                    possibleTags.add("Portrayed by");
                    String extractRes = extractTag(myGoodTag, myResponse);

                    //largeLog("myResposne", myResponse);
                    Log.d("my Char : ", charName);


                    if (extractRes != "") {
                        myGoodMainChar = charName;
                        answers.add(extractRes);
                    }
                    charsVisited.add(charUrl);
                }

                while (answers.size() < 4) {
                    int randomChar = new Random().nextInt(characters.length());

                    JSONObject jj = characters.getJSONObject(randomChar);

                    String charName = jj.getString("title");
                    String charUrl = jj.getString("url");

                    if (!charsVisited.contains(charUrl)) {
                        String myResponse = downloadAsString(WIKIAURL + charUrl + "?action=raw");

                        String extractRes = extractTag(myGoodTag, myResponse);

                        //largeLog("myResposne", myResponse);
                        Log.d("my Char : ", charName);


                        if (extractRes != "") {
                            answers.add(extractRes);
                        }
                        charsVisited.add(charUrl);
                    }
                }

                Log.d("CharFinder", "My Good char is " + myGoodMainChar + answers.toString() + "had to visit: " + charsVisited.toString());
                Log.d("All answers", answers.toString());

            } catch (Exception e) {
                largeLog("log", e.toString());
                e.printStackTrace();
            }

            return 1L;
        }


        public String extractTag(String tag, String input) {
            String s;
            s = input.substring(input.indexOf("|" + tag) + 1);
            s = s.substring(0, s.indexOf("\n"));


            Log.d("test1", s);

            //check if found actual result
            if (!input.substring(0, 10).equals(s.substring(0, 10))) {
                s = s.substring(tag.length() + " = ".length());
                ArrayList<Integer> positions = new ArrayList<Integer>();
                positions.add(s.indexOf("<"));
                positions.add(s.indexOf("{"));

                int smallest = Integer.MAX_VALUE;
                if (positions.size() >= 1) {
                    for (int i = 0; i < positions.size(); i++) {
                        if (positions.get(i) < smallest && positions.get(i) != -1) {
                            smallest = positions.get(i);
                        }
                    }
                }
                if (smallest != Integer.MAX_VALUE)
                    s = s.substring(0, smallest);

            } else {
                return "";
            }
            Log.d("test2", s);

            String myReturn = "";

            if (s.length() > 0) {
                myReturn = s;
            }

            Log.d("myReturn", myReturn);


            return myReturn;
        }

        public String constructQuestion() {
            String question = "";
            if (myGoodTag == "Aliases") {
                question = "What is an alias of " + myGoodMainChar + " ?";
            }
            return question;
        }

        protected void onPostExecute(Long result) {
            /*ImageView i = findViewById(R.id.img_Image);
            i.setImageBitmap(imgBit);

            ((TextView) findViewById(R.id.lbl_Question)).setText(constructQuestion());

            String myCorrectAnswer = answers.get(0);

            ArrayList<String> randList = (ArrayList<String>) answers.clone();
            Collections.shuffle(randList);

            int correctAnswer = randList.indexOf(myCorrectAnswer) + 1;

            Log.d("Correct answer is", Integer.toString(randList.indexOf(myCorrectAnswer)));

            ((Button) findViewById(R.id.btn_Answer1)).setText(randList.get(0));
            ((Button) findViewById(R.id.btn_Answer2)).setText(randList.get(1));
            ((Button) findViewById(R.id.btn_Answer3)).setText(randList.get(2));
            ((Button) findViewById(R.id.btn_Answer4)).setText(randList.get(3));
*/

            Intent intent = ((Activity)context).getIntent();
            intent.putExtra("correctAnswer", 1);
            ((Activity)context).finish();
        }
    }
}
