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
import javax.swing.JPanel;

import com.reelfx.model.AudioRecorder;

public class AudioSelector extends JComboBox implements MouseListener, ItemListener {
	
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
			setBackground(new Color(34, 34, 34));
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
			g2.setColor(Color.ORANGE);
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
	
	/*class ItemRenderer extends JLabel implements ListCellRenderer {
		private static final long serialVersionUID = 7794106572744408569L;

		public ItemRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
        }
		
		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {

			if(cellHasFocus) {
				setText("C:"+value.toString());
			}
			else if(isSelected) {
				setText("V: "+volume);
			} else {
				setText("D:"+value.toString());
			}
			
			return this;
		}
	}*/

}
