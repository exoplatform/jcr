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
package org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan;

import java.io.Serializable;

/**
 * Created by The eXo Platform SAS. <br/>
 * 
 * Base class for WorkspaceCache keys.<br/>
 * 
 * Date: 10.06.2008<br/>
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: CacheKey.java 2845 2010-07-30 13:29:37Z tolusha $
 */
public abstract class CacheKey implements Serializable, Comparable<CacheKey>
{

   protected final String id;

   protected final int hash;

   CacheKey(String id)
   {
      this.id = id;
      this.hash = id.hashCode();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode()
   {
      return this.hash;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString()
   {
      return this.id.toString();
   }

   /**
    * {@inheritDoc}
    */
   public int compareTo(CacheKey o)
   {
      return id.compareTo(o.id);
   }
}
