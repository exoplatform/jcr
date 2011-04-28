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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 21 01 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: ComplexDataRestore.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class ComplexDataRestore implements DataRestore
{

   /**
    * List of restorers.
    */
   private List<DataRestore> restorers = new ArrayList<DataRestore>();

   /**
    * Constructor ComplexDataRestor.
    * 
    * @param restorers
    */
   public ComplexDataRestore(List<DataRestore> restorers)
   {
      this.restorers.addAll(restorers);
   }

   /**
    * {@inheritDoc}
    */
   public void clean() throws BackupException
   {
      for (DataRestore restorer : restorers)
      {
         restorer.clean();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void restore() throws BackupException
   {
      for (DataRestore restorer : restorers)
      {
         restorer.restore();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void commit() throws BackupException
   {
      for (DataRestore restorer : restorers)
      {
         restorer.commit();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void rollback() throws BackupException
   {
      for (DataRestore restorer : restorers)
      {
         restorer.rollback();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void close() throws BackupException
   {
      try
      {
         for (DataRestore restorer : restorers)
         {
            restorer.close();
         }
      }
      finally
      {
         restorers.clear();
      }
   }
}
