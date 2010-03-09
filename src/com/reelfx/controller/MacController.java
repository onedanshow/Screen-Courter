package com.reelfx.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;

import javax.sound.sampled.Mixer;
import javax.swing.Timer;

import org.apache.commons.net.telnet.TelnetClient;

import com.reelfx.Applet;
import com.reelfx.model.AudioRecorder;
import com.reelfx.model.PostProcessor;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.view.Interface;

public class MacController extends ApplicationController {
	
	private TelnetClient telnet = new TelnetClient();
	private AudioRecorder audio;

	public MacController() {
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
        	/* might revisit copying the jar locally later
    		if(!VLC_EXEC.exists() && Applet.DEV_MODE) {
    			Applet.copyFolderFromRemoteJar(new URL("jar", "", "/Users/daniel/Documents/Java/java-review-tool/lib"+File.separator+"bin-mac.jar" + "!/"), "bin-mac");
    			Runtime.getRuntime().exec("chmod 755 "+VLC_EXEC.getAbsolutePath()).waitFor();
    			if(!VLC_EXEC.exists()) throw new IOException("Did not copy VLC to its execution directory!");
    		} else */
			if(!Applet.BIN_FOLDER.exists()){
				Applet.copyFolderFromRemoteJar(new URL(Applet.CODE_BASE+"/bin-mac.jar"), "bin-mac");
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"VLC").waitFor();
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"ffmpeg").waitFor();
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"ffplay").waitFor();
				if(!Applet.BIN_FOLDER.exists()) throw new IOException("Did not copy VLC to its execution directory!");
			}
        } catch (MalformedURLException e1) {
			gui.status.setText("Error downloading native extensions");
			e1.printStackTrace();
		} catch (InterruptedException e) {
			gui.status.setText("Error setting up native extentions");
			e.printStackTrace();
		} catch (IOException e) {
			gui.status.setText("Error downloading native extentions");
			e.printStackTrace();
		}
	}
	
	public void processUpdate(int event) {
		super.processUpdate(event);
		switch(event) {

			case AudioRecorder.RECORDING_STARTED:
				startVideoRecording();
				break;
		}
	}

	@Override
	public void prepareForRecording() {
		// start up VLC
		screen = new ScreenRecorder();
		screen.start();
		// TODO check that it starts up correctly
	}

	@Override
	// --------- START AUDIO ---------
	public void startRecording(Mixer audioSource) {
        if(audioSource != null) {
        	audio = new AudioRecorder(audioSource);
        	audio.addProcessListener(this);
        	audio.startRecording();
        } else {
        	audio = null;
        	startVideoRecording();
        }
        connectToVLC();
	}

	// --------- START VIDEO (once audio has trigger it) ---------
	private void startVideoRecording() {
        try {
            connectToVLC();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(telnet.getOutputStream()));
            bw.write("add screen:// \n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	@Override
	public void stopRecording() {
		try {
            connectToVLC();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(telnet.getOutputStream()));
            bw.write("stop \n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(audio != null)
            audio.stopRecording();
	}

	@Override
	public void closeDown() {
		super.closeDown();
		try {
        	System.out.println("Closing interface...");
            if (telnet.isConnected()) {
            	BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(telnet.getOutputStream()));
                bw.write("quit \n");
                bw.flush();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	private void connectToVLC() {
		if (!telnet.isConnected()) {
            try {
				telnet.connect("localhost", 4444);
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}
	
}
