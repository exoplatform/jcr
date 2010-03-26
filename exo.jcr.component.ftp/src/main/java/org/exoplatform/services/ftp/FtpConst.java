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

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly <gavrik-vetal@ukr.net/mail.ru>.
 * 
 * @version $Id: $
 */
public class FtpConst
{

   /**
    * FTP prefix.
    */
   public static final String FTP_PREFIX = "exo.jcr.component.ftp.";

   /**
    * FTP command catalog.
    */
   public static final String FTP_COMMAND_CATALOG = "FTP";

   /**
    * FTP cache-file extention.
    */
   public static final String FTP_CACHEFILEEXTENTION = ".ftpcache";

   /**
    * Time stamped block size.
    */
   public static final int FTP_TIMESTAMPED_BLOCK_SIZE = 2048;

   /**
    * eXoPlatform Logo.
    */
   public static final String[] EXO_LOGO =
      {"220- ", "              _/_/_/_/  *** eXo Platform JCR FTP Server        _/_/_/_/",
         "            _/                                                      _/",
         "           _/                          _/                          _/",
         "          _/            _/_/_/_/      _/      _/_/_/              _/",
         "                       _/        _/  _/     _/     _/              ",
         "                      _/          _/_/     _/     _/              ",
         "                     _/_/_/        _/     _/     _/              ",
         "                    _/            _/_/   _/     _/              ",
         "                   _/_/_/_/      _/  _/   _/_/_/               ",
         "   _/                           _/                          _/",
         "  _/                           _/                          _/",
         " _/                                                       _/",
         "_/_/_/_/               http://eXoPlatform.org ***  _/_/_/_/", "220 - "};

   /**
    * Help info.
    */
   public static final String[] EXO_HELP_INFO =
      {"214-The following commands are recognized:", "",
         "             _/_/_/_/  *** eXo Platform JCR FTP Server               _/_/_/_/",
         "            _/                                                            _/ ",
         "           _/                 _/                  CDUP    CWD     DELE   _/  ",
         "          _/   _/_/_/_/      _/      _/_/_/      HELP    LIST    MKD    _/   ",
         "              _/        _/  _/     _/     _/    MODE    NLST    NOOP         ",
         "             _/          _/_/     _/     _/    PASS    PASV    PORT          ",
         "            _/_/_/        _/     _/     _/    PWD     QUIT    REST           ",
         "           _/            _/_/   _/     _/    RETR    RMD     RNFR            ",
         "          _/_/_/_/      _/  _/   _/_/_/     RNTO    SIZE    STAT             ",
         "   _/                  _/                  STOR    SYST    TYPE   _/         ",
         "  _/                  _/                          USER           _/          ",
         " _/                                                             _/           ",
         "_/_/_/_/                     http://eXoPlatform.org ***  _/_/_/_/            ", "",
         "214 http://eXoPlatForm.org"};

   /**
    * Supported encodings.
    */
   public class Encoding
   {

      /**
       * Windows NT encoding.
       */
      public static final String WINDOWS_NT = "Windows_NT";

      /**
       * UNIX encoding.
       */
      public static final String UNIX = "UNIX";

      /**
       * UNIX L8 encoding.
       */
      public static final String UNIX_L8 = "UNIX Type: L8";
   }

   /**
    * Command name constants.
    */
   public class Commands
   {
      /**
       * USER.
       */
      public static final String CMD_USER = "USER";

      /**
       * PASS.
       */
      public static final String CMD_PASS = "PASS";

      /**
       * TYPE.
       */
      public static final String CMD_TYPE = "TYPE";

      /**
       * CWD.
       */
      public static final String CMD_CWD = "CWD";

      /**
       * PWD.
       */
      public static final String CMD_PWD = "PWD";

      /**
       * QUIT.
       */
      public static final String CMD_QUIT = "QUIT";

      /**
       * HELP.
       */
      public static final String CMD_HELP = "HELP";

      /**
       * SYST.
       */
      public static final String CMD_SYST = "SYST";

      /**
       * PASV.
       */
      public static final String CMD_PASV = "PASV";

      /**
       * NOOP.
       */
      public static final String CMD_NOOP = "NOOP";

      /**
       * LIST.
       */
      public static final String CMD_LIST = "LIST";

      /**
       * CDUP.
       */
      public static final String CMD_CDUP = "CDUP";

      /**
       * MKD.
       */
      public static final String CMD_MKD = "MKD";

      /**
       * MODE.
       */
      public static final String CMD_MODE = "MODE";

      /**
       * RMD.
       */
      public static final String CMD_RMD = "RMD";

      /**
       * RNFR.
       */
      public static final String CMD_RNFR = "RNFR";

      /**
       * RNTO.
       */
      public static final String CMD_RNTO = "RNTO";

      /**
       * STOR.
       */
      public static final String CMD_STOR = "STOR";

      /**
       * RETR.
       */
      public static final String CMD_RETR = "RETR";

      /**
       * DELE.
       */
      public static final String CMD_DELE = "DELE";

      /**
       * REST.
       */
      public static final String CMD_REST = "REST";

      /**
       * NLST.
       */
      public static final String CMD_NLST = "NLST";

      /**
       * PORT.
       */
      public static final String CMD_PORT = "PORT";

      /**
       * SIZE.
       */
      public static final String CMD_SIZE = "SIZE";

      /**
       * STAT.
       */
      public static final String CMD_STAT = "STAT";

      /**
       * STRU.
       */
      public static final String CMD_STRU = "STRU";
   }

   /**
    * Reply messages.
    */
   public class Replyes
   {

      /**
       * REPLY 125 - Data connection opened.
       */
      public static final String REPLY_125 = "125 Data connection already open; Transfer starting";

      /**
       * REPLY 200.
       */
      public static final String REPLY_200 = "200 %s";

      /**
       * REPLY 213.
       */
      public static final String REPLY_213 = "213 %s";

      /**
       * REPLY 215.
       */
      public static final String REPLY_215 = "215 %s";

      /**
       * REPLY 221 - exit.
       */
      public static final String REPLY_221 = "221 eXo JCR FTP Server. Goodbye :)";

      /**
       * REPLY 226 - transfer complete.
       */
      public static final String REPLY_226 = "226 Transfer complete";

      /**
       * REPLY 227 - Entering passive mode.
       */
      public static final String REPLY_227 = "227 Entering Passive Mode (%s)";

      /**
       * REPLY 230 - user logged in.
       */
      public static final String REPLY_230 = "230 %s user logged in";

      /**
       * REPLY 250 - command successful.
       */
      public static final String REPLY_250 = "250 %s command successful";

      /**
       * REPLY 257 - current directory.
       */
      public static final String REPLY_257 = "257 \"%s\" is current directory";

      /**
       * REPLY 257 CREATED - directory created.
       */
      public static final String REPLY_257_CREATED = "257 \"%s\" directory created";

      /**
       * REPLY 331 - password required.
       */
      public static final String REPLY_331 = "331 Password required for %s";

      /**
       * REPLY 350 - file or directory exist.
       */
      public static final String REPLY_350 = "350 File or directory exists, ready for destination name";

      /**
       * REPLY 350 REST - restarting at directory.
       */
      public static final String REPLY_350_REST = "350 Restarting at %s. Send STORE or RETRIEVE to initiate transfer";

      /**
       * REPLY 421 - idle timeout.
       */
      public static final String REPLY_421 = "421 Idle Timeout (%d seconds): closing control connection";

      /**
       * REPLY 421 DATA - service not available.
       */
      public static final String REPLY_421_DATA = "421 Service not available";

      /**
       * REPLY 425 - unable to build data connection.
       */
      public static final String REPLY_425 = "425 Unable to build data connection";

      /**
       * REPLY 450 - no such file or directory.
       */
      public static final String REPLY_450 = "450 %s No such file or directory";

      /**
       * REPLY 451 - transfer aborted.
       */
      public static final String REPLY_451 = "451 Transfer aborted";

      /**
       * REPLY 500 - not understood.
       */
      public static final String REPLY_500 = "500 %s not understood";

      /**
       * REPLY 500 PARAMREQUIRED - command requires parameter.
       */
      public static final String REPLY_500_PARAMREQUIRED = "500 %s: command requires a parameter";

      /**
       * REPLY 500 ILLEGAL - illegal command.
       */
      public static final String REPLY_500_ILLEGAL = "500 Illegal %s command";

      /**
       * REPLY 501 MODE - unrecognized transfer mode.
       */
      public static final String REPLY_501_MODE = "501 '%s' unrecognized transfer mode";

      /**
       * REPLY 501 STRU - unrecognized structure mode.
       */
      public static final String REPLY_501_STRU = "501 '%s' unrecognized structure type";

      /**
       * REPLY 503 - bad sequence of command.
       */
      public static final String REPLY_503 = "503 Bad sequence of commands";

      /**
       * REPLY 503 PASS - login with user first.
       */
      public static final String REPLY_503_PASS = "503 Login with USER first";

      /**
       * REPLY 504 - unsupported transfer mode.
       */
      public static final String REPLY_504 = "504 '%s' unsupported transfer mode";

      /**
       * REPLY 530.
       */
      public static final String REPLY_530 = "530 Please login with USER and PASS";

      /**
       * REPLY 550 - permission denied.
       */
      public static final String REPLY_550 = "550 %s: Permission denied";

      /**
       * REPLY 550 SIZE - no such file.
       */
      public static final String REPLY_550_SIZE = "550 %s: No such file";

      /**
       * REPLY 550 RESTORE - restore value invalid.
       */
      public static final String REPLY_550_RESTORE = "550 Restore value invalid";

      /**
       * REPLY 553 - unable to rename file or directory.
       */
      public static final String REPLY_553 = "553 %s: Unable to rename file or directory";
   }

   /**
    * Supported OS types.
    */
   public class SystemTypes
   {

      /**
       * Windows NT type.
       */
      public static final String WINDOWS_NT = "Windows_NT";

      /**
       * UNIX L8 type.
       */
      public static final String UNIX_L8 = "UNIX Type: L8";
   }

   /**
    * Node types.
    */
   public class NodeTypes
   {

      /**
       * nt:folder.
       */
      public static final String NT_FOLDER = "nt:folder";

      /**
       * nt:file.
       */
      public static final String NT_FILE = "nt:file";

      /**
       * jcr:content.
       */
      public static final String JCR_CONTENT = "jcr:content";

      /**
       * jcr:data.
       */
      public static final String JCR_DATA = "jcr:data";

      /**
       * jcr:created.
       */
      public static final String JCR_CREATED = "jcr:created";

      /**
       * jcr:lastModified.
       */
      public static final String JCR_LASTMODIFIED = "jcr:lastModified";

      /**
       * nt:resource.
       */
      public static final String NT_RESOURCE = "nt:resource";

      /**
       * jcr:mimetype.
       */
      public static final String JCR_MIMETYPE = "jcr:mimeType";

      /**
       * mix:versionable.
       */
      public static final String MIX_VERSIONABLE = "mix:versionable";
   }

}
