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
package org.exoplatform.services.jcr.ext.replication.recovery;

import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.dataflow.serialization.UnknownClassIdException;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectReaderImpl;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ReaderSpoolFileHolder;
import org.exoplatform.services.jcr.impl.dataflow.serialization.TransactionChangesLogReader;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: RecoveryReader.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class RecoveryReader extends AbstractFSAccess
{
   /**
    * The apache logger.
    */
   private static Log log = ExoLogger.getLogger("ext.RecoveryReader");

   /**
    * The FileCleaner for delete the temporary files and correct TransientValueData deserialization.
    */
   private FileCleaner fileCleaner;

   /**
    * For deserialization.
    */
   private int maxBufferSize;

   /**
    * For deserialization.
    */
   private ReaderSpoolFileHolder holder;

   /**
    * Definition the folder to ChangesLog.
    */
   private File recoveryDir;

   /**
    * RecoveryReader constructor.
    * 
    * @param fileCleaner
    *          the FileCleaner
    * @param recoveryDir
    *          the recoveryDir
    * @param maxBufferSize
    *          the max buffer size
    * @param holder
    *          ReaderSpoolFileHolder, the reader spool file holder 
    */
   public RecoveryReader(FileCleaner fileCleaner, File recoveryDir, int maxBufferSize, ReaderSpoolFileHolder holder)
   {
      this.fileCleaner = fileCleaner;
      this.maxBufferSize = maxBufferSize;
      this.holder = holder;
      this.recoveryDir = recoveryDir;
   }

   /**
    * getChangesLog.
    * 
    * @param filePath
    *          full path to binary ChangesLog
    * @return TransactionChangesLog return the TransactionChangesLog
    * @throws IOException
    *           will be generated the IOException
    * @throws ClassNotFoundException
    *           will be generated the ClassNotFoundException
    */
   public TransactionChangesLog getChangesLog(String filePath) throws IOException, ClassNotFoundException
   {

      ObjectReaderImpl in = new ObjectReaderImpl(new FileInputStream(filePath));
      TransactionChangesLogReader rdr = new TransactionChangesLogReader(fileCleaner, maxBufferSize, holder);

      TransactionChangesLog tcl;
      try
      {
         tcl = rdr.read(in);
      }
      catch (UnknownClassIdException e)
      {
         throw new ClassNotFoundException(e.getMessage(), e);
      }
      in.close();
      return tcl;
   }

   /**
    * getFilePathList.
    * 
    * @param timeStamp
    *          up to date
    * @param ownName
    *          owner name
    * @return List list of binary changes log up to date
    * @throws IOException
    *           will be generated IOException if fail.
    */
   public List<String> getFilePathList(Calendar timeStamp, String ownName) throws IOException
   {
      File dataInfo = new File(recoveryDir.getAbsolutePath() + File.separator + ownName);

      List<String> list = new ArrayList<String>();

      if (dataInfo.exists())
      {
         BufferedReader br = new BufferedReader(new FileReader(dataInfo));

         String sPath;

         while ((sPath = br.readLine()) != null)
         {
            if (sPath.startsWith(PREFIX_REMOVED_DATA) == false)
            {
               File f = new File(sPath);
               Calendar time = getTimeStamp(f.getName());

               if (timeStamp.after(time))
               {
                  list.add(sPath);

                  if (log.isDebugEnabled())
                     log.debug(sPath);
               }
               else
                  break;
            }
         }
      }

      return list;
   }

   /**
    * getTimeStamp.
    * 
    * @param fileName
    *          name of file
    * @return Calendar TimeStamp from file name
    */
   private Calendar getTimeStamp(String fileName)
   {
      // 20080415_090302_824_50e4cf9d7f000001009bb457938f425b
      DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
      Calendar timeStamp = Calendar.getInstance();

      try
      {
         timeStamp.setTime(dateFormat.parse(fileName));
      }
      catch (ParseException e)
      {
         log.error("Can't parce date", e);
      }
      return timeStamp;
   }
}
