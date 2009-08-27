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
package org.exoplatform.frameworks.ftpclient;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Vitaly Guly
 * @version $Id: $
 */

public class FtpConst
{

   public static final String FTP_PREFIX = "exo.ftpclient.";

   public static final String EXC_MSG = "Unhandled exception. ";

   public class Commands
   {
      public static final String CMD_USER = "USER";

      public static final String CMD_PASS = "PASS";

      public static final String CMD_TYPE = "TYPE";

      public static final String CMD_CWD = "CWD";

      public static final String CMD_PWD = "PWD";

      public static final String CMD_QUIT = "QUIT";

      public static final String CMD_HELP = "HELP";

      public static final String CMD_SYST = "SYST";

      public static final String CMD_PASV = "PASV";

      public static final String CMD_NOOP = "NOOP";

      public static final String CMD_LIST = "LIST";

      public static final String CMD_CDUP = "CDUP";

      public static final String CMD_MKD = "MKD";

      public static final String CMD_MODE = "MODE";

      public static final String CMD_RMD = "RMD";

      public static final String CMD_RNFR = "RNFR";

      public static final String CMD_RNTO = "RNTO";

      public static final String CMD_STOR = "STOR";

      public static final String CMD_RETR = "RETR";

      public static final String CMD_DELE = "DELE";

      public static final String CMD_REST = "REST";

      public static final String CMD_NLST = "NLST";

      public static final String CMD_PORT = "PORT";

      public static final String CMD_SIZE = "SIZE";

      public static final String CMD_STRU = "STRU";

      public static final String CMD_STAT = "STAT";
   }

   public class Replyes
   {
      public static final int REPLY_125 = 125;

      public static final int REPLY_150 = 150;

      public static final int REPLY_200 = 200;

      public static final int REPLY_211 = 211;

      public static final int REPLY_213 = 213;

      public static final int REPLY_214 = 214;

      public static final int REPLY_215 = 215;

      public static final int REPLY_221 = 221;

      public static final int REPLY_226 = 226;

      public static final int REPLY_227 = 227;

      public static final int REPLY_230 = 230;

      public static final int REPLY_250 = 250;

      public static final int REPLY_257 = 257;

      public static final int REPLY_331 = 331;

      public static final int REPLY_350 = 350;

      public static final int REPLY_421 = 421;

      public static final int REPLY_425 = 425;

      public static final int REPLY_450 = 450;

      public static final int REPLY_500 = 500;

      public static final int REPLY_501 = 501;

      public static final int REPLY_503 = 503;

      public static final int REPLY_504 = 504;

      public static final int REPLY_530 = 530;

      public static final int REPLY_550 = 550;

      public static final int REPLY_553 = 553;
   }

   public class SysTypes
   {
      public static final String WINDOWS_NT = "Windows_NT";

      public static final String UNIX_L8 = "UNIX Type: L8";
   }

}
