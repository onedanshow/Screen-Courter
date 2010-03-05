package com.reelfx;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.reelfx.util.StreamGobbler;

public class PostProcessor extends Thread {
	
	public static String OUTPUT_FILE = RfxApplet.DESKTOP_FOLDER.getAbsolutePath()+File.separator+"output-final.mp4";
	
	Process postProcess;
	StreamGobbler errorGobbler, inputGobbler;
	
	public void run() {
		try {
	        //Process p = Runtime.getRuntime().exec("/Applications/VLC.app/Contents/MacOS/VLC -I telnet --telnet-host=localhost:4442 -I rc --rc-host=localhost:4444");
	        //Process p = Runtime.getRuntime().exec("/Applications/VLC.app/Contents/MacOS/VLC -I rc --rc-host=localhost:4444");
	
	    	List<String> vlcArgs = new ArrayList<String>();
	    	vlcArgs.add(RfxApplet.RFX_FOLDER.getAbsoluteFile()+File.separator+"bin-mac"+File.separator+"ffmpeg");
	    	vlcArgs.add("-ar");
	    	vlcArgs.add("44100");
	    	vlcArgs.add("-i");
	    	vlcArgs.add(AudioRecorder.OUTPUT_FILE);
	    	vlcArgs.add("-i"); 
	    	vlcArgs.add(ScreenRecorder.OUTPUT_FILE);
	    	vlcArgs.add("-vcodec"); 
	    	vlcArgs.add("libx264");
	    	//vlcArgs.add("-vpre");
	    	//vlcArgs.add("normal");
	    	
	    	// these are from the normal x264 preset; TODO have a way of reading these in:
	    	vlcArgs.add("-coder");
	    	vlcArgs.add("1");
	    	vlcArgs.add("-flags");
	    	vlcArgs.add("+loop");
	    	vlcArgs.add("-cmp");
	    	vlcArgs.add("+chroma");
	    	vlcArgs.add("-partitions");
	    	vlcArgs.add("+parti8x8+parti4x4+partp8x8+partb8x8");
	    	vlcArgs.add("-me_method");
	    	vlcArgs.add("hex");
	    	vlcArgs.add("-subq");
	    	vlcArgs.add("6");
	    	vlcArgs.add("-me_range");
	    	vlcArgs.add("16");
	    	vlcArgs.add("-g");
	    	vlcArgs.add("250");
	    	vlcArgs.add("-keyint_min");
	    	vlcArgs.add("25");
	    	vlcArgs.add("-sc_threshold");
	    	vlcArgs.add("40");
	    	vlcArgs.add("-i_qfactor");
	    	vlcArgs.add("0.71");
	    	vlcArgs.add("-b_strategy");
	    	vlcArgs.add("1");
	    	vlcArgs.add("-qcomp");
	    	vlcArgs.add("0.6");
	    	vlcArgs.add("-qmin");
	    	vlcArgs.add("10");
	    	vlcArgs.add("-qmax");
	    	vlcArgs.add("51");
	    	vlcArgs.add("-qdiff");
	    	vlcArgs.add("4");
	    	vlcArgs.add("-bf");
	    	vlcArgs.add("3");
	    	vlcArgs.add("-refs");
	    	vlcArgs.add("2");
	    	vlcArgs.add("-directpred");
	    	vlcArgs.add("3");
	    	vlcArgs.add("-trellis");
	    	vlcArgs.add("0");
	    	vlcArgs.add("-flags2");
	    	vlcArgs.add("+wpred+dct8x8+fastpskip+mbtree");
	    	vlcArgs.add("-wpredp");
	    	vlcArgs.add("2");
	    	vlcArgs.add(OUTPUT_FILE);
	        ProcessBuilder pb = new ProcessBuilder(vlcArgs);
	        postProcess = pb.start();
	
	        errorGobbler = new StreamGobbler(postProcess.getErrorStream(), false, "ffmpeg E");
	        inputGobbler = new StreamGobbler(postProcess.getInputStream(), false, "ffmpeg O");
	        
	        System.out.println("Starting listener threads...");
	        errorGobbler.start();
	        inputGobbler.start();   
	        
	        // TODO monitor the progress of the event
	        // TODO delete the temporary files when done
	        // TODO allow canceling of the transcoding
	        // TODO increment output file name if another already exists
	        // TODO allow people to save to desktop if they wish
	        
	  } catch (IOException ioe) {
		  ioe.printStackTrace();
	  } catch (Exception ie) {
		  ie.printStackTrace();
	  }
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
		postProcess.destroy();
	}
}
