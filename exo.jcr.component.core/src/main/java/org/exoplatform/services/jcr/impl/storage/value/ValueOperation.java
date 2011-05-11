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
package org.exoplatform.services.jcr.impl.storage.value;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: ValueOperation.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public interface ValueOperation
{

   /**
    * Execute operation.
    * 
    * @throws IOException
    *           if error occurs
    */
   void execute() throws IOException;

   /**
    * Rollback Value content.
    * 
    * @throws IOException
    *           if error occurs
    */
   void rollback() throws IOException;

   /**
    * Commit Value content.
    * 
    * @throws IOException
    *           if error occurs
    */
   void commit() throws IOException;

}
