package com.reelfx.util;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.EventListenerList;

public abstract class ProcessWrapper extends Thread {
	protected List<ProcessListener> listeners = new ArrayList<ProcessListener>();
	
    public void addProcessListener(ProcessListener listener) {
        listeners.add(listener);
    }

    public void removeProcessListener(ProcessListener listener) {
        listeners.remove(listener);
    }
    
    protected void fireProcessUpdate(int event) {
    	for (ProcessListener listener : listeners) {
            listener.processUpdate(event);
        }
    }
    
}
