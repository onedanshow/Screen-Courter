package com.reelfx.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.sound.sampled.Mixer;
import javax.swing.JFileChooser;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

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

	public RecordInterface recordGUI;
	public OptionsInterface optionsGUI;
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
			optionsGUI.changeState(OptionsInterface.THINKING, "Encoding...");
			//Applet.sendShowStatus("Encoding...");
			break;
		case PostProcessor.ENCODING_COMPLETE:
			//recordGUI.changeState(RecordInterface.READY_WITH_OPTIONS, "Finished encoding.");
			recordGUI.changeState(RecordInterface.READY, "Finished encoding.");
			optionsGUI.changeState(OptionsInterface.OPTIONS, "Finished encoding.");
			//Applet.sendHideStatus();
			break;
		case PostProcessor.POST_STARTED:
			recordGUI.changeState(RecordInterface.THINKING, "Uploading to Insight...");
			optionsGUI.changeState(OptionsInterface.THINKING, "Uploading your screen recording to Insight. Do NOT close the browser window.");
			//Applet.sendShowStatus("Uploading to Insight...");
			break;
		case PostProcessor.POST_FAILED:
			recordGUI.changeState(RecordInterface.FATAL,"Uploading failed!");
			optionsGUI.changeState(OptionsInterface.FATAL,"An error occurred while uploading the screen recording. It is stored locally, so you can try again later.");
			break;
		case PostProcessor.POST_COMPLETE:
			//recordGUI.changeState(RecordInterface.READY_WITH_OPTIONS,"Finished uploading.");
			recordGUI.changeState(RecordInterface.READY,"Finished uploading.");
			optionsGUI.changeState(OptionsInterface.OPTIONS_NO_UPLOAD,"Would you like to do anything else with your screen recording?");
			preferences.setUploaded(true);
			preferences.writePreferences();
			//Applet.sendHideStatus();
		}
	}

	public void prepareForRecording() {
		preferences.setPostUrl(Applet.POST_URL);
		preferences.setScreenCaptureName(Applet.SCREEN_CAPTURE_NAME);
		preferences.setDate(new Date());
		preferences.setUploaded(false);
		preferences.writePreferences();
	}

	public abstract void startRecording(AudioRecorder audio);

	public void prepareAndRecord() {
		recordGUI.prepareForRecording();
	}
	
	public void stopRecording() {
		recordGUI.changeState(RecordInterface.READY);
		optionsGUI.changeState(OptionsInterface.OPTIONS,"What would you like to do with your new screen recording?");
		Applet.handleFreshRecording();
	}

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
	
	// called first
	public void postData() {
		if (previewPlayer != null)
			previewPlayer.stopPlayer();
		if (postProcess != null)
			postProcess.removeAllProcessListeners();
		postProcess = new PostProcessor();
		postProcess.addProcessListener(this);
		postProcess.postDataToInsight(preferences.getPostUrl());
	}

	// called on the upload page
	public void postRecording() {
		if (previewPlayer != null)
			previewPlayer.stopPlayer();
		if (postProcess != null)
			postProcess.removeAllProcessListeners();
		postProcess = new PostProcessor();
		postProcess.addProcessListener(this);
		postProcess.postRecordingToInsight(preferences.getPostUrl());
	}

	public void deleteRecording() {
		ScreenRecorder.deleteOutput();
		AudioRecorder.deleteOutput();
		PostProcessor.deleteOutput();
		PreferenceManager.deleteOutput();
		recordGUI.changeState(RecordInterface.READY);
		optionsGUI.changeState(OptionsInterface.DISABLED);
		Applet.handleDeletedRecording();
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
			//recordGUI.changeState(RecordInterface.READY_WITH_OPTIONS);
			recordGUI.changeState(RecordInterface.READY);
			SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm");
			String message = "You have a screen recording for "+preferences.getScreenCaptureName()+" on "+sdf.format(preferences.getDate())+". ";
			if(preferences.isUploaded()) {
				message += "It has already been uploaded.";
				optionsGUI.changeState(OptionsInterface.OPTIONS_NO_UPLOAD,message);
			} else {
				optionsGUI.changeState(OptionsInterface.OPTIONS,message);
			}
			Applet.handleExistingRecording();
		} else {
			recordGUI.changeState(RecordInterface.READY);
			optionsGUI.changeState(OptionsInterface.DISABLED);
		}
	}

	public void showRecordingInterface() {
		recordGUI.setVisible(true);
		recordGUI.pack();
	}

	public void hideRecordingInterface() {
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
