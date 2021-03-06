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
package org.exoplatform.services.jcr.impl.backup;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: Suspendable.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public interface Suspendable
{

   /**
    * Priority high constant.
    */
   public static final int PRIORITY_HIGH = 5;

   /**
    * Priority normal constant.
    */
   public static final int PRIORITY_NORMAL = 0;

   /**
    * Priority low constant.
    */
   public static final int PRIORITY_LOW = -5;

   /**
    * Suspend component.
    *  
    * @throws SuspendException if error occurred 
    */
   void suspend() throws SuspendException;

   /**
    * Resume component.
    *  
    * @throws ResumeException if error occurred 
    */
   void resume() throws ResumeException;

   /**
    * Indicates if component is suspended or not.
    * 
    * @return
    */
   boolean isSuspended();

   /**
    * Priority of the component.
    * Uses for suspend/resume comparator order.
    * The higher number gives higher priority.
    * 
    * @return
    */
   int getPriority();

}
