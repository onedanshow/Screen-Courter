package com.reelfx.model.util;

import java.util.EventListener;

public interface ProcessListener extends EventListener {
	public void processUpdate(int event,Object body);
}
