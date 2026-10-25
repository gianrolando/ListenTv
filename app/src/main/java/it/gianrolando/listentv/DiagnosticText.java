package it.gianrolando.listentv;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class DiagnosticText {
    /** Exception messages can contain signed stream URLs, so retain only types and frames. */
    static String describe(Throwable error) {
        StringBuilder text=new StringBuilder();
        Set<Throwable> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        for(int cause=0;error!=null && cause<6 && seen.add(error);cause++,error=error.getCause()) {
            text.append(error.getClass().getName()).append('\n');
            StackTraceElement[] frames=error.getStackTrace();
            for(int i=0;i<Math.min(frames.length,12);i++)
                text.append("  ").append(frames[i].getClassName()).append('.').append(frames[i].getMethodName())
                    .append(':').append(frames[i].getLineNumber()).append('\n');
        }
        return text.toString();
    }
}
