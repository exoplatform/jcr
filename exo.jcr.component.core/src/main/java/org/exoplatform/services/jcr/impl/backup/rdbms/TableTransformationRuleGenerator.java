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

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectReaderImpl;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.IOException;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

/**
 * Keeps information required by table transformation process during
 * backup/respore. Provides methods to aquire
 * transformation rules for ITEM, REFERENCE and VALUE tables.
 * 
 * @author <a href="mailto:dmitry.kuleshov@exoplatform.com">Dmitry Kuleshov</a>
 * @version $Id: tableTransformationRule.java 34360 2009-07-22 23:58:59Z dkuleshov $
 */
public class TableTransformationRuleGenerator
{
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.TableTransformationRuleGenerator");

   private final DatabaseStructureType srcDbType;

   private final DatabaseStructureType dstDbType;

   private final String srcContainerName;

   private final String dstContainerName;

   private final String srcItemTableName;

   private final String srcValueTableName;

   private final String srcRefTableName;

   public TableTransformationRuleGenerator(JDBCDataContainerConfig containerConfig, File storageDir) throws IOException

   {
      ObjectReader backupInfoReader = null;
      try
      {
         this.dstDbType = containerConfig.dbStructureType;
         this.dstContainerName = containerConfig.containerName;

         backupInfoReader =
            new ObjectReaderImpl(PrivilegedFileHelper.fileInputStream(new File(storageDir,
               "JDBCWorkspaceDataContainer.info")));;

         this.srcContainerName = backupInfoReader.readString();
         this.srcDbType = DatabaseStructureType.valueOf(backupInfoReader.readString());
         this.srcItemTableName = backupInfoReader.readString();
         this.srcValueTableName = backupInfoReader.readString();
         this.srcRefTableName = backupInfoReader.readString();
      }
      finally
      {
         if (backupInfoReader != null)
         {
            try
            {
               backupInfoReader.close();
            }
            catch (IOException e)
            {
               LOG.error("Can't close object reader", e);
            }
         }
      }
   }

   private TableTransformationRule getBasicTransformationRule()
   {
      TableTransformationRule basicTableTransformationRule = new TableTransformationRule();
      basicTableTransformationRule.setSrcContainerName(srcContainerName);
      basicTableTransformationRule.setSrcMultiDb(srcDbType.isMultiDatabase());
      basicTableTransformationRule.setDstContainerName(dstContainerName);
      basicTableTransformationRule.setDstMultiDb(dstDbType.isMultiDatabase());

      return basicTableTransformationRule;
   }

   public TableTransformationRule getValueTableTransformationRule()
   {
      TableTransformationRule valueTableTransformationRule = getBasicTransformationRule();
      valueTableTransformationRule.setSrcTableName(srcValueTableName);

      // auto increment ID column
      valueTableTransformationRule.setSkipColumnIndex(0);

      if (srcDbType == DatabaseStructureType.SINGLE || dstDbType == DatabaseStructureType.SINGLE)
      {
         // PROPERTY_ID column index
         Set<Integer> convertColumnIndex = new HashSet<Integer>();
         convertColumnIndex.add(3);
         valueTableTransformationRule.setConvertColumnIndex(convertColumnIndex);
      }

      return valueTableTransformationRule;
   }

   public TableTransformationRule getRefTableTransformationRule()
   {
      TableTransformationRule refTableTransformationRule = getBasicTransformationRule();
      refTableTransformationRule.setSrcTableName(srcRefTableName);

      if (srcDbType == DatabaseStructureType.SINGLE || dstDbType == DatabaseStructureType.SINGLE)
      {
         // NODE_ID and PROPERTY_ID column indexes
         Set<Integer> convertColumnIndex = new HashSet<Integer>();
         convertColumnIndex.add(0);
         convertColumnIndex.add(1);
         refTableTransformationRule.setConvertColumnIndex(convertColumnIndex);
      }

      return refTableTransformationRule;
   }

   public TableTransformationRule getItemTableTransformationRule()
   {
      TableTransformationRule itemTableTransformationRule = getBasicTransformationRule();
      itemTableTransformationRule.setSrcTableName(srcItemTableName);

      if (dstDbType == DatabaseStructureType.MULTI || dstDbType == DatabaseStructureType.ISOLATED)
      {
         if (srcDbType == DatabaseStructureType.SINGLE)
         {
            // CONTAINER_NAME column index
            itemTableTransformationRule.setSkipColumnIndex(4);

            // ID and PARENT_ID column indexes
            Set<Integer> convertColumnIndex = new HashSet<Integer>();
            convertColumnIndex.add(0);
            convertColumnIndex.add(1);
            itemTableTransformationRule.setConvertColumnIndex(convertColumnIndex);
         }
      }
      // single-db
      else
      {
         if (srcDbType == DatabaseStructureType.MULTI || srcDbType == DatabaseStructureType.ISOLATED)
         {
            // CONTAINER_NAME column index
            itemTableTransformationRule.setNewColumnIndex(4);
            itemTableTransformationRule.setNewColumnName("CONTAINER_NAME");
            itemTableTransformationRule.setNewColumnType(Types.VARCHAR);

            // ID and PARENT_ID column indexes
            Set<Integer> convertColumnIndex = new HashSet<Integer>();
            convertColumnIndex.add(0);
            convertColumnIndex.add(1);
            itemTableTransformationRule.setConvertColumnIndex(convertColumnIndex);
         }
         else
         {
            // ID and PARENT_ID and CONTAINER_NAME column indexes
            Set<Integer> convertColumnIndex = new HashSet<Integer>();
            convertColumnIndex.add(0);
            convertColumnIndex.add(1);
            convertColumnIndex.add(4);
            itemTableTransformationRule.setConvertColumnIndex(convertColumnIndex);
         }
      }

      return itemTableTransformationRule;
   }
}
