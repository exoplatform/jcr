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
package org.exoplatform.services.jcr.impl.tools.tree;

import java.util.HashSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemDataTraversingVisitor;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: NameTraversingVisitor.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class NameTraversingVisitor
   extends ItemDataTraversingVisitor
{
   private HashSet<QPath> validNames = new HashSet<QPath>();

   private HashSet<String> validUuids = new HashSet<String>();

   public final static int SCOPE_NODES = 1;

   public final static int SCOPE_PROPERTYES = 2;

   public final static int SCOPE_ALL = SCOPE_NODES | SCOPE_PROPERTYES;

   private final int scope;

   public NameTraversingVisitor(ItemDataConsumer dataManager, int scope)
   {
      super(dataManager);
      this.scope = scope;
   }

   @Override
   protected void entering(PropertyData property, int level) throws RepositoryException
   {
      if ((scope & SCOPE_PROPERTYES) != 0)
      {
         validNames.add(property.getQPath());
         validUuids.add(property.getIdentifier());
      }

   }

   private HashSet<QPath> getValidNames()
   {
      return validNames;
   }

   @Override
   protected void entering(NodeData node, int level) throws RepositoryException
   {
      if ((scope & SCOPE_NODES) != 0)
      {
         validNames.add(node.getQPath());
         validUuids.add(node.getIdentifier());
      }

   }

   @Override
   protected void leaving(PropertyData property, int level) throws RepositoryException
   {

   }

   @Override
   protected void leaving(NodeData node, int level) throws RepositoryException
   {
   }

   public static QPath[] getValidNames(Node rootNode, int scope) throws RepositoryException
   {
      NameTraversingVisitor visitor =
               new NameTraversingVisitor(((SessionImpl) rootNode.getSession()).getTransientNodesManager(), scope);
      (((NodeImpl) rootNode).getData()).accept(visitor);
      HashSet<QPath> valNames = visitor.getValidNames();
      return valNames.toArray(new QPath[valNames.size()]);
   }

   public static String[] getValidUuids(Node rootNode, int scope) throws RepositoryException
   {
      NameTraversingVisitor visitor =
               new NameTraversingVisitor(((SessionImpl) rootNode.getSession()).getTransientNodesManager(), scope);
      (((NodeImpl) rootNode).getData()).accept(visitor);
      HashSet<String> valUuids = visitor.getValidUuids();
      return valUuids.toArray(new String[valUuids.size()]);

   }

   private HashSet<String> getValidUuids()
   {
      return validUuids;
   }

}
