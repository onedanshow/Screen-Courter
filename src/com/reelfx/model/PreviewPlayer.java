package com.reelfx.model;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import com.reelfx.Applet;
import com.reelfx.controller.WindowsController;
import com.reelfx.model.util.ProcessWrapper;
import com.reelfx.model.util.StreamGobbler;

public class PreviewPlayer extends ProcessWrapper {

    Process ffplayProcess;
    AudioPlayer audioPlayer;
    StreamGobbler errorGobbler, inputGobbler;
    boolean keepPlaying = true;
    
    @Override
	public void run() {
    	// need this when doing file IO and JavaScript is calling the method (signed Applet or not)
    	AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
		    	try {
		    		if(Applet.IS_MAC && Desktop.isDesktopSupported()) {
		    			Desktop.getDesktop().open(ScreenRecorder.OUTPUT_FILE);
		    		}	
		    		else if (false) { //Applet.IS_WINDOWS && Desktop.isDesktopSupported()) {
		        		Desktop.getDesktop().open(WindowsController.MERGED_OUTPUT_FILE);
		    			/*List<String> ffplayArgs = new ArrayList<String>();
		    			ffplayArgs.add(WindowsController.MERGED_OUTPUT_FILE.getAbsolutePath());
		    			new ProcessBuilder(ffplayArgs).start();*/
		        	} 
		    		else {
		        		String ffplay = "ffplay" + (Applet.IS_WINDOWS ? ".exe" : "");
						
				        List<String> ffplayArgs = new ArrayList<String>();
				        ffplayArgs.add(Applet.BIN_FOLDER.getAbsolutePath()+File.separator+ffplay);
				        if(Applet.IS_WINDOWS) {
				        	ffplayArgs.addAll(parseParameters("-x 800 -y 600"));
				        }
				        if(Applet.IS_WINDOWS)
				        	ffplayArgs.add(WindowsController.MERGED_OUTPUT_FILE.getAbsolutePath());
				        else
				        	ffplayArgs.add(ScreenRecorder.OUTPUT_FILE.getAbsolutePath());
				        	
				        ProcessBuilder pb = new ProcessBuilder(ffplayArgs);
				        ffplayProcess = pb.start();
				
				        errorGobbler = new StreamGobbler(ffplayProcess.getErrorStream(), true, "ffplay E");
				        inputGobbler = new StreamGobbler(ffplayProcess.getInputStream(), true, "ffplay O");
				
				        System.out.println("Starting listener threads...");
				        errorGobbler.start();
				        inputGobbler.start();
				
				        //playAudio();
				        ffplayProcess.waitFor();
				        //stopAudio();
				        ffplayProcess = null;
		        	}
		      } catch (IOException ioe) {
		    	  ioe.printStackTrace();
		      } catch (Exception ie) {
		    	  ie.printStackTrace();
		      }
      		return null;
			}
		});
	}
    
    public void stopPlayer() {
    	if(ffplayProcess != null)
    		ffplayProcess.destroy();
    }

    @Deprecated
    private void playAudio() {
    	if(Applet.IS_WINDOWS) {
	        System.out.println("Playing audio independently...");
	        audioPlayer = new AudioPlayer();
	        audioPlayer.start();
    	}
    }
    
    @Deprecated
    private void stopAudio() {
    	if(Applet.IS_WINDOWS) {
	        System.out.println("Stopping independent audio...");
	        if(audioPlayer.isAlive())
	            audioPlayer.keepPlaying = false;
    	}
    }

    @Override
    protected void finalize() throws Throwable {
    	super.finalize();
    	if(ffplayProcess != null)
    		ffplayProcess.destroy();
    }
}

class AudioPlayer extends Thread {
    private static final int	EXTERNAL_BUFFER_SIZE = 128000;

    public boolean keepPlaying = true;

    @Override
    public void run() {
        System.out.println("Playing audio...");
        File soundFile = AudioRecorder.OUTPUT_FILE;

        /*
          We have to read in the sound file.
        */
        AudioInputStream audioInputStream = null;
        try
        {
                audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        }
        catch (Exception e)
        {
                System.err.println("Could not playback the audio file!");
                e.printStackTrace();
                return;
        }

        /*
          From the AudioInputStream, i.e. from the sound file,
          we fetch information about the format of the
          audio data.
          These information include the sampling frequency,
          the number of
          channels and the size of the samples.
          These information
          are needed to ask Java Sound for a suitable output line
          for this audio file.
        */
        AudioFormat	audioFormat = audioInputStream.getFormat();

        /*
          Asking for a line is a rather tricky thing.
          We have to construct an Info object that specifies
          the desired properties for the line.
          First, we have to say which kind of line we want. The
          possibilities are: SourceDataLine (for playback), Clip
          (for repeated playback)	and TargetDataLine (for
          recording).
          Here, we want to do normal playback, so we ask for
          a SourceDataLine.
          Then, we have to pass an AudioFormat object, so that
          the Line knows which format the data passed to it
          will have.
          Furthermore, we can give Java Sound a hint about how
          big the internal buffer for the line should be. This
          isn't used here, signaling that we
          don't care about the exact size. Java Sound will use
          some default value for the buffer size.
        */
        SourceDataLine	line = null;
        DataLine.Info	info = new DataLine.Info(SourceDataLine.class,audioFormat);
        try
        {
                line = (SourceDataLine) AudioSystem.getLine(info);

                /*
                  The line is there, but it is not yet ready to
                  receive audio data. We have to open the line.
                */
                line.open(audioFormat);
        }
        catch (LineUnavailableException e)
        {
                e.printStackTrace();
                return;
        }
        catch (Exception e)
        {
                e.printStackTrace();
                return;
        }

        /*
          Still not enough. The line now can receive data,
          but will not pass them on to the audio output device
          (which means to your sound card). This has to be
          activated.
        */
        line.start();

        /*
          Ok, finally the line is prepared. Now comes the real
          job: we have to write data to the line. We do this
          in a loop. First, we read data from the
          AudioInputStream to a buffer. Then, we write from
          this buffer to the Line. This is done until the end
          of the file is reached, which is detected by a
          return value of -1 from the read method of the
          AudioInputStream.
        */
        int	nBytesRead = 0;
        byte[]	abData = new byte[EXTERNAL_BUFFER_SIZE];
        while (nBytesRead != -1 && keepPlaying)
        {
                try
                {
                        nBytesRead = audioInputStream.read(abData, 0, abData.length);
                }
                catch (IOException e)
                {
                        e.printStackTrace();
                }
                if (nBytesRead >= 0)
                {
                        int	nBytesWritten = line.write(abData, 0, nBytesRead);
                }
        }

        /*
          Wait until all data are played.
          This is only necessary because of the bug noted below.
          (If we do not wait, we would interrupt the playback by
          prematurely closing the line and exiting the VM.)

          Thanks to Margie Fitch for bringing me on the right
          path to this solution.
        */
        line.drain();

        /*
          All data are played. We can close the shop.
        */
        line.close();
    }
}

/**	<titleabbrev>SimpleAudioPlayer</titleabbrev>
<title>Playing an audio file (easy)</title>

<formalpara><title>Purpose</title>
<para>Plays a single audio file.</para></formalpara>

<formalpara><title>Usage</title>
<cmdsynopsis>
<command>java SimpleAudioPlayer</command>
<replaceable class="parameter">audiofile</replaceable>
</cmdsynopsis>
</formalpara>

<formalpara><title>Parameters</title>
<variablelist>
<varlistentry>
<term><option><replaceable class="parameter">audiofile</replaceable></option></term>
<listitem><para>the name of the
audio file that should be played</para></listitem>
</varlistentry>
</variablelist>
</formalpara>

<formalpara><title>Bugs, limitations</title>

<para>Only PCM encoded files are supported. A-law, &mu;-law,
ADPCM, ogg vorbis, mp3 and other compressed data formats are not
supported. For playing these, see <olink targetdoc="AudioPlayer"
targetptr="AudioPlayer">AudioPlayer</olink>.</para>

</formalpara>

<formalpara><title>Source code</title>
<para>
<ulink url="SimpleAudioPlayer.java.html">SimpleAudioPlayer.java</ulink>
</para>
</formalpara>

*/