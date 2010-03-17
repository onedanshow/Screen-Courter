package com.reelfx.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.sampled.Mixer;

import com.reelfx.Applet;
import com.reelfx.model.AudioRecorder;
import com.reelfx.model.ScreenRecorder;

public class WindowsController extends ApplicationController {

	private AudioRecorder audio;
	private boolean stopped = false;
	
	public WindowsController() {
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
			if(!Applet.BIN_FOLDER.exists()){
				Applet.copyFolderFromRemoteJar(new URL(Applet.CODE_BASE+"/bin-windows.jar"), "bin-windows");
				if(!Applet.BIN_FOLDER.exists()) throw new IOException("Did not copy Windows extensions to the execution directory!");
			}
			System.out.println("Have access to execution folder: "+Applet.BIN_FOLDER.getAbsolutePath());
			gui.enable();
        } catch (MalformedURLException e1) {
			gui.status.setText("Error downloading native extensions");
			e1.printStackTrace();
		} catch (IOException e) {
			gui.status.setText("Error downloading native extentions");
			e.printStackTrace();
		}
	}
	
	@Override
	public void prepareForRecording() {
		screen = new ScreenRecorder();
	}

	@Override
	public void startRecording(Mixer audioSource, int index) {
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
		// start up CamStudio
		screen.start();
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

	public void processUpdate(int event, Object body) {
		super.processUpdate(event);
		switch(event) {

			case AudioRecorder.RECORDING_STARTED:
				startVideoRecording();
				break;
		}
	}

}
