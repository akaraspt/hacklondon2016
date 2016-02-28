package com.whosthatguy.whosthatguy;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.CallbackManager;
import android.content.Intent;
import android.widget.ImageView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

public class MainActivity extends AppCompatActivity {
    CallbackManager callbackManager;
    LoginButton loginButton;
    public final static String FACEBOOK_ID = "com.whosthatguy.whosthatguy.FACEBOOK_ID";
    public final static String FACEBOOK_FNAME = "com.whosthatguy.whosthatguy.FACEBOOK_FNAME";
    public final static String FACEBOOK_LNAME = "com.whosthatguy.whosthatguy.FACEBOOK_LNAME";
    public static String fbid,fbfname,fblname,fbfriendlist;
    public static Boolean isfirstpage=true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("user_friends");
        Log.d("facebook", "test");
        ImageView mImageView = (ImageView)findViewById(R.id.imageView);
        Bitmap fbBmp= BitmapFactory.decodeResource(MainActivity.this.getApplicationContext().getResources(), R.drawable.app_icon2);
        mImageView.setImageBitmap(fbBmp);

        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null &&isfirstpage) {
            // redirect to profile page
            fbid = Profile.getCurrentProfile().getId();
            fbfname = Profile.getCurrentProfile().getFirstName();
            fblname = Profile.getCurrentProfile().getLastName();
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            intent.putExtra(FACEBOOK_ID, fbid);
            intent.putExtra(FACEBOOK_FNAME, fbfname);
            intent.putExtra(FACEBOOK_LNAME, fblname);
            startActivity(intent);
        }

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                Log.d("facebook", "fbid"+ Profile.getCurrentProfile().getId());

                fbid = Profile.getCurrentProfile().getId();
                fbfname = Profile.getCurrentProfile().getFirstName();
                fblname = Profile.getCurrentProfile().getLastName();


                /* make the API call */
                new GraphRequest(
                        AccessToken.getCurrentAccessToken(),
                        "/"+fbid+"/friendlists",
                        null,
                        HttpMethod.GET,
                        new GraphRequest.Callback() {
                            public void onCompleted(GraphResponse response) {
            /* handle the result */
                                Log.d("facebook",response.toString());
                            }
                        }
                ).executeAsync();

                ConnectionTask task = new ConnectionTask();
                String[] params = new String[1];
                params[0] = "http://104.46.48.140:5000/user/"+fbid;
                task.execute(params);

            }

            @Override
            public void onCancel() {
                Log.d("facebook", "cancel facebook");
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                Log.d("facebook", "error facebook");
                // App code
            }
        });

    }
    private class ConnectionTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            URL url;
            String message="";
            HttpURLConnection urlConnection = null;
            Log.d("httprequest", "startcheckuser ");
            try {
                url = new URL(urls[0]);

                urlConnection = (HttpURLConnection) url
                        .openConnection();

                InputStream in = urlConnection.getInputStream();

                InputStreamReader isw = new InputStreamReader(in);

                int data = isw.read();
                while (data != -1) {
                    char current = (char) data;
                    data = isw.read();
                    message+=""+ current;
                }
                Log.d("httprequest",message);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return message;
        }

        @Override
        protected void onPostExecute(String result) {
            // result is what you got from your connection
            Log.d("httprequest", "result " + result);
            if(result.contains("success")) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.putExtra(FACEBOOK_ID, fbid);
                intent.putExtra(FACEBOOK_FNAME, fbfname);
                intent.putExtra(FACEBOOK_LNAME, fblname);
                startActivity(intent);
            }
            else {
                //register
                Log.d("httprequest","register");
                RegisterTask task = new RegisterTask();
                String[] params = new String[4];
                params[0] = "http://104.46.48.140:5000/user/add";
                params[1] = fbid;
                params[2] = fbfname+" "+fblname;
                params[3] = "";
                task.execute(params);

            }

        }

    }

    private class RegisterTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            HttpURLConnection urlConnection=null;
            String json = null;
            HttpEntity sresponse;
            String jsonstr ="";
            // The Username & Password
            // -----------------------
            try {
                HttpResponse response;
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("facebook_id", urls[1]);
                jsonObject.accumulate("name", urls[2]);
                jsonObject.accumulate("friends", "");
                json = jsonObject.toString();
                Log.d("httprequest","request register");
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(urls[0]);
                httpPost.setEntity(new StringEntity(json, "UTF-8"));
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Accept-Encoding", "application/json");
                httpPost.setHeader("Accept-Language", "en-US");
                response = httpClient.execute(httpPost);
                sresponse = response.getEntity();
                jsonstr = EntityUtils.toString(sresponse);

                Log.d("httprequest","result"+jsonstr);
            }
            catch (Exception e) {

                Log.d("httprequest", "error "+e.getLocalizedMessage());

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
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.putExtra(FACEBOOK_ID, fbid);
                intent.putExtra(FACEBOOK_FNAME, fbfname);
                intent.putExtra(FACEBOOK_LNAME, fblname);
                startActivity(intent);
            }
        }

    }

    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    protected void onResume() {
        super.onResume();

        // Logs 'install' and 'app activate' App Events.
       //AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Logs 'app deactivate' App Event.
       // AppEventsLogger.deactivateApp(this);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
