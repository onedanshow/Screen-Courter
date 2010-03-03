/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.reelfx;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.reelfx.util.StreamGobbler;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.ProtectionDomain;

/**
 *
 * @author daniel
 */
public class ScreenRecorder extends Thread {
	
	public static String OUTPUT_FILE = RfxApplet.RFX_FOLDER.getAbsolutePath()+File.separator+"output-java.mov";
	//public static File VLC_JAR = new File(System.getProperty("java.class.path")+File.separator+"bin-mac.jar");
	//public static File VLC_JAR = new File("/Users/daniel/Documents/Java/java-review-tool/lib"+File.separator+"bin-mac.jar");
	protected static File VLC_EXEC = new File(RfxApplet.RFX_FOLDER.getAbsoluteFile()+File.separator+"bin-mac"+File.separator+"VLC");
	
    Process vlcProcess;
    StreamGobbler errorGobbler, inputGobbler;

	public void run() {
    	try {
    		
    		/* might revisit copying the jar locally later
    		if(!VLC_EXEC.exists() && RfxApplet.DEV_MODE) {
    			RfxApplet.copyFolderFromRemoteJar(new URL("jar", "", "/Users/daniel/Documents/Java/java-review-tool/lib"+File.separator+"bin-mac.jar" + "!/"), "bin-mac");
    			Runtime.getRuntime().exec("chmod 755 "+VLC_EXEC.getAbsolutePath()).waitFor();
				if(!VLC_EXEC.exists()) throw new IOException("Did not copy VLC to its execution directory!");
    		} else */
    		if(!VLC_EXEC.exists()){
    			RfxApplet.copyFolderFromRemoteJar(new URL("http://localhost/~daniel/test2/"+getVlcJarFile()), "bin-mac");
    			Runtime.getRuntime().exec("chmod 755 "+VLC_EXEC.getAbsolutePath()).waitFor();
				if(!VLC_EXEC.exists()) throw new IOException("Did not copy VLC to its execution directory!");
    		}
    		
    		// TODO disable UI while copying; move to RfxApplet?
    		
        	List<String> vlcArgs = new ArrayList<String>();
            vlcArgs.add(VLC_EXEC.getAbsolutePath());
        	//vlcArgs.add("vlc-bin"+File.separator+"VLC");
        	vlcArgs.add("-IRC");
        	vlcArgs.add("--rc-host=localhost:4444"); // formatting is mac specific
        	vlcArgs.add("--rc-fake-tty");
        	vlcArgs.add("--sout=#transcode{vcodec=mp4v,vb=2048,fps=10,scale=0.8}:standard{access=file,mux=mov,dst="+OUTPUT_FILE+"}");
        	
            ProcessBuilder pb = new ProcessBuilder(vlcArgs);
            vlcProcess = pb.start();

            errorGobbler = new StreamGobbler(vlcProcess.getErrorStream(), false, "vlc E");
            inputGobbler = new StreamGobbler(vlcProcess.getInputStream(), false, "vlc O");
            
            System.out.println("Starting listener threads...");
            errorGobbler.start();
            inputGobbler.start();         
            
      } catch (IOException ioe) {
    	  ioe.printStackTrace();
      } catch (Exception ie) {
    	  ie.printStackTrace();
      }
	}
	
	public String getVlcJarFile() {
		return "bin-mac.jar";
	}
	
    protected void finalize() throws Throwable {
    	super.finalize();
    	vlcProcess.destroy();
    }
}
