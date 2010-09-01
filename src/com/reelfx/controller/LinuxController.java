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

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.reelfx.Applet;
import com.reelfx.model.AudioRecorder;
import com.reelfx.model.PostProcessor;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.view.PostOptions;
import com.reelfx.view.RecordControls;
import com.reelfx.view.util.MessageNotification;
import com.reelfx.view.util.ViewNotifications;

/**
 * 
 * @author Daniel Dixon (http://www.danieldixon.com)
 *
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
public class LinuxController extends AbstractController {
	
	public static File MERGED_OUTPUT_FILE = new File(Applet.BASE_FOLDER.getAbsolutePath()+File.separator+"screen_capture_temp.mov");
	private static Logger logger = Logger.getLogger(LinuxController.class);
	
	private AudioRecorder audio;
	private boolean recordingDone = false;
	private long audioStart, videoStart;
	
	public LinuxController() {
		super();
	}
	
	@Override
	public void setupExtensions() {
		//super.setupExtensions();
		try {
			if(!Applet.BIN_FOLDER.exists()) {
				// delete any old versions
				for(File file : Applet.BASE_FOLDER.listFiles()) {
					if(file.getName().startsWith("bin") && file.isDirectory()) {
						FileUtils.deleteDirectory(file);
						if(file.exists())
							throw new Exception("Could not delete the old native extentions!");
					}
				}
				// download an install the new one
				Applet.copyFolderFromRemoteJar(new URL(Applet.HOST_URL+"/"+Applet.getBinFolderName()+".jar?"+Math.random()*10000),Applet.getBinFolderName());
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"ffmpeg").waitFor();
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"ffplay").waitFor();
				if(!Applet.BIN_FOLDER.exists()) throw new IOException("Did not copy Linux extensions to the execution directory!");
			}
			logger.info("Have access to execution folder: "+Applet.BIN_FOLDER.getAbsolutePath());
			setReadyStateBasedOnPriorRecording();
        } catch (Exception e) { // possibilities: MalformedURL, InterriptedException, IOException
        	Applet.sendViewNotification(ViewNotifications.FATAL, new MessageNotification(
        			"Error with install",
        			"Sorry, an error occurred while installing the native extensions. Please contact an Insight admin."));
        	logger.error("Could not install native extensions",e);
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
						throw new Exception("Can't delete the old preview file on Linux!");
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		});
	}
}
