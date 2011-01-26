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
package org.exoplatform.services.jcr.impl.backup.rdbms;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: RestoreTableRule.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class RestoreTableRule
{
   private File contentFile;

   private File contentLenFile;

   private Integer deleteColumnIndex = null;

   private Integer skipColumnIndex = null;

   private Integer newColumnIndex = null;

   private String newColumnName = null;

   private Integer newColumnType = null;

   private Set<Integer> convertColumnIndex = new HashSet<Integer>();

   private String srcContainerName = null;

   private String dstContainerName = null;

   private Boolean srcMultiDb = null;

   private Boolean dstMultiDb = null;

   public File getContentFile()
   {
      return contentFile;
   }

   public void setContentFile(File contentFile)
   {
      this.contentFile = contentFile;
   }

   public File getContentLenFile()
   {
      return contentLenFile;
   }

   public void setContentLenFile(File contentLenFile)
   {
      this.contentLenFile = contentLenFile;
   }

   public Integer getDeleteColumnIndex()
   {
      return deleteColumnIndex;
   }

   public void setDeleteColumnIndex(Integer deleteColumnIndex)
   {
      this.deleteColumnIndex = deleteColumnIndex;
   }

   public Integer getSkipColumnIndex()
   {
      return skipColumnIndex;
   }

   public void setSkipColumnIndex(Integer skipColumnIndex)
   {
      this.skipColumnIndex = skipColumnIndex;
   }

   public Integer getNewColumnIndex()
   {
      return newColumnIndex;
   }

   public void setNewColumnIndex(Integer newColumnIndex)
   {
      this.newColumnIndex = newColumnIndex;
   }

   public Set<Integer> getConvertColumnIndex()
   {
      return convertColumnIndex;
   }

   public void setConvertColumnIndex(Set<Integer> convertColumnIndex)
   {
      this.convertColumnIndex = convertColumnIndex;
   }

   public String getSrcContainerName()
   {
      return srcContainerName;
   }

   public void setSrcContainerName(String srcContainerName)
   {
      this.srcContainerName = srcContainerName;
   }

   public String getDstContainerName()
   {
      return dstContainerName;
   }

   public void setDstContainerName(String dstContainerName)
   {
      this.dstContainerName = dstContainerName;
   }

   public Boolean getSrcMultiDb()
   {
      return srcMultiDb;
   }

   public void setSrcMultiDb(Boolean srcMultiDb)
   {
      this.srcMultiDb = srcMultiDb;
   }

   public Boolean getDstMultiDb()
   {
      return dstMultiDb;
   }

   public void setDstMultiDb(Boolean dstMultiDb)
   {
      this.dstMultiDb = dstMultiDb;
   }

   public String getNewColumnName()
   {
      return newColumnName;
   }

   public void setNewColumnName(String newColumnName)
   {
      this.newColumnName = newColumnName;
   }

   public Integer getNewColumnType()
   {
      return newColumnType;
   }

   public void setNewColumnType(Integer newColumnType)
   {
      this.newColumnType = newColumnType;
   }

}
