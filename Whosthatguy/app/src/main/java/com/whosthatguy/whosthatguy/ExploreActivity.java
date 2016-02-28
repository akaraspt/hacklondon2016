package com.whosthatguy.whosthatguy;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;

public class ExploreActivity extends AppCompatActivity {
    private Camera mCamera;
    private SurfaceHolder.Callback cameraSurfaceHolderCallbacks;
    private int ScreenWidth;
    private int ScreenHeight;
    RelativeLayout layout;
    private CustomView myCustomView;
    Bitmap fbBmp;
    Boolean processingphoto;
    String URLparam;
    byte[] imageBytes;
    byte[] newImageByteArray;
    Bitmap bitmap;
    int loop =0;
    public String cacheresult="";
    Boolean showfblogo =false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);
        Intent intent = getIntent();
        mCamera = getCameraInstance();
        fbBmp = BitmapFactory.decodeResource(ExploreActivity.this.getApplicationContext().getResources(), R.drawable.facebookicon);
        myCustomView = (CustomView)findViewById(R.id.myCustomView);
        layout = (RelativeLayout) findViewById(R.id.mylayout);
        Display display = getWindowManager().getDefaultDisplay();
        ScreenHeight = display.getHeight() ;
        ScreenWidth = display.getWidth() ;
        processingphoto=false;

        Button detbuttion = (Button)findViewById(R.id.detailbutton);
        detbuttion.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Log.d("sendimg", "sendimg");
                Intent i = new Intent(ExploreActivity.this.getApplicationContext(), ListActivity.class);
                i.putExtra("cacheresult", cacheresult);
                startActivity(i);
            }
        });
        //First get a reference to the SurfaceView displaying the camera preview
        SurfaceView cameraSurface = (SurfaceView) findViewById(R.id.surfaceView);

        SurfaceHolder cameraSurfaceHolder = cameraSurface.getHolder();

        cameraSurfaceHolderCallbacks = new SurfaceHolder.Callback() {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if(mCamera == null)return;
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                try {
                    //mCamera = Camera.open();
                    Log.d("camera", "cameraopen");
                    mCamera.setPreviewDisplay(holder);
                    Log.d("camera", "setpreview");
                    mCamera.startFaceDetection();
                    mCamera.setFaceDetectionListener(faceDetectionListener);
                    Log.d("camera", "detection");
                } catch (Exception e) {
                    Log.d("camera", "error"+e.getMessage());
                    if(mCamera == null)return;
                    mCamera.release();
                    mCamera = null;
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
               //Camera.Parameters cameraParameters = mCamera.getParameters();
               // List<Size> localSizes = mCamera.getParameters().getSupportedPreviewSizes();
                //cameraParameters.setPreviewSize(320, 240);
                //cameraParameters.setPictureSize(320, 240);
                //int avrgExposure = (cameraParameters.getMinExposureCompensation() + cameraParameters.getMaxExposureCompensation())/2;
                //cameraParameters.setExposureCompensation(avrgExposure);
                //mCamera.setParameters(cameraParameters);
                Camera.Parameters parameters = mCamera.getParameters();
                Display display = getWindowManager().getDefaultDisplay();
                int cheight =display.getHeight();
                int cwidth =display.getWidth();

                Log.d("camwh","height "+cheight+" width "+cwidth);
                if(display.getRotation() == Surface.ROTATION_0)
                {
                    parameters.setRotation(90);
                    parameters.setPreviewSize(cheight, cwidth);
                    mCamera.setDisplayOrientation(90);
                }

                if(display.getRotation() == Surface.ROTATION_90)
                {
                    parameters.setPreviewSize(cwidth, cheight);
                }

                if(display.getRotation() == Surface.ROTATION_180)
                {
                    parameters.setPreviewSize(cheight, cwidth);
                }

                if(display.getRotation() == Surface.ROTATION_270)
                {
                    parameters.setPreviewSize(cwidth, cheight);
                    mCamera.setDisplayOrientation(180);
                }


                mCamera.setParameters(parameters);
                mCamera.startPreview();
                //mCamera.takePicture(null, null, new HandlePictureStorage());
            }
        };
        Log.d("camera", "cameratest");
        cameraSurfaceHolder.addCallback(cameraSurfaceHolderCallbacks);
    }
    private Camera.FaceDetectionListener faceDetectionListener = new Camera.FaceDetectionListener() {

        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            //Faces have been detected...
            Log.d("facedetected", "facedetected");

            if(showfblogo)
            {
                loop++;
                myCustomView.clearPoints();
            for(int i = 0 ; faces!=null &&i < faces.length ; i++){
                Log.d("facedetected", "facedetected" + faces[i].rect.centerX() + " " + faces[i].rect.centerY());

                int camX = faces[i].rect.top;
                int camY = faces[i].rect.left;

                int posX = (int)((camX + 1000) * ScreenWidth / 2000.0);
                int posY = (int)((camY+1000) *ScreenHeight /2000.0);

                Log.d("facedetected", "facedetected" + posX + " Y: " + posY);

                myCustomView.setPoints(posX, posY);

            }
            myCustomView.invalidate();


                if(myCustomView.getPoint()==0 &&loop>1000 )
                {
                    showfblogo=false;
                }
            }

            if(!processingphoto && showfblogo==false) {
                if(faces!=null&& faces.length>0) {
                    try {
                        mCamera.takePicture(null, null,
                                new HandlePictureStorage());
                    }
                    catch(Exception e){
                        Log.d("takephoto","error "+e.getMessage());
                    }
                }
            }

        }
    };

    private class HandlePictureStorage implements Camera.PictureCallback {
        @Override
        public void onPictureTaken(byte[] data, final Camera camera) {
            Log.d("takephoto", "takephoto"+data.toString());
            processingphoto=true;
            myCustomView.clearPoints();
            myCustomView.invalidate();
            mCamera.startPreview();

            try{
                Log.d("httprequest", "uploadphoto");
                FaceDetectTask task = new FaceDetectTask();
                newImageByteArray = data;
                String[] params = new String[1];
                URLparam = "http://104.46.48.140:5000/user/detect/"+MainActivity.fbid;
                params[0] = "";
                task.execute(params);


            }catch(Exception e)
            {

            }

            // send to server
            //  get return value and draw facebook icon link

            /*
                 int posX =;
                int posY = ;
                myCustomView.setPoints(posX, posY);

            */
            // myCustomView.invalidate();

        }
    }
    public Bitmap rotateImage(int angle, Bitmap bitmapSrc) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmapSrc, 0, 0,
                bitmapSrc.getWidth(), bitmapSrc.getHeight(), matrix, true);
    }

    private class FaceDetectTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            String json = null;
            HttpEntity sresponse;
            String jsonstr = "";


            // The Username & Password
            // -----------------------
            try {

                imageBytes=newImageByteArray;

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
                processingphoto=false;
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
            processingphoto=false;
            loop=0;
            try{
                mCamera.startFaceDetection();
            }
            catch(Exception e)
            {

            }

            if(result.contains("success")) {
                // dummy
                cacheresult = result;
                showfblogo =true;
            }
            else {
                // couldn't detect face please upload your face
            }
        }
    }


    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
            Log.d("camera", "cameraopen1");
        }
        catch (Exception e){
            Log.d("camera", "cameraerror1"+e.getMessage());
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }


}
