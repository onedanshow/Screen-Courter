package playground;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;

public class AudioPlayground {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<Mixer> microphones = new ArrayList<Mixer>();
		
		System.out.println("AUDIO SOURCES:\n-------------------------------------------------------");
		for(Mixer.Info info : AudioSystem.getMixerInfo()) {
			System.out.println(info.getName());
			System.out.println(info.getDescription());
			System.out.println(info.getVendor());			
			System.out.println(info.getVersion());
			System.out.println(info.toString()+"\n");

			if(AudioSystem.getMixer(info).getTargetLineInfo().length != 0) {
				microphones.add(AudioSystem.getMixer(info));
			}
		}
		
		System.out.println("MIC SOURCE LINES:\n-------------------------------------------------------");
		for(Mixer mic : microphones) {
			System.out.println("Microphone: "+mic.getMixerInfo().getName());
			System.out.println("Source info: ");
			printLineInfo(mic.getSourceLineInfo());
			System.out.println("Target info: ");
			printLineInfo(mic.getTargetLineInfo());
			System.out.println("\n");
		}
		
		//Port.Info.MICROPHONE;
	}
	
	public static void printLineInfo(Line.Info[] infos) {
		for(Line.Info i : infos)
			System.out.println(i.toString());
	}

}
