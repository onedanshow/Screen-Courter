package com.reelfx.view;

import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JComboBox;

import com.reelfx.model.AudioRecorder;

public class AudioSelectBox extends JComboBox implements MouseListener, ItemListener {
	
	private static final long serialVersionUID = -1739456139353607514L;
	private VolumeMonitor monitor = new VolumeMonitor();
	private Mixer selectedMixer = null;
	private TargetDataLine selectedDataLine = null;

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
	
	public Mixer getSelectedMixer() {
		if(selectedMixer == null)
			for(Mixer.Info info : AudioSystem.getMixerInfo())
	        	if(info.getName().equals(getSelectedItem()))
	        		selectedMixer = AudioSystem.getMixer(info);
		
		return selectedMixer;
	}
	
	public TargetDataLine getSelectedDataLine() {
		if(selectedDataLine == null) {
			try {
				// need to sample in 8-bits so we can calculate the audio easily in the VolumeMonitor
				//AudioFormat af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 11025, 8, 2, 2, 11025, true);
				selectedDataLine = (TargetDataLine)getSelectedMixer().getLine(new DataLine.Info(TargetDataLine.class,AudioRecorder.AUDIO_FORMAT));
				System.out.println("Format: "+selectedDataLine.getFormat().toString());
				selectedDataLine.open(AudioRecorder.AUDIO_FORMAT);
				selectedDataLine.start();
			} catch (Exception e) {
				selectedDataLine = null;
			}
		}
		
		return selectedDataLine;
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
			if(selectedDataLine != null) {
				selectedDataLine.flush();
				selectedDataLine.stop();
				selectedDataLine.close();
			}
			selectedDataLine = null; 
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
		
		@Override
		public void run() {
			try {
				while(gogo) {
					byte[] audioData = new byte[getSelectedDataLine().getBufferSize() / 5];
					getSelectedDataLine().read(audioData, 0, audioData.length);
			        double sumMeanSquare = 0;
			        for(int j=0; j<audioData.length; j++) {
			        	sumMeanSquare += Math.pow(audioData[j], 2);
			        }
			        double averageMeanSquare = sumMeanSquare / audioData.length;
					
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
}
