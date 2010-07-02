package com.reelfx;


import java.applet.AppletContext;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JApplet;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import com.reelfx.controller.ApplicationController;
import com.reelfx.controller.LinuxController;
import com.reelfx.controller.MacController;
import com.reelfx.controller.WindowsController;
import com.reelfx.model.CaptureViewport;
import com.reelfx.model.PreferencesManager;
import com.reelfx.view.AudioSelector;
import com.reelfx.view.InformationBox;
import com.reelfx.view.VolumeVisualizer;
import com.reelfx.view.util.MoveableWindow;
import com.reelfx.view.util.ViewListener;
import com.reelfx.view.util.ViewNotifications;
import com.sun.JarClassLoader;

/**
 * 
 * @author daniel
 *
 * SPECIAL NOTE ON JSObject on Mac (Used for communicating with Javascript)
 * In Eclipse, initially couldn't find the class.  This guy said to add a reference to 'plugin.jar' 
 * (http://stackoverflow.com/questions/1664604/jsobject-download-it-or-available-in-jre-1-6) however
 * the only plugin.jar's I found for Java via the 'locate' command were either bad symlinks or inside
 * .bundle so I had to create a good symlink called plugin-daniel.jar in /System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Home/lib
 * that pointed to /System/Library/Frameworks/JavaVM.framework/Versions/A/Resources/Deploy.bundle/Contents/Resources/Java/plugin.jar
 * I had no issue adding it on Windows or Linux.
 * 
 * further information: http://java.sun.com/j2se/1.5.0/docs/guide/plugin/developer_guide/java_js.html
 */

// TODO setup log4j

public class Applet extends JApplet {

	private static final long serialVersionUID = 4544354980928245103L;
	
	public static File RFX_FOLDER, BIN_FOLDER, DESKTOP_FOLDER;
	public static URL DOCUMENT_BASE, CODE_BASE;
	public static JApplet APPLET;
	public static JSObject JS_BRIDGE;
	public static String POST_URL = null, SCREEN_CAPTURE_NAME = null, API_KEY = null, HOST_URL = null;
	public static boolean HEADLESS = false;
	public static boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
	public static boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux");
	public static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
	public static boolean DEV_MODE = System.getProperty("user.dir").contains(System.getProperty("user.home")); // assume dev files are in developer's home
	public final static Dimension SCREEN = new Dimension( // for the primary monitor only
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth(), 
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight());
	public final static CaptureViewport CAPTURE_VIEWPORT = new CaptureViewport();
	public static Vector<Window> WINDOWS = new Vector<Window>(); // didn't want to manually manage windows, but Safari would only return a Frame through Window.getWindows() on commands called via JS
	
	private ApplicationController controller = null;
	private static Logger logger = Logger.getLogger(Applet.class);
	
	// called when this applet is loaded into the browser.
	@Override
    public void init() {
		try {
			// finish initializing all static variables
			RFX_FOLDER = new File(getRfxFolderPath()); // should be first
			BIN_FOLDER = new File(getBinFolderPath());
			DESKTOP_FOLDER = new File(getDesktopFolderPath());
			// from: http://www.theserverside.com/discussions/thread.tss?thread_id=42709
			if(Applet.DEV_MODE) {
				System.setProperty("log.file.path", "../../logs/development.log");
				PropertyConfigurator.configure("../../logs/config.properties");
			} else {
				System.setProperty("log.file.path", RFX_FOLDER.getAbsolutePath()+File.separator+"production.log");
				PropertyConfigurator.configure(this.getClass().getClassLoader().getResource("config.properties"));
			}
			try {
				JS_BRIDGE = JSObject.getWindow(this);
			} catch(JSException e) {
				logger.error("Could not create JSObject.  Probably in development mode.");
			}
			DOCUMENT_BASE = getDocumentBase();
			CODE_BASE = getCodeBase();
			APPLET = this; // breaking OOP so I can have a "root"
			POST_URL = getParameter("post_url");
			API_KEY = getParameter("api_key");
			if(!DEV_MODE && getParameter("dev_mode") != null)
				DEV_MODE = getParameter("dev_mode").equals("true");
			SCREEN_CAPTURE_NAME = getParameter("screen_capture_name");
			HOST_URL = DOCUMENT_BASE.getProtocol() + "://" + DOCUMENT_BASE.getHost();
			if(getParameter("headless") != null)
				HEADLESS = !getParameter("headless").isEmpty() && getParameter("headless").equals("true"); // Boolean.getBoolean(string) didn't work			 
			if( RFX_FOLDER.exists() && !RFX_FOLDER.isDirectory() && !RFX_FOLDER.delete() )
		        throw new IOException("Could not delete file for folder: " + RFX_FOLDER.getAbsolutePath());
			if( !RFX_FOLDER.exists() && !RFX_FOLDER.mkdir() )
		        throw new IOException("Could not create folder: " + RFX_FOLDER.getAbsolutePath());
			
			// print information to console
			logger.info(getAppletInfo());
			
			// execute a job on the event-dispatching thread; creating this applet's GUI
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                	// start up the os-specific controller
                	if(IS_MAC)
                		controller = new MacController();
                	else if(IS_LINUX)
                		controller = new LinuxController();
                	else if(IS_WINDOWS)
                		controller = new WindowsController();
                	else 
                		System.err.println("Want to launch controller but don't which operating system this is!");
                }
            });
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                	if(controller != null)
                		controller.setupExtensions();
                }
            });
		} catch (Exception e) {
            logger.error("Could not create GUI!",e);
        }
    }
	
	/**
	 * Sends a notification to all the views and each can update itself accordingly.
	 * 
	 * @param notification
	 * @param body
	 */
	public static void sendViewNotification(ViewNotifications notification,Object body) {
		//logger.debug("View Notification: "+notification);
		// applet is a special case (see ApplicationController constructor)
		if(APPLET.getContentPane().getComponents().length > 0)
			((ViewListener) APPLET.getContentPane().getComponent(0)).receiveViewNotification(notification, body);
		// another special case where the capture viewport is a pseudo-model
		CAPTURE_VIEWPORT.receiveViewNotification(notification, body);
		// notify all the open windows
		//Window[] windows = Window.getWindows();
		for(Window win : Applet.WINDOWS) {
			//System.out.println("Window: "+win);
			if(win instanceof ViewListener) {
				((ViewListener) win).receiveViewNotification(notification, body);
			}
		}
	}
	public static void sendViewNotification(ViewNotifications notification) {
		sendViewNotification(notification, null);
	}
	
	// ---------- BEGIN JAVASCRIPT API ----------
	/*
	public void prepareForRecording() {
		controller.prepareForRecording();
	}
	
	public void startRecording() {
		// TODO grabs default mixer right now, need a way to select microphones...
		//controller.startRecording(AudioSelector.get); // TODO fix when needed
	}
	*/
	
	/**
	 *  This method piggy backs on record GUI to drive any external (i.e. Flash) GUI.
	 */
	public void prepareAndRecord() { 
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				try {
					controller.recordGUI.prepareForRecording();
				} catch (Exception e) {
					logger.error("Can't prepare and start the recording!",e);
				}
				return null;
			}
		});
	}
	
	public void stopRecording() {
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				try {
					controller.recordGUI.stopRecording();
				} catch (Exception e) {
					logger.error("Can't stop the recording!",e);
				}
				return null;
			}
		});
	}
	/*
	public void previewRecording() {
		controller.previewRecording();
	}

	public void postRecording() {
		// TODO TEMPORARY until I get the Insight posting process down
		controller.askForAndSaveRecording();
	}
	*/
	public void showRecordingInterface() {
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				try {
					SwingUtilities.invokeLater(new Runnable() {
		                public void run() {
		                	if(controller != null) {
		                		controller.showRecordingInterface();
		                		logger.info("Outside call to show recording interface...");
		                	}
		                	else {
		                		logger.error("No controller exists!");
		                	}
		                	
		                }
		            });
				} catch (Exception e) {
					logger.error("Can't show the recording interface!",e);
				}
				return null;
			}
		});
	}
	
	public void hideRecordingInterface() {
		AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				try {
					
					SwingUtilities.invokeLater(new Runnable() {
		                public void run() {
		                	if(controller != null)
		                		controller.hideRecordingInterface();
		                }
		            });
				} catch (Exception e) {
					logger.error("Can't hide the recording interface!",e);
				}
				return null;
			}
		});
	}
	
	public static void handleRecordingUpdate(ViewNotifications state,String status) {
		if(status == null) status = "";
		jsCall("sct_handle_recording_update(\""+state+"\",\""+status+"\");");
	}
	
	public static void handleRecordingUIHide() {
		jsCall("sct_handle_recording_ui_hide();");
	}
	
	public static void handleExistingRecording() {
		jsCall("sct_handle_existing_recording()");
	}
	
	public static void handleFreshRecording() {
		jsCall("sct_handle_fresh_recording()");
	}
	
	public static void handleDeletedRecording() {
		jsCall("sct_handle_deleted_recording()");
	}
	
	public static void redirectWebPage(String url) {
		jsCall("sct_redirect_page(\""+url+"\");");
	}
	
	public static void sendShowStatus(String message) {
		jsCall("sct_show_status(\""+message+"\");");
	}
	
	public static void sendHideStatus() {
		jsCall("sct_hide_status();");
	}
	
	public static void sendInfo(String message) {
		jsCall("sct_info(\""+message+"\");");
	}
	
	public static void sendError(String message) {
		jsCall("sct_error(\""+message+"\");");
	}
	// ---------- END JAVASCRIPT API ----------
	
	private static void jsCall(String method) {
		if(JS_BRIDGE == null) {
			logger.error("Call to "+method+" but no JS Bridge exists. Probably in development mode...");
		} else {
			//System.out.println("Sending javascript call: "+method);
			//JSObject doc = (JSObject) JS_BRIDGE.getMember("document");
			//doc.eval(method);
			JS_BRIDGE.eval(method);
		}
	}
	
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
		
		// base code: http://stackoverflow.com/questions/2234476/how-to-detect-the-current-display-with-java
		
		// screen the Applet is on
		GraphicsDevice myScreen = getGraphicsConfiguration().getDevice();
		// screen the start bar, OS bar, whatever is on
		GraphicsDevice primaryScreen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		// all screens
		GraphicsDevice[] allScreens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		int myScreenIndex = -1, primaryScreenIndex = -1;
		for (int i = 0; i < allScreens.length; i++) {
		    if (allScreens[i].equals(myScreen))
		    {
		        myScreenIndex = i;
		    }
		    if (allScreens[i].equals(primaryScreen))
		    {
		    	primaryScreenIndex = i;
		    }
		}
		try {
			return 
				"APPLET PROPERLY INITIALIZED WITH THIS VARIABLES:\n"+
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
				"Multiple Monitors: \t"+(allScreens.length > 1)+"\n"+
				"Applet window is on screen " + myScreenIndex+"\n"+
				"Primary screen is index " + primaryScreenIndex+"\n"+
				"Primary screen resolution: "+SCREEN+"\n"+
				"Headless: \t"+HEADLESS+"\n";
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return "Error";
		}
		
		/*
		System.out.println("Have these system variables:");
		Map<String, String> sysEnv = System.getenv();
        for (String envName : sysEnv.keySet()) {
            System.out.format("%s=%s%n", envName, sysEnv.get(envName));
        }
		*/
		
		//System.out.println("Free space: \n"+TEMP_FOLDER.getFreeSpace()+" GBs"); // Java 1.6 only
		//System.out.println("Total space: \n"+TEMP_FOLDER.getTotalSpace()+" GBs");
	}
	
	/**
	 * These must start with "bin".
	 * 
	 * @return Name of folder and JAR with folder of same name for holding native extensions.
	 * @throws IOException
	 */
	public static String getBinFolderName() throws IOException {
		if(IS_MAC)
			return "bin-mac";
		else if(IS_LINUX)
			return "bin-linux";
		else if(IS_WINDOWS)
			return "bin-windows-v1.1";
		else
			throw new IOException("I don't know what bin folder to use!");
	}
	
	public static String getBinFolderPath() throws IOException {
		return RFX_FOLDER.getAbsolutePath()+File.separator+getBinFolderName();
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
    	System.out.println("Closing down...");
    	if(controller != null)
    		controller.closeDown();
    	controller = null;
    }

}
