package com.reelfx.model.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.swing.SwingUtilities;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.log4j.Logger;

import com.reelfx.Applet;
import com.reelfx.model.PostProcessor;
import com.reelfx.view.util.ViewNotifications;

/**
 * Implemented so I can maintain a progress bar while uploading a screen capture.  Based on discussion from here:
 * http://stackoverflow.com/questions/254719/file-upload-with-java-with-progress-bar/3154929
 * http://stackoverflow.com/questions/3163131/how-to-properly-extend-java-filteroutputstream-class
 * 
 * @author daniel
 *
 */
public class CountingMultipartEntity extends MultipartEntity {
	
	private static Logger logger = Logger.getLogger(CountingMultipartEntity.class);
	
	@Override
	public void writeTo(OutputStream outstream) throws IOException {
		super.writeTo(new CountingOutputStream(outstream));
	}
	
	public class CountingOutputStream extends FilterOutputStream {

        public long transferred;
        public int progress;

        public CountingOutputStream(final OutputStream out) {
            super(out);
            this.transferred = 0;
            this.progress = 0;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            this.transferred += len;
            this.progress = Math.round((float)transferred / (float)getContentLength() * 100.0F);
            //logger.info("Transferred: "+this.transferred+ " Length: "+getContentLength());
            SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					Applet.sendViewNotification(ViewNotifications.THINKING_PROGRESS,progress);
				}
			});
        }

        public void write(int b) throws IOException {
            out.write(b);
            this.transferred++;
            this.progress = Math.round((float)transferred / (float)getContentLength() * 100.0F);
            //logger.info("Transferred: "+this.transferred+" Length: "+getContentLength());
            SwingUtilities.invokeLater(new Runnable() {	
				public void run() {
					Applet.sendViewNotification(ViewNotifications.THINKING_PROGRESS,progress);
				}
			});
        }
    }

	
}
