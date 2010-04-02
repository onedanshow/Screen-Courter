package com.reelfx.controller;

import java.io.File;

import javax.sound.sampled.Mixer;

import com.reelfx.Applet;
import com.reelfx.model.PostProcessor;
import com.reelfx.model.PreviewPlayer;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.model.util.ProcessListener;
import com.reelfx.view.Interface;

public abstract class ApplicationController implements ProcessListener {
	
	protected Interface gui;
	protected ScreenRecorder screen;
	protected PostProcessor postProcess;
	protected PreviewPlayer previewPlayer = null;
	
	public ApplicationController() {
		super();
		
		postProcess = new PostProcessor();
    	postProcess.addProcessListener(this);
		
		gui = new Interface(this);
		
		if(Applet.HEADLESS) {
			
		} else {
			gui.setVisible(true);
	    	gui.pack();
		}
	}
	
	public void processUpdate(int event,Object body) {
		switch(event) {
		case PostProcessor.ENCODING_STARTED:
			gui.status.setText("Encoding...");
			break;
		case PostProcessor.ENCODING_COMPLETE:
			gui.recordBtn.setEnabled(true);
			gui.saveBtn.setEnabled(true);
			gui.closeBtn.setEnabled(true);
			gui.status.setText("Finished encoding.");
			break;
		case PostProcessor.POST_STARTED:
			gui.status.setText("Uploading to Insight...");
			gui.disable();
			break;
		case PostProcessor.POST_COMPLETE:
			gui.recordBtn.setEnabled(true);
			gui.saveBtn.setEnabled(true);
			gui.closeBtn.setEnabled(true);
			gui.status.setText("Finished uploading. All done.");
		}
	}
	
	public abstract void prepareForRecording();
	
	public abstract void startRecording(Mixer mixer,int index);
	
	public abstract void stopRecording();
	
	public void previewRecording() {
        previewPlayer = new PreviewPlayer();
        previewPlayer.start();
    }
	
	public void saveRecording(File file) {
		if(previewPlayer != null)
			previewPlayer.stopPlayer();
		postProcess.saveToComputer(file);
	}
	
	public void postRecording() {
		if(previewPlayer != null)
			previewPlayer.stopPlayer();
		postProcess.postToInsight();
	}
	
	/**
     * Installs VLC, ffmpeg and ffplay if needed.
     */
	public void setupExtensions() {
		gui.status.setText("");
	}
	
	public void closeDown() {
		gui.closeInterface();
        gui.setVisible(false);
        gui = null;
        if(screen != null)
        	screen.closeDown();
        screen = null;
	}
	
}
