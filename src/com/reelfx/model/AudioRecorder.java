package com.reelfx.model;

/**
 *	Base code from: SimpleAudioRecorder.java
 *
 *	This file is part of jsresources.org
 *
 * Copyright (c) 1999 - 2003 by Matthias Pfisterer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.spi.AudioFileWriter;

import com.reelfx.Applet;
import com.reelfx.model.util.ProcessWrapper;
import com.sun.media.sound.JDK13Services;

public class AudioRecorder extends ProcessWrapper implements LineListener
{
	
    public static File OUTPUT_FILE = new File(Applet.RFX_FOLDER.getAbsolutePath()+File.separator+"screen_capture.wav");
    public static File TEMP_FILE = new File(Applet.RFX_FOLDER.getAbsolutePath()+File.separator+"temp.wav");
	
    // AUDIO SETTINGS
    public static float FREQ = 22050; //11025.0F; // 22050; //44100;  lowered because it was skipping and dropping audio
    public static AudioFormat AUDIO_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, FREQ, 8, 2, 2, FREQ, false);
    
    // STATES
    public final static int RECORDING_STARTED = 200;
    public final static int RECORDING_COMPLETE = 201;
    
	private TargetDataLine m_line = null;
	private AudioFileFormat.Type m_targetType;
	private AudioInputStream m_audioInputStream;
	private File m_outputFile;
	private ByteArrayOutputStream m_byteArrayOutputStream;
	private boolean m_captureToFile = false, m_saveFile = false;
	private double m_volume = 0;

	public AudioRecorder(Mixer mixer)
	{
		// tried switching to mono, but it threw an exception
		//AudioFormat	audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, FREQ, 16, 2, 4, FREQ, false);

		System.out.println("Creating new AudioRecorder");
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
		try
		{
			if(mixer != null) {// try with the mixer given
				System.out.println("Grabbing the specified audio mixer: "+mixer.getLineInfo().toString());
				m_line = (TargetDataLine) mixer.getLine(info);
				m_outputFile = OUTPUT_FILE;
			}
			else { // try to grab one ourselves
				System.out.println("Grabbing whatever AudioSystem line I can get...(system audio?)");
				m_line = (TargetDataLine) AudioSystem.getLine(info);
				m_outputFile = new File(Applet.RFX_FOLDER.getAbsolutePath()+File.separator+"screen_capture_system.wav");
			}
			m_line.addLineListener(this);
			m_line.open(AUDIO_FORMAT);
			m_line.start();
		}
		catch (LineUnavailableException e)
		{
			System.err.println("Unable to get a recording line");
			e.printStackTrace();
		}
		
		m_audioInputStream = new AudioInputStream(m_line);
		m_targetType = AudioFileFormat.Type.WAVE;
		
		super.start(); // moved from startRecording()
	}

	/** Starts the recording. */
	public void startRecording()
	{
		System.out.println("Setting up audio line recording...");
		/* Starting the thread. This call results in the
		   method 'run()' (see below) being called. There, the
		   data is actually read from the line.
		*/
		m_captureToFile = true;
		m_saveFile = false;
		fireProcessUpdate(RECORDING_STARTED); // moved from processUpdate
		// super.start();
	}

	/** Stops the recording. */
	public void stopRecording()
	{
		m_captureToFile = false;
		m_saveFile = true;
	}

    @Override
	public void run()
	{		
    	AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				// why: http://www.jsresources.org/faq_audio.html#dataline_getlevel
				// used: http://forums.sun.com/thread.jspa?threadID=5433582
				// used: http://forums.sun.com/thread.jspa?threadID=5402696
				// used: http://www.jsresources.org/examples/RawAudioDataConverter.java.html
				// another approach: http://proteo.me.uk/?p=38
				BufferedOutputStream bos = null;
		    	try
				{
		    		// start looping to grab bytes to sample volume, writing out a file if necessary
		    		byte[] audioData;	
		    		while(true) {
		    			// are we ready to save the file and finish the thread?
		    			if(m_saveFile || m_line == null) {
		    				if(bos != null) {
		    					System.out.println("Saving audio file...");
								bos.flush();
								bos.close();
								// now properly reconstruct the audio file that we just made (if we made one)
				    			BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(TEMP_FILE));
				    			long lLengthInFrames = TEMP_FILE.length() / AudioRecorder.AUDIO_FORMAT.getFrameSize();
				    			AudioInputStream ais = new AudioInputStream(inputStream,AudioRecorder.AUDIO_FORMAT,lLengthInFrames);
				    			AudioSystem.write(ais, m_targetType, m_outputFile);
				    			fireProcessUpdate(RECORDING_COMPLETE); // thread may keep going though
				    			bos = null;
		    				}
							m_saveFile = false;
						}
		    			// are we destroying this thread?
		    			if(m_line == null) {
		    				break;
		    			}
		    			// sample audio
		    			audioData = new byte[m_line.getBufferSize() / 5];
		    			m_line.read(audioData, 0, audioData.length);
		    			// calculate the volume (RMS: http://en.wikipedia.org/wiki/Root_mean_square)
		    			double sumMeanSquare = 0;
				        for(int j=0; j<audioData.length; j++) {
				        	sumMeanSquare += Math.pow(audioData[j], 2);
				        }				        
						m_volume = Math.round(Math.sqrt(sumMeanSquare / audioData.length));
						// write the audio
						if(m_captureToFile) {
							if(bos == null) {
					    		bos = new BufferedOutputStream(new FileOutputStream(TEMP_FILE));
					    		System.out.println("Writing audio...");
							}
							bos.write(audioData);
						}
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				return null;
			}
    	});
	}
    
    public TargetDataLine getDataLine() {
    	return m_line;
    }
    
    public double getVolume() {
    	return m_volume;
    }
    
    @Override
    public void destroy() {
    	System.out.println("Destroying AudioRecorder...");
    	stopRecording();
    	if(m_line != null) {
			System.out.println("Starting to the stop the line...");
			m_line.flush();
			m_line.stop();
			System.out.println("Starting to close the line...");
			m_line.close();
			System.out.println("Done closing and stopping the audio line");
			m_line = null;
		}
    	m_audioInputStream = null;
    	m_line = null;
    }
    
    /**
     * Part of the LineListener implementation. 
     */
    public void update(LineEvent event) {
		if(event.getType().equals(LineEvent.Type.OPEN)) {
			//System.out.println("Audio Line Opened...");
		} 
		else if(event.getType().equals(LineEvent.Type.START)) {
			//System.out.println("Audio Recording Started...");
			//fireProcessUpdate(RECORDING_STARTED); // moved to run() because line is already started
		} 
		else if(event.getType().equals(LineEvent.Type.STOP)) {
			System.out.println("Audio Recording Stopped...");
			//fireProcessUpdate(RECORDING_COMPLETE);
		} 
		else if(event.getType().equals(LineEvent.Type.CLOSE)) {
			System.out.println("Audio Line Closed...");
		}
	}
	
	public static void deleteOutput() {
		AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				try {
					if(TEMP_FILE.exists() && !TEMP_FILE.delete())
						throw new Exception("Can't delete temporary audio file!");
					if(OUTPUT_FILE.exists() && !OUTPUT_FILE.delete())
						throw new Exception("Can't delete the old audio file!");
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		});
	}
}