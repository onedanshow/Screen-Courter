package com.reelfx.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.sampled.Mixer;

import com.reelfx.Applet;
import com.reelfx.model.AudioRecorder;
import com.reelfx.model.ScreenRecorder;

public class MacController extends ApplicationController {

	public MacController() {
		super();
    	
    	if(!Applet.BIN_FOLDER.exists()){
			gui.status.setText("Performing one-time install...");
			gui.disable();
        }
	}
	
	@Override
	public void setupExtensions() {
		super.setupExtensions();
		try {
        	/* might revisit copying the jar locally later
    		if(!MAC_EXEC.exists() && Applet.DEV_MODE) {
    			Applet.copyFolderFromRemoteJar(new URL("jar", "", "/Users/daniel/Documents/Java/java-review-tool/lib"+File.separator+"bin-mac.jar" + "!/"), "bin-mac");
    			Runtime.getRuntime().exec("chmod 755 "+MAC_EXEC.getAbsolutePath()).waitFor();
    			if(!MAC_EXEC.exists()) throw new IOException("Did not copy VLC to its execution directory!");
    		} else */
			if(!Applet.BIN_FOLDER.exists()){
				Applet.copyFolderFromRemoteJar(new URL(Applet.HOST_URL+"/bin-mac.jar"), "bin-mac");
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"mac-screen-recorder").waitFor();
				if(!Applet.BIN_FOLDER.exists()) throw new IOException("Did not copy Mac extensions to the execution directory!");
			}
			System.out.println("Have access to execution folder: "+Applet.BIN_FOLDER.getAbsolutePath());
			gui.enable();
        } catch (MalformedURLException e1) {
			gui.status.setText("Error downloading native extensions");
			e1.printStackTrace();
		} catch (InterruptedException e) {
			gui.status.setText("Error setting up native extentions");
			e.printStackTrace();
		} catch (IOException e) {
			gui.status.setText("Error downloading native extentions");
			e.printStackTrace();
		}
	}

	@Override
	public void prepareForRecording() {
		// start up VLC
		screen = new ScreenRecorder();
		screen.addProcessListener(this);
		screen.start();
		// TODO check that it starts up correctly
	}

	@Override
	public void startRecording(Mixer audioSource,int audioIndex) {
		screen.startRecording();
	}

	@Override
	public void stopRecording() {		
		screen.stopRecording();
	}
}
