/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.reelfx;


import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.reelfx.util.ProcessWrapper;
import com.reelfx.util.StreamGobbler;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import org.apache.commons.io.FileUtils;

/**
 *
 * @author daniel
 */
public class ScreenRecorder extends ProcessWrapper {
	
	// FILE LOCATIONS
	public static String OUTPUT_FILE = RfxApplet.RFX_FOLDER.getAbsolutePath()+File.separator+"output-java"+(RfxApplet.IS_MAC ? ".mov" : ".mp4");
	//public static File VLC_JAR = new File(System.getProperty("java.class.path")+File.separator+"bin-mac.jar");
	//public static File VLC_JAR = new File("/Users/daniel/Documents/Java/java-review-tool/lib"+File.separator+"bin-mac.jar");
	protected static File VLC_EXEC = new File(RfxApplet.BIN_FOLDER.getAbsoluteFile()+File.separator+"VLC");
	
	// VIDEO SETTINGS
	public static double SCALE = 0.8;
	public static int FPS = 10;
	public static int BIT_RATE = 2048;
	
	// STATES
	public static int SCREEN_RECORDING_COMPLETE = 100;
	
    Process recordingProcess;
    StreamGobbler errorGobbler, inputGobbler;

	public void run() {
    	try {
    		if(RfxApplet.IS_MAC) {
	        	List<String> vlcArgs = new ArrayList<String>();
	            vlcArgs.add(VLC_EXEC.getAbsolutePath());
	        	//vlcArgs.add("vlc-bin"+File.separator+"VLC");
	        	vlcArgs.add("-IRC");
	        	vlcArgs.add("--rc-host=localhost:4444"); // formatting is mac specific
	        	vlcArgs.add("--rc-fake-tty");
	        	vlcArgs.add("--sout=#transcode{vcodec=mp4v,vb="+BIT_RATE+",fps="+FPS+",scale="+SCALE+"}:standard{access=file,mux=mov,dst="+OUTPUT_FILE+"}");
	        	
	            ProcessBuilder pb = new ProcessBuilder(vlcArgs);
	            recordingProcess = pb.start();
	
	            errorGobbler = new StreamGobbler(recordingProcess.getErrorStream(), false, "vlc E");
	            inputGobbler = new StreamGobbler(recordingProcess.getInputStream(), false, "vlc O");
	            
	            System.out.println("Starting listener threads...");
	            errorGobbler.start();
	            inputGobbler.start();    
    		} 
    		
    		else if(RfxApplet.IS_LINUX) {
    			File old = new File(OUTPUT_FILE);
    			if( old.exists() && !old.delete() )
    				throw new IOException("Could not delete the old video file: " + old.getAbsolutePath());
    			
    			Toolkit tk = Toolkit.getDefaultToolkit();
    	        Dimension dim = tk.getScreenSize();
    	        
    			List<String> ffmpegArgs = new ArrayList<String>();
    	    	//ffmpegArgs.add(RfxApplet.RFX_FOLDER.getAbsoluteFile()+File.separator+"bin-mac"+File.separator+"ffmpeg");
    	    	//ffmpegArgs.add(RfxApplet.BIN_FOLDER.getAbsoluteFile()+File.separator+"ffmpeg");
    			ffmpegArgs.add("/usr/bin/ffmpeg");
    	    	// screen capture settings
    	    	ffmpegArgs.addAll(parseParameters("-f x11grab -s "+dim.width+"x"+dim.height+" -r "+FPS+" -b "+BIT_RATE+"k -i :0.0"));
    	    	// microphone settings
    	    	ffmpegArgs.addAll(parseParameters("-f oss -ac 1 -ar "+AudioRecorder.FREQ+" -i /dev/audio"));
    	    	// output file settings
    	    	ffmpegArgs.addAll(parseParameters("-vcodec libx264 -r 10 -s "+Math.round(dim.width*SCALE)+"x"+Math.round(dim.height*SCALE)));
    	    	ffmpegArgs.add(OUTPUT_FILE);
    	    	System.out.println("Executing this command: "+ffmpegArgs.toString());
    	        ProcessBuilder pb = new ProcessBuilder(ffmpegArgs);
    	        recordingProcess = pb.start();
    	        
    	        errorGobbler = new StreamGobbler(recordingProcess.getErrorStream(), false, "ffmpeg E");
	            inputGobbler = new StreamGobbler(recordingProcess.getInputStream(), false, "ffmpeg O");
	            
	            System.out.println("Starting listener threads...");
	            errorGobbler.start();
	            inputGobbler.start();
	            
	            recordingProcess.waitFor();
	            
	            fireProcessUpdate(SCREEN_RECORDING_COMPLETE);
    		}
            
      } catch (IOException ioe) {
    	  ioe.printStackTrace();
      } catch (Exception ie) {
    	  ie.printStackTrace();
      }
	}
	
	public void stopRecording() {

        	//OutputStreamWriter osw = new OutputStreamWriter(recordingProcess.getOutputStream());
            //osw.write("q");
			
        	//recordingProcess.getOutputStream().write("q".getBytes());
        	
        	PrintWriter pw = new PrintWriter(recordingProcess.getOutputStream());
        	pw.print("q");
        	pw.flush();
	}
	
    protected void finalize() throws Throwable {
    	super.finalize();
    	recordingProcess.destroy();
    }
}
