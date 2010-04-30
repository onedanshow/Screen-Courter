package com.reelfx.view;

import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.JComboBox;

public class AudioSelectBox extends JComboBox implements MouseListener {
	
	private static final long serialVersionUID = -1739456139353607514L;
	private VolumeMonitor monitor;

	public AudioSelectBox() {
		super();
		addMouseListener(this);
		setFont(new Font("Arial", 0, 11));
		listAudioSources();
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
	
	public Mixer getSelectedMixer() {
		Mixer result = null;
		
		for(Mixer.Info info : AudioSystem.getMixerInfo())
        	if(info.getName().equals(getSelectedItem()))
        		result = AudioSystem.getMixer(info);
		
		return result;
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

	// TODO mouse events don't work
	public void mouseClicked(MouseEvent e) {}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		listAudioSources();
	}

	public void mouseReleased(MouseEvent e) {}
	
	class VolumeMonitor extends Thread {
		public boolean gogo = true;
		
		@Override
		public void run() {
			super.run();
			
			
		}
	}
}
