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
package org.exoplatform.services.jcr.impl.storage.jdbc.backup;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectZipReaderImpl;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: DumpTable.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class RestoreTable
{
   /**
    * List of temporary files.
    */
   private final List<File> spoolFileList = new ArrayList<File>();

   /**
    *  The file cleaner.
    */
   private final FileCleaner fileCleaner;

   /**
    * Temporary directory.
    */
   private final File tempDir;

   /**
    * Maximum buffer size.
    */
   private final int maxBufferSize;

   private File contentFile;

   private File contentLenFile;

   private Integer deleteColumnIndex = null;

   private Integer skipColumnIndex = null;

   private Integer newColumnIndex = null;

   private Set<Integer> convertColumnIndex = new HashSet<Integer>();

   private String srcContainerName;

   private String dstContainerName;

   private Boolean srcMultiDb;

   private Boolean dstMultiDb;

   /**
    * Constructor RestoreTable.
    */
   public RestoreTable(FileCleaner fileCleaner, File tempDir, int maxBufferSize)
   {
      this.fileCleaner = fileCleaner;
      this.tempDir = tempDir;
      this.maxBufferSize = maxBufferSize;
   }

   /**
    * Restore table.
    */
   public void restore(Connection jdbcConn, String tableName, File storageDir) throws IOException, SQLException
   {
      // Need privileges
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(Backupable.BACKUP_RESTORE_PERMISSION);
      }

      ObjectZipReaderImpl contentReader = null;
      ObjectZipReaderImpl contentLenReader = null;

      PreparedStatement insertNode = null;
      ResultSet tableMetaData = null;

      int dialect = DialectDetecter.detect(jdbcConn.getMetaData()).hashCode();

      try
      {
         contentReader = new ObjectZipReaderImpl(PrivilegedFileHelper.zipInputStream(contentFile));
         contentReader.getNextEntry();

         contentLenReader = new ObjectZipReaderImpl(PrivilegedFileHelper.zipInputStream(contentLenFile));
         contentLenReader.getNextEntry();

         // get information about source table
         int sourceColumnCount = contentReader.readInt();

         List<Integer> columnType = new ArrayList<Integer>();
         List<String> columnName = new ArrayList<String>();

         for (int i = 0; i < sourceColumnCount; i++)
         {
            columnType.add(contentReader.readInt());
            columnName.add(contentReader.readString());
         }

         // collect information about target table 
         List<Integer> newColumnType = new ArrayList<Integer>();
         List<String> newColumnName = new ArrayList<String>();

         tableMetaData = jdbcConn.getMetaData().getColumns(null, null, tableName, "%");
         while (tableMetaData.next())
         {
            newColumnName.add(tableMetaData.getString("COLUMN_NAME"));
            newColumnType.add(tableMetaData.getInt("DATA_TYPE"));
         }

         int targetColumnCount = sourceColumnCount;
         if (deleteColumnIndex != null)
         {
            targetColumnCount--;
         }
         else if (newColumnIndex != null)
         {
            targetColumnCount++;
            columnType.add(newColumnIndex, newColumnType.get(newColumnIndex));
         }

         // construct statement
         String names = "";
         String parameters = "";
         for (int i = 0; i < targetColumnCount; i++)
         {
            if (skipColumnIndex != null && skipColumnIndex == i)
            {
               continue;
            }
            names += newColumnName.get(i) + (i == targetColumnCount - 1 ? "" : ",");
            parameters += "?" + (i == targetColumnCount - 1 ? "" : ",");
         }
         insertNode =
            jdbcConn.prepareStatement("INSERT INTO " + tableName + " (" + names + ") VALUES(" + parameters + ")");

         // set data
         outer : while (true)
         {
            for (int i = 0, targetIndex = 0; i < columnType.size(); i++, targetIndex++)
            {
               InputStream stream;
               long len;

               if (newColumnIndex != null && newColumnIndex == i)
               {
                  stream = new ByteArrayInputStream(dstContainerName.getBytes(Constants.DEFAULT_ENCODING));
                  len = ((ByteArrayInputStream)stream).available();
               }
               else
               {
                  try
                  {
                     len = contentLenReader.readLong();
                  }
                  catch (EOFException e)
                  {
                     if (i == 0)
                     {
                        // content length file is empty check content file
                        try
                        {
                           contentReader.readByte();
                        }
                        catch (EOFException e1)
                        {
                           break outer;
                        }
                     }

                     throw new IOException("Content length file is empty but content still present", e);
                  }
                  stream = len == -1 ? null : spoolInputStream(contentReader, len);
               }

               if (skipColumnIndex != null && skipColumnIndex == i)
               {
                  targetIndex--;
                  continue;
               }
               else if (deleteColumnIndex != null && deleteColumnIndex == i)
               {
                  targetIndex--;
                  continue;
               }

               // set 
               if (stream != null)
               {
                  if (convertColumnIndex != null && convertColumnIndex.contains(i))
                  {
                     // convert column value
                     ByteArrayInputStream ba = (ByteArrayInputStream)stream;
                     byte[] readBuffer = new byte[ba.available()];
                     ba.read(readBuffer);

                     String currentValue = new String(readBuffer, Constants.DEFAULT_ENCODING);
                     if (currentValue.equals(Constants.ROOT_PARENT_UUID))
                     {
                        stream = new ByteArrayInputStream(Constants.ROOT_PARENT_UUID.getBytes());
                     }
                     else
                     {
                        if (dstMultiDb)
                        {
                           if (!srcMultiDb)
                           {
                              stream =
                                 new ByteArrayInputStream(new String(readBuffer, Constants.DEFAULT_ENCODING).substring(
                                    srcContainerName.length()).getBytes());
                           }
                        }
                        else
                        {
                           if (srcMultiDb)
                           {
                              StringBuilder builder = new StringBuilder();
                              builder.append(dstContainerName);
                              builder.append(currentValue);

                              stream = new ByteArrayInputStream(builder.toString().getBytes());
                           }
                           else
                           {
                              StringBuilder builder = new StringBuilder();
                              builder.append(dstContainerName);
                              builder.append(new String(readBuffer, Constants.DEFAULT_ENCODING)
                                 .substring(srcContainerName.length()));

                              stream = new ByteArrayInputStream(builder.toString().getBytes());
                           }
                        }
                     }

                     len = ((ByteArrayInputStream)stream).available();
                  }

                  if (columnType.get(i) == Types.INTEGER || columnType.get(i) == Types.BIGINT
                     || columnType.get(i) == Types.SMALLINT || columnType.get(i) == Types.TINYINT)
                  {
                     ByteArrayInputStream ba = (ByteArrayInputStream)stream;
                     byte[] readBuffer = new byte[ba.available()];
                     ba.read(readBuffer);

                     String value = new String(readBuffer, Constants.DEFAULT_ENCODING);
                     insertNode.setLong(targetIndex + 1, Integer.parseInt(value));
                  }
                  else if (columnType.get(i) == Types.BIT)
                  {
                     ByteArrayInputStream ba = (ByteArrayInputStream)stream;
                     byte[] readBuffer = new byte[ba.available()];
                     ba.read(readBuffer);

                     String value = new String(readBuffer);
                     if (dialect == DumpTable.DB_DIALECT_PGSQL)
                     {
                        insertNode.setBoolean(targetIndex + 1, value.equals("t"));
                     }
                     else
                     {
                        insertNode.setBoolean(targetIndex + 1, value.equals("1"));
                     }
                  }
                  else if (columnType.get(i) == Types.BOOLEAN)
                  {
                     ByteArrayInputStream ba = (ByteArrayInputStream)stream;
                     byte[] readBuffer = new byte[ba.available()];
                     ba.read(readBuffer);

                     String value = new String(readBuffer);
                     insertNode.setBoolean(targetIndex + 1, value.equals("true"));
                  }
                  else
                  {
                     if (dialect == DumpTable.DB_DIALECT_HSQLDB)
                     {
                        if (columnType.get(i) == Types.VARBINARY)
                        {
                           insertNode.setBinaryStream(targetIndex + 1, stream, (int)len);
                        }
                        else
                        {
                           byte[] readBuffer = new byte[(int)len];
                           stream.read(readBuffer);

                           insertNode.setString(targetIndex + 1, new String(readBuffer, Constants.DEFAULT_ENCODING));
                        }
                     }
                     else
                     {
                        insertNode.setBinaryStream(targetIndex + 1, stream, (int)len);
                     }
                  }
               }
               else
               {
                  insertNode.setNull(targetIndex + 1, columnType.get(i));
               }
            }
            insertNode.addBatch();
         }

         insertNode.executeBatch();
         jdbcConn.commit();
      }
      finally
      {
         if (contentReader != null)
         {
            contentReader.close();
         }

         if (contentLenReader != null)
         {
            contentLenReader.close();
         }

         if (insertNode != null)
         {
            insertNode.close();
         }

         // delete all temporary files
         for (File file : spoolFileList)
         {
            if (!PrivilegedFileHelper.delete(file))
            {
               fileCleaner.addFile(file);
            }
         }

         if (tableMetaData != null)
         {
            tableMetaData.close();
         }
      }
   }

   /**
    * Spool input stream.
    */
   private InputStream spoolInputStream(ObjectReader in, long contentLen) throws IOException
   {
      byte[] buffer = new byte[0];
      byte[] tmpBuff;
      long readLen = 0;
      File sf = null;
      OutputStream sfout = null;

      try
      {
         while (true)
         {
            int needToRead = contentLen - readLen > 2048 ? 2048 : (int)(contentLen - readLen);
            tmpBuff = new byte[needToRead];

            if (needToRead == 0)
            {
               break;
            }

            in.readFully(tmpBuff);

            if (sfout != null)
            {
               sfout.write(tmpBuff);
            }
            else if (readLen + needToRead > maxBufferSize && fileCleaner != null)
            {
               sf = PrivilegedFileHelper.createTempFile("jcrvd", null, tempDir);
               sfout = PrivilegedFileHelper.fileOutputStream(sf);

               sfout.write(buffer);
               sfout.write(tmpBuff);
               buffer = null;
            }
            else
            {
               // reallocate new buffer and spool old buffer contents
               byte[] newBuffer = new byte[(int)(readLen + needToRead)];
               System.arraycopy(buffer, 0, newBuffer, 0, (int)readLen);
               System.arraycopy(tmpBuff, 0, newBuffer, (int)readLen, needToRead);
               buffer = newBuffer;
            }

            readLen += needToRead;
         }

         if (buffer != null)
         {
            return new ByteArrayInputStream(buffer);
         }
         else
         {
            return PrivilegedFileHelper.fileInputStream(sf);
         }
      }
      finally
      {
         if (sfout != null)
         {
            sfout.close();
         }

         if (sf != null)
         {
            spoolFileList.add(sf);
         }
      }
   }

   public void setContentFile(File contentFile)
   {
      this.contentFile = contentFile;
   }

   public void setContentLenFile(File contentLenFile)
   {
      this.contentLenFile = contentLenFile;
   }

   public void setDeleteColumnIndex(Integer deleteColumnIndex)
   {
      this.deleteColumnIndex = deleteColumnIndex;
   }

   public void setSkipColumnIndex(Integer skipColumnIndex)
   {
      this.skipColumnIndex = skipColumnIndex;
   }

   public void setNewColumnIndex(Integer newColumnIndex)
   {
      this.newColumnIndex = newColumnIndex;
   }

   public void setConvertColumnIndex(Set<Integer> convertColumnIndex)
   {
      this.convertColumnIndex = convertColumnIndex;
   }

   public void setSrcContainerName(String srcContainerName)
   {
      this.srcContainerName = srcContainerName;
   }

   public void setDstContainerName(String dstContainerName)
   {
      this.dstContainerName = dstContainerName;
   }

   public void setSrcMultiDb(Boolean srcMultiDb)
   {
      this.srcMultiDb = srcMultiDb;
   }

   public void setDstMultiDb(Boolean dstMultiDb)
   {
      this.dstMultiDb = dstMultiDb;
   }
}
