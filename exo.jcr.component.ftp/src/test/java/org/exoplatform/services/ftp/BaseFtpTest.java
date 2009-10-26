/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.ftp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public abstract class BaseFtpTest extends BaseStandaloneTest
{

   protected Socket commandConn;

   protected String user = "root";

   protected String password = "exo";

   protected String host = "localhost";

   protected int port = 2122;

   protected BufferedReader inputCommand;

   protected BufferedWriter outputCommand;

   public void setUp() throws Exception
   {
      super.setUp();
      root.addNode("ftp-root", "nt:folder");
      session.save();
   }

   protected void connect() throws IOException
   {
      commandConn = new Socket(host, port);
      inputCommand = new BufferedReader(new InputStreamReader(commandConn.getInputStream()));
      outputCommand = new BufferedWriter(new OutputStreamWriter(commandConn.getOutputStream()));

      String response = readResponse();
      assertTrue(response.startsWith(FtpConst.EXO_LOGO[0]));

      sendCommand(FtpConst.Commands.CMD_USER + ' ' + user);

      response = readResponse();
      assertTrue(response.startsWith("331 "));

      sendCommand(FtpConst.Commands.CMD_PASS + ' ' + password);

      response = readResponse();
      assertTrue(response.startsWith("230 "));
   }

   protected void disconnect() throws IOException
   {
      try
      {
         sendCommand(FtpConst.Commands.CMD_QUIT);
      }
      finally
      {
         inputCommand.close();
         outputCommand.close();
         commandConn.close();
      }
   }

   protected String pwd() throws IOException
   {
      sendCommand(FtpConst.Commands.CMD_PWD);
      String response = readResponse();
      assertTrue(response.startsWith("257 "));
      int q1 = response.indexOf('"');
      int q2 = response.indexOf('"', q1 + 1);
      return response.substring(q1 + 1, q2);
   }

   protected void cwd(String dir) throws IOException
   {
      sendCommand(FtpConst.Commands.CMD_CWD + ' ' + dir);
      String response = readResponse();
      assertTrue(response.startsWith("250 "));
   }

   protected void stor(byte[] data, String filename) throws IOException
   {
      String response = pasv();

      int begin = response.indexOf('(');
      int end = response.indexOf(')', begin + 1);
      String address = response.substring(begin + 1, end);
      StringTokenizer tokens = new StringTokenizer(address, ",");
      String _host =
         tokens.nextToken() + "." + tokens.nextToken() + "." + tokens.nextToken() + "." + tokens.nextToken();
      int _port = Integer.parseInt(tokens.nextToken()) * 256 + Integer.parseInt(tokens.nextToken());

      sendCommand(FtpConst.Commands.CMD_STOR + ' ' + filename);

      Socket dataConn = new Socket(_host, _port);

      response = readResponse();
      assertTrue(response.startsWith("125 "));

      OutputStream output = null;
      try
      {
         output = dataConn.getOutputStream();
         output.write(data);
         output.flush();
      }
      finally
      {
         if (outputCommand != null)
         {
            output.close();
         }
      }

      response = readResponse();
      dataConn.close();

      assertTrue(response.startsWith("226 "));
   }

   protected void bin() throws IOException
   {
      type('I');
   }

   protected void ascii() throws IOException
   {
      type('A');
   }

   protected void type(char type) throws IOException
   {
      // I - bin, A - ASCII
      sendCommand(FtpConst.Commands.CMD_TYPE + ' ' + type);
      String response = readResponse();
      assertTrue(response.startsWith("200 "));
   }

   protected String pasv() throws IOException
   {
      sendCommand(FtpConst.Commands.CMD_PASV);
      String response = readResponse();
      assertTrue(response.startsWith("227 "));
      return response;
   }

   protected byte[] retr(String filename) throws IOException
   {
      String response = pasv();

      int begin = response.indexOf('(');
      int end = response.indexOf(')', begin + 1);
      String address = response.substring(begin + 1, end);
      StringTokenizer tokens = new StringTokenizer(address, ",");
      String _host =
         tokens.nextToken() + "." + tokens.nextToken() + "." + tokens.nextToken() + "." + tokens.nextToken();
      int _port = Integer.parseInt(tokens.nextToken()) * 256 + Integer.parseInt(tokens.nextToken());

      sendCommand(FtpConst.Commands.CMD_RETR + ' ' + filename);

      Socket dataConn = new Socket(_host, _port);

      response = readResponse();
      assertTrue(response.startsWith("125 "));

      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buff = new byte[1024];
      InputStream input = null;;
      try
      {
         input = dataConn.getInputStream();
         int bytes = -1;
         while ((bytes = input.read(buff)) != -1)
            output.write(buff, 0, bytes);
      }
      finally
      {
         if (input != null)
         {
            input.close();
         }
      }

      response = readResponse();
      dataConn.close();

      assertTrue(response.startsWith("226 "));

      return output.toByteArray();
   }

   protected void sendCommand(String command) throws IOException
   {
      try
      {
         outputCommand.write(command + "\r\n");
         outputCommand.flush();
         System.out.println("> " + command);
      }
      catch (IOException e)
      {
         disconnect();
         throw e;
      }
   }

   protected String readResponse() throws IOException
   {

      StringBuffer buff = new StringBuffer();

      String line = inputCommand.readLine();
      assertTrue(line.length() >= 3);
      buff.append(line);
      if (line.length() > 3 && line.charAt(3) == '-')
      {
         do
         {
            buff.append('\n');
            buff.append(line);
         }
         while (!isLastLine(line = inputCommand.readLine()));
      }

      String response = buff.toString();
      System.out.println("< " + response);
      return response;
   }

   private static boolean isLastLine(String line)
   {
      return Character.isDigit(line.charAt(0)) && Character.isDigit(line.charAt(1))
         && Character.isDigit(line.charAt(2)) && line.charAt(3) == ' ';
   }

}
