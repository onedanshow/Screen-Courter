package com.reelfx.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.Mixer;

import com.reelfx.Applet;
import com.reelfx.model.AudioRecorder;
import com.reelfx.model.PostProcessor;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.model.util.ProcessListener;
import com.reelfx.model.util.StreamGobbler;

public class WindowsController extends ApplicationController {

	public static File MERGED_OUTPUT_FILE = new File(Applet.RFX_FOLDER.getAbsolutePath()+File.separator+"output-final.mov");
	
	private AudioRecorder audio;
	private boolean stopped = false;
	private boolean recordingDone = false;
	
	public WindowsController() {
		super();
		
		if(!Applet.BIN_FOLDER.exists()){
			gui.status.setText("Performing one-time install...");
			gui.disable();
        }
	}

	@Override
	public void setupExtensions() {
		super.setupExtensions();
		try {
			if(!Applet.BIN_FOLDER.exists()){
				Applet.copyFolderFromRemoteJar(new URL(Applet.HOST_URL+"/bin-windows.jar"), "bin-windows");
				if(!Applet.BIN_FOLDER.exists()) throw new IOException("Did not copy Windows extensions to the execution directory!");
			}
			System.out.println("Have access to execution folder: "+Applet.BIN_FOLDER.getAbsolutePath());
			gui.enable();
        } catch (MalformedURLException e1) {
			gui.status.setText("Error downloading native extensions");
			e1.printStackTrace();
		} catch (IOException e) {
			gui.status.setText("Error downloading native extentions");
			e.printStackTrace();
		}
	}
	
	@Override
	public void prepareForRecording() {
		screen = new ScreenRecorder();
		screen.addProcessListener(this);
		recordingDone = false;
	}

	private Mixer audioSource = null;
	private ProcessListener listener;
	public void startRecording(Mixer source, int index) {
		audioSource = source;
		listener = this;
		
		recordingDone = false;
		
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			
			@Override
			public Object run() {
				
				deleteOutput();
				AudioRecorder.deleteOutput();
				stopped = false;
		        if(audioSource != null) {
		        	audio = new AudioRecorder(audioSource);
		        	audio.addProcessListener(listener);
		        	audio.startRecording();
		        } else {
		        	System.out.println("No audio source specified.");
		        	audio = null;
		        	startVideoRecording();
		        }
				// start up CamStudio
				screen.start();
		
				return null;
			}
		});
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
		super.processUpdate(event,body);
		switch(event) {

			case AudioRecorder.RECORDING_STARTED:
				startVideoRecording();
				break;
				
			case ScreenRecorder.RECORDING_COMPLETE:
			case AudioRecorder.RECORDING_COMPLETE:
				if(recordingDone) {
					// merge the video and audio file together (ffmpeg does this pretty quickly)
					deleteOutput();
					postProcess.saveToComputer(MERGED_OUTPUT_FILE);
				} else {
					recordingDone = true;
				}
				break;
		}
	}

	public static void deleteOutput() {
    	AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				try {
					if(MERGED_OUTPUT_FILE.exists() && !MERGED_OUTPUT_FILE.delete())
						throw new Exception("Can't delete the old preview file on Windows!");
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		});
	}
}

