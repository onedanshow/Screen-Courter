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
import com.reelfx.model.CaptureViewport;
import com.reelfx.model.PostProcessor;
import com.reelfx.model.PreferenceManager;
import com.reelfx.model.PreviewPlayer;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.model.util.ProcessListener;
import com.reelfx.view.CropHandle;
import com.reelfx.view.InformationBox;
import com.reelfx.view.PostOptions;
import com.reelfx.view.RecordControls;
import com.reelfx.view.util.ViewNotifications;

public abstract class ApplicationController implements ProcessListener {

	public RecordControls recordGUI;
	public PostOptions optionsGUI;

	protected InformationBox infoBox;
	// protected CropHandle TLhandle, TMhandle, TRhandle, MRhandle, BRhandle,
	// BMhandle, BLhandle, MLhandle;

	protected ScreenRecorder screen;
	protected PostProcessor postProcess;
	protected PreviewPlayer previewPlayer = null;
	protected PreferenceManager preferences = new PreferenceManager();
	protected CaptureViewport captureViewport = new CaptureViewport();

	public ApplicationController() {
		super();

		// generate the common GUI
		recordGUI = new RecordControls(this);
		optionsGUI = new PostOptions(this);
		infoBox = new InformationBox();
		new CropHandle(CropHandle.TOP_LEFT);
		new CropHandle(CropHandle.TOP_MIDDLE);
		new CropHandle(CropHandle.TOP_RIGHT);
		new CropHandle(CropHandle.BOTTOM_LEFT);
		new CropHandle(CropHandle.BOTTOM_RIGHT);

		Applet.APPLET.getContentPane().add(optionsGUI); // note, if this line
														// changes, also change
														// Applet.sendViewNotification

		if (Applet.HEADLESS) {
			// don't show interface initially
		} else {
			Applet.sendViewNotification(ViewNotifications.SHOW_ALL);
			if (!Applet.BIN_FOLDER.exists()) {
				recordGUI.changeState(RecordControls.THINKING,
						"Performing one-time install...");
			} else {
				setReadyStateBasedOnPriorRecording();
			}
		}
	}

	public void processUpdate(int event, Object body) {
		switch (event) {
		case PostProcessor.ENCODING_STARTED:
			recordGUI.changeState(RecordControls.THINKING, "Encoding...");
			optionsGUI.changeState(PostOptions.THINKING, "Encoding...");
			// Applet.sendShowStatus("Encoding...");
			break;
		case PostProcessor.ENCODING_COMPLETE:
			// recordGUI.changeState(RecordControls.READY_WITH_OPTIONS,
			// "Finished encoding.");
			recordGUI.changeState(RecordControls.READY, "Finished encoding.");
			optionsGUI.changeState(PostOptions.OPTIONS, "Finished encoding.");
			// Applet.sendHideStatus();
			break;
		case PostProcessor.POST_STARTED:
			recordGUI.changeState(RecordControls.THINKING,
					"Uploading to Insight...");
			optionsGUI
					.changeState(PostOptions.THINKING,
							"Uploading your screen recording to Insight. Do NOT close the browser window.");
			// Applet.sendShowStatus("Uploading to Insight...");
			break;
		case PostProcessor.POST_FAILED:
			recordGUI.changeState(RecordControls.FATAL, "Uploading failed!");
			optionsGUI
					.changeState(
							PostOptions.FATAL,
							"An error occurred while uploading the screen recording. It is stored locally, so you can try again later.");
			break;
		case PostProcessor.POST_COMPLETE:
			// recordGUI.changeState(RecordControls.READY_WITH_OPTIONS,"Finished uploading.");
			recordGUI.changeState(RecordControls.READY, "Finished uploading.");
			optionsGUI
					.changeState(PostOptions.OPTIONS_NO_UPLOAD,
							"Would you like to do anything else with your screen recording?");
			preferences.setUploaded(true);
			preferences.writePreferences();
			// Applet.sendHideStatus();
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
		recordGUI.changeState(RecordControls.READY);
		optionsGUI.changeState(PostOptions.OPTIONS,
				"What would you like to do with your new screen recording?");
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
		recordGUI.changeState(RecordControls.READY);
		optionsGUI.changeState(PostOptions.DISABLED);
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
			// recordGUI.changeState(RecordControls.READY_WITH_OPTIONS);
			recordGUI.changeState(RecordControls.READY);
			SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm");
			String message = "You have a screen recording for "
					+ preferences.getScreenCaptureName() + " on "
					+ sdf.format(preferences.getDate()) + ". ";
			if (preferences.isUploaded()) {
				message += "It has already been uploaded.";
				optionsGUI.changeState(PostOptions.OPTIONS_NO_UPLOAD, message);
			} else {
				optionsGUI.changeState(PostOptions.OPTIONS, message);
			}
			Applet.handleExistingRecording();
		} else {
			recordGUI.changeState(RecordControls.READY);
			optionsGUI.changeState(PostOptions.DISABLED);
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
