package com.reelfx;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

import com.sun.JarClassLoader;

/**
 * INSTALL:
 * 	-put "bin-(OS).jar" on the class path
 * 
 * @author daniel
 *
 */

public class RfxApplet extends JApplet {

	private static final long serialVersionUID = 4544354980928245103L;
	
	public static File RFX_FOLDER, BIN_FOLDER, DESKTOP_FOLDER;
	public static URL DOCUMENT_BASE, CODE_BASE;
	public static boolean IS_MAC = System.getProperty("os.name").contains("Mac");
	public static boolean IS_WINDOWS = System.getProperty("os.name").contains("Windows");
	public static boolean DEV_MODE = System.getProperty("user.dir").contains(System.getProperty("user.home")); // assume dev files are in developer's home
	
	private Interface ui = null;
	
	// called when this applet is loaded into the browser.
	@Override
    public void init() {
		try {
			RFX_FOLDER = new File(getRfxFolder());
			BIN_FOLDER = new File(RFX_FOLDER.getAbsolutePath()+File.separator+"bin-mac");
			DESKTOP_FOLDER = new File(getDesktopFolder());
			DOCUMENT_BASE = getDocumentBase();
			CODE_BASE = getCodeBase();
			
			System.out.println(getAppletInfo());			 
						
			if( RFX_FOLDER.exists() && !RFX_FOLDER.isDirectory() && !RFX_FOLDER.delete() )
		        throw new IOException("Could not delete file for folder: " + RFX_FOLDER.getAbsolutePath());
			if( !RFX_FOLDER.exists() && !RFX_FOLDER.mkdir() )
		        throw new IOException("Could not create folder: " + RFX_FOLDER.getAbsolutePath());
			
			System.out.println("Have access to folder: "+RFX_FOLDER.getAbsolutePath()+File.separator);
			
			// execute a job on the event-dispatching thread; creating this applet's GUI.
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                	// start up the GUI
                	ui = new Interface();
                	ui.setVisible(true);
                	ui.pack();
                }
            });
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                	if(ui != null)
                		ui.setupExtensions();
                }
            });
        
		} catch (IOException e1) {
			System.err.println("Could not create temporary folder!");
			e1.printStackTrace();
		}	catch (Exception e) {
            System.err.println("Could not create GUI!");
            e.printStackTrace();
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
    	 return copyFileFromJar(sResource, fDest, RfxApplet.class.getClassLoader());
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
				"User Desktop: \t"+DESKTOP_FOLDER.getPath()+"\n"+
				"Code Base: \t"+getCodeBase().getPath()+"\n"+
				"Document Base: \t"+getDocumentBase().getPath()+"\n"+
				"Execution URL: \t"+RfxApplet.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()+"\n";
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "Error";
		}
		
		//System.out.println("Free space: \n"+TEMP_FOLDER.getFreeSpace()+" GBs"); // Java 1.6 only
		//System.out.println("Total space: \n"+TEMP_FOLDER.getTotalSpace()+" GBs");
	}
	
	public String getRfxFolder() {
		// TODO resolve based on OS
		return System.getProperty("user.home")+File.separator+"Library"+File.separator+"ReelFX";
	}
	
	public String getDesktopFolder() {
		// TODO resolve based on OS
		return System.getProperty("user.home")+File.separator+"Desktop";
	}
    
    /**
     * Called when the browser closes the web page.
     * 
     *  NOTE: A bug in Mac OS X may prevent this from being called: http://lists.apple.com/archives/java-dev///2009/Oct/msg00042.html
     */
    @Override
    public void destroy() {
        System.out.println("Setting up to close down...");
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                	System.out.println("Closing down...");
                	ui.closeApplication();
                    ui.setVisible(false);
                    ui = null;
                }
            });
        } catch (Exception e) { }

    }

}
