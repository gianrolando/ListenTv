package it.gianrolando.listentv;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Small, consistently weighted icons drawn at any display density. */
final class UiIcon extends Drawable {
    static final int PLAY=0, PAUSE=1, STOP=2, STAR=3, SEARCH=4, MORE=5, HEADPHONES=6, PLUS=7, WAVE=8;
    private final int type;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    UiIcon(int type,int color) { this.type=type; paint.setColor(color); }
    @Override public void draw(Canvas canvas) {
        Rect b=getBounds(); canvas.save(); canvas.translate(b.left,b.top); canvas.scale(b.width()/24f,b.height()/24f);
        paint.setStrokeWidth(1.8f); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND); paint.setStyle(Paint.Style.STROKE);
        Path p=new Path();
        switch(type) {
            case PLAY -> { paint.setStyle(Paint.Style.FILL); p.moveTo(8,5);p.lineTo(20,12);p.lineTo(8,19);p.close();canvas.drawPath(p,paint); }
            case PAUSE -> { paint.setStyle(Paint.Style.FILL);canvas.drawRoundRect(6,5,10,19,1,1,paint);canvas.drawRoundRect(14,5,18,19,1,1,paint); }
            case STOP -> canvas.drawRoundRect(6,6,18,18,3,3,paint);
            case STAR -> { for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float r=i%2==0?9:4.3f;float x=12+(float)Math.cos(a)*r,y=12+(float)Math.sin(a)*r;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}p.close();canvas.drawPath(p,paint); }
            case SEARCH -> { canvas.drawCircle(10.5f,10.5f,6.5f,paint);canvas.drawLine(15.5f,15.5f,21,21,paint); }
            case MORE -> { paint.setStyle(Paint.Style.FILL);for(int i=0;i<3;i++)canvas.drawCircle(12,5+i*7,1.5f,paint); }
            case PLUS -> { canvas.drawLine(12,5,12,19,paint);canvas.drawLine(5,12,19,12,paint); }
            case HEADPHONES -> { canvas.drawArc(4,3,20,20,180,180,false,paint);canvas.drawRoundRect(4,11,8,20,2,2,paint);canvas.drawRoundRect(16,11,20,20,2,2,paint); }
            case WAVE -> { canvas.drawLine(4,10,4,14,paint);canvas.drawLine(8,6,8,18,paint);canvas.drawLine(12,3,12,21,paint);canvas.drawLine(16,7,16,17,paint);canvas.drawLine(20,10,20,14,paint); }
            default -> { }
        }
        canvas.restore();
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha);invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter);invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
