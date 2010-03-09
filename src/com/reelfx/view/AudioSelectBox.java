package com.reelfx.view;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.JComboBox;

public class AudioSelectBox extends JComboBox implements MouseListener {
	
	private static final long serialVersionUID = -1739456139353607514L;

	public AudioSelectBox() {
		super();
		addMouseListener(this);
		listAudioSources();
	}

	private void listAudioSources() {
		removeAllItems();
		addItem("No Audio");
		
		for(Mixer.Info info : AudioSystem.getMixerInfo())
        	if(!info.getName().contains("Java"))
        		addItem(info.getName());
	}
	
	public Mixer getSelectedMixer() {
		Mixer result = null;
		
		for(Mixer.Info info : AudioSystem.getMixerInfo())
        	if(!info.getName().equals(getSelectedItem().toString()))
        		result = AudioSystem.getMixer(info);
		
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
}
