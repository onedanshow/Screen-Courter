package com.reelfx.controller;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.sound.sampled.Mixer;
import javax.swing.JFileChooser;

import com.reelfx.Applet;
import com.reelfx.model.AudioRecorder;
import com.reelfx.model.PostProcessor;
import com.reelfx.model.PreferenceManager;
import com.reelfx.model.PreviewPlayer;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.model.util.ProcessListener;
import com.reelfx.view.OptionsInterface;
import com.reelfx.view.RecordInterface;

public abstract class ApplicationController implements ProcessListener {

	protected RecordInterface recordGUI;
	protected OptionsInterface optionsGUI;
	protected ScreenRecorder screen;
	protected PostProcessor postProcess;
	protected PreviewPlayer previewPlayer = null;
	protected PreferenceManager preferences = new PreferenceManager();

	public ApplicationController() {
		super();

		recordGUI = new RecordInterface(this);
		optionsGUI = new OptionsInterface(this);
		
		Applet.APPLET.getContentPane().add(optionsGUI);

		if (Applet.HEADLESS) {
			// don't show interface initially
		} else {
			recordGUI.setVisible(true);
			if (!Applet.BIN_FOLDER.exists()) {
				recordGUI.changeState(RecordInterface.THINKING,"Performing one-time install...");
			} else {
				setReadyStateBasedOnPriorRecording();
			}
		}
	}

	public void processUpdate(int event, Object body) {
		switch (event) {
		case PostProcessor.ENCODING_STARTED:
			recordGUI.changeState(RecordInterface.THINKING, "Encoding...");
			//Applet.sendShowStatus("Encoding...");
			break;
		case PostProcessor.ENCODING_COMPLETE:
			recordGUI.changeState(RecordInterface.READY_WITH_OPTIONS, "Finished encoding.");
			//Applet.sendHideStatus();
			break;
		case PostProcessor.POST_STARTED:
			recordGUI.changeState(RecordInterface.THINKING, "Uploading to Insight...");
			//Applet.sendShowStatus("Uploading to Insight...");
			break;
		case PostProcessor.POST_FAILED:
			recordGUI.changeState(RecordInterface.FATAL,"Uploading failed!");
			break;
		case PostProcessor.POST_COMPLETE:
			recordGUI.changeState(RecordInterface.READY_WITH_OPTIONS,"Finished uploading.");
			//Applet.sendHideStatus();
		}
	}

	public void prepareForRecording() {
		preferences.setPostUrl(Applet.POST_URL);
		preferences.setScreenCaptureName(Applet.SCREEN_CAPTURE_NAME);
		preferences.setDate(new Date());
		preferences.writePreferences();
	}

	public abstract void startRecording(Mixer mixer, int index);

	public abstract void stopRecording();

	public void previewRecording() {
		previewPlayer = new PreviewPlayer();
		previewPlayer.start();
	}

	public void askForAndSaveRecording() {
		JFileChooser fileSelect = new JFileChooser();
		int returnVal = fileSelect.showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fileSelect.getSelectedFile();
			saveRecording(file);
		}
	}

	public void saveRecording(File file) {
		if (previewPlayer != null)
			previewPlayer.stopPlayer();
		if (postProcess != null)
			postProcess.removeAllProcessListeners();
		postProcess = new PostProcessor();
		postProcess.addProcessListener(this);
		postProcess.saveToComputer(file);
	}

	public void postRecording() {
		if (previewPlayer != null)
			previewPlayer.stopPlayer();
		if (postProcess != null)
			postProcess.removeAllProcessListeners();
		postProcess = new PostProcessor();
		postProcess.addProcessListener(this);
		postProcess.postToInsight(preferences.getPostUrl());
	}

	public void deleteRecording() {
		ScreenRecorder.deleteOutput();
		AudioRecorder.deleteOutput();
		PostProcessor.deleteOutput();
		PreferenceManager.deleteOutput();
		recordGUI.changeState(RecordInterface.READY);
	}

	/**
	 * Installs ffmpeg, ffplay, and other extensions if needed.
	 */
	public void setupExtensions() {
		// recordGUI.setStatus("Hello");
	}
	
	/**
	 * Check if we need to deal with a prior screen recording or not
	 */
	protected void setReadyStateBasedOnPriorRecording() {
		if (ScreenRecorder.OUTPUT_FILE.exists()) {
			recordGUI.changeState(RecordInterface.READY_WITH_OPTIONS);
			Applet.handleExistingRecording();
		} else {
			recordGUI.changeState(RecordInterface.READY);
		}
	}
	
	public String getOptionsMessage() {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm");
		return "You have a review for "+preferences.getScreenCaptureName()+" on "+sdf.format(preferences.getDate());
	}

	public void showInterface() {
		recordGUI.setVisible(true);
		recordGUI.pack();
	}

	public void hideInterface() {
		recordGUI.setVisible(false);
	}

	/**
	 * Called by the Applet
	 */
	public void closeDown() {
		recordGUI.setVisible(false);
		recordGUI = null;
		if (postProcess != null) {
			postProcess.removeAllProcessListeners();
		}
		if (screen != null)
			screen.closeDown();
		screen = null;
	}

}
