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

import com.reelfx.Applet;
import com.reelfx.model.AudioRecorder;
import com.reelfx.model.ScreenRecorder;

public abstract class ProcessWrapper extends Thread {
	protected List<ProcessListener> listeners = new ArrayList<ProcessListener>();
	
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
    
    protected void fireProcessUpdate(int event) {
    	for (ProcessListener listener : listeners) {
            listener.processUpdate(event);
        }
    }
    
    public List<String> getFfmpegX264Params() {
    	
    	List<String> ffmpegArgs = new ArrayList<String>();
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
    	
    	return ffmpegArgs;
    }
    
    /**
     * Get duration and other information about a media file.  
     * 
     * Lifted from Kelly's pruview gem: http://github.com/kelredd/pruview/blob/master/bin/ffyml
     * 
     * @param path to media file
     */
    protected Map<String,Object> parseMediaFile(String path) {
        try {
        	results = new HashMap<String,Object>();
        	
        	String tempFile = Applet.BIN_FOLDER.getAbsolutePath()+File.separator+"output_dump.txt";
			String command = Applet.BIN_FOLDER.getAbsoluteFile()+File.separator+"ffmpeg -i "+path; //+" 2> "+tempFile;
        	
	        Process postProcess = Runtime.getRuntime().exec(command);
	
	        StreamGobbler errorGobbler = new StreamGobbler(postProcess.getErrorStream(), false, "ffmpeg E");
	        StreamGobbler inputGobbler = new StreamGobbler(postProcess.getInputStream(), false, "ffmpeg O");
	        
	        System.out.println("Starting listener threads...");
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
			
		} catch (InterruptedException e) {
			e.printStackTrace();
			results = null;
		} catch (IOException e) {
			e.printStackTrace();
			results = null;
		}
		
		return results;

    	/*
    	ffmpeg -i "$video_file" 2> "$temp_out"
    	 
    	# parse temp_out for the file properties
    	# format
    	var_format=`cat $temp_out | grep "Input #0" | gawk -F', ' '{print $2}'`
    	var_duration=`cat $temp_out | grep "Duration:" | gawk -F': ' '{print $2}' | gawk -F', ' '{print $1}'`
    	var_bitrate=`cat $temp_out | grep "Duration:" | gawk -F': ' '{print $4}' | gawk -F' ' '{print $1}'`
    	var_codec=`cat $temp_out | grep "Video:" | gawk -F': ' '{print $3}' | gawk -F', ' '{print $1}'`
    	var_width=`cat $temp_out | grep "Video:" | gawk -F': ' '{print $3}' | gawk -F', ' '{print $3}' | gawk -F'x' '{print $1}'`
    	var_height=`cat $temp_out | grep "Video:" | gawk -F': ' '{print $3}' | gawk -F', ' '{print $3}' | gawk -F'x' '{print $2}' | gawk -F' ' '{print $1}'`
    	var_framerate=`cat $temp_out | grep "Video:" | gawk -F': ' '{print $3}' | gawk -F', ' '{print $5}' | gawk -F' ' '{print $1}'`
    	if [ "$var_framerate" = "" ]; then
    	var_framerate=`cat $temp_out | grep "Video:" | gawk -F': ' '{print $3}' | gawk -F', ' '{print $4}' | gawk -F' ' '{print $1}'`
    	fi
    	var_audio_codec=`cat $temp_out | grep "Audio:" | gawk -F': ' '{print $3}' | gawk -F', ' '{print $1}'`
    	var_audio_bitrate=`cat $temp_out | grep "Audio:" | gawk -F', ' '{print $5}' | gawk -F' ' '{print $1}'`
    	var_audio_sampling=`cat $temp_out | grep "Audio:" | gawk -F', ' '{print $2}' | gawk -F' ' '{print $1}'`
    	*/
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
    
}
