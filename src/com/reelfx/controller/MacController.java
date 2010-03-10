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
	
	private AudioRecorder audio;
	private boolean stopped = true;

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
    		if(!VLC_EXEC.exists() && Applet.DEV_MODE) {
    			Applet.copyFolderFromRemoteJar(new URL("jar", "", "/Users/daniel/Documents/Java/java-review-tool/lib"+File.separator+"bin-mac.jar" + "!/"), "bin-mac");
    			Runtime.getRuntime().exec("chmod 755 "+VLC_EXEC.getAbsolutePath()).waitFor();
    			if(!VLC_EXEC.exists()) throw new IOException("Did not copy VLC to its execution directory!");
    		} else */
			if(!Applet.BIN_FOLDER.exists()){
				Applet.copyFolderFromRemoteJar(new URL(Applet.CODE_BASE+"/bin-mac.jar"), "bin-mac");
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"VLC").waitFor();
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"ffmpeg").waitFor();
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"ffplay").waitFor();
				if(!Applet.BIN_FOLDER.exists()) throw new IOException("Did not copy VLC to its execution directory!");
			}
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
	
	public void processUpdate(int event,Object body) {
		super.processUpdate(event);
		switch(event) {

			case AudioRecorder.RECORDING_STARTED:
				startVideoRecording();
				break;
		}
	}

	@Override
	public void prepareForRecording() {
		// start up VLC
		screen = new ScreenRecorder();
		screen.start();
		// TODO check that it starts up correctly
	}

	@Override
	// --------- START AUDIO ---------
	public void startRecording(Mixer audioSource) {
		AudioRecorder.deleteOutput();
		stopped = false;
        if(audioSource != null) {
        	audio = new AudioRecorder(audioSource);
        	audio.addProcessListener(this);
        	audio.startRecording();
        } else {
        	audio = null;
        	startVideoRecording();
        }
	}

	// --------- START VIDEO (once audio has trigger it) ---------
	private void startVideoRecording() {
		if(!stopped) // had issue with AudioRecorder firing a START event immediately after stop
			screen.startRecording();
		else
			System.err.println("Received start video request when stopped!");
	}

	@Override
	public void stopRecording() {
		stopped = true;
		
		if(audio != null)
            audio.stopRecording();
		
		screen.stopRecording();
	}
}
