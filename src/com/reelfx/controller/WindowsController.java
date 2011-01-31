package com.reelfx.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.reelfx.Applet;
import com.reelfx.model.AudioRecorder;
import com.reelfx.model.PostProcessor;
import com.reelfx.model.AttributesManager;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.model.util.ProcessListener;
import com.reelfx.model.util.StreamGobbler;
import com.reelfx.view.PostOptions;
import com.reelfx.view.RecordControls;
import com.reelfx.view.util.MessageNotification;
import com.reelfx.view.util.ViewNotifications;

/**
 * 
 * @author Daniel Dixon (http://www.danieldixon.com)
 * 
 * 	Copyright (C) 2010  ReelFX Creative Studios (http://www.reelfx.com)
 *
 *	This program is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 * 	
 * 	This program is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 * 	
 * 	You should have received a copy of the GNU General Public License
 * 	along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
public class WindowsController extends AbstractController {

	public static File MERGED_OUTPUT_FILE = new File(Applet.BASE_FOLDER.getAbsolutePath()+File.separator+"screen_capture_temp.avi");
	private static Logger logger = Logger.getLogger(WindowsController.class);
	
	private AudioRecorder mic, systemAudio;
	private boolean stopped = false;
	private boolean recordingDone = false;
	private long audioStart, videoStart;
	
	public WindowsController() {
		super();
	}

	@Override
	public void setupExtensions() {
		//super.setupExtensions();
		try {
			if(!Applet.BIN_FOLDER.exists()){
				// delete any old versions
				for(File file : Applet.BASE_FOLDER.listFiles()) {
					if(file.getName().startsWith("bin") && file.isDirectory()) {
						FileUtils.deleteDirectory(file);
						if(file.exists())
							throw new Exception("Could not delete the old native extentions!");
					}
				}
				// download an install the new one
				String url = Applet.HOST_URL+File.separator+Applet.getBinFolderName()+".jar?"+Math.random()*10000;
				Applet.copyFolderFromRemoteJar(new URL(url), Applet.getBinFolderName());
				if(!Applet.BIN_FOLDER.exists()) {
					logger.info("Could not find native extensions at "+url+". Trying next code base url...");
					url = Applet.CODE_BASE.toString()+File.separator+Applet.getBinFolderName()+".jar?"+Math.random()*10000;
					Applet.copyFolderFromRemoteJar(new URL(url), Applet.getBinFolderName());
					if(!Applet.BIN_FOLDER.exists()) {
						logger.info("Could not find native extensions at "+url+". Trying next document base url...");
						url = Applet.DOCUMENT_BASE.toString()+File.separator+Applet.getBinFolderName()+".jar?"+Math.random()*10000;
						Applet.copyFolderFromRemoteJar(new URL(url), Applet.getBinFolderName());
						if(!Applet.BIN_FOLDER.exists()) {
							throw new IOException("Did not copy Windows extensions to the execution directory! Last url: "+url);
						}
					}
				}
			}
			logger.info("Have access to execution folder: "+Applet.BIN_FOLDER.getAbsolutePath());
			setReadyStateBasedOnPriorRecording();
        } catch (Exception e) { // possibilities: MalformedURL, IOException, etc.
        	Applet.sendViewNotification(ViewNotifications.FATAL, new MessageNotification(
        			"Error with install", 
        			"Sorry, an error occurred while installing the native extensions. Please contact an admin."));
        	logger.error("Could not install the native extensions",e);
		}
	}
	
	@Override
	public void prepareForRecording() {
		super.prepareForRecording();
		recordingDone = false;
		screen = new ScreenRecorder();
		screen.addProcessListener(this);
	}

	private AudioRecorder audioSource = null;
	private ProcessListener listener;
	public void startRecording(AudioRecorder selectedAudio) {
		audioSource = selectedAudio;
		listener = this;
		
		recordingDone = false;
		
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			
			@Override
			public Object run() {
				
				deleteOutput();
				AudioRecorder.deleteOutput();
				stopped = false;
		        if(audioSource != null) {
		        	mic = audioSource;
		        	mic.addProcessListener(listener);
		        	mic.startRecording();

		        	/*
		        	Mixer systemMixer = null;
		        	for(Mixer.Info info : AudioSystem.getMixerInfo())
		        		// select the "stereo mix" on Windows
		            	if(!info.getName().toLowerCase().contains("port")
		            			&& info.getName().toLowerCase().contains("stereo mix")
		            			&& AudioSystem.getMixer(info).getTargetLineInfo().length != 0)
		            		systemMixer = AudioSystem.getMixer(info);
		        	
		        	if(systemMixer != null) {
			        	systemAudio = new AudioRecorder(systemMixer);
			        	//systemAudio.addProcessListener(listener);
			        	systemAudio.startRecording();
		        	}
		        	*/
		        } else {
		        	logger.info("No audio source specified.");
		        	mic = null;
		        	systemAudio = null;
		        }
				// start up ffmpeg
				screen.start();
		
				return null;
			}
		});
	}

	@Override
	public void stopRecording() {
		stopped = true;
		
		if(mic != null)
            mic.stopRecording();
		else
			recordingDone = true; // if no audio, queue the post process
		
		if(systemAudio != null)
			systemAudio.stopRecording();
		
		screen.stopRecording();
		
		super.stopRecording();
	}

	public void processUpdate(int event, Object body) {
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
					opts.put(PostProcessor.MERGE_AUDIO_VIDEO, null);
					if(audioStart > videoStart) {
						long ms = videoStart - audioStart;
						float s = ((float)ms)/1000f;
						logger.info("Video delay: "+ms+" ms "+s+" s");
						opts.put(PostProcessor.OFFSET_VIDEO, s+"");
					} else if(videoStart > audioStart) {
						long ms = audioStart - videoStart;
						float s = ((float)ms)/1000f;
						logger.info("Audio delay: "+ms+" ms "+s+" s");
						opts.put(PostProcessor.OFFSET_AUDIO, s+"");
					}
			    	//postProcess.setSilent(true); // no need to notify UI for this encoding
			    	postProcess.encodingOptions(opts);
			    	postProcess.addProcessListener(this);
					postProcess.saveToComputer(MERGED_OUTPUT_FILE);
				} else {
					recordingDone = true;
				}
				break;
			
			case PostProcessor.ENCODING_COMPLETE:
				Applet.sendViewNotification(ViewNotifications.POST_OPTIONS, 
						new MessageNotification("", "What would you like to do with your new screen recording?"));
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
						throw new Exception("Can't delete the old preview file on Windows!");
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		});
	}
}