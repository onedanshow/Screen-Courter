package com.reelfx.controller;

import javax.sound.sampled.Mixer;

import com.reelfx.model.ScreenRecorder;

public class LinuxController extends ApplicationController {

	@Override
	public void setupExtensions() {
		super.setupExtensions();
		// TODO copy the exec jar for linux
	}
	
	@Override
	public void prepareForRecording() {
		screen = new ScreenRecorder();
	}

	@Override
	public void startRecording(Mixer mixer) {
		screen.start(mixer);
	}

	@Override
	public void stopRecording() {
		screen.stopRecording();
	}

	public void processUpdate(int event,Object body) {
		super.processUpdate(event);
	}

}
