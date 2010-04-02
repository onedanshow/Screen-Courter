package com.reelfx;


import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import com.reelfx.controller.ApplicationController;
import com.reelfx.controller.LinuxController;
import com.reelfx.controller.MacController;
import com.reelfx.controller.WindowsController;
import com.reelfx.view.AudioSelectBox;
import com.sun.JarClassLoader;

/**
 * 
 * @author daniel
 *
 * SPECIAL NOTE ON JSObject (Used for communicating with Javascript)
 * In Eclipse, initially couldn't find the class.  This guy said to add a reference to 'plugin.jar' 
 * (http://stackoverflow.com/questions/1664604/jsobject-download-it-or-available-in-jre-1-6) however
 * the only plugin.jar's I found for Java via the 'locate' command were either bad symlinks or inside
 * .bundle so I had to create a good symlink called plugin-daniel.jar in /System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Home/lib
 * that pointed to /System/Library/Frameworks/JavaVM.framework/Versions/A/Resources/Deploy.bundle/Contents/Resources/Java/plugin.jar
 * 
 * further information: http://java.sun.com/j2se/1.5.0/docs/guide/plugin/developer_guide/java_js.html
 */

// TODO setup log4j

public class Applet extends JApplet {

	private static final long serialVersionUID = 4544354980928245103L;
	
	public static File RFX_FOLDER, BIN_FOLDER, DESKTOP_FOLDER;
	public static URL DOCUMENT_BASE, CODE_BASE;
	public static JSObject JS_BRIDGE;
	public static String POST_URL = null, API_KEY = null, HOST_URL = null;
	public static boolean HEADLESS = false;
	public static boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
	public static boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux");
	public static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
	public static boolean DEV_MODE = System.getProperty("user.dir").contains(System.getProperty("user.home")); // assume dev files are in developer's home
	public static GraphicsConfiguration GRAPHICS_CONFIG = null;
	
	private ApplicationController controller = null;
	
	// called when this applet is loaded into the browser.
	@Override
    public void init() {
		try {
			RFX_FOLDER = new File(getRfxFolderPath()); // should be first
			BIN_FOLDER = new File(getBinFolderPath());
			DESKTOP_FOLDER = new File(getDesktopFolderPath());
			try {
				JS_BRIDGE = JSObject.getWindow(this);
			} catch(JSException e) {
				System.err.println("Could not create JSObject.  Probably in development mode.");
			}
			DOCUMENT_BASE = getDocumentBase();
			CODE_BASE = getCodeBase();
			POST_URL = getParameter("post_url");
			API_KEY = getParameter("api_key");
			HOST_URL = DOCUMENT_BASE.getProtocol() + "://" + DOCUMENT_BASE.getHost();
			if(getParameter("headless") != null)
				HEADLESS = !getParameter("headless").isEmpty() && getParameter("headless").equals("true"); // Boolean.getBoolean(string) didn't work
		
			// base code: http://stackoverflow.com/questions/2234476/how-to-detect-the-current-display-with-java
			GRAPHICS_CONFIG = getGraphicsConfiguration();
			GraphicsDevice myScreen = GRAPHICS_CONFIG.getDevice();
			GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] allScreens = env.getScreenDevices();
			int myScreenIndex = -1;
			for (int i = 0; i < allScreens.length; i++) {
			    if (allScreens[i].equals(myScreen))
			    {
			        myScreenIndex = i;
			        break;
			    }
			}
			System.out.println("Applet window is on screen" + myScreenIndex);

			System.out.println(getAppletInfo());			 
			/*
			System.out.println("Have these system variables:");
			Map<String, String> sysEnv = System.getenv();
	        for (String envName : sysEnv.keySet()) {
	            System.out.format("%s=%s%n", envName, sysEnv.get(envName));
	        }
			*/			
			if( RFX_FOLDER.exists() && !RFX_FOLDER.isDirectory() && !RFX_FOLDER.delete() )
		        throw new IOException("Could not delete file for folder: " + RFX_FOLDER.getAbsolutePath());
			if( !RFX_FOLDER.exists() && !RFX_FOLDER.mkdir() )
		        throw new IOException("Could not create folder: " + RFX_FOLDER.getAbsolutePath());
			
			System.out.println("Have access to folder: "+RFX_FOLDER.getAbsolutePath()+File.separator);
			
			// execute a job on the event-dispatching thread; creating this applet's GUI.
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                	// start up the GUI
                	if(IS_MAC)
                		controller = new MacController();
                	else if(IS_LINUX)
                		controller = new LinuxController();
                	else if(IS_WINDOWS)
                		controller = new WindowsController();
                	else 
                		System.err.println("Want to launch controller but don't which operating system this is.");
                }
            });
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                	if(controller != null)
                		controller.setupExtensions();
                }
            });
        
		} catch (IOException e) {
			System.err.println("Could not create temporary folder!");
			e.printStackTrace();
		}	catch (Exception e) {
            System.err.println("Could not create GUI!");
            e.printStackTrace();
        }
    }
	
	// ---------- Headless API ----------
	public void prepareForRecording() {
		controller.prepareForRecording();
	}
	
	public void startRecording() {
		// TODO grabs default mixer right now, need a way to select microphones...
		controller.startRecording(AudioSelectBox.getDefaultMixer(),0);
	}
	
	public void stopRecording() {
		controller.stopRecording();
	}
	
	public void previewRecording() {
		controller.previewRecording();
	}
	
	public void postRecording() {
		// TODO TEMPORARY until I get the Insight posting process down
		controller.askForAndSaveRecording();
	}
	
	public static void sendShowStatus(String message) {
		if(JS_BRIDGE == null) {
			System.err.println("Call to sendShowStatus but no JS Bridge exists.");
		} else {
			JSObject doc = (JSObject) JS_BRIDGE.getMember("document");
			doc.eval("showStatus(\""+message+"\");");
		}
	}
	
	public static void sendHideStatus() {
		if(JS_BRIDGE == null) {
			System.err.println("Call to sendShowStatus but no JS Bridge exists.");
		} else {
			JSObject doc = (JSObject) JS_BRIDGE.getMember("document");
			doc.eval("hideStatus();");
		}
	}
	
	public static void sendInfo(String message) {
		if(JS_BRIDGE == null) {
			System.err.println("Call to sendShowStatus but no JS Bridge exists.");
		} else {
			JSObject doc = (JSObject) JS_BRIDGE.getMember("document");
			doc.eval("info(\""+message+"\");");
		}
	}
	
	public static void sendError(String message) {
		if(JS_BRIDGE == null) {
			System.err.println("Call to sendShowStatus but no JS Bridge exists.");
		} else {
			JSObject doc = (JSObject) JS_BRIDGE.getMember("document");
			doc.eval("error(\""+message+"\");");
		}
	}
	// ---------- Headless API ----------
	
	/** 
	 * Copies an entire folder out of a jar to a physical location. 
	 * 
	 * Base code: http://forums.sun.com/thread.jspa?threadID=5154854
	 * Helpful: http://mindprod.com/jgloss/getresourceasstream.html
	 * Helpful: http://stackoverflow.com/questions/810284/putting-bat-file-inside-a-jar-file
	 * 
	 * @param jarName     Path and name of the jar to extract from
	 * @param folderName  Single name, not path, of the folder to pull from the root of the jar.
	 */
     public static void copyFolderFromCurrentJar(String jarName, String folderName) {
    	if(jarName == null || folderName == null) return;
    	
		try {
			ZipFile z = new ZipFile(jarName);
			Enumeration<? extends ZipEntry> entries = z.entries();
			// make the folder first
			//File folder = new File(RFX_FOLDER.getAbsolutePath()+File.separator+folderName);
			//if( !folder.exists() ) folder.mkdir();
			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry)entries.nextElement();
				if (entry.getName().contains(folderName)) {
					File f = new File(RFX_FOLDER.getAbsolutePath()+File.separator+entry.getName());
					if (entry.isDirectory() && f.mkdir()) { 
						System.out.println("Created folder "+f.getAbsolutePath()+" for "+entry.getName());
					}
					else if (!f.exists()) {
						if (copyFileFromJar(entry.getName(), f)) {
							System.out.println("Copied file: " + entry.getName());
						} else {
							System.err.println("Could not copy file: "+entry.getName());
						}
					}
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
     
     /**
      * Use this one or loading from a remote jar and extracting it.
      * 
      * @param jar
      * @param folderName
      */
     public static void copyFolderFromRemoteJar(URL jar, String folderName) {
    	 if(jar == null || folderName == null) return;
    	 
    	 try {
    		JarClassLoader jarLoader = new JarClassLoader(jar);
    		URL u = new URL("jar", "", jar + "!/");
			JarURLConnection uc = (JarURLConnection)u.openConnection();
			JarFile jarFile = uc.getJarFile();
			Enumeration<? extends JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry)entries.nextElement();
				if (entry.getName().contains(folderName)) {
					File f = new File(RFX_FOLDER.getAbsolutePath()+File.separator+entry.getName());
					if (entry.isDirectory() && f.mkdir()) { 
						System.out.println("Created folder "+f.getAbsolutePath()+" for "+entry.getName());
					}
					else if (!f.exists()) {
						if (copyFileFromJar(entry.getName(), f, jarLoader)) {
							System.out.println("Copied file: " + entry.getName());
						} else {
							System.err.println("Could not copy file: "+entry.getName());
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
     }
     
     protected static boolean copyFileFromJar(String sResource, File fDest) {
    	 return copyFileFromJar(sResource, fDest, Applet.class.getClassLoader());
     }
	
	/** 
	 * Copies a file out of the jar to a physical location.  
	 *    Doesn't need to be private, uses a resource stream, so may have
	 *    security errors if run from webstart application
	 *    
	 * Base code: http://forums.sun.com/thread.jspa?threadID=5154854    
	 * Helpful: http://mindprod.com/jgloss/getresourceasstream.html
	 * Helpful: http://stackoverflow.com/questions/810284/putting-bat-file-inside-a-jar-file
	 */
     protected static boolean copyFileFromJar(String sResource, File fDest, ClassLoader loader) {
		if (sResource == null || fDest == null) return false;
		
		InputStream sIn = null;
		OutputStream sOut = null;
		File sFile = null;
		
		try {
			fDest.getParentFile().mkdirs();
			sFile = new File(sResource);
		} 
		catch(Exception e) {
			e.printStackTrace();
		}
		
		try {
			int nLen = 0;
			sIn = loader.getResourceAsStream(sResource);
			if (sIn == null)
				throw new IOException("Could not get resource as stream to copy " + sResource + " from the jar to " + fDest.getAbsolutePath() + ")");
			sOut = new FileOutputStream(fDest);
			byte[] bBuffer = new byte[1024];
			while ((nLen = sIn.read(bBuffer)) > 0)
				sOut.write(bBuffer, 0, nLen);
			sOut.flush();
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
		
		finally {
			try {
				if (sIn != null)
					sIn.close();
				if (sOut != null)
					sOut.close();
			}
			catch (IOException eError) {
				eError.printStackTrace();
			}
		}
		return fDest.exists();
	}
	
	@Override
	public String getAppletInfo() {
		
		try {
			return 
				"Java Version: \t"+System.getProperty("java.version")+"\n"+
				"OS Name: \t"+System.getProperty("os.name")+"\n"+
				"OS Version: \t"+System.getProperty("os.version")+"\n"+
				"Run Directory: \t"+System.getProperty("user.dir")+"\n"+
				"User Home: \t"+System.getProperty("user.home")+"\n"+
				"User Name: \t"+System.getProperty("user.name")+"\n"+
				"ReelFX Folder: \t"+RFX_FOLDER.getPath()+"\n"+
				"Bin Folder: \t"+BIN_FOLDER.getPath()+"\n"+
				"User Desktop: \t"+DESKTOP_FOLDER.getPath()+"\n"+
				"Code Base: \t"+getCodeBase()+"\n"+
				"Document Base: \t"+getDocumentBase()+"\n"+
				"Execution URL: \t"+Applet.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()+"\n"+
				"Multiple Monitors: \t"+(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length > 1)+"\n"+
				"Headless: \t"+HEADLESS+"\n";
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return "Error";
		}
		
		//System.out.println("Free space: \n"+TEMP_FOLDER.getFreeSpace()+" GBs"); // Java 1.6 only
		//System.out.println("Total space: \n"+TEMP_FOLDER.getTotalSpace()+" GBs");
	}
	
	public static String getBinFolderPath() throws IOException {
		if(IS_MAC)
			return RFX_FOLDER.getAbsolutePath()+File.separator+"bin-mac";
		else if(IS_LINUX)
			return RFX_FOLDER.getAbsolutePath()+File.separator+"bin-linux";
		else if(IS_WINDOWS)
			return RFX_FOLDER.getAbsolutePath()+File.separator+"bin-windows";
		else
			throw new IOException("I don't know what bin folder to use!");
	}
	
	public static String getRfxFolderPath() throws IOException {
		if(IS_MAC)
			return System.getProperty("user.home")+File.separator+"Library"+File.separator+"ReelFX";
		else if(IS_LINUX)
			return System.getProperty("user.home")+File.separator+".ReelFX";
		else if(IS_WINDOWS)
			return System.getenv("TEMP")+File.separator+"ReelFX";
		else 
			throw new IOException("I don't know where to find the native extensions!");
	}
	
	// not tested, and not used
	public static String getDesktopFolderPath() throws IOException {
		if(IS_MAC || IS_LINUX || IS_WINDOWS)
			return System.getProperty("user.home")+File.separator+"Desktop";
		else
			throw new IOException("I don't know where to find the user's desktop!");
	}
    
    /**
     * Called when the browser closes the web page.
     * 
     *  NOTE: A bug in Mac OS X may prevent this from being called: http://lists.apple.com/archives/java-dev///2009/Oct/msg00042.html
     */
    @Override
    public void destroy() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                	System.out.println("Closing down...");
                	controller.closeDown();
                	controller = null;
                }
            });
        } catch (Exception e) { }

    }

}
