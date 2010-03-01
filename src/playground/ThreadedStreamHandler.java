package playground;

import java.io.*;

/**
 * ThreadedStreamHandler.java
 * @version 0.1
 *
 * This class is intended to be used with the SystemCommandExecutor
 * class to let users execute system commands from Java applications.
 * 
 * This class is based on work that was shared in a JavaWorld article
 * named "When System.exec() won't". That article is available at this
 * url:
 * 
 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html
 *
 * Our documentation is available at this URL:
 * 
 * http://www.devdaily.com/java/java-exec-processbuilder-process-3
 * 
 * Copyright 2010 alvin j. alexander, devdaily.com.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.

 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please ee the following page for the LGPL license:
 * http://www.gnu.org/licenses/lgpl.txt
 * 
 */
class ThreadedStreamHandler extends Thread
{
  InputStream inputStream;
  String adminPassword;
  OutputStream outputStream;
  PrintWriter printWriter;
  StringBuilder outputBuffer = new StringBuilder();
  private boolean sudoIsRequested = false;
  
  /**
   * A simple constructor for when the sudo command is not necessary.
   * This constructor will just run the command you provide, without
   * running sudo before the command, and without expecting a password.
   * 
   * @param inputStream
   * @param streamType
   */
  ThreadedStreamHandler(InputStream inputStream)
  {
    this.inputStream = inputStream;
  }

  /**
   * Use this constructor when you want to invoke the 'sudo' command.
   * The outputStream must not be null. If it is, you'll regret it. :)
   * 
   * TODO this currently hangs if the admin password given for the sudo command is wrong.
   * 
   * @param inputStream
   * @param streamType
   * @param outputStream
   * @param adminPassword
   */
  ThreadedStreamHandler(InputStream inputStream, OutputStream outputStream, String adminPassword)
  {
    this.inputStream = inputStream;
    this.outputStream = outputStream;
    this.printWriter = new PrintWriter(outputStream);
    this.adminPassword = adminPassword;
    this.sudoIsRequested = true;
  }
  
  public void run()
  {
    // on mac os x 10.5.x, when i run a 'sudo' command, i need to write
    // the admin password out immediately; that's why this code is
    // here.
    if (sudoIsRequested)
    {
      //doSleep(500);
      printWriter.println(adminPassword);
      printWriter.flush();
    }

    BufferedReader bufferedReader = null;
    try
    {
      bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
      String line = null;
      while ((line = bufferedReader.readLine()) != null)
      {
        outputBuffer.append(line + "\n");
      }
    }
    catch (IOException ioe)
    {
      // TODO handle this better; users won't want the code doing this
      ioe.printStackTrace();
    }
    catch (Throwable t)
    {
      // TODO handle this better; users won't want the code doing this
      t.printStackTrace();
    }
    finally
    {
      try
      {
        bufferedReader.close();
      }
      catch (IOException e)
      {
        // ignore this one
      }
    }
  }
  
  private void doSleep(long millis)
  {
    try
    {
      Thread.sleep(millis);
    }
    catch (InterruptedException e)
    {
      // ignore
    }
  }
  
  public StringBuilder getOutputBuffer()
  {
    return outputBuffer;
  }

}

