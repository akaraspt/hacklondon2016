package com.whosthatguy.whosthatguy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

public class ListActivity extends AppCompatActivity {
    ImageButton bt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        Intent intent = getIntent();
        String result = intent.getStringExtra("cacheresult");
        Log.d("result", "result" + result);
        LinearLayout layout = (LinearLayout) findViewById(R.id.linearlayout);
        Bitmap fbBmp = BitmapFactory.decodeResource(this.getApplicationContext().getResources(), R.drawable.facebookicon);

        try {

            JSONArray arr = new JSONArray(result);
            JSONObject jObj = arr.getJSONObject(0);
            Log.d("jsonresult", jObj.toString());

        } catch (Throwable t) {
            Log.e("jsonresult", "Could not parse malformed JSON: \"" + result + "\"");
        }


        if(result.contains("success")) {

            bt = new ImageButton(this);
            bt.setImageBitmap(fbBmp);
            bt.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            layout.addView(bt);

            TextView tv = new TextView(this);
            tv.setText(result);
            tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            layout.addView(tv);
        }
        else
        {
            TextView tv = new TextView(this);
            tv.setText("No result" + result);
            tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            layout.addView(tv);
        }

    }

}
