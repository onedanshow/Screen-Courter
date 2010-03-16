package com.reelfx.controller;

import javax.sound.sampled.Mixer;

import com.reelfx.model.AudioRecorder;
import com.reelfx.model.ScreenRecorder;

public class WindowsController extends ApplicationController {

	private AudioRecorder audio;
	private boolean stopped = false;
	
	public WindowsController() {
		super();
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
