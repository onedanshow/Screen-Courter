package playground;

////////////////////////////////////////////////////////////////////////////
//Program: copyURL.java
//Author: Anil Hemrajani (anil@patriot.net)
//Purpose: Utility for copying files from the Internet to local disk
//Example: 1. java copyURL http://www.patriot.net/users/anil/resume/resume.gif
//       2. java copyURL http://www.ibm.com/index.html abcd.html
////////////////////////////////////////////////////////////////////////////
/**
 * Taken from: http://www.javaworld.com/javatips/jw-javatip19.html?page=1
 */
import java.net.*;
import java.io.*;
import java.util.Date;
import java.util.StringTokenizer;

class DownloadTest
{
public static void main(String args[])
{
   if (args.length < 1)
   {
       System.err.println
            ("usage: java copyURL URL [LocalFile]");
       System.exit(1);
   }
   try
   {
       URL url  = new URL(args[0]);
       System.out.println("Opening connection to " + args[0] + "...");
       URLConnection urlC = url.openConnection();
       // Copy resource to local file, use remote file
       // if no local file name specified
       InputStream is = url.openStream();
       // Print info about resource
       System.out.print("Copying resource (type: "+urlC.getContentType());
       
       Date date=new Date(urlC.getLastModified());
       System.out.println(", modified on: " + date.toLocaleString() + ")...");
       System.out.flush();
       
       FileOutputStream fos=null;
       if (args.length < 2)
       {
           String localFile=null;
           // Get only file name
           StringTokenizer st=new StringTokenizer(url.getFile(), "/");
           while (st.hasMoreTokens())
                  localFile=st.nextToken();
           fos = new FileOutputStream(localFile);
       }
       else
           fos = new FileOutputStream(args[1]);
       
       int oneChar, count=0;
       
       while ((oneChar=is.read()) != -1)
       {
          fos.write(oneChar);
          count++;
       }
       is.close();
       fos.close();
       System.out.println(count + " byte(s) copied");
   }
   catch (MalformedURLException e)
   { System.err.println(e.toString()); }
   catch (IOException e)
   { System.err.println(e.toString()); }
}
}

