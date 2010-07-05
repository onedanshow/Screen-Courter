package com.reelfx.model.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import javax.swing.event.EventListenerList;

import org.apache.log4j.Logger;

import com.reelfx.Applet;
import com.reelfx.model.AudioRecorder;
import com.reelfx.model.ScreenRecorder;

public abstract class ProcessWrapper extends Thread {
	protected List<ProcessListener> listeners = new ArrayList<ProcessListener>();
	protected boolean silentMode = false;
	private static Logger logger = Logger.getLogger(ProcessWrapper.class);

	// KEYS
	public static class Metadata {
		public static String DURATION = "dur";
		public static String READABLE_DURATION = "readDur";
		public static String HOURS = "hrs";
		public static String MINUTES = "mins";
		public static String SECONDS = "secs";
		public static String SPLIT_SECONDS = "splitSecs";
		public static String TOTAL_SECONDS = "totalSecs";
		public static String FPS = "fps";
		public static String TOTAL_FRAMES = "totalFrames";
	}
	// 
	private Map<String,Object> results;
	
	public void addProcessListener(ProcessListener listener) {
        listeners.add(listener);
    }

    public void removeProcessListener(ProcessListener listener) {
        listeners.remove(listener);
    }
    
    public void removeAllProcessListeners() {
    	listeners.clear();
    }
    
    protected void fireProcessUpdate(int event) {
    	fireProcessUpdate(event, null);
    }
    
    protected void fireProcessUpdate(int event,Object body) {
    	if(isSilent()) return;
    	
    	for (ProcessListener listener : listeners) {
            listener.processUpdate(event,body);
        }
    }
    
	public boolean isSilent() {
		return silentMode;
	}

	public void setSilent(boolean silentMode) {
		this.silentMode = silentMode;
	}
    
    public List<String> getFfmpegCopyParams() {
    	List<String> ffmpegArgs = new ArrayList<String>();
    	
    	ffmpegArgs.addAll(parseParameters("-vcodec copy -acodec copy"));
    	
    	return ffmpegArgs;
    }
    
    public List<String> getFfmpegX264FastFirstPastBaselineParams() {
    	
    	List<String> ffmpegArgs = new ArrayList<String>();
    	// starters
    	ffmpegArgs.addAll(parseParameters("-vcodec libx264"));
    	// fastfirstpass preset
    	ffmpegArgs.addAll(parseParameters("-coder 1 -flags +loop -cmp +chroma -partitions -parti8x8-parti4x4-partp8x8-partp4x4-partb8x8"));
    	ffmpegArgs.addAll(parseParameters("-me_method dia -subq 2 -me_range 16 -g 250 -keyint_min 25 -sc_threshold 40 -i_qfactor 0.71"));
    	ffmpegArgs.addAll(parseParameters("-b_strategy 1 -qcomp 0.6 -qmin 10 -qmax 51 -qdiff 4 -bf 3 -refs 1 -directpred 3 -trellis 0"));
    	ffmpegArgs.addAll(parseParameters("-flags2 -bpyramid-wpred-mixed_refs-dct8x8+fastpskip+mbtree -wpredp 2"));
    	// baseline
    	ffmpegArgs.addAll(parseParameters("-coder 0 -bf 0 -flags2 -wpred-dct8x8+mbtree -wpredp 0"));
    	// so we can do this as fast as possible and good quality
    	ffmpegArgs.addAll(parseParameters("-crf 22 -threads 0"));
    	return ffmpegArgs;
	}
    
    @Deprecated
    public List<String> getFfmpegX264Params() {
    	
    	List<String> ffmpegArgs = new ArrayList<String>();
    	
    	ffmpegArgs.addAll(parseParameters("-vcodec libx264 -r 15 -coder 1 -flags +loop -cmp +chroma -partitions +parti8x8+parti4x4+partp8x8+partp4x4+partb8x8"));
    	
    	/*
    	if(Applet.IS_WINDOWS) {    		
    		// this is for main x264 preset (large file (though framerate helps) and still not good quality)
    		//ffmpegArgs.addAll(parseParameters("-flags2 -dct8x8+mbtree -r 15"));
    		
    		// -ffmpeg doesn't like medium x264 preset (something with me_method setting)
    		// -ffmpeg doesn't like hq x264 preset (same reason)
    		// -placebo firstpass preset doesn't handle motion at all
    		
    		ffmpegArgs.addAll(parseParameters("-vcodec libx264 -r 15 -coder 1 -flags +loop -cmp +chroma -partitions +parti8x8+parti4x4+partp8x8+partp4x4+partb8x8"));
    		ffmpegArgs.addAll(parseParameters("-me_method tesa -subq 10 -me_range 24 -g 250 -keyint_min 25 -sc_threshold 40"));
    		ffmpegArgs.addAll(parseParameters("-i_qfactor 0.71 -b_strategy 2 -qcomp 0.6 -qmin 10 -qmax 51 -qdiff 4 -bf 16 -refs 16"));
    		ffmpegArgs.addAll(parseParameters("-directpred 3 -trellis 2 -flags2 +bpyramid+mixed_refs+wpred+dct8x8-fastpskip+mbtree -wpredp 2"));
    	} else {
    	*/
			// these are from the normal x264 preset; TODO convert to use parseParameters
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
    	//}
    	return ffmpegArgs;
    }
    
    /**
     * Get duration and other information about a media file.  
     * 
     * Idea lifted from Kelly's pruview gem: http://github.com/kelredd/pruview/blob/master/bin/ffyml
     * 
     * @param path to media file
     */
    protected Map<String,Object> parseMediaFile(String path) {
        try {
        	results = new HashMap<String,Object>();
        	
        	// purposefully incorrectly call ffmpeg so it gives us some information about the file...
			String command = Applet.BIN_FOLDER.getAbsoluteFile()+File.separator+"ffmpeg -i "+path;
        	
	        Process postProcess = Runtime.getRuntime().exec(command);
	
	        StreamGobbler errorGobbler = new StreamGobbler(postProcess.getErrorStream(), false, "ffmpeg E");
	        StreamGobbler inputGobbler = new StreamGobbler(postProcess.getInputStream(), false, "ffmpeg O");
	        
	        logger.info("Starting listener threads...");
	        errorGobbler.addActionListener("Input #0", new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					
				}
			});
	        errorGobbler.addActionListener("Duration:", new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					 StringTokenizer st = new StringTokenizer(e.getActionCommand(),",");
					 results.put(Metadata.READABLE_DURATION, st.nextToken().replaceAll("[a-zA-Z]","").trim().replaceFirst(": ",""));
					 String[] temp = ((String)results.get(Metadata.READABLE_DURATION)).split(":");
					 results.put(Metadata.HOURS, temp[0]);
					 results.put(Metadata.MINUTES, temp[1]);
					 results.put(Metadata.SECONDS, temp[2]); // had issue splitting seconds string using .split(".")
					 results.put(Metadata.TOTAL_SECONDS, Double.parseDouble(temp[0])*3600.0 + Double.parseDouble(temp[1])*60.0 + Double.parseDouble(temp[2]));
				}
			});
	        errorGobbler.addActionListener("fps", new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					 StringTokenizer st = new StringTokenizer(e.getActionCommand(),",");
					 while(st.hasMoreTokens()) {
						 String token = st.nextToken();
						 if(token.contains("fps")) {
							 results.put(Metadata.FPS,Double.parseDouble(token.replaceAll("[a-zA-Z]","").trim()));
							 results.put(Metadata.TOTAL_FRAMES, (Double)results.get(Metadata.FPS) * (Double)results.get(Metadata.TOTAL_SECONDS) );
						 }
					 }
				}
			});
	        errorGobbler.start();
	        inputGobbler.start();  
	        
	        postProcess.waitFor();
			
		} catch (Exception e) {
			logger.error("Problem while parsing media file!",e);
			results = null;
		}
		
		return results;
    }
    
    protected List<String> parseParameters(String commandLine) {
    	StringTokenizer st = new StringTokenizer(commandLine," ");
    	List<String> result = new ArrayList<String>();
    	while(st.hasMoreTokens())
    		result.add(st.nextToken());
    	return result;
    }
    
    protected void printMetadata(Map<String,Object> metadata) {
    	for(Entry<String,Object> en : metadata.entrySet()) {
    		System.out.println(en.getKey()+" => "+en.getValue());
    	}
    }
    
    protected String prettyCommand(List<String> args) {
    	String result = "";
    	for(String arg : args)
    		result += arg + " ";
    	return result;
    }
    
}
