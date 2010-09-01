package com.reelfx.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.sampled.Mixer;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import netscape.javascript.JSException;

import com.reelfx.Applet;
import com.reelfx.model.AudioRecorder;
import com.reelfx.model.ScreenRecorder;
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
public class MacController extends AbstractController {

	private static Logger logger = Logger.getLogger(MacController.class);	
	
	public MacController() {
		super();
	}
	
	@Override
	public void setupExtensions() {
		//super.setupExtensions();
		try {
        	/* might revisit copying the jar locally later
    		if(!MAC_EXEC.exists() && Applet.DEV_MODE) {
    			Applet.copyFolderFromRemoteJar(new URL("jar", "", "/Users/daniel/Documents/Java/java-review-tool/lib"+File.separator+"bin-mac.jar" + "!/"), "bin-mac");
    			Runtime.getRuntime().exec("chmod 755 "+MAC_EXEC.getAbsolutePath()).waitFor();
    			if(!MAC_EXEC.exists()) throw new IOException("Did not copy VLC to its execution directory!");
    		} else */
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
				String url = Applet.HOST_URL+"/"+Applet.getBinFolderName()+".jar?"+Math.random()*10000;
				Applet.copyFolderFromRemoteJar(new URL(url), Applet.getBinFolderName());
				if(!Applet.BIN_FOLDER.exists()) {
					logger.info("Could not find native extensions at "+url+". Trying code base url...");
					url = Applet.CODE_BASE.toString()+"/"+Applet.getBinFolderName()+".jar?"+Math.random()*10000;
					Applet.copyFolderFromRemoteJar(new URL(url), Applet.getBinFolderName());
					if(!Applet.BIN_FOLDER.exists()) {
						logger.info("Could not find native extensions at "+url+". Trying document base url...");
						url = Applet.DOCUMENT_BASE.toString()+"/"+Applet.getBinFolderName()+".jar?"+Math.random()*10000;
						Applet.copyFolderFromRemoteJar(new URL(url), Applet.getBinFolderName());
						if(!Applet.BIN_FOLDER.exists()) {
							throw new IOException("Did not copy Mac extensions to the execution directory! Last url: "+url);
						}
					}
				}
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"mac-screen-recorder").waitFor();
			}
			logger.info("Have access to execution folder: "+Applet.BIN_FOLDER.getAbsolutePath());
			setReadyStateBasedOnPriorRecording();
        } catch (Exception e) { // possibilities: MalformedURL, InterruptedException, IOException
        	Applet.sendViewNotification(ViewNotifications.FATAL, new MessageNotification(
        			"Error with install",
        			"Sorry, an error occurred while installing the native extensions. Please contact an admin."));
			logger.error("Could not install the native extensions",e);
		}

		if(Applet.IS_MAC && !System.getProperty("os.version").contains("10.6")) {
			Applet.sendViewNotification(ViewNotifications.FATAL, new MessageNotification(
					"Sorry, Snow Leopard required.", "Sorry, this tool requires that you have Mac OS X 10.6 (Snow Leopard)"));
		}
	}

	@Override
	public void prepareForRecording() {
		super.prepareForRecording();
		
		if(screen != null) {
			screen.closeDown();
		}
		screen = new ScreenRecorder();
		screen.addProcessListener(this);
		screen.start();
		// TODO check that it starts up correctly?
	}

	@Override
	public void startRecording(AudioRecorder audio) {
		if(screen != null)
			screen.startRecording();
	}

	@Override
	public void stopRecording() {	
		super.stopRecording();
		if(screen != null)
			screen.stopRecording();
		Applet.sendViewNotification(ViewNotifications.POST_OPTIONS, 
				new MessageNotification("", "What would you like to do with your new screen recording?"));
	}
	
	@Override
	public void showRecordingInterface() {
		Applet.sendViewNotification(ViewNotifications.SHOW_RECORD_CONTROLS);
	}
}
