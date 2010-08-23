package com.reelfx.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
import org.apache.log4j.Logger;

import com.reelfx.Applet;
import com.reelfx.model.AudioRecorder;
import com.reelfx.model.CaptureViewport;
import com.reelfx.model.PostProcessor;
import com.reelfx.model.AttributesManager;
import com.reelfx.model.PreferencesManager;
import com.reelfx.model.PreviewPlayer;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.model.util.ProcessListener;
import com.reelfx.view.CropHandle;
import com.reelfx.view.CropLine;
import com.reelfx.view.InformationBox;
import com.reelfx.view.PostOptions;
import com.reelfx.view.RecordControls;
import com.reelfx.view.util.MessageNotification;
import com.reelfx.view.util.ViewNotifications;

/**
 * Base class for the OS-specific controllers.
 * 
 * @author Daniel Dixon (http://www.danieldixon.com)
 *
 */
public abstract class AbstractController implements ProcessListener {

	public RecordControls recordGUI;
	public PostOptions optionsGUI;
	private static Logger logger = Logger.getLogger(AbstractController.class);

	protected InformationBox infoBox;

	protected ScreenRecorder screen;
	protected PostProcessor postProcess;
	protected PreviewPlayer previewPlayer = null;
	protected AttributesManager recordingAttributes = new AttributesManager();
	protected CaptureViewport captureViewport = new CaptureViewport();

	public AbstractController() {
		super();
		
		if (PreferencesManager.hasPreferences()) {
			Applet.sendViewNotification(ViewNotifications.SET_CAPTURE_VIEWPORT,PreferencesManager.FROM_PREFERENCES);
		}

		// generate the common GUI
		recordGUI = new RecordControls(this);
		optionsGUI = new PostOptions(this);
		infoBox = new InformationBox();
		
		Applet.APPLET_WINDOWS.add(recordGUI);
		Applet.APPLET_WINDOWS.add(infoBox);
		
		Applet.APPLET_WINDOWS.add(new CropLine(CropLine.TOP));
		Applet.APPLET_WINDOWS.add(new CropLine(CropLine.RIGHT));
		Applet.APPLET_WINDOWS.add(new CropLine(CropLine.BOTTOM));
		Applet.APPLET_WINDOWS.add(new CropLine(CropLine.LEFT));
		
		Applet.APPLET_WINDOWS.add(new CropHandle(CropHandle.TOP_LEFT));
		Applet.APPLET_WINDOWS.add(new CropHandle(CropHandle.TOP_MIDDLE));
		Applet.APPLET_WINDOWS.add(new CropHandle(CropHandle.TOP_RIGHT));
		Applet.APPLET_WINDOWS.add(new CropHandle(CropHandle.MIDDLE_RIGHT));
		Applet.APPLET_WINDOWS.add(new CropHandle(CropHandle.BOTTOM_LEFT));
		Applet.APPLET_WINDOWS.add(new CropHandle(CropHandle.BOTTOM_MIDDLE));
		Applet.APPLET_WINDOWS.add(new CropHandle(CropHandle.BOTTOM_RIGHT));
		Applet.APPLET_WINDOWS.add(new CropHandle(CropHandle.MIDDLE_LEFT));

		Applet.APPLET.getContentPane().add(optionsGUI); // note, if this line
														// changes, also change
														// Applet.sendViewNotification
		
		if (Applet.HEADLESS) {
			// don't show interface initially
		} else {
			if(Applet.IS_MAC && !Applet.DEV_MODE) { // TODO temporary
				Applet.sendViewNotification(ViewNotifications.SHOW_RECORD_CONTROLS);
			} else {
				Applet.sendViewNotification(ViewNotifications.SHOW_ALL);
			}
			if (!Applet.BIN_FOLDER.exists()) {
				Applet.sendViewNotification(ViewNotifications.THINKING, 
						new MessageNotification("Performing one-time install...","Please wait as the program performs a one-time install / update of its native extensions."));
			} else {
				setReadyStateBasedOnPriorRecording();
			}
		}
	}

	/**
	 * 	The listener method when a process has a notification.
	 */
	public void processUpdate(int event, Object body) {
		switch (event) {
		case PostProcessor.ENCODING_STARTED:
			Applet.sendViewNotification(ViewNotifications.THINKING, new MessageNotification("Encoding...", "Encoding..."));
			break;
		case PostProcessor.ENCODING_FAILED:	
			Applet.sendViewNotification(ViewNotifications.FATAL, 
					new MessageNotification("Encoding failed!", "An error occurred while encoding the screen recording. Please contact an Insight development."));
			break;	
		case PostProcessor.ENCODING_COMPLETE:
			Applet.sendViewNotification(ViewNotifications.POST_OPTIONS, 
					new MessageNotification("Finished encoding.","Your screen recording has been encoded and saved."));
			break;
		case PostProcessor.POST_STARTED:
			Applet.sendViewNotification(ViewNotifications.THINKING, 
					new MessageNotification("Uploading to Insight...", "Uploading your screen recording to Insight. Do NOT close the browser window."));
			break;	
		case PostProcessor.POST_FAILED:
			Applet.sendViewNotification(ViewNotifications.FATAL, 
					new MessageNotification("Upload failed!", "An error occurred while uploading the screen recording. It is stored locally, so you can try again later."));
			break;
		case PostProcessor.POST_COMPLETE:
			Applet.sendViewNotification(ViewNotifications.POST_OPTIONS_NO_UPLOADING, 
					new MessageNotification("Finished uploading.", "Would you like to do anything else with your screen recording?"));			
			recordingAttributes.setUploaded(true);
			recordingAttributes.writeAttributes();
			Applet.handleUploadedRecording();
		}
	}
	
	public void prepareForRecording() {
		recordingAttributes.setPostUrl(Applet.POST_URL);
		recordingAttributes.setScreenCaptureName(Applet.SCREEN_CAPTURE_NAME);
		recordingAttributes.setDate(new Date());
		recordingAttributes.setUploaded(false);
		recordingAttributes.writeAttributes();
	}

	public abstract void startRecording(AudioRecorder audio);

	public void prepareAndRecord() {
		recordGUI.prepareForRecording();
	}

	public void stopRecording() {
		/* moved to OS specific controllers themselves
		 Applet.sendViewNotification(ViewNotifications.POST_OPTIONS, 
				new MessageNotification("", "What would you like to do with your new screen recording?"));*/
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
		Map<Integer,String> opts = new HashMap<Integer, String>();
		opts.put(PostProcessor.ENCODE_TO_X264, null);
		postProcess.encodingOptions(opts);
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
		postProcess.postDataToInsight(recordingAttributes.getPostUrl());
	}

	// called on the upload page
	public void postRecording() {
		if (previewPlayer != null)
			previewPlayer.stopPlayer();
		if (postProcess != null)
			postProcess.removeAllProcessListeners();
		postProcess = new PostProcessor();
		Map<Integer,String> opts = new HashMap<Integer, String>();
		opts.put(PostProcessor.ENCODE_TO_X264, null);
		postProcess.encodingOptions(opts);
		postProcess.addProcessListener(this);
		postProcess.postRecordingToInsight(recordingAttributes.getPostUrl());
	}

	public void deleteRecording() {
		ScreenRecorder.deleteOutput();
		AudioRecorder.deleteOutput();
		PostProcessor.deleteOutput();
		AttributesManager.deleteOutput();
		Applet.sendViewNotification(ViewNotifications.READY);
		Applet.handleDeletedRecording();
	}

	/**
	 * Installs ffmpeg, ffplay, and other extensions if needed.
	 */
	public abstract void setupExtensions();

	/**
	 * Check if we need to deal with a prior screen recording or not
	 */
	protected void setReadyStateBasedOnPriorRecording() {		
		if (AttributesManager.OUTPUT_FILE.exists()) {
			SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm");
			String message = "You have a screen recording for \""
					+ recordingAttributes.getScreenCaptureName() + "\" on "
					+ sdf.format(recordingAttributes.getDate()) + ". ";
			if (recordingAttributes.isUploaded()) {
				message += "It has already been uploaded.";
				Applet.sendViewNotification(ViewNotifications.POST_OPTIONS_NO_UPLOADING,new MessageNotification("", message));
			} else {
				Applet.sendViewNotification(ViewNotifications.POST_OPTIONS,new MessageNotification("", message));
			}
			Applet.handleExistingRecording();
		} else if (Applet.HEADLESS) {
			// don't show interface initially
		} else {
			if(Applet.IS_MAC && !Applet.DEV_MODE) { // TODO temporary
				Applet.sendViewNotification(ViewNotifications.SHOW_RECORD_CONTROLS);
			} else {
				Applet.sendViewNotification(ViewNotifications.SHOW_ALL);
			}
		}
	}

	public void showRecordingInterface() {
		Applet.sendViewNotification(ViewNotifications.SHOW_ALL);
	}

	public void hideRecordingInterface() {
		Applet.sendViewNotification(ViewNotifications.HIDE_ALL);
	}

	/**
	 * Called by the Applet
	 */
	public void closeDown() {
		recordGUI.setVisible(false);
		recordGUI.audioSelect.closeDown();
		recordGUI = null;
		if (postProcess != null) {
			postProcess.removeAllProcessListeners();
		}
		if (screen != null)
			screen.closeDown();
		screen = null;
	}

}
