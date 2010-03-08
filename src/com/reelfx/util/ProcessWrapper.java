package com.reelfx.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.event.EventListenerList;

public abstract class ProcessWrapper extends Thread {
	protected List<ProcessListener> listeners = new ArrayList<ProcessListener>();	

	public void addProcessListener(ProcessListener listener) {
        listeners.add(listener);
    }

    public void removeProcessListener(ProcessListener listener) {
        listeners.remove(listener);
    }
    
    protected void fireProcessUpdate(int event) {
    	for (ProcessListener listener : listeners) {
            listener.processUpdate(event);
        }
    }
    
    public List<String> getFfmpegX264Params() {
    	
    	List<String> ffmpegArgs = new ArrayList<String>();
		// these are from the normal x264 preset; TODO have a way of reading these in:
    	ffmpegArgs.add("-vcodec"); 
    	ffmpegArgs.add("libx264");
    	ffmpegArgs.add("-coder");
    	ffmpegArgs.add("1");
    	ffmpegArgs.add("-flags");
    	ffmpegArgs.add("+loop");
    	ffmpegArgs.add("-cmp");
    	ffmpegArgs.add("+chroma");
    	ffmpegArgs.add("-partitions");
    	ffmpegArgs.add("+parti8x8+parti4x4+partp8x8+partb8x8");
    	ffmpegArgs.add("-me_method");
    	ffmpegArgs.add("hex");
    	ffmpegArgs.add("-subq");
    	ffmpegArgs.add("6");
    	ffmpegArgs.add("-me_range");
    	ffmpegArgs.add("16");
    	ffmpegArgs.add("-g");
    	ffmpegArgs.add("250");
    	ffmpegArgs.add("-keyint_min");
    	ffmpegArgs.add("25");
    	ffmpegArgs.add("-sc_threshold");
    	ffmpegArgs.add("40");
    	ffmpegArgs.add("-i_qfactor");
    	ffmpegArgs.add("0.71");
    	ffmpegArgs.add("-b_strategy");
    	ffmpegArgs.add("1");
    	ffmpegArgs.add("-qcomp");
    	ffmpegArgs.add("0.6");
    	ffmpegArgs.add("-qmin");
    	ffmpegArgs.add("10");
    	ffmpegArgs.add("-qmax");
    	ffmpegArgs.add("51");
    	ffmpegArgs.add("-qdiff");
    	ffmpegArgs.add("4");
    	ffmpegArgs.add("-bf");
    	ffmpegArgs.add("3");
    	ffmpegArgs.add("-refs");
    	ffmpegArgs.add("2");
    	ffmpegArgs.add("-directpred");
    	ffmpegArgs.add("3");
    	ffmpegArgs.add("-trellis");
    	ffmpegArgs.add("0");
    	ffmpegArgs.add("-flags2");
    	ffmpegArgs.add("+wpred+dct8x8+fastpskip+mbtree");
    	ffmpegArgs.add("-wpredp");
    	ffmpegArgs.add("2");
    	
    	return ffmpegArgs;
    }
    
    protected List<String> parseParameters(String commandLine) {
    	StringTokenizer st = new StringTokenizer(commandLine," ");
    	List<String> result = new ArrayList<String>();
    	while(st.hasMoreTokens())
    		result.add(st.nextToken());
    	return result;
    }
    
}
