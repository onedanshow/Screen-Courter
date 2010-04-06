package com.reelfx.controller;

import java.io.File;

import javax.sound.sampled.Mixer;
import javax.swing.JFileChooser;

import com.reelfx.Applet;
import com.reelfx.model.AudioRecorder;
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

		gui = new Interface(this);

		if (Applet.HEADLESS) {
			// don't show interface initially
		} else {
			gui.setVisible(true);
			if (!Applet.BIN_FOLDER.exists()) {
				gui.changeState(Interface.THINKING,
						"Performing one-time install...");
			} else {
				if (ScreenRecorder.OUTPUT_FILE.exists()) {
					gui.changeState(Interface.READY_WITH_OPTIONS);
				} else {
					gui.changeState(Interface.READY);
				}
			}
		}
	}

	public void processUpdate(int event, Object body) {
		switch (event) {
		case PostProcessor.ENCODING_STARTED:
			gui.changeState(Interface.THINKING, "Encoding...");
			Applet.sendShowStatus("Encoding...");
			break;
		case PostProcessor.ENCODING_COMPLETE:
			gui.changeState(Interface.READY_WITH_OPTIONS, "Finished encoding.");
			Applet.sendHideStatus();
			break;
		case PostProcessor.POST_STARTED:
			gui.changeState(Interface.THINKING, "Uploading to Insight...");
			Applet.sendShowStatus("Uploading to Insight...");
			break;
		case PostProcessor.POST_COMPLETE:
			gui
					.changeState(Interface.READY_WITH_OPTIONS,
							"Finished uploading.");
			Applet.sendHideStatus();
		}
	}

	public abstract void prepareForRecording();

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
		postProcess.postToInsight();
	}

	public void deleteRecording() {
		ScreenRecorder.deleteOutput();
		AudioRecorder.deleteOutput();
		PostProcessor.deleteOutput();
		gui.changeState(Interface.READY);
	}

	/**
	 * Installs VLC, ffmpeg and ffplay if needed.
	 */
	public void setupExtensions() {
		// gui.setStatus("Hello");
	}

	public void showInterface() {
		gui.setVisible(true);
		gui.pack();
	}

	public void hideInterface() {
		gui.setVisible(false);
	}

	public void closeDown() {
		gui.setVisible(false);
		gui = null;
		if (postProcess != null) {
			postProcess.removeAllProcessListeners();
		}
		if (screen != null)
			screen.closeDown();
		screen = null;
	}

}
