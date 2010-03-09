package com.reelfx.model.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.event.EventListenerList;

public class StreamGobbler extends Thread {
    protected InputStream is;
    protected boolean discardOutput;
    protected String prefix;
    protected Map<String,EventListenerList> listeners = new HashMap<String, EventListenerList>();
    
    public StreamGobbler(InputStream is, boolean discard, String prefix) {
      this.is = is;
      this.discardOutput = discard;
      this.prefix = prefix;
    }
    
    @Override
    public void run() {
	    try {
	      InputStreamReader isr = new InputStreamReader(is);
	      BufferedReader br = new BufferedReader(isr);
	      String line=null;
	      while ( (line = br.readLine()) != null) {
	        if(!discardOutput)
	          System.out.println(prefix+": "+line);
	        for(String word : listeners.keySet())
	        	if(line.contains(word))
	        		fireActionPerformed(word,line);
	      }
	    }
	    
	    catch (IOException ioe) {
	      ioe.printStackTrace();  
	    }
	 }
    
    /**
     * Adds an <code>ActionListener</code> to the button.
     * @param l the <code>ActionListener</code> to be added
     */
    public void addActionListener(String word,ActionListener l) {
    	if(!listeners.containsKey(word)) {
    		listeners.put(word, new EventListenerList());
    	}
        listeners.get(word).add(ActionListener.class, l);
    }
    
    /**
     * Removes an <code>ActionListener</code>.
     * If the listener is the currently set <code>Action</code>
     * for the button, then the <code>Action</code>
     * is set to <code>null</code>.
     *
     * @param l the listener to be removed
     */
    public void removeActionListener(ActionListener l) {
    	for(EventListenerList forWord : listeners.values())
    		forWord.remove(ActionListener.class, l);
    }
    
    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance 
     * is lazily created using the <code>event</code> 
     * parameter.
     *
     * @param event  the <code>ActionEvent</code> object
     * @see EventListenerList
     */
    protected void fireActionPerformed(String word,String line) {
        // Guaranteed to return a non-null array
        Object[] forWord = listeners.get(word).getListenerList();
        ActionEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = forWord.length-2; i>=0; i-=2) {
            if (forWord[i]==ActionListener.class) {
                // Lazily create the event:
                if (e == null) {
                	e = new ActionEvent(StreamGobbler.this,ActionEvent.ACTION_PERFORMED,line);
                }
                ((ActionListener)forWord[i+1]).actionPerformed(e);
            }          
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
    	super.finalize();
    	is.close();
    }
}