package com.reelfx.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.sampled.Mixer;

import com.reelfx.Applet;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.view.RecordInterface;

public class LinuxController extends ApplicationController {
	
	public static File MERGED_OUTPUT_FILE = new File(Applet.RFX_FOLDER.getAbsolutePath()+File.separator+"screen_capture_temp.mov");
	
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
			}
			System.out.println("Have access to execution folder: "+Applet.BIN_FOLDER.getAbsolutePath());
			setReadyStateBasedOnPriorRecording();
        } catch (MalformedURLException e1) {
        	recordGUI.changeState(RecordInterface.FATAL,"Error with install");
			e1.printStackTrace();
		} catch (InterruptedException e) {
			recordGUI.changeState(RecordInterface.FATAL,"Error with install");
			e.printStackTrace();
		} catch (IOException e) {
			recordGUI.changeState(RecordInterface.FATAL,"Error with install");
			e.printStackTrace();
		}
	}
	
	@Override
	public void prepareForRecording() {
		super.prepareForRecording();
		screen = new ScreenRecorder();
		screen.addProcessListener(this);
	}

	@Override
	public void startRecording(Mixer mixer,int index) {
		screen.start(mixer,index);
	}

	@Override
	public void stopRecording() {
		super.stopRecording();
		screen.stopRecording();
	}

	public void processUpdate(int event,Object body) {
		super.processUpdate(event,body);
	}

}
