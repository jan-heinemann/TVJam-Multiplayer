package com.example.tvjam;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class LeaderboardActivity extends AppCompatActivity {


    private class Entry {
        String name;
        long score;
    }

    private class LeaderboardAdapter extends BaseAdapter {

        private Context context;
        private ArrayList<Entry> leaderboardModelArrayList;

        public LeaderboardAdapter(Context context, ArrayList<Entry> leaderboardModelArrayList) {

            this.context = context;
            this.leaderboardModelArrayList = leaderboardModelArrayList;
        }

        @Override
        public int getViewTypeCount() {
            return getCount();
        }
        @Override
        public int getItemViewType(int position) {

            return position;
        }

        @Override
        public int getCount() {
            return leaderboardModelArrayList.size();
        }

        @Override
        public Object getItem(int position) {
            return leaderboardModelArrayList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_item, null, true);

                holder.tvProduct = (TextView) convertView.findViewById(R.id.tvProduct);
                holder.tvQty = (TextView) convertView.findViewById(R.id.tvQty);

                convertView.setTag(holder);
            }else {
                // the getTag returns the viewHolder object set as a tag to the view
                holder = (ViewHolder)convertView.getTag();
            }

            holder.tvProduct.setText(leaderboardModelArrayList.get(position).name);
            holder.tvQty.setText(String.valueOf(leaderboardModelArrayList.get(position).score));

            return convertView;
        }

        private class ViewHolder {

            protected TextView tvProduct, tvQty;

        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        setTitle("Leaderboard");

        Map<String, Long> leaderboard = readMap("leaderboard");

        Map<String, Long> sorted = leaderboard
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                LinkedHashMap::new));



        ListView listView = findViewById(R.id.listView);

        //leaderboardModelArrayList = new ArrayList<>();
        ArrayList<Entry> leaderboardModelArrayList = populateList(sorted);

        LeaderboardAdapter LeaderboardAdapter = new LeaderboardAdapter(this,leaderboardModelArrayList);
        listView.setAdapter(LeaderboardAdapter);
    }

    private ArrayList<Entry> populateList(Map<String, Long> myObj){
        ArrayList<Entry> list = new ArrayList<>();

        for(Map.Entry<String, Long> entry : myObj.entrySet()) {
            Entry listEntry = new Entry();
            listEntry.name = entry.getKey();
            if(listEntry.name.length() > 10) listEntry.name = listEntry.name.substring(0, listEntry.name.length()-10);
            listEntry.score = entry.getValue();
            list.add(listEntry);
        }
        return list;
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
}
