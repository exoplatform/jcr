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
package org.exoplatform.services.jcr.dataflow;

/**
 * Created by The eXo Platform SAS.<br> Changes log containined other changes logs inside
 * 
 * @author Gennady Azarenkov
 * @version $Id: CompositeChangesLog.java 11907 2008-03-13 15:36:21Z ksm $
 */

public interface CompositeChangesLog extends ItemStateChangesLog
{

   /**
    * creates new ChangesLogIterator
    * 
    * @return
    */
   ChangesLogIterator getLogIterator();

   /**
    * adds new PlainChangesLog
    * 
    * @param log
    */
   void addLog(PlainChangesLog log);

   /**
    * Removes a given PlainChangesLog
    * 
    * @param log
    */
   void removeLog(PlainChangesLog log);

   /**
    * @return systemId
    */
   String getSystemId();
}
