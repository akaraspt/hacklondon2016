package com.whosthatguy.whosthatguy;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;

public class ProfileActivity extends AppCompatActivity {
    TextView nameview;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    public String URLparam="";
    Bitmap imageBitmap;
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Intent intent = getIntent();
        MainActivity.isfirstpage=false;
        String fbid = intent.getStringExtra(MainActivity.FACEBOOK_ID);
        String fbfname = intent.getStringExtra(MainActivity.FACEBOOK_FNAME);
        String fblname = intent.getStringExtra(MainActivity.FACEBOOK_LNAME);
        nameview = (TextView) findViewById(R.id.nameView);
        nameview.setText(fbfname + " " + fblname);
        nameview.setGravity(Gravity.CENTER_HORIZONTAL);
        Log.d("profile", fbid);
        Log.d("profile", fbfname);
        Log.d("profile", fblname);

        Button explorebuttion = (Button)findViewById(R.id.explorebutton);
        explorebuttion.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent i = new Intent(getApplicationContext(), ExploreActivity.class);
                startActivity(i);
            }
        });

        Button takephotobut = (Button)findViewById(R.id.uploadbutton);
        takephotobut.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                dispatchTakePictureIntent();
            }
        });

        // get user photo
        // if nophoto
        ImageView mImageView = (ImageView)findViewById(R.id.imgView);
        Bitmap fbBmp= BitmapFactory.decodeResource(ProfileActivity.this.getApplicationContext().getResources(), R.drawable.app_icon2);
        mImageView.setImageBitmap(fbBmp);
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            ImageView mImageView = (ImageView)findViewById(R.id.imgView);
            mImageView.setImageBitmap(imageBitmap);
            // send photo to server

            Log.d("httprequest", "uploadphoto");
            UploadPhotoTask task = new UploadPhotoTask();
            String[] params = new String[1];
            URLparam = "http://104.46.48.140:5000/user/img/upload/"+MainActivity.fbid;
            Log.d("imagebyte",imageBitmap.toString());
            params[0] = "";
            task.execute(params);
        }
    }
    public static byte[] toByteArray (Bitmap raw) {

        byte[] byteArray = null;

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream ();
            raw.compress (Bitmap.CompressFormat.JPEG, 100, stream);
            byteArray = stream.toByteArray ();
        }
        catch (Exception e) {
            e.printStackTrace ();
        }

        return byteArray;
    }
    private class UploadPhotoTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            String json = null;
            HttpEntity sresponse;
            String jsonstr = "";


            byte[] imageBytes = toByteArray(imageBitmap);
            Log.d("imagebyte","l"+imageBytes.length);
            Log.d("imagebyte",imageBytes.toString());

            // The Username & Password
            // -----------------------
            try {
                HttpResponse response;
                Log.d("httprequest", "request upload");
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(URLparam);
                //String boundary = "-------------" + System.currentTimeMillis();
               // httpPost.setHeader("Content-type", "multipart/form-data; boundary=" + boundary);
                //ByteArrayBody bab = new ByteArrayBody(imageBytes, "pic.jpg");

                //StringBody sbOwner = new StringBody(StaticData.loggedUserId, ContentType.TEXT_PLAIN);
                //StringBody sbGroup = new StringBody("group", ContentType.TEXT_PLAIN);
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                builder.addBinaryBody("file", imageBytes, ContentType.create("image/jpeg"), "file.jpg");

                HttpEntity entity = builder.build();
                httpPost.setEntity(entity);

                response = httpClient.execute(httpPost);
                sresponse = response.getEntity();
                jsonstr = EntityUtils.toString(sresponse);
                Log.d("httprequest", "result" + jsonstr);
            } catch (Exception e) {
                Log.d("httprequest", "error " + e.getLocalizedMessage());
            } finally {
        /* nothing to do here */
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return jsonstr;
        }

        @Override
        protected void onPostExecute(String result) {
            // result is what you got from your connection
            if(result.contains("success")) {
                // dummy
            }
            else {
                // couldn't detect face please upload your face
            }
        }
    }




}
