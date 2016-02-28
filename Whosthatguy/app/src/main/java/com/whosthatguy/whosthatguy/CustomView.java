package com.whosthatguy.whosthatguy;


        import android.content.Context;
        import android.graphics.Bitmap;
        import android.graphics.BitmapFactory;
        import android.graphics.Canvas;
        import android.graphics.Paint;
        import android.graphics.Point;
        import android.util.AttributeSet;
        import android.view.View;

        import java.util.ArrayList;

public class CustomView extends View {
    private int posX;
    private int posY;

    private ArrayList<Point> point = new ArrayList<Point>();
    private ArrayList<Bitmap> imglist = new ArrayList<Bitmap>();
    private Bitmap leftEyeBmp;
    private Paint paint = new Paint();

    public CustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        leftEyeBmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.facebookicon);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(leftEyeBmp != null && !leftEyeBmp.isRecycled())leftEyeBmp.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for(int i=0;i< point.size();i++) {
           int px = point.get(i).x;
            int py = point.get(i).y;
            canvas.drawBitmap(leftEyeBmp, px, py, paint);
        }

    }
    public void clearPoints()
    {
        point.clear();
    }
    public int getPoint()
    {
        return point.size();
    }
    public void setPoints(int x, int y){
       point.add(new Point(x, y));
    }

}
