package com.reelfx.controller;

import javax.sound.sampled.Mixer;

import com.reelfx.model.ScreenRecorder;

public class LinuxController extends ApplicationController {

	@Override
	public void setupExtensions() {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void prepareForRecording() {
		screen = new ScreenRecorder();
	}

	@Override
	public void startRecording(Mixer mixer) {
		screen.start();
	}

	@Override
	public void stopRecording() {
		screen.stopRecording();
	}

	public void processUpdate(int event) {
		super.processUpdate(event);
	}

}
