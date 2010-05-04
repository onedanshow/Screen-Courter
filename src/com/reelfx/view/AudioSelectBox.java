package com.reelfx.view;

import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JComboBox;

import com.reelfx.model.AudioRecorder;

public class AudioSelectBox extends JComboBox implements MouseListener, ItemListener {
	
	private static final long serialVersionUID = -1739456139353607514L;
	private VolumeMonitor monitor = new VolumeMonitor();
	private AudioRecorder selectedAudioRecorder = null;
	private Mixer selectedMixer = null;
	//private TargetDataLine selectedDataLine = null;

	public AudioSelectBox() {
		super();
		addMouseListener(this);
		addItemListener(this);
		setFont(new Font("Arial", 0, 11));
		listAudioSources();
		
		monitor.start();
	}

	private void listAudioSources() {
		removeAllItems();
		
		// find all audio inputs that have target outputs 
		// (excluding those with 'port' is a Linux hack, possibly temporary)
		// (exclude 'stereo mix' for capturing system audio
		for(Mixer.Info info : AudioSystem.getMixerInfo())
        	if(!info.getName().toLowerCase().contains("port")
        			//&& !info.getName().toLowerCase().contains("stereo mix")
        			&& AudioSystem.getMixer(info).getTargetLineInfo().length != 0)
        		addItem(info.getName());
		
		addItem("No Audio");
	}
	
	// for when you are starting a new recording (and subsequently a new thread)
	public AudioRecorder getFreshAudioRecorder() {
		selectedMixer = null;
		if(selectedAudioRecorder != null)
			selectedAudioRecorder.destroy();
		selectedAudioRecorder = null;
		return getSelectedAudioRecorder();
	}
	
	// when you just need an instance
	public AudioRecorder getSelectedAudioRecorder() {
		if(selectedAudioRecorder == null) {
			selectedAudioRecorder = new AudioRecorder(getSelectedMixer());
			selectedAudioRecorder.start(); // start reading the line
		}
		return selectedAudioRecorder;
	}
	
	public Mixer getSelectedMixer() {
		if(selectedMixer == null)
			for(Mixer.Info info : AudioSystem.getMixerInfo())
	        	if(info.getName().equals(getSelectedItem()))
	        		selectedMixer = AudioSystem.getMixer(info);
		
		return selectedMixer;
	}
	
	public static Mixer getDefaultMixer() {
		Mixer result = null;
		for(Mixer.Info info : AudioSystem.getMixerInfo()) {
        	if(!info.getName().toLowerCase().contains("port") && AudioSystem.getMixer(info).getTargetLineInfo().length != 0) {
        		result = AudioSystem.getMixer(info);
        		break;
        	}
		}
		
		return result;
	}
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		if(e.getStateChange() == ItemEvent.SELECTED) {
			selectedMixer = null;
			if(selectedAudioRecorder != null)
				selectedAudioRecorder.destroy();
			selectedAudioRecorder = null;
		}
	}

	// TODO mouse events don't work
	public void mouseClicked(MouseEvent e) {}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		listAudioSources();
	}

	public void mouseReleased(MouseEvent e) {}
	
	// why: http://www.jsresources.org/faq_audio.html#dataline_getlevel
	// helpful: http://forums.sun.com/thread.jspa?threadID=5433582
	class VolumeMonitor extends Thread {
		public boolean gogo = true;
		private AudioInputStream ais;
		@Override
		public void run() {
			try {
				while(gogo) {
					Thread.sleep(500);
					
					//if(selectedAudioRecorder == null)
					//	continue;
					/*
					byte[] audioData = new byte[2]; //getSelectedAudioRecorder().getDataLine().getBufferSize() / 5];
					//byte[] b = new byte[2];
					ais = new AudioInputStream(getSelectedAudioRecorder().getDataLine()); // required to not steal bytes from TargetDataLine
					//if(!ais.markSupported()) throw new IOException("Mark/reset not supported.");
					ais.read(audioData, 0, audioData.length);
					
					System.out.println("volume: "+Math.pow(audioData[0],2));
					*/
					System.out.println("volume: "+getSelectedAudioRecorder().getVolume());
			        /*
			        double sumMeanSquare = 0;
			        for(int j=0; j<audioData.length; j++) {
			        	sumMeanSquare += Math.pow(audioData[j], 2);
			        }
			        double averageMeanSquare = Math.round(sumMeanSquare / audioData.length);
					System.out.println("volume: "+averageMeanSquare);
					*/
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} /*catch (IOException e) {
				e.printStackTrace();
			}*/
		}
	}

	public void destroy() {
		// kill the volume listener thread
		monitor.gogo = false; 
	}
}
