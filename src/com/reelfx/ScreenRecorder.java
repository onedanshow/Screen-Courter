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
	public static File VLC_BIN = new File(System.getProperty("java.class.path")+File.separator+"bin-mac.jar");
	//protected static File VLC_BIN;
	protected static File VLC_EXEC = new File(RfxApplet.RFX_FOLDER.getAbsoluteFile()+File.separator+"bin-mac"+File.separator+"VLC");
	
    Process vlcProcess;
    StreamGobbler errorGobbler, inputGobbler;

	public void run() {
    	try {
    		
      	  	//System.err.println("VLC-BIN: "+RfxApplet.class.getClassLoader().getResource("vlc-bin.jar").toString());
    		if(VLC_BIN == null) {
    			VLC_BIN = new File(RfxApplet.class.getClassLoader().getResource(getVlcJarFile()).toURI());
    		}
    		
    		if(!VLC_EXEC.exists() && RfxApplet.DEV_MODE) {
    			if (VLC_BIN.exists()) {
    				RfxApplet.copyFolderFromJar(VLC_BIN.getPath(), "bin-mac");
    				Runtime.getRuntime().exec("chmod 755 "+VLC_EXEC.getAbsolutePath()).waitFor();
    				if(!VLC_EXEC.exists()) throw new IOException("Did not copy VLC to its execution directory!");
    			}
    			else {
    				throw new IOException("Can't find '"+getVlcJarFile()+"' to extract into its execution directory! "+VLC_BIN.getPath());
    			}
    		} else if(!VLC_EXEC.exists()) {
    			URL url = new URL("http://localhost/~daniel/test2/"+getVlcJarFile());
    			URLConnection urlConnection = url.openConnection();
    			InputStream is = url.openStream();
    			FileOutputStream file = new FileOutputStream(RfxApplet.RFX_FOLDER.getAbsoluteFile()+File.separator+getVlcJarFile());
    			int oneByte;
    			while ((oneByte=is.read()) != -1)
    		          file.write(oneByte);
    		    is.close();
    		    file.close();
    		    RfxApplet.copyFolderFromJar(RfxApplet.RFX_FOLDER.getAbsolutePath()+File.separator+getVlcJarFile(), "bin-mac");
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
            
            //System.out.println("Waiting for process.");
            //vlcProcess.waitFor();
            
            //System.out.println("Interrupting threads...");
            //errorGobbler.interrupt();
            //inputGobbler.interrupt();
            
            //System.out.println("Joining threads...");
            //errorGobbler.join();
            //inputGobbler.join();
            
            //OutputStreamWriter osw = new OutputStreamWriter(vlcProcess.getOutputStream());
            //osw.write("quit \n");
            //vlcProcess.getOutputStream().write("stop".getBytes());
            
      } catch (IOException ioe) {
    	  ioe.printStackTrace();
      } catch (URISyntaxException urie) {
    	  urie.printStackTrace();
      } catch (Exception ie) {
    	  ie.printStackTrace();
      }
	}

	/**
	 * Return location of this Jar file
	 * 
	 * @return
	 * @throws URISyntaxException
	 */
    private static URI getJarURI() throws URISyntaxException
	{
	    final ProtectionDomain domain;
	    final CodeSource       source;
	    final URL              url;
	    final URI              uri;
	
	    domain = ScreenRecorder.class.getProtectionDomain();
	    source = domain.getCodeSource();
	    url    = source.getLocation();
	    uri    = url.toURI();
	
	    return (uri);
	}
	
	public String getVlcJarFile() {
		return "bin-mac.jar";
	}
	
    protected void finalize() throws Throwable {
    	super.finalize();
    	vlcProcess.destroy();
    }
}
