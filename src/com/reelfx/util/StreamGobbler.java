package com.reelfx.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamGobbler extends Thread {
    InputStream is;
    boolean discard;
    String prefix;
    
    public StreamGobbler(InputStream is, boolean discard, String prefix) {
      this.is = is;
      this.discard = discard;
      this.prefix = prefix;
    }
    
    @Override
    public void run() {
	    try {
	      InputStreamReader isr = new InputStreamReader(is);
	      BufferedReader br = new BufferedReader(isr);
	      String line=null;
	      while ( (line = br.readLine()) != null)
	        if(!discard)
	          System.out.println(prefix+": "+line); 
	    }
	    
	    catch (IOException ioe) {
	      ioe.printStackTrace();  
	    }
	 }
    
    @Override
    protected void finalize() throws Throwable {
    	super.finalize();
    	is.close();
    }
}