package com.reelfx.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.reelfx.Applet;

public class PreferenceManager {

	public static File OUTPUT_FILE = new File(Applet.RFX_FOLDER.getAbsolutePath()+File.separator+"prefs");
	
	private String postUrl, screenCaptureName, userID;
	private Date date;
	private boolean uploaded = false;

	public PreferenceManager() {
		super();
		
		readPreferences();
	}

	public void readPreferences() {
		if(!OUTPUT_FILE.exists()) return;
		
		try {
	      BufferedReader input =  new BufferedReader(new FileReader(OUTPUT_FILE));
	      try {
	        postUrl = input.readLine();
	        screenCaptureName = input.readLine();
	        userID = input.readLine();
	        date = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy").parse(input.readLine());
	        uploaded = Boolean.parseBoolean(input.readLine());
	      } 
	      catch (ParseException e) {			
			e.printStackTrace();
	      }
	      finally {
	        input.close();
	      }
	    }
	    catch (IOException ex){
	      ex.printStackTrace();
	    }
	}
	
	public void writePreferences() {
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(OUTPUT_FILE));
			try {
				output.write(postUrl+"\n");
				output.write(screenCaptureName+"\n");
				output.write(userID+"\n");
				output.write(date+"\n");
				output.write(uploaded+"\n");
			}
			finally {
				output.close();
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public String getPostUrl() {
		return postUrl;
	}

	public void setPostUrl(String postUrl) {
		this.postUrl = postUrl;
	}

	public String getScreenCaptureName() {
		return screenCaptureName;
	}

	public void setScreenCaptureName(String screenCaptureName) {
		this.screenCaptureName = screenCaptureName;
	} 
	
	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}
	
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
	
	public boolean isUploaded() {
		return uploaded;
	}

	public void setUploaded(boolean uploaded) {
		this.uploaded = uploaded;
	}
	
	public static void deleteOutput() {
		AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				try {
					if(OUTPUT_FILE.exists() && !OUTPUT_FILE.delete())
						throw new Exception("Can't delete the old preference file!");
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		});
	}
}
