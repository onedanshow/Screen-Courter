package com.reelfx.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.reelfx.Applet;
import com.reelfx.model.AudioRecorder;
import com.reelfx.model.util.ProcessListener;

/**
 * A drop down box for selecting an audio source.
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
public class AudioSelector extends JComboBox implements MouseListener, ItemListener, ProcessListener {
	
	private static final long serialVersionUID = -1739456139353607514L;
	private VolumeMonitor monitor = new VolumeMonitor();
	private AudioRecorder selectedAudioRecorder = null;
	private Mixer selectedMixer = null;
	private VolumeVisualizer visualizer = new VolumeVisualizer();
	
	public AudioSelector() {
		super();
		addMouseListener(this);
		addItemListener(this);
		setEditable(true);
        setEditor(visualizer);
		setFont(new Font("Arial", 0, 11));
		listAudioSources();
		
		monitor.start();
	}

	private void listAudioSources() {
		removeAllItems();
		
		// find all audio inputs that have target outputs 
		// (excluding those with 'port' and 'PulseAudio' is a Linux hack, possibly temporary)
		for(Mixer.Info info : AudioSystem.getMixerInfo())
        	if(!info.getName().toLowerCase().contains("port")
        	//		&& !info.getName().toLowerCase().contains("PulseAudio")
        			&& AudioSystem.getMixer(info).getTargetLineInfo().length != 0)
        		addItem(info.getName());
		
		addItem("No Audio");
	}
	
	// for when you are starting a new recording (and subsequently a new thread)
	@Deprecated	// no longer needed since AudioRecorder is a running thread from the get-go
	public AudioRecorder getFreshAudioRecorder() {
		selectedMixer = null;
		if(selectedAudioRecorder != null)
			selectedAudioRecorder.destroy();
		selectedAudioRecorder = null;
		return getSelectedAudioRecorder();
	}
	
	// when you just need an instance
	public AudioRecorder getSelectedAudioRecorder() {
		if(selectedAudioRecorder == null && getSelectedMixer() != null) {
			selectedAudioRecorder = new AudioRecorder(getSelectedMixer());
			selectedAudioRecorder.addProcessListener(this);
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
	
	public void closeDown() {
		if(selectedAudioRecorder != null) {
			selectedAudioRecorder.destroy();
		}
		monitor.gogo = false;
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
	
	@Override
	public void processUpdate(int event, Object body) {
		switch(event) {
		case AudioRecorder.RECORDING_ERROR:
			setSelectedIndex(getItemCount()-1);
			if(isShowing()) {
				JOptionPane.showMessageDialog(null, 
						"The selected audio source could not be initialized. Please choose another.",
						"Error",JOptionPane.ERROR_MESSAGE);
			}
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
	
	class VolumeMonitor extends Thread {
		public boolean gogo = true;

		@Override
		public void run() {
			try {
				while(gogo) {
					if(getSelectedAudioRecorder() != null) {
						visualizer.setVolume(getSelectedAudioRecorder().getVolume());
					} else {
						visualizer.setVolume(0);
					}
					Thread.sleep(100);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void destroy() {
		// kill the volume listener thread
		monitor.gogo = false; 
	}
	
	class VolumeVisualizer extends JPanel implements ComboBoxEditor  {
		private static final long serialVersionUID = -2493872910538751344L;
		private double volume = 0;
		
		public VolumeVisualizer() {
			super();
			setLayout(null);
			setDoubleBuffered(true);
			setBackground(new Color(200,200,200));
			//setBackground(new Color(34, 34, 34));
		}

		public void setVolume(double volume) {
			if (volume == this.volume) return;
			this.volume = volume;			
			revalidate();
			repaint();
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Dimension dim = getSize();
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(Color.GRAY);
			g2.fillRoundRect(15, dim.height/4, dim.width-30, dim.height/2, 8, 8);
			g2.setColor(Color.YELLOW);
			g2.fillRoundRect(15, dim.height/4, (int)Math.min(dim.width-30, dim.width*volume/50), dim.height/2, 8, 8);
		}
		
		@Override
		public void addActionListener(ActionListener l) {}

		@Override
		public Component getEditorComponent() {
			return this;
		}

		@Override
		public Object getItem() {
			return null;
		}

		@Override
		public void removeActionListener(ActionListener l) {}

		@Override
		public void selectAll() {}

		@Override
		public void setItem(Object anObject) {}	
	}
}
