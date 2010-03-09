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


import java.io.IOException;
import java.io.File;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.AudioFileFormat;

import com.reelfx.Applet;
import com.reelfx.model.util.ProcessWrapper;

/**	<titleabbrev>SimpleAudioRecorder</titleabbrev>
	<title>Recording to an audio file (simple version)</title>

	<formalpara><title>Purpose</title>
	<para>Records audio data and stores it in a file. The data is
	recorded in CD quality (44.1 kHz, 16 bit linear, stereo) and
	stored in a <filename>.wav</filename> file.</para></formalpara>

	<formalpara><title>Usage</title>
	<para>
	<cmdsynopsis>
	<command>java SimpleAudioRecorder</command>
	<arg choice="plain"><option>-h</option></arg>
	</cmdsynopsis>
	<cmdsynopsis>
	<command>java SimpleAudioRecorder</command>
	<arg choice="plain"><replaceable>audiofile</replaceable></arg>
	</cmdsynopsis>
	</para></formalpara>

	<formalpara><title>Parameters</title>
	<variablelist>
	<varlistentry>
	<term><option>-h</option></term>
	<listitem><para>print usage information, then exit</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><option><replaceable>audiofile</replaceable></option></term>
	<listitem><para>the file name of the
	audio file that should be produced from the recorded data</para></listitem>
	</varlistentry>
	</variablelist>
	</formalpara>

	<formalpara><title>Bugs, limitations</title>
	<para>
	You cannot select audio formats and the audio file type
	on the command line. See
	AudioRecorder for a version that has more advanced options.
	Due to a bug in the Sun jdk1.3/1.4, this program does not work
	with it.
	</para></formalpara>

	<formalpara><title>Source code</title>
	<para>
	<ulink url="SimpleAudioRecorder.java.html">SimpleAudioRecorder.java</ulink>
	</para>
	</formalpara>

*/
public class AudioRecorder extends ProcessWrapper implements LineListener
{
	
    public static String OUTPUT_FILE = Applet.RFX_FOLDER.getAbsolutePath()+File.separator+"output-java.wav";
	
    // AUDIO SETTINGS
    public static int FREQ = 44100;
    
    // STATES
    public final static int RECORDING_STARTED = 200;
    public final static int RECORDING_COMPLETE = 201;
    
	private TargetDataLine m_line = null;
	private AudioFileFormat.Type m_targetType;
	private AudioInputStream m_audioInputStream;
	private File m_outputFile;

	public AudioRecorder(Mixer mixer)
	{
		/* For simplicity, the audio data format used for recording
		   is hardcoded here. We use PCM 44.1 kHz, 16 bit signed, stereo.
		*/
		// tried switching to mono, but it threw an exception
		AudioFormat	audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0F, 16, 2, 4, 44100.0F, false);

		/* Now, we are trying to get a TargetDataLine. The
		   TargetDataLine is used later to read audio data from it.
		   If requesting the line was successful, we are opening it (important!).
		*/
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
		try
		{
			if(mixer != null) // try with the mixer given
				m_line = (TargetDataLine) mixer.getLine(info);
			
			else // try to grab one ourselves
				m_line = (TargetDataLine) AudioSystem.getLine(info);
			m_line.addLineListener(this);
			m_line.open(audioFormat);
		}
		catch (LineUnavailableException e)
		{
			out("Unable to get a recording line");
			e.printStackTrace();
		}
		
		m_audioInputStream = new AudioInputStream(m_line);

		/* Again for simplicity, we've hardcoded the audio file
		   type, too.
		*/
		m_targetType = AudioFileFormat.Type.WAVE;
		
		m_outputFile = new File(OUTPUT_FILE);
	}

	/** Starts the recording.
	    To accomplish this, (i) the line is started and (ii) the
	    thread is started.
	*/
	public void startRecording()
	{
		/* Starting the TargetDataLine. It tells the line that
		   we now want to read data from it. If this method
		   isn't called, we won't
		   be able to read data from the line at all.
		*/
		m_line.start();

		/* Starting the thread. This call results in the
		   method 'run()' (see below) being called. There, the
		   data is actually read from the line.
		*/
		super.start();
	}

	/** Stops the recording.

	    Note that stopping the thread explicitly is not necessary. Once
	    no more data can be read from the TargetDataLine, no more data
	    be read from our AudioInputStream. And if there is no more
	    data from the AudioInputStream, the method 'AudioSystem.write()'
	    (called in 'run()' returns. Returning from 'AudioSystem.write()'
	    is followed by returning from 'run()', and thus, the thread
	    is terminated automatically.

	    It's not a good idea to call this method just 'stop()'
	    because stop() is a (deprecated) method of the class 'Thread'.
	    And we don't want to override this method.
	*/
	public void stopRecording()
	{
		m_line.stop();
		m_line.close();
		
		// TODO handle stopping this thread properly
	}


	/** Main working method.
	    You may be surprised that here, just 'AudioSystem.write()' is
	    called. But internally, it works like this: AudioSystem.write()
	    contains a loop that is trying to read from the passed
	    AudioInputStream. Since we have a special AudioInputStream
	    that gets its data from a TargetDataLine, reading from the
	    AudioInputStream leads to reading from the TargetDataLine. The
	    data read this way is then written to the passed File. Before
	    writing of audio data starts, a header is written according
	    to the desired audio file type. Reading continues until no
	    more data can be read from the AudioInputStream. In our case,
	    this happens if no more data can be read from the TargetDataLine.
	    This, in turn, happens if the TargetDataLine is stopped or closed
	    (which implies stopping). (Also see the comment above.) Then,
	    the file is closed and 'AudioSystem.write()' returns.
	*/
    @Override
	public void run()
	{
		try
		{
			AudioSystem.write(m_audioInputStream, m_targetType, m_outputFile);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
    
    /**
     * Part of the LineListener implementation. 
     */
    public void update(LineEvent event) {
		if(event.getType().equals(LineEvent.Type.OPEN)) {
			
		} 
		else if(event.getType().equals(LineEvent.Type.START)) {
			fireProcessUpdate(RECORDING_STARTED);
		} 
		else if(event.getType().equals(LineEvent.Type.STOP)) {
			fireProcessUpdate(RECORDING_COMPLETE);
		} 
		else if(event.getType().equals(LineEvent.Type.CLOSE)) {
			
		}
	}

	private static void out(String strMessage)
	{
		System.out.println(strMessage);
	}
	
	public static void deleteOutput() {
		File oldOutput = new File(OUTPUT_FILE);
		try {
			if(oldOutput.exists() && !oldOutput.delete())
				throw new Exception("Can't delete the old audio file!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
