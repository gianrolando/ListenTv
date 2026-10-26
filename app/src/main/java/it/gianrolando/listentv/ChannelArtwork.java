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
            case "raisport" -> R.drawable.logo_raisport;
            case "rairadio2" -> R.drawable.logo_rairadio2;
            case "rai4k" -> R.drawable.logo_rai4k;
            case "rai4" -> R.drawable.logo_rai4;
            case "rai5" -> R.drawable.logo_rai5;
            case "raimovie" -> R.drawable.logo_raimovie;
            case "raipremium" -> R.drawable.logo_raipremium;
            case "raistoria" -> R.drawable.logo_raistoria;
            case "italiana" -> R.drawable.logo_italiana;
            case "raiyoyo" -> R.drawable.logo_raiyoyo;
            case "raigulp" -> R.drawable.logo_raigulp;
            case "nove" -> R.drawable.logo_nove;
            case "realtime" -> R.drawable.logo_realtime;
            case "dmax" -> R.drawable.logo_dmax;
            case "giallo" -> R.drawable.logo_giallo;
            case "foodnetwork" -> R.drawable.logo_foodnetwork;
            case "hgtv" -> R.drawable.logo_hgtv;
            case "discovery" -> R.drawable.logo_discovery;
            case "discoveryturbo" -> R.drawable.logo_discoveryturbo;
            case "warnertv" -> R.drawable.logo_warnertv;
            case "k2" -> R.drawable.logo_k2;
            case "frisbee" -> R.drawable.logo_frisbee;
            default -> 0;
        };
    }
    static int background(String id) {
        return switch(id) {
            case "rai1","rai2","rai3","rainews","raisport","rairadio2","rai4k","rai4","rai5","raimovie","raipremium","raistoria","raiyoyo","raigulp" -> Color.rgb(243,246,251);
            case "italiana" -> Color.rgb(5,50,93);
            case "skytg24","cielo","tv8","tv2000","rete4","canale5","italia1","tgcom24","mediaset20","iris","la5","mediasetextra","focus","topcrime","italia2","cine34","twentyseven","boing","cartoonito" -> Color.rgb(243,246,251);
            default -> Color.rgb(33,46,65);
        };
    }
    static byte[] data(Context context,String id) {
        int res=resource(id); if(res==0) return null;
        return CACHE.computeIfAbsent(id,key-> {
            Bitmap source=BitmapFactory.decodeResource(context.getResources(),res);
            if(source==null) return null;
            Bitmap output=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(output);canvas.drawColor(background(id));
            float scale=Math.min(448f/source.getWidth(),384f/source.getHeight());
            float width=source.getWidth()*scale,height=source.getHeight()*scale;
            canvas.drawBitmap(source,null,new RectF((512-width)/2,(512-height)/2,(512+width)/2,(512+height)/2),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();output.compress(Bitmap.CompressFormat.PNG,100,bytes);
            source.recycle();output.recycle();return bytes.toByteArray();
        });
    }
}
