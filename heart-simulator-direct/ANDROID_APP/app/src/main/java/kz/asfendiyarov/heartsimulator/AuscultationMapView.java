package kz.asfendiyarov.heartsimulator;

import android.content.*;
import android.graphics.*;
import android.view.*;

public class AuscultationMapView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private DeviceState state;
    private final String[] names={"Аортальная","Лёгочная","Эрба","Трикуспидальная","Митральная"};
    private final float[][] pts={{0.42f,0.27f},{0.58f,0.27f},{0.575f,0.38f},{0.56f,0.58f},{0.68f,0.67f}};

    public AuscultationMapView(Context c){ super(c); setMinimumHeight(dp(360)); }
    public void setState(DeviceState s){ state=s; invalidate(); }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float w=getWidth(), h=getHeight();
        p.setStyle(Paint.Style.FILL); p.setColor(Color.rgb(248,246,250)); c.drawRoundRect(0,0,w,h,dp(22),dp(22),p);

        Path torso=new Path();
        torso.moveTo(w*.34f,h*.10f); torso.cubicTo(w*.20f,h*.20f,w*.19f,h*.50f,w*.28f,h*.88f);
        torso.lineTo(w*.72f,h*.88f); torso.cubicTo(w*.81f,h*.50f,w*.80f,h*.20f,w*.66f,h*.10f);
        torso.cubicTo(w*.60f,h*.17f,w*.55f,h*.19f,w*.50f,h*.19f);
        torso.cubicTo(w*.45f,h*.19f,w*.40f,h*.17f,w*.34f,h*.10f); torso.close();
        p.setColor(Color.rgb(239,233,243)); c.drawPath(torso,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(Color.rgb(211,199,218)); c.drawPath(torso,p);
        p.setStrokeWidth(dp(4)); p.setColor(Color.rgb(220,211,225)); c.drawLine(w*.50f,h*.21f,w*.50f,h*.80f,p);

        for(int i=0;i<5;i++){
            int color=Color.rgb(158,148,164);
            if(state!=null && state.track>0){
                if(i<state.modules.size()){
                    ModuleState m=state.modules.get(i);
                    if(m.trackMatch) color=Color.rgb(48,151,83);
                    else if(m.feedbackOk) color=Color.rgb(211,163,45);
                    else if(m.checked) color=Color.rgb(84,118,188);
                    else color=Color.rgb(158,148,164);
                }else color=Color.rgb(84,118,188);
            }
            float x=w*pts[i][0], y=h*pts[i][1];
            p.setStyle(Paint.Style.FILL); p.setColor(Color.WHITE); c.drawCircle(x,y,dp(14),p);
            p.setColor(color); c.drawCircle(x,y,dp(10),p);
            p.setTextSize(sp(12)); p.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));
            p.setColor(Color.rgb(63,52,68));
            float tx = (i==0) ? x-dp(115) : x+dp(17);
            float ty = y+dp(4);
            c.drawText(names[i], tx, ty, p);
        }
    }

    private int dp(float v){ return Math.round(v*getResources().getDisplayMetrics().density); }
    private float sp(float v){ return v*getResources().getDisplayMetrics().scaledDensity; }
}
