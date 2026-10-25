package it.gianrolando.listentv;

import android.content.Context;
import android.graphics.*;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;

final class ChannelArtwork {
    private static final ConcurrentHashMap<String,byte[]> CACHE=new ConcurrentHashMap<>();
    static int resource(String id) {
        return switch(id) {
            case "rai1" -> R.drawable.logo_rai1;
            case "rai2" -> R.drawable.logo_rai2;
            case "rai3" -> R.drawable.logo_rai3;
            case "rainews" -> R.drawable.logo_rainews;
            case "rete4" -> R.drawable.logo_rete4;
            case "canale5" -> R.drawable.logo_canale5;
            case "italia1" -> R.drawable.logo_italia1;
            case "tgcom24" -> R.drawable.logo_tgcom24;
            case "mediaset20" -> R.drawable.logo_mediaset20;
            case "iris" -> R.drawable.logo_iris;
            case "la5" -> R.drawable.logo_la5;
            case "mediasetextra" -> R.drawable.logo_mediasetextra;
            case "focus" -> R.drawable.logo_focus;
            case "topcrime" -> R.drawable.logo_topcrime;
            case "italia2" -> R.drawable.logo_italia2;
            case "cine34" -> R.drawable.logo_cine34;
            case "twentyseven" -> R.drawable.logo_twentyseven;
            case "boing" -> R.drawable.logo_boing;
            case "cartoonito" -> R.drawable.logo_cartoonito;
            case "skytg24" -> R.drawable.logo_skytg24;
            case "cielo" -> R.drawable.logo_cielo;
            case "tv8" -> R.drawable.logo_tv8;
            case "la7" -> R.drawable.logo_la7;
            case "tv2000" -> R.drawable.logo_tv2000;
            default -> 0;
        };
    }
    static int background(String id) {
        return switch(id) {
            case "skytg24","cielo","tv8","tv2000" -> Color.rgb(243,246,251);
            default -> Color.rgb(33,46,65);
        };
    }
    static byte[] data(Context context,String id) {
        int res=resource(id); if(res==0) return null;
        return CACHE.computeIfAbsent(id,key-> {
            Bitmap source=BitmapFactory.decodeResource(context.getResources(),res);
            if(source==null) return null;
            Bitmap output=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(output);canvas.drawColor(background(id));
            float scale=Math.min(192f/source.getWidth(),160f/source.getHeight());
            float width=source.getWidth()*scale,height=source.getHeight()*scale;
            canvas.drawBitmap(source,null,new RectF((256-width)/2,(256-height)/2,(256+width)/2,(256+height)/2),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();output.compress(Bitmap.CompressFormat.PNG,100,bytes);
            source.recycle();output.recycle();return bytes.toByteArray();
        });
    }
}
