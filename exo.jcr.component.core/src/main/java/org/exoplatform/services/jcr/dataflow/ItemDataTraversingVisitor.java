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

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: ItemDataTraversingVisitor.java 11907 2008-03-13 15:36:21Z ksm $
 */

public abstract class ItemDataTraversingVisitor implements ItemDataVisitor
{
   /**
    * Maximum level.
    */
   protected final int maxLevel;

   public static final int INFINITE_DEPTH = -1;

   /**
    * Current level.
    */
   protected int currentLevel = 0;

   protected boolean interrupted;

   protected final ItemDataConsumer dataManager;

   /**
    * @param dataManager
    *          - ItemDataConsumer.
    * @param maxLevel
    *          - maximum level.
    */
   public ItemDataTraversingVisitor(ItemDataConsumer dataManager, int maxLevel)
   {
      this.maxLevel = maxLevel;
      this.dataManager = dataManager;
   }

   /**
    * @param dataManager
    *          - ItemDataConsumer
    */
   public ItemDataTraversingVisitor(ItemDataConsumer dataManager)
   {
      this.maxLevel = INFINITE_DEPTH;
      this.dataManager = dataManager;
   }

   /**
    * {@inheritDoc}
    */
   public void visit(PropertyData property) throws RepositoryException
   {
      entering(property, currentLevel);
      leaving(property, currentLevel);
   }

   /**
    * {@inheritDoc}
    */
   public void visit(NodeData node) throws RepositoryException
   {
      try
      {
         entering(node, currentLevel);
         if (maxLevel == INFINITE_DEPTH || currentLevel < maxLevel)
         {
            currentLevel++;

            visitChildProperties(node);
            visitChildNodes(node);

            currentLevel--;
         }
         leaving(node, currentLevel);
      }
      catch (RepositoryException re)
      {
         currentLevel = 0;
         throw re;
      }
   }

   /**
    * Visit all child properties.
    */
   protected void visitChildProperties(NodeData node) throws RepositoryException
   {
      if (isInterrupted())
         return;
      for (PropertyData data : dataManager.getChildPropertiesData(node))
      {
         if (isInterrupted())
            return;
         data.accept(this);
      }
   }

   /**
    * Visit all child nodes.
    */
   protected void visitChildNodes(NodeData node) throws RepositoryException
   {
      if (isInterrupted())
         return;
      for (NodeData data : dataManager.getChildNodesData(node))
      {
         if (isInterrupted())
            return;
         data.accept(this);
      }
   }

   /**
    * {@inheritDoc}
    */
   public ItemDataConsumer getDataManager()
   {
      return dataManager;
   }

   /**
    * Indicates whether the visit process has been interrupted
    * @return <code>true</code> if the visit process is interrupted, <code>false</code> otherwise
    */
   protected boolean isInterrupted()
   {
      return interrupted;
   }

   /**
    * handler for PropertyData entering
    * 
    * @param property
    * @param level
    * @throws RepositoryException
    */
   protected abstract void entering(PropertyData property, int level) throws RepositoryException;

   /**
    * handler for NodeData entering
    * 
    * @param node
    * @param level
    * @throws RepositoryException
    */
   protected abstract void entering(NodeData node, int level) throws RepositoryException;

   /**
    * handler for PropertyData leaving
    * 
    * @param property
    * @param level
    * @throws RepositoryException
    */
   protected abstract void leaving(PropertyData property, int level) throws RepositoryException;

   /**
    * handler for NodeData entering
    * 
    * @param node
    * @param level
    * @throws RepositoryException
    */
   protected abstract void leaving(NodeData node, int level) throws RepositoryException;

}
