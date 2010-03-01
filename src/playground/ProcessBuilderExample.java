package playground;

import java.io.IOException;
import java.util.*;

/**
 * This class demonstrates how to execute (exec) a system command
 * from a Java application using the ProcessBuilder and Process
 * classes, and our additional classes.
 * 
 * Documentation for this class is available at this URL:
 * 
 * http://www.devdaily.com/java/java-exec-processbuilder-process-1
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
 * Please see the following page for the LGPL license:
 * http://www.gnu.org/licenses/lgpl.txt
 * 
 */
public class ProcessBuilderExample
{
  
  public static void main(String[] args) throws Exception
  {
    new ProcessBuilderExample();
  }

  // can run basic ls or ps commands
  // can run command pipelines
  // can run sudo command if you know the password is correct
  public ProcessBuilderExample() throws IOException, InterruptedException
  {
    List<String> commands = new ArrayList<String>();
    //commands.add("/bin/sh");
    //commands.add("-c");
    //commands.add("ls -l /var/tmp | grep tmp");
    commands.add("./vlc-test/VLC -IRC --rc-host=localhost:4444");
    SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands);
    int result = commandExecutor.execCommand();
    /*StringBuilder stdout = commandExecutor.getStandardOutputFromCommand();
    StringBuilder stderr = commandExecutor.getStandardErrorFromCommand();
    System.out.println("The numeric result of the command was: " + result);
    System.out.println("STDOUT");
    System.out.println(stdout);
    System.out.println("STDERR");
    System.out.println(stderr);*/
  }
}

