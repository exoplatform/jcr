/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.backup.rdbms;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: RestoreTableRule.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class TableTransformationRule
{
   private String srcTableName;

   private Integer skipColumnIndex = null;

   private Integer newColumnIndex = null;

   private String newColumnName = null;

   private Integer newColumnType = null;

   private Set<Integer> convertColumnIndex = new HashSet<Integer>();

   private String srcContainerName = null;

   private String dstContainerName = null;

   private Boolean srcMultiDb = null;

   private Boolean dstMultiDb = null;

   public String getSrcTableName()
   {
      return srcTableName;
   }

   public void setSrcTableName(String srcTableName)
   {
      this.srcTableName = srcTableName;
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
