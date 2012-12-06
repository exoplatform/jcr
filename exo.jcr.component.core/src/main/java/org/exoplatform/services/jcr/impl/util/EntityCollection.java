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
package org.exoplatform.services.jcr.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RangeIterator;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: EntityCollection.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class EntityCollection implements NodeIterator, PropertyIterator, NodeTypeIterator, EventIterator,
   EventListenerIterator, RowIterator, VersionIterator
{

   private Iterator iter;

   private List list;

   private long pos;

   public EntityCollection(Collection col)
   {
      this(new ArrayList(col));
   }

   public EntityCollection(List list)
   {
      if (list == null)
         this.list = new ArrayList();
      else
         this.list = list;

      this.iter = this.list.iterator();
      this.pos = 0;
   }

   public EntityCollection()
   {
      this.list = new ArrayList();
      this.iter = list.iterator();
      this.pos = 0;
   }

   /**
    * @see NodeIterator#nextNode()
    */
   public Node nextNode()
   {
      pos++;
      return (Node)iter.next();
   }

   /**
    * @see RangeIterator#skip(int)
    */
   public void skip(long skipNum)
   {
      pos += skipNum;
      while (skipNum-- > 0)
      {
         iter.next();
      }
   }

   /**
    * Returns the number of elements in the iterator. If this information is unavailable, returns -1.
    * 
    * @return a long
    */
   public long getSize()
   {
      return list.size();
   }

   /**
    * Returns the current position within the iterator. The number returned is the 0-based index of
    * the next element in the iterator, i.e. the one that will be returned on the subsequent
    * <code>next</code> call. <p/> Note that this method does not check if there is a next element,
    * i.e. an empty iterator will always return 0.
    * 
    * @return a long
    */
   public long getPosition()
   {
      return pos;
   }

   /**
    * @see Iterator#hasNext()
    */
   public boolean hasNext()
   {
      return iter.hasNext();
   }

   /**
    * @see Iterator#next()
    */
   public Object next()
   {
      pos++;
      return iter.next();
   }

   /**
    * @see Iterator#remove()
    */
   public void remove()
   {
      iter.remove();
   }

   /**
    * @see PropertyIterator#nextProperty()
    */
   public Property nextProperty()
   {
      pos++;
      return (Property)iter.next();
   }

   /**
    * Returns the next <code>String</code> in the iteration.
    * 
    * @return the next <code>String</code> in the iteration.
    * @throws java.util.NoSuchElementException
    *           if iteration has no more <code>String</code>s.
    */
   public String nextString()
   {
      pos++;
      return (String)iter.next();
   }

   /**
    *
    */
   public NodeType nextNodeType()
   {
      pos++;
      return (NodeType)iter.next();
   }

   /**
    *@see javax.jcr.query.RowIterator#nextRow()
    */

   public Row nextRow()
   {
      pos++;
      return (Row)iter.next();
   }

   /**
    *
    */
   public Event nextEvent()
   {
      pos++;
      return (Event)iter.next();
   }

   /**
    *
    */
   public EventListener nextEventListener()
   {
      pos++;
      return (EventListener)iter.next();
   }

   /**
   *
   */
   public Version nextVersion()
   {
      pos++;
      return (Version)iter.next();
   }

   public void add(Object obj)
   {
      pos = 0;
      list.add(obj);
      iter = list.iterator();
   }

   public void addAll(Collection col)
   {
      pos = 0;
      list.addAll(col);
      iter = list.iterator();
   }

   /**
    * For TESTs only.
    * 
    * @return List backed the iterator
    */
   public List getList()
   {
      return list;
   }
}
