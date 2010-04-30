package com.reelfx.model;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import com.reelfx.Applet;
import com.reelfx.controller.LinuxController;
import com.reelfx.controller.WindowsController;
import com.reelfx.model.util.ProcessWrapper;
import com.reelfx.model.util.StreamGobbler;
import com.reelfx.view.OptionsInterface;

public class PostProcessor extends ProcessWrapper implements ActionListener {
	
	// FILE LOCATIONS
	private static String ext = ".mov"; //Applet.IS_MAC ? ".mov" : ".mp4";
	public static File DEFAULT_OUTPUT_FILE = new File(Applet.RFX_FOLDER.getAbsolutePath()+File.separator+"review"+ext);
	private File outputFile = null;
	private String postUrl = null;
	private boolean postRecording = false, postData = false;
	
	// ENCODING SETTINGS
	public final static int OFFSET_VIDEO = 0;
	public final static int OFFSET_AUDIO = 1;
	private Map<Integer, String> encodingOpts = new HashMap<Integer, String>();
	
	// STATES
	public final static int ENCODING_STARTED = 0;
	public final static int ENCODING_PROGRESS = 1;
	public final static int ENCODING_COMPLETE = 2;
	public final static int POST_STARTED = 3;
	public final static int POST_PROGRESS = 4;
	public final static int POST_FAILED = 5;
	public final static int POST_COMPLETE = 6;
	
	protected Process ffmpegProcess;
	protected StreamGobbler errorGobbler, inputGobbler;
	
	public void encodingOptions(Map<Integer,String> opts) {
		encodingOpts = opts;
	}
	
	public synchronized void saveToComputer(File file) {
		if(!file.getName().endsWith(ext) && !file.getName().endsWith(".avi"))
			file = new File(file.getAbsoluteFile()+ext); // extension will probably change for Windows
		outputFile = file;
		postRecording = false;
		postData = false;
		super.start();
	}
	
	// implemented and works, but ended up not using
	public synchronized void postDataToInsight(String url) {
		outputFile = null;
		postUrl = url;
		postRecording = false;
		postData = true;
		super.start();
	}
	
	public synchronized void postRecordingToInsight(String url) {
		outputFile = DEFAULT_OUTPUT_FILE;
		postUrl = url;
		postRecording = true;
		postData = false;
		super.start();
	}

	@Override
	public synchronized void start() {
		System.err.println("Don't call this directly!");
	}

	public void run() {
		try {
			// ----- encode a file -----------------------
			if(outputFile != null) {
				String ffmpeg = "ffmpeg" + (Applet.IS_WINDOWS ? ".exe" : "");
				
				if(Applet.IS_WINDOWS 
						&& outputFile.getAbsolutePath().equals(WindowsController.MERGED_OUTPUT_FILE.getAbsolutePath())
						&& WindowsController.MERGED_OUTPUT_FILE.exists()) {
					// do no encoding
				}
				else if(Applet.IS_LINUX 
						&& outputFile.getAbsolutePath().equals(LinuxController.MERGED_OUTPUT_FILE.getAbsolutePath())
						&& LinuxController.MERGED_OUTPUT_FILE.exists()) {
					// do no encoding
				}
				else if(Applet.IS_WINDOWS || Applet.IS_LINUX) {
					fireProcessUpdate(ENCODING_STARTED);
					// get information about the media file:
					//Map<String,Object> metadata = parseMediaFile(ScreenRecorder.OUTPUT_FILE.getAbsolutePath());
					//printMetadata(metadata);
					
					if(outputFile.exists() && !outputFile.delete()) // ffmpeg will halt and ask what to do if file exists
						throw new IOException("Could not delete the old exported file!");
					
					List<String> ffmpegArgs = new ArrayList<String>();
			    	ffmpegArgs.add(Applet.BIN_FOLDER.getAbsoluteFile()+File.separator+ffmpeg);
			    	// audio and video files
			    	if(AudioRecorder.OUTPUT_FILE.exists()) { // if opted for microphone
			    		// delay the audio if needed ( http://howto-pages.org/ffmpeg/#delay )
			    		if(encodingOpts.containsKey(OFFSET_AUDIO))
			    			ffmpegArgs.addAll(parseParameters("-itsoffset 00:00:0"+encodingOpts.get(OFFSET_AUDIO))); // assume offset is less than 10 seconds
			    		ffmpegArgs.addAll(parseParameters("-i "+AudioRecorder.OUTPUT_FILE.getAbsolutePath()));
			    		// delay the video if needed ( http://howto-pages.org/ffmpeg/#delay )
			    		if(encodingOpts.containsKey(OFFSET_VIDEO))
			    			ffmpegArgs.addAll(parseParameters("-itsoffset 00:00:0"+encodingOpts.get(OFFSET_VIDEO)));
			    	}
			    	ffmpegArgs.addAll(parseParameters("-i "+ScreenRecorder.OUTPUT_FILE));
			    	// export settings
			    	ffmpegArgs.addAll(getFfmpegCopyParams());
			    	// resize screen
			    	Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			    	ffmpegArgs.addAll(parseParameters("-s 1024x"+Math.round(1024.0/(double)dim.width*(double)dim.height)));
			    	//ffmpegArgs.addAll(getFfmpegX264FastFirstPastBaselineParams());
			    	ffmpegArgs.add(outputFile.getAbsolutePath());
			    	System.out.println("Executing this command: "+prettyCommand(ffmpegArgs));
			        ProcessBuilder pb = new ProcessBuilder(ffmpegArgs);
			        ffmpegProcess = pb.start();
			
			        errorGobbler = new StreamGobbler(ffmpegProcess.getErrorStream(), false, "ffmpeg E");
			        inputGobbler = new StreamGobbler(ffmpegProcess.getInputStream(), false, "ffmpeg O");
			        
			        System.out.println("Starting listener threads...");
			        errorGobbler.addActionListener("frame", this);
			        errorGobbler.start();
			        inputGobbler.start();  
			        
			        ffmpegProcess.waitFor();
			        
			        fireProcessUpdate(ENCODING_COMPLETE);
				}
				else if(Applet.IS_MAC) {
					FileUtils.copyFile(ScreenRecorder.OUTPUT_FILE, outputFile);
					fireProcessUpdate(ENCODING_COMPLETE);
				}
			} // end if outputFile
			
			// ----- post data of screen capture to Insight -----------------------			
	        if(postRecording) {
	        	fireProcessUpdate(POST_STARTED);
	        	
	        	// base code: http://stackoverflow.com/questions/1067655/how-to-upload-a-file-using-java-httpclient-library-working-with-php-strange-pro
	        	
	        	HttpClient client = new DefaultHttpClient();
	        	client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
	        	
	        	MultipartEntity entity = new MultipartEntity();
	        	ContentBody body = new FileBody(outputFile,"video/quicktime");
	        	entity.addPart("capture_file",body);
	        	
	        	HttpPost post = new HttpPost(postUrl+"?api_key="+Applet.API_KEY); // TODO make this url construction more robust
	        	post.setEntity(entity);
	        	
	        	System.out.println("Posting file to Insight... "+post.getRequestLine());
	        	
	        	HttpResponse response = client.execute(post);
	        	HttpEntity responseEntity = response.getEntity();

	        	System.out.println("Response Status Code: "+response.getStatusLine());
	            /*if (responseEntity != null) {
	            	System.out.println(EntityUtils.toString(responseEntity)); // to see the response body
	            }*/
	            
	            // redirection to show page (meaning everything was correct)
	            if(response.getStatusLine().getStatusCode() == 302) {
	            	//Header header = response.getFirstHeader("Location");
	            	//System.out.println("Redirecting to "+header.getValue());
	            	//Applet.redirectWebPage(header.getValue());
	            	//Applet.APPLET.showDocument(new URL(header.getValue()),"_self");
	            	fireProcessUpdate(POST_COMPLETE);
	            } else {
	            	fireProcessUpdate(POST_FAILED);
	            }
	            	
	            if (responseEntity != null) {
	            	responseEntity.consumeContent();
	            }
	        	
	        	client.getConnectionManager().shutdown();
	        }
			// ----- post data of screen capture to Insight -----------------------	        
	        else if(postData) {
	        	fireProcessUpdate(POST_STARTED);
	        	
	        	HttpClient client = new DefaultHttpClient();
		    	client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		    	
		    	HttpPost post = new HttpPost(postUrl+"?api_key="+Applet.API_KEY); // TODO make this url construction more robust
		    	//post.setEntity(new StringEntity("I'm an entity that needs to be saved..."));
		    	
		    	System.out.println("Sending data to Insight... "+post.getRequestLine());
		    	
		    	HttpResponse response = client.execute(post);
		    	HttpEntity responseEntity = response.getEntity();
		
		    	System.out.println("Response Status Code: "+response.getStatusLine());
		        if (responseEntity != null) {
		        	System.out.println(EntityUtils.toString(responseEntity)); // to see the response body
		        }
		        
		        // redirection to show page (meaning everything was correct)
		        if(response.getStatusLine().getStatusCode() == 302) {
		        	Header header = response.getFirstHeader("Location");
		        	System.out.println("Redirecting to "+header.getValue());
		        	Applet.redirectWebPage(header.getValue());
		        	fireProcessUpdate(POST_COMPLETE);
		        } else {
		        	fireProcessUpdate(POST_FAILED);
		        }
		        	
		        if (responseEntity != null) {
		        	responseEntity.consumeContent();
		        }
		    	
		    	client.getConnectionManager().shutdown();
	        }
	        
	        // TODO monitor the progress of the event
	        // TODO allow canceling of the transcoding?
	        
	  } catch (IOException ioe) {
		  ioe.printStackTrace();
	  } catch (Exception ie) {
		  ie.printStackTrace();
	  }
	  
	  outputFile = null;
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
		ffmpegProcess.destroy();
	}
	
	/**
	 * Called when a stream gobbler finds a line that relevant to this wrapper.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().contains("frame")) {
			//System.out.println("Found frame!"); // TODO exact the frame
			fireProcessUpdate(ENCODING_PROGRESS, null);
		}
	}
	
	public static void deleteOutput() {
		AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				try {
					if(DEFAULT_OUTPUT_FILE.exists() && !DEFAULT_OUTPUT_FILE.delete())
						throw new Exception("Can't delete the old audio file!");
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		});
	}
}
