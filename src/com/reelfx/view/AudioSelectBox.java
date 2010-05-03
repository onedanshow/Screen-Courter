package com.reelfx.view;

import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
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
				AudioFormat af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 11025, 8, 2, 2, 11025, true);
				selectedDataLine = (TargetDataLine)getSelectedMixer().getLine(new DataLine.Info(TargetDataLine.class,af));
				System.out.println("Format: "+selectedDataLine.getFormat().toString());
				selectedDataLine.open();
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
	
	class VolumeMonitor extends Thread {
		public boolean gogo = true;
		
		@Override
		public void run() {
			try {
				while(gogo) {
					/*
					int avail = getSelectedDataLine().available();
					//System.out.println("Avail: "+avail);
					byte[] b = new byte[getSelectedDataLine().getBufferSize() / 5];
					getSelectedDataLine().read(b, 0, b.length);
					long sum = 0;
					for(int i = 0; i < b.length; i++) {
						sum += b[i];
					}
					double avg = sum / b.length;
					System.out.println("Blah..."+avg);
					*/
					
					/*
					AudioFormat af = getSelectedDataLine().getFormat();
					AudioInputStream ais = getSelectedDataLine().
						
					if(AudioSystem.isConversionSupported(AudioFormat.Encoding.PCM_SIGNED, af)) {
						System.out.println("Converting to PCM_SIGNED");
						
						AudioInputStream ais_ = (af.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) ? ais : AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
				        af = ais_.getFormat();
						
						int maxLines = getSelectedMixer().getMaxLines(Port.Info.MICROPHONE);
						if(maxLines > 0)
							System.out.println("controls: "+getSelectedMixer().getLine(Port.Info.MICROPHONE).getControls().length);
					}
					*/
					
					/*
					byte[] audioData = new byte[getSelectedDataLine().getBufferSize() / 5];
					getSelectedDataLine().read(audioData, 0, audioData.length);
					
			        long lSum = 0;
			        for(int i=0; i<audioData.length; i++)
			            lSum = lSum + audioData[i];
			 
			        double dAvg = lSum / audioData.length;
			 
			        double sumMeanSquare = 0d;
			        for(int j=0; j<audioData.length; j++)
			            sumMeanSquare = sumMeanSquare + Math.pow(audioData[j] - dAvg, 2d);
			 
			        double averageMeanSquare = sumMeanSquare / audioData.length;
			        
			        System.out.println("output: "+(int)(Math.pow(averageMeanSquare,0.5d) + 0.5));
					*/
					
					byte[] audioData = new byte[getSelectedDataLine().getBufferSize() / 5];
					getSelectedDataLine().read(audioData, 0, audioData.length);
					//System.out.println("avil: "+getSelectedDataLine().available());
					//getSelectedDataLine().flush();
					//System.out.println("avil: "+getSelectedDataLine().available());
					//System.out.println("length "+audioData.length);
			        double sumMeanSquare = 0;
			        for(int j=0; j<audioData.length; j++) {
			        	sumMeanSquare += Math.pow(audioData[j], 2);
			        }
			 
			        double averageMeanSquare = sumMeanSquare / audioData.length;
			        
			        System.out.println("output: "+(Math.round(Math.sqrt(averageMeanSquare))));
					
					Thread.sleep(100);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} /*catch (LineUnavailableException e) {
				e.printStackTrace();
			}*/
		}
	}

	public void destroy() {
		// kill the volume listener thread
		monitor.gogo = false; 
	}
}
