package com.reelfx.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import com.reelfx.Applet;
import com.reelfx.model.util.ProcessWrapper;
import com.reelfx.model.util.StreamGobbler;

public class PostProcessor extends ProcessWrapper implements ActionListener {
	
	// FILE LOCATIONS
	public static String DEFAULT_OUTPUT_FILE = Applet.RFX_FOLDER.getAbsolutePath()+File.separator+"output-final.mp4";
	private File outputFile = null;
	private boolean postFile = false;
	
	// STATES
	public final static int ENCODING_STARTED = 0;
	public final static int ENCODING_PROGRESS = 1;
	public final static int ENCODING_COMPLETE = 2;
	public final static int POST_STARTED = 3;
	public final static int POST_PROGRESS = 4;
	public final static int POST_COMPLETE = 5;
	
	protected Process postProcess;
	protected StreamGobbler errorGobbler, inputGobbler;
	
	public synchronized void saveToComputer(File file) {
		if(!file.getName().endsWith(".mp4"))
			file = new File(file.getAbsoluteFile()+".mp4"); // extension will probably change for Windows
		outputFile = file;
		postFile = false;
		super.start();
	}
	
	public synchronized void postToInsight() {
		outputFile = new File(DEFAULT_OUTPUT_FILE);
		postFile = true;
		super.start();
	}

	@Override
	public synchronized void start() {
		System.err.println("Don't call this directly!");
	}

	public void run() {
		try {
			fireProcessUpdate(ENCODING_STARTED);
			
			if(Applet.IS_MAC) {
				Map<String,Object> metadata = parseMediaFile(ScreenRecorder.OUTPUT_FILE);
				printMetadata(metadata);
				
				if(outputFile.exists() && !outputFile.delete()) // ffmpeg will halt and ask what to do if file exists
					throw new IOException("Could not delete the old exported file!");
				
				List<String> ffmpegArgs = new ArrayList<String>();
		    	ffmpegArgs.add(Applet.BIN_FOLDER.getAbsoluteFile()+File.separator+"ffmpeg");
		    	// audio settings
		    	if(AudioRecorder.OUTPUT_FILE.exists()) // if opted for microphone
		    		ffmpegArgs.addAll(parseParameters("-ar 44100 -i "+AudioRecorder.OUTPUT_FILE.getAbsolutePath()));
		    	// video settings
		    	ffmpegArgs.addAll(parseParameters("-i "+ScreenRecorder.OUTPUT_FILE));
		    	// export settings
		    	ffmpegArgs.addAll(getFfmpegX264Params());
		    	ffmpegArgs.add(outputFile.getAbsolutePath());
		        ProcessBuilder pb = new ProcessBuilder(ffmpegArgs);
		        postProcess = pb.start();
		
		        errorGobbler = new StreamGobbler(postProcess.getErrorStream(), false, "ffmpeg E");
		        inputGobbler = new StreamGobbler(postProcess.getInputStream(), false, "ffmpeg O");
		        
		        System.out.println("Starting listener threads...");
		        errorGobbler.addActionListener("frame", this);
		        errorGobbler.start();
		        inputGobbler.start();  
		        
		        postProcess.waitFor();
			}
			else if(Applet.IS_LINUX) {
				FileUtils.moveFile(new File(ScreenRecorder.OUTPUT_FILE), new File(DEFAULT_OUTPUT_FILE));
			}
			
	        fireProcessUpdate(ENCODING_COMPLETE);
	        
	        if(postFile) {
	        	fireProcessUpdate(POST_STARTED);
	        	
	        	HttpClient client = new DefaultHttpClient();
	        	client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
	        	
	        	MultipartEntity entity = new MultipartEntity();
	        	ContentBody body = new FileBody(outputFile,"video/quicktime");
	        	entity.addPart("review_media_file",body);
	        	
	        	HttpPost post = new HttpPost(Applet.POST_URL+"&api_key="+Applet.API_KEY); // TODO make this url construction more robust
	        	post.setEntity(entity);
	        	
	        	System.out.println("Posting file to Insight... "+post.getRequestLine());
	        	
	        	HttpResponse response = client.execute(post);
	        	HttpEntity responseEntity = response.getEntity();

	        	System.out.println(response.getStatusLine());
	        	// TODO verify that the response came back properly
	            if (responseEntity != null) {
	            	System.out.println(EntityUtils.toString(responseEntity));
	            }
	            if (responseEntity != null) {
	            	responseEntity.consumeContent();
	            }
	        	
	        	client.getConnectionManager().shutdown();
	        	
	        	fireProcessUpdate(POST_COMPLETE);
	        }
	        
	        // TODO monitor the progress of the event
	        // TODO allow canceling of the transcoding
	        // TODO increment output file name if another already exists
	        // TODO allow people to save to desktop if they wish
	        
	  } catch (IOException ioe) {
		  ioe.printStackTrace();
	  } catch (Exception ie) {
		  ie.printStackTrace();
	  }
	  
	  outputFile = null;
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
		postProcess.destroy();
	}
	
	/**
	 * Called when a stream gobbler finds a line that relevant to this wrapper.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().contains("frame")) {
			System.out.println("Found frame!"); // TODO exact the frame
			fireProcessUpdate(ENCODING_PROGRESS, null);
		}
	}
}
