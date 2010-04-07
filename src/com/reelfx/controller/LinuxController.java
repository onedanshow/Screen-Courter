package com.reelfx.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.sampled.Mixer;

import com.reelfx.Applet;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.view.Interface;

public class LinuxController extends ApplicationController {
	
	public LinuxController() {
		super();
	}
	
	@Override
	public void setupExtensions() {
		super.setupExtensions();
		try {
			if(!Applet.BIN_FOLDER.exists()){
				Applet.copyFolderFromRemoteJar(new URL(Applet.HOST_URL+"/bin-linux.jar"), "bin-linux");
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"ffmpeg").waitFor();
				Runtime.getRuntime().exec("chmod 755 "+Applet.BIN_FOLDER+File.separator+"ffplay").waitFor();
				if(!Applet.BIN_FOLDER.exists()) throw new IOException("Did not copy Linux extensions to the execution directory!");
			}
			System.out.println("Have access to execution folder: "+Applet.BIN_FOLDER.getAbsolutePath());
			setReadyStateBasedOnPriorRecording();
        } catch (MalformedURLException e1) {
        	gui.changeState(Interface.FATAL,"Error with install");
			e1.printStackTrace();
		} catch (InterruptedException e) {
			gui.changeState(Interface.FATAL,"Error with install");
			e.printStackTrace();
		} catch (IOException e) {
			gui.changeState(Interface.FATAL,"Error with install");
			e.printStackTrace();
		}
	}
	
	@Override
	public void prepareForRecording() {
		screen = new ScreenRecorder();
		screen.addProcessListener(this);
	}

	@Override
	public void startRecording(Mixer mixer,int index) {
		screen.start(mixer,index);
	}

	@Override
	public void stopRecording() {
		screen.stopRecording();
	}

	public void processUpdate(int event,Object body) {
		super.processUpdate(event,body);
	}

}
