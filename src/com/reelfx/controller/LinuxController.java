package com.reelfx.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.reelfx.Applet;
import com.reelfx.model.AudioRecorder;
import com.reelfx.model.PostProcessor;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.view.PostOptions;
import com.reelfx.view.RecordControls;
import com.reelfx.view.util.MessageNotification;
import com.reelfx.view.util.ViewNotifications;

public class LinuxController extends ApplicationController {
	
	public static File MERGED_OUTPUT_FILE = new File(Applet.RFX_FOLDER.getAbsolutePath()+File.separator+"screen_capture_temp.mov");
	
	private AudioRecorder audio;
	private boolean recordingDone = false;
	private long audioStart, videoStart;
	
	public LinuxController() {
		super();
	}
	
	@Override
	public void setupExtensions() {
		super.setupExtensions();
		try {
			if(!Applet.BIN_FOLDER.exists()){
				Applet.copyFolderFromRemoteJar(new URL(Applet.HOST_URL+"/bin-linux.jar?"+Math.random()*10000), "bin-linux");
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"ffmpeg").waitFor();
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"ffplay").waitFor();
				if(!Applet.BIN_FOLDER.exists()) throw new IOException("Did not copy Linux extensions to the execution directory!");
				setReadyStateBasedOnPriorRecording();
			}
			System.out.println("Have access to execution folder: "+Applet.BIN_FOLDER.getAbsolutePath());
        } catch (Exception e) { // possibilities: MalformedURL, InterriptedException, IOException
        	Applet.sendViewNotification(ViewNotifications.FATAL, new MessageNotification(
        			"Error with install",
        			"Sorry, an error occurred while installing the native extensions. Please contact an Insight admin."));
        	//recordGUI.changeState(RecordControls.FATAL,"Error with install");
        	//optionsGUI.changeState(PostOptions.FATAL, "Sorry, an error occurred while installing the native extensions. Please contact an Insight admin.");
			e.printStackTrace();
		}
	}
	
	@Override
	public void prepareForRecording() {
		super.prepareForRecording();
		recordingDone = false;
		screen = new ScreenRecorder();
		screen.addProcessListener(this);
	}

	@Override
	public void startRecording(AudioRecorder selectedAudio) {
		deleteOutput();
		AudioRecorder.deleteOutput();
        if(selectedAudio != null) {
        	audio = selectedAudio;
        	audio.addProcessListener(this);
        	audio.startRecording();
        } else {
        	System.out.println("No audio source specified.");
        	audio = null;
        }
		// start up ffmpeg
		screen.start();
	}

	@Override
	public void stopRecording() {
		if(audio != null)
            audio.stopRecording();
		else
			recordingDone = true; // if no audio, queue the post process
		System.out.println("About to stop screen recording...");
		screen.stopRecording();
		super.stopRecording(); // call after (has nothing but GUI updates)
	}

	public void processUpdate(int event,Object body) {
		super.processUpdate(event,body);
		switch(event) {
	
			case AudioRecorder.RECORDING_STARTED:
				audioStart = Calendar.getInstance().getTimeInMillis();
				break;
			case ScreenRecorder.RECORDING_STARTED:
				videoStart = Calendar.getInstance().getTimeInMillis();
				break;
				
			case ScreenRecorder.RECORDING_COMPLETE:
			case AudioRecorder.RECORDING_COMPLETE:
				if(recordingDone) {
					// merge the video and audio file together (ffmpeg does this pretty quickly)
					deleteOutput();
					if(postProcess != null)
						postProcess.removeAllProcessListeners();
					postProcess = new PostProcessor();
					Map<Integer,String> opts = new HashMap<Integer, String>();
					if(audioStart > videoStart) {
						long ms = videoStart - audioStart;
						float s = ((float)ms)/1000f;
						System.out.println("Video delay: "+ms+" ms "+s+" s");
						opts.put(PostProcessor.OFFSET_VIDEO, s+"");
					} else if(videoStart > audioStart) {
						long ms = audioStart - videoStart;
						float s = ((float)ms)/1000f;
						System.out.println("Audio delay: "+ms+" ms "+s+" s");
						opts.put(PostProcessor.OFFSET_AUDIO, s+"");
					}
			    	postProcess.setSilent(true); // no need to notify UI for this encoding
			    	postProcess.encodingOptions(opts);
					postProcess.saveToComputer(MERGED_OUTPUT_FILE);
				} else {
					recordingDone = true;
				}
				break;
			}
	}
	
	public void deleteRecording() {
		deleteOutput();
		super.deleteRecording();
	}
	
	public static void deleteOutput() {
    	AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				try {
					if(MERGED_OUTPUT_FILE.exists() && !MERGED_OUTPUT_FILE.delete())
						throw new Exception("Can't delete the old preview file on Linux!");
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		});
	}
}
