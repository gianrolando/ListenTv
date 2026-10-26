// SPDX-License-Identifier: GPL-3.0-or-later
package it.gianrolando.listentv;

import java.io.*;
import java.util.*;
import org.xmlpull.v1.*;

final class EpgXmlTv {
    static List<EpgProgramme> read(InputStream input,Set<String> known,long now) throws Exception {
        XmlPullParser parser=XmlPullParserFactory.newInstance().newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false);
        parser.setInput(new LimitedInput(input,24*1024*1024),"UTF-8");
        List<EpgProgramme> out=new ArrayList<>();
        String channel=null,title=null;long start=0,end=0;boolean root=false,inTitle=false;int programmeDepth=-1;
        StringBuilder text=new StringBuilder();
        for(int event=parser.getEventType();event!=XmlPullParser.END_DOCUMENT;event=parser.nextToken()) {
            if(event==XmlPullParser.DOCDECL) throw new IOException("XMLTV document declarations are not allowed.");
            if(event==XmlPullParser.START_TAG) {
                if(!root) {if(!"tv".equals(parser.getName())) throw new IOException("Invalid XMLTV root.");root=true;}
                if("programme".equals(parser.getName())) {
                    programmeDepth=parser.getDepth();title=null;channel=EpgProgramme.channelId(parser.getAttributeValue(null,"channel"),known);
                    try {start=EpgProgramme.xmlTime(parser.getAttributeValue(null,"start"));end=EpgProgramme.xmlTime(parser.getAttributeValue(null,"stop"));}
                    catch(RuntimeException badTime) {channel=null;}
                    if(end<=start || end-start>24*60*60*1000L || end<=now-6*60*60*1000L || start>now+48*60*60*1000L)channel=null;
                } else if(programmeDepth>=0 && channel!=null && title==null && "title".equals(parser.getName())) {inTitle=true;text.setLength(0);}
            } else if(inTitle && (event==XmlPullParser.TEXT || event==XmlPullParser.CDSECT || event==XmlPullParser.ENTITY_REF)) {
                String value=parser.getText();if(value==null) throw new IOException("Unresolved XMLTV entity.");
                int count=Math.min(value.length(),300-text.length());if(count>0)text.append(value,0,count);
            } else if(event==XmlPullParser.END_TAG) {
                if(inTitle && "title".equals(parser.getName())) {title=text.toString().replaceAll("\\s+"," ").trim();inTitle=false;}
                if(programmeDepth==parser.getDepth() && "programme".equals(parser.getName())) {
                    if(channel!=null && EpgProgramme.validTitle(title)) {
                        if(out.size()>=10000)throw new IOException("XMLTV programme limit exceeded.");
                        out.add(new EpgProgramme(channel,title,start,end));
                    }
                    programmeDepth=-1;channel=null;inTitle=false;
                }
            }
        }
        if(!root)throw new IOException("Empty XMLTV document.");
        return out;
    }
    static final class LimitedInput extends InputStream {
        private final InputStream source;private final long limit;private long count;
        LimitedInput(InputStream source,long limit) {this.source=source;this.limit=limit;}
        private void add(long bytes) throws IOException {if(bytes>0 && (count+=bytes)>limit)throw new IOException("Guide byte limit exceeded.");}
        @Override public int read() throws IOException {int value=source.read();if(value!=-1)add(1);return value;}
        @Override public int read(byte[] b,int off,int len) throws IOException {int n=source.read(b,off,len);add(n);return n;}
        @Override public void close() throws IOException {source.close();}
    }
}
