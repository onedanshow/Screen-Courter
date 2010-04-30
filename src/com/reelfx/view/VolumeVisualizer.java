package com.reelfx.view;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Port;

public class VolumeVisualizer extends Thread {
	
	public boolean gogogo = true;
	
	@Override
	public void run() {
		final Port lineOut;
		try
		{
			if(AudioSystem.isLineSupported(Port.Info.LINE_OUT))
			{
				lineOut = (Port)AudioSystem.getLine(Port.Info.LINE_OUT);
				lineOut.open();
			}
			else if(AudioSystem.isLineSupported(Port.Info.HEADPHONE))
			{
				lineOut = (Port)AudioSystem.getLine(Port.Info.HEADPHONE);
				lineOut.open();
			}
			else if(AudioSystem.isLineSupported(Port.Info.SPEAKER))
			{
				lineOut = (Port)AudioSystem.getLine(Port.Info.SPEAKER);
				lineOut.open();
			}
			else
			{
				System.out.println("Unable to get Output Port");
				return;
			}
	
			final FloatControl controlIn = (FloatControl)lineOut.getControl(FloatControl.Type.VOLUME);
			
			while(gogogo) {
				System.out.println("Line: "+lineOut.toString()+" - Volume: "+controlIn.getValue());
				Thread.sleep(333);
			}
			//final float volume = 100 * (controlIn.getValue() / controlIn.getMaximum());
			//controlIn.setValue((float)vol / 100);
			//System.out.println("SetSystemVolume : volume = " + volume);
		}
		catch(final Exception e)
		{
			System.out.println(e + " LINE_OUT");
		}
	}
}
