package com.reelfx.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.sampled.Mixer;

import netscape.javascript.JSException;

import com.reelfx.Applet;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.view.Interface;

public class MacController extends ApplicationController {

	public MacController() {
		super();
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
			setReadyStateBasedOnPriorRecording();
        } catch (MalformedURLException e1) {
        	gui.changeState(Interface.FATAL,"Error with install");
			e1.printStackTrace();
		} catch (InterruptedException e) {
			gui.changeState(Interface.FATAL,"Error with install");
			e.printStackTrace();
		} catch (IOException e) {
			gui.changeState(Interface.FATAL,"Error with install");
			e.printStackTrace();
		}
	}

	@Override
	public void prepareForRecording() {
		boolean allowRecording = true;
		
		if(Applet.IS_MAC && System.getProperty("os.version").startsWith("10.6"))
			Applet.sendInfo("You have Snow Leopard.");
		else {
			Applet.sendError("You need to have Snow Leopard installed to record a review.");
			allowRecording = false;
			screen = null;
		}
		
		if(allowRecording) {
			if(screen != null) {
				screen.closeDown();
			}
			// start up CamStudio
			screen = new ScreenRecorder();
			screen.addProcessListener(this);
			screen.start();
			// TODO check that it starts up correctly
		}
	}

	@Override
	public void startRecording(Mixer audioSource,int audioIndex) {
		if(screen != null)
			screen.startRecording();
	}

	@Override
	public void stopRecording() {		
		if(screen != null)
			screen.stopRecording();
	}
}
