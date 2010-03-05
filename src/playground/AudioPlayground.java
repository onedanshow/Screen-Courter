package playground;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;

public class AudioPlayground {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Mixer mic = null;
		
		System.out.println("AUDIO SOURCES:");
		for(Mixer.Info info : AudioSystem.getMixerInfo()) {
			System.out.println(info.getName());
			System.out.println(info.getDescription());
			System.out.println(info.getVendor());
			System.out.println(info.getVersion()+"\n");
			if(info.getName().equals("Built-in Microphone")) {
				mic = AudioSystem.getMixer(info);
			}
		}
		
		System.out.println("MIC SOURCE LINES:");
		for(Line.Info line : mic.getSourceLineInfo()) {
			System.out.println(line+"\n");
		}
		
		System.out.println("MIC TARGET LINES:");
		for(Line.Info line : mic.getTargetLineInfo()) {
			System.out.println(line+"\n");
		}
		//Port.Info.MICROPHONE;
	}

}
