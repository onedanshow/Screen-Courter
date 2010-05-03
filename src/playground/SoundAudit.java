package playground;

// from here: http://stackoverflow.com/questions/562273/what-output-and-recording-ports-does-the-java-sound-api-find-on-your-computer

import javax.sound.sampled.*;

public class SoundAudit {
	public static void main(String[] args) {
		try {
			System.out.println("OS: " + System.getProperty("os.name") + " "
					+ System.getProperty("os.version") + "/"
					+ System.getProperty("os.arch") + "\nJava: "
					+ System.getProperty("java.version") + " ("
					+ System.getProperty("java.vendor") + ")\n");
			for (Mixer.Info thisMixerInfo : AudioSystem.getMixerInfo()) {
				System.out.println("Mixer: " + thisMixerInfo.getDescription()
						+ " [" + thisMixerInfo.getName() + "]");
				Mixer thisMixer = AudioSystem.getMixer(thisMixerInfo);
				for (Line.Info thisLineInfo : thisMixer.getSourceLineInfo()) {
					if (thisLineInfo.getLineClass().getName().equals(
							"javax.sound.sampled.Port")) {
						Line thisLine = thisMixer.getLine(thisLineInfo);
						thisLine.open();
						System.out.println("  Source Port: "
								+ thisLineInfo.toString());
						for (Control thisControl : thisLine.getControls()) {
							System.out.println(AnalyzeControl(thisControl));
						}
						thisLine.close();
					}
				}
				for (Line.Info thisLineInfo : thisMixer.getTargetLineInfo()) {
					if (thisLineInfo.getLineClass().getName().equals(
							"javax.sound.sampled.Port")) {
						Line thisLine = thisMixer.getLine(thisLineInfo);
						thisLine.open();
						System.out.println("  Target Port: "
								+ thisLineInfo.toString());
						for (Control thisControl : thisLine.getControls()) {
							System.out.println(AnalyzeControl(thisControl));
						}
						thisLine.close();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String AnalyzeControl(Control thisControl) {
		String type = thisControl.getType().toString();
		if (thisControl instanceof BooleanControl) {
			return "    Control: " + type + " (boolean)";
		}
		if (thisControl instanceof CompoundControl) {
			System.out.println("    Control: " + type
					+ " (compound - values below)");
			String toReturn = "";
			for (Control children : ((CompoundControl) thisControl)
					.getMemberControls()) {
				toReturn += "  " + AnalyzeControl(children) + "\n";
			}
			return toReturn.substring(0, toReturn.length() - 1);
		}
		if (thisControl instanceof EnumControl) {
			return "    Control:" + type + " (enum: " + thisControl.toString()
					+ ")";
		}
		if (thisControl instanceof FloatControl) {
			return "    Control: " + type + " (float: from "
					+ ((FloatControl) thisControl).getMinimum() + " to "
					+ ((FloatControl) thisControl).getMaximum() + ")";
		}
		return "    Control: unknown type";
	}
}
