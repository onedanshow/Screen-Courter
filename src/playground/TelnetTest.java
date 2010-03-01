/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package playground;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import org.apache.commons.net.telnet.TelnetClient;

/**
 *
 * @author daniel
 */
public class TelnetTest {
    public static void main(String args[]) {


        TelnetClient tl = new TelnetClient();
        try {
            tl.connect("localhost", 4444);
            if(tl.isConnected()) {
                System.out.println("Connected successfully!");

                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(tl.getOutputStream()));
                bw.write("quit");
                bw.flush();

            } else {
                System.err.println("Problem with connection");
            }
        } catch(Exception e) {
            System.err.println("Telnet connection threw an exception: "+e.getMessage());
        }
    }
}
