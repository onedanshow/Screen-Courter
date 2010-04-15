package com.reelfx.view;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JWindow;

public class CountDown extends JWindow {

	private static final long serialVersionUID = -7241374504656558463L;

	public CountDown() {
		
		Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screen = tk.getScreenSize();
        
		setPreferredSize(new Dimension(300, 300));
		setLocation(screen.width/2-150, screen.height/2-150);
		setAlwaysOnTop(true);
		
		
	}
}
