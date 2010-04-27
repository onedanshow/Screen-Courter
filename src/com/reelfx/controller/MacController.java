package com.reelfx.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.sampled.Mixer;

import netscape.javascript.JSException;

import com.reelfx.Applet;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.view.RecordInterface;

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
				Applet.copyFolderFromRemoteJar(new URL(Applet.HOST_URL+"/bin-mac.jar?"+Math.random()*10000), "bin-mac");
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"mac-screen-recorder").waitFor();
				if(!Applet.BIN_FOLDER.exists()) throw new IOException("Did not copy Mac extensions to the execution directory!");
			}
			System.out.println("Have access to execution folder: "+Applet.BIN_FOLDER.getAbsolutePath());
			setReadyStateBasedOnPriorRecording();
        } catch (MalformedURLException e1) {
        	recordGUI.changeState(RecordInterface.FATAL,"Error with install");
			e1.printStackTrace();
		} catch (InterruptedException e) {
			recordGUI.changeState(RecordInterface.FATAL,"Error with install");
			e.printStackTrace();
		} catch (IOException e) {
			recordGUI.changeState(RecordInterface.FATAL,"Error with install");
			e.printStackTrace();
		}
		
		if(Applet.IS_MAC && !System.getProperty("os.version").contains("10.6")) {
			recordGUI.changeState(RecordInterface.FATAL, "Sorry, Snow Leopard required.");
		}
	}

	@Override
	public void prepareForRecording() {
		super.prepareForRecording();
		
		if(screen != null) {
			screen.closeDown();
		}
		// start up CamStudio
		screen = new ScreenRecorder();
		screen.addProcessListener(this);
		screen.start();
		// TODO check that it starts up correctly
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
