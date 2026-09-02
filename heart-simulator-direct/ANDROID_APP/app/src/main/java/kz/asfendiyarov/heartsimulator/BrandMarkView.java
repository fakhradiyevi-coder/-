package kz.asfendiyarov.heartsimulator;

import android.content.*;
import android.graphics.*;
import android.view.*;

public class BrandMarkView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    public BrandMarkView(Context c){ super(c); }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float w=getWidth(),h=getHeight(),cx=w/2f,cy=h/2f;
        p.setStyle(Paint.Style.FILL); p.setColor(Color.rgb(199,164,91)); c.drawCircle(cx,cy,Math.min(w,h)*.46f,p);
        p.setColor(Color.rgb(92,42,115)); c.drawCircle(cx,cy,Math.min(w,h)*.39f,p);
        p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));
        p.setColor(Color.WHITE); p.setTextSize(Math.min(w,h)*.26f); c.drawText("AU",cx,cy-Math.min(w,h)*.01f,p);
        p.setTextSize(Math.min(w,h)*.13f); p.setColor(Color.rgb(231,211,165)); c.drawText("1930",cx,cy+Math.min(w,h)*.22f,p);
    }
}
