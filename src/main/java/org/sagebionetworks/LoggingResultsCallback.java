package org.sagebionetworks;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.commons.io.IOUtils;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;

public class LoggingResultsCallback implements ResultCallback<Frame> {
    private boolean isComplete=false;
    private OutputStream os;
    private Thread parentThread;
    private Integer maxTailLengthInCharacters;
    private String tail;

    public LoggingResultsCallback(OutputStream os, Thread parentThread, Integer maxTailLengthInCharacters) {
        this.os=os;
        this.parentThread=parentThread;
        this.maxTailLengthInCharacters=maxTailLengthInCharacters;
    }

    public void close() throws IOException {
        // no-op:  output stream will close when function ends
    }

    public void onStart(Closeable closeable) {
        // no-op:  output stream is already open
    }

    public void onNext(Frame object) {
        try {
            String content = object.toString()+"\r\n";
            IOUtils.write(content, os);
            if (maxTailLengthInCharacters!=null) {
                String spliced = (tail==null)?content:(tail+content);
                int splicedLength = spliced.length();
                if (splicedLength<=maxTailLengthInCharacters) {
                    tail = spliced;
                } else {
                    tail = spliced.substring(splicedLength-maxTailLengthInCharacters, splicedLength);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onError(Throwable throwable) {
        throwable.printStackTrace(new PrintStream(os));
    }

    public void onComplete() {
        this.isComplete=true;
        this.parentThread.interrupt();
    }

    public boolean isComplete() {
        return this.isComplete;
    }

    public String getTail() {return tail;}

}
