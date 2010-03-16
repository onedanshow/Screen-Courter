package com.reelfx.controller;

import javax.sound.sampled.Mixer;

import com.reelfx.model.AudioRecorder;
import com.reelfx.model.ScreenRecorder;

public class WindowsController extends ApplicationController {

	private AudioRecorder audio;
	
	public WindowsController() {

	}

	@Override
	public void prepareForRecording() {
		screen = new ScreenRecorder();
	}

	@Override
	public void startRecording(Mixer mixer, int index) {
		// start up CamStudio
		screen.start();
	}

	@Override
	public void stopRecording() {
		screen.stopRecording();
	}

	public void processUpdate(int event, Object body) {

	}

}
