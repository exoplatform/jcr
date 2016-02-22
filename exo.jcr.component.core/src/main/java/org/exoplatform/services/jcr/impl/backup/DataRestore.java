/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.backup;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 22 01 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: DataRestore.java.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public interface DataRestore
{

   /**
    * Clean old data from the storage. 
    * 
    * @throws BackupException
    *          if any exception is occurred
    */
   void clean() throws BackupException;

   /**
    * Restore new data into storage.
    * 
    * @throws BackupException
    *          if any exception is occurred
    */
   void restore() throws BackupException;

   /**
    * Commit changes. 
    * 
    * @throws BackupException
    *          if any exception is occurred
    */
   void commit() throws BackupException;

   /**
    * Rollback changes. 
    * 
    * @throws BackupException
    *          if any exception is occurred
    */
   void rollback() throws BackupException;

   /**
    * Close DataRestor. 
    * 
    * @throws BackupException
    *          if any exception is occurred
    */
   void close() throws BackupException;
}
