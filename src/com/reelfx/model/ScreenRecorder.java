/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.reelfx.model;


import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.reelfx.Applet;
import com.reelfx.model.util.ProcessWrapper;
import com.reelfx.model.util.StreamGobbler;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

import javax.sound.sampled.Mixer;

import org.apache.commons.io.FileUtils;

/**
 *
 * @author daniel
 */
public class ScreenRecorder extends ProcessWrapper implements ActionListener {
	
	private static String EXT = Applet.IS_WINDOWS ? ".avi" : ".mov";
	
	// FILE LOCATIONS
	public static File OUTPUT_FILE = new File(Applet.RFX_FOLDER.getAbsolutePath()+File.separator+"screen_capture"+EXT);
	//public static File VLC_JAR = new File(System.getProperty("java.class.path")+File.separator+"bin-mac.jar");
	//public static File VLC_JAR = new File("/Users/daniel/Documents/Java/java-review-tool/lib"+File.separator+"bin-mac.jar");
	protected static File MAC_EXEC = new File(Applet.BIN_FOLDER.getAbsoluteFile()+File.separator+"mac-screen-recorder");
	protected static File FFMPEG_EXEC = new File(Applet.BIN_FOLDER.getAbsoluteFile()+File.separator+"ffmpeg"+(Applet.IS_WINDOWS ? ".exe" : ""));
	
	// STATES
	public final static int RECORDING_STARTED = 100;
	public final static int RECORDING_COMPLETE = 101;
	
    private Process recordingProcess;
    private StreamGobbler errorGobbler, inputGobbler;
    private Mixer audioSource = null;
    private int audioIndex = 0;
    private ScreenRecorder self = this;
    
    /**
     * If this guy is to handle audio as well, give it the Java Mixer object to read from.
     * 
     * @param mixer
     */
	public synchronized void start(Mixer mixer,int index) {
		audioSource = mixer;
		audioIndex = index;
		super.start();
	}

	public void run() {
    	try {
    		if(Applet.IS_MAC) {
	        	List<String> macArgs = new ArrayList<String>();
	            macArgs.add(MAC_EXEC.getAbsolutePath());
	            macArgs.add(OUTPUT_FILE.getAbsolutePath());

	            ProcessBuilder pb = new ProcessBuilder(macArgs);
	            recordingProcess = pb.start();
	            fireProcessUpdate(RECORDING_STARTED);
	            
	            errorGobbler = new StreamGobbler(recordingProcess.getErrorStream(), false, "mac E");
	            inputGobbler = new StreamGobbler(recordingProcess.getInputStream(), false, "mac O");
	            
	            System.out.println("Starting listener threads...");
	            errorGobbler.start();
	            inputGobbler.start();
	            
	            recordingProcess.waitFor();
	            fireProcessUpdate(RECORDING_COMPLETE);
    		} 
    		
    		else if(Applet.IS_LINUX) {
    			// can have problem with file permissions when methods are invoked via Javascript even if applet is signed, 
    			// thus some code needs to wrapped in a privledged block
    			AccessController.doPrivileged(new PrivilegedAction<Object>() {

					@Override
					public Object run() {
						
						try {
							int width = Applet.CAPTURE_VIEWPORT.width;
							if(width % 2 != 0) width--;
							int height = Applet.CAPTURE_VIEWPORT.height;
							if(height % 2 != 0) height--;
							
			    			List<String> ffmpegArgs = new ArrayList<String>();
			    			//ffmpegArgs.add("/usr/bin/ffmpeg");
			    	    	ffmpegArgs.add(Applet.BIN_FOLDER.getAbsoluteFile()+File.separator+"ffmpeg");
			    	    	// screen capture settings
			    	    	ffmpegArgs.addAll(parseParameters("-y -f x11grab -s "+width+"x"+height+" -r 20 -i :0.0+"+Applet.CAPTURE_VIEWPORT.x+","+Applet.CAPTURE_VIEWPORT.y));
			    	    	// microphone settings (good resource: http://www.oreilly.de/catalog/multilinux/excerpt/ch14-05.htm)
			    	    	/* 04/29/2010 - ffmpeg gets much better framerate when not recording microphone (let Java do this)
			    	    	 * if(audioSource != null) { 
			    	    		String driver = audioIndex > 0 ? "/dev/dsp"+audioIndex : "/dev/dsp";
			    	    		ffmpegArgs.addAll(parseParameters("-f oss -ac 1 -ar "+AudioRecorder.FREQ+" -i "+driver));
			    	    	}*/
			    	    	// output file settings
			    	    	ffmpegArgs.addAll(parseParameters("-vcodec mpeg4 -r 20 -b 5000k")); // -s "+Math.round(width*SCALE)+"x"+Math.round(height*SCALE))
			    	    	ffmpegArgs.add(OUTPUT_FILE.getAbsolutePath());
			    	    	
			    	    	System.out.println("Executing this command: "+prettyCommand(ffmpegArgs));
			    	    	
			    	        ProcessBuilder pb = new ProcessBuilder(ffmpegArgs);
			    	        recordingProcess = pb.start();
			    	        // fireProcessUpdate(RECORDING_STARTED); // moved to action listener method
			    	        
			    	        errorGobbler = new StreamGobbler(recordingProcess.getErrorStream(), false, "ffmpeg E");
				            inputGobbler = new StreamGobbler(recordingProcess.getInputStream(), false, "ffmpeg O");
				            
				            System.out.println("Starting listener threads...");
				            errorGobbler.start();
				            errorGobbler.addActionListener("Stream mapping:", self);
				            inputGobbler.start();
				            
				            recordingProcess.waitFor();
				            
				            fireProcessUpdate(RECORDING_COMPLETE);
	            
						}
			            catch (InterruptedException e) {
							e.printStackTrace();
						}
			            catch (IOException e1) {
							e1.printStackTrace();
						}
						return null;
					}
				});
    		}
    		
    		else if(Applet.IS_WINDOWS) {
    			// can have problem with file permissions when methods are invoked via Javascript even if applet is signed, 
    			// thus some code needs to wrapped in a privileged block
    			AccessController.doPrivileged(new PrivilegedAction<Object>() {

					@Override
					public Object run() {
						
						try {
							List<String> ffmpegArgs = new ArrayList<String>();
				            ffmpegArgs.add(FFMPEG_EXEC.getAbsolutePath());
				            // for full screen, use simply "cursor:desktop"
				            String viewport = ":offset="+Applet.CAPTURE_VIEWPORT.x+","+Applet.CAPTURE_VIEWPORT.y;
				            viewport += ":size="+Applet.CAPTURE_VIEWPORT.width+","+Applet.CAPTURE_VIEWPORT.height;
				            ffmpegArgs.addAll(parseParameters("-y -f gdigrab -r 20 -i cursor:desktop"+viewport+" -vcodec mpeg4 -b 5000k "+OUTPUT_FILE));
				            
				        	System.out.println("Executing this command: "+prettyCommand(ffmpegArgs));
				            ProcessBuilder pb = new ProcessBuilder(ffmpegArgs);
							recordingProcess = pb.start();
				            //fireProcessUpdate(RECORDING_STARTED); // moved to action listener method
				            
							// ffmpeg doesn't get the microphone on Windows, but this allows it to record a better frame rate
							
				            errorGobbler = new StreamGobbler(recordingProcess.getErrorStream(), false, "ffmpeg E");
				            inputGobbler = new StreamGobbler(recordingProcess.getInputStream(), false, "ffmpeg O");
				            
				            System.out.println("Starting listener threads...");
				            errorGobbler.start();
				            errorGobbler.addActionListener("Stream mapping:", self);
				            inputGobbler.start();
				            			         
							recordingProcess.waitFor();

				            fireProcessUpdate(RECORDING_COMPLETE);
			            
						}
			            catch (InterruptedException e) {
							e.printStackTrace();
						}
			            catch (IOException e1) {
							e1.printStackTrace();
						}
						return null;
					}
				});
	            
    		}
            
      } catch (IOException ioe) {
    	  ioe.printStackTrace();
      } catch (Exception ie) {
    	  ie.printStackTrace();
      }
	}
	
	public void startRecording() {      
		if(Applet.IS_MAC) {
	    	PrintWriter pw = new PrintWriter(recordingProcess.getOutputStream());
	    	pw.println("start");
	    	pw.flush();
		}
		// nothing for linux or windows
	}
	
	public void stopRecording() {  
		System.out.println("Screen recording stopped...");
		if(Applet.IS_LINUX || Applet.IS_WINDOWS) {
	    	PrintWriter pw = new PrintWriter(recordingProcess.getOutputStream());
	    	pw.print("q");
	    	pw.flush();
		} else if(Applet.IS_MAC) {
	    	PrintWriter pw = new PrintWriter(recordingProcess.getOutputStream());
	    	pw.println("stop");
	    	pw.flush();
		}
	}
	
	public void closeDown() {
		System.out.println("Closing down ScreenRecorder...");
		if(Applet.IS_MAC && recordingProcess != null) {
	    	PrintWriter pw = new PrintWriter(recordingProcess.getOutputStream());
	    	pw.println("quit");
	    	pw.flush();
		} else if((Applet.IS_LINUX || Applet.IS_WINDOWS) && recordingProcess != null) {
	    	PrintWriter pw = new PrintWriter(recordingProcess.getOutputStream());
	    	pw.print("q");
	    	pw.flush();
		}
		// nothing for linux or windows
	}
	
	@Override
    protected void finalize() throws Throwable {
		System.out.println("Finalizing ScreenRecorder...");
    	super.finalize();
    	closeDown();
    	if(recordingProcess != null)
    		recordingProcess.destroy();
    }
    
    public static void deleteOutput() {
    	AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				try {
					if(OUTPUT_FILE.exists() && !OUTPUT_FILE.delete())
						throw new Exception("Can't delete the old video file!");
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		fireProcessUpdate(RECORDING_STARTED);
	}
}
