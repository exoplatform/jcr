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
package org.exoplatform.services.jcr.ext.backup.server;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date: 24.02.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: WorkspaceRestoreExeption.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class WorkspaceRestoreExeption extends Exception
{

   /**
    * WorkspaceRestoreExeption constructor.
    * 
    * @param message
    *          String, the exception message
    */
   public WorkspaceRestoreExeption(String message)
   {
      super(message);
   }

   /**
    * WorkspaceRestoreExeption constructor.
    * 
    * @param message
    *          String, the exception message
    * @param e
    *          the cause exception
    */
   public WorkspaceRestoreExeption(String message, Throwable e)
   {
      super(message, e);
   }

   /**
    * WorkspaceRestoreExeption constructor.
    * 
    * @param e
    *          the cause exception
    */
   public WorkspaceRestoreExeption(Throwable e)
   {
      super(e.getMessage(), e);
   }
}
