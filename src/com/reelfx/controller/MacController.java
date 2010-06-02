package com.reelfx.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.sampled.Mixer;

import netscape.javascript.JSException;

import com.reelfx.Applet;
import com.reelfx.model.AudioRecorder;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.view.PostOptions;
import com.reelfx.view.RecordControls;
import com.reelfx.view.util.MessageNotification;
import com.reelfx.view.util.ViewNotifications;

public class MacController extends ApplicationController {

	public MacController() {
		super();
	}
	
	@Override
	public void setupExtensions() {
		super.setupExtensions();
		try {
        	/* might revisit copying the jar locally later
    		if(!MAC_EXEC.exists() && Applet.DEV_MODE) {
    			Applet.copyFolderFromRemoteJar(new URL("jar", "", "/Users/daniel/Documents/Java/java-review-tool/lib"+File.separator+"bin-mac.jar" + "!/"), "bin-mac");
    			Runtime.getRuntime().exec("chmod 755 "+MAC_EXEC.getAbsolutePath()).waitFor();
    			if(!MAC_EXEC.exists()) throw new IOException("Did not copy VLC to its execution directory!");
    		} else */
			if(!Applet.BIN_FOLDER.exists()){
				Applet.copyFolderFromRemoteJar(new URL(Applet.HOST_URL+"/bin-mac.jar?"+Math.random()*10000), "bin-mac");
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"mac-screen-recorder").waitFor();
				if(!Applet.BIN_FOLDER.exists()) throw new IOException("Did not copy Mac extensions to the execution directory!");
				setReadyStateBasedOnPriorRecording();
			}
			System.out.println("Have access to execution folder: "+Applet.BIN_FOLDER.getAbsolutePath());
        } catch (Exception e) { // possibilities: MalformedURL, InterruptedException, IOException
        	Applet.sendViewNotification(ViewNotifications.FATAL, new MessageNotification(
        			"Error with install",
        			"Sorry, an error occurred while installing the native extensions. Please contact an Insight admin."));
        	//recordGUI.changeState(RecordControls.FATAL,"Error with install");
        	//optionsGUI.changeState(PostOptions.FATAL, "Sorry, an error occurred while installing the native extensions. Please contact an Insight admin.");
			e.printStackTrace();
		}

		if(Applet.IS_MAC && !System.getProperty("os.version").contains("10.6")) {
			Applet.sendViewNotification(ViewNotifications.FATAL, new MessageNotification(
					"Sorry, Snow Leopard required.", "Sorry, this tool requires that you have Mac OS X 10.6 (Snow Leopard)"));
			//recordGUI.changeState(RecordControls.FATAL, "Sorry, Snow Leopard required.");
		}
	}

	@Override
	public void prepareForRecording() {
		super.prepareForRecording();
		
		if(screen != null) {
			screen.closeDown();
		}
		// start up CamStudio
		screen = new ScreenRecorder();
		screen.addProcessListener(this);
		screen.start();
		// TODO check that it starts up correctly
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
	}
	
	@Override
	public void showRecordingInterface() {
		Applet.sendViewNotification(ViewNotifications.SHOW_RECORD_CONTROLS);
	}
}
