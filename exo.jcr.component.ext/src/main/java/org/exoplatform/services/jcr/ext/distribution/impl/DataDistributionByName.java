/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.distribution.impl;

import org.exoplatform.services.jcr.impl.core.NodeImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * This data distribution will distribute the data in a understandable way for a human being.
 * The expected data id is for example a login of a user.
 * It will generate a hierarchy of sub-nodes with <code>n</code> levels of depth for example
 *  with <code>n = 4</code>:
 * <ul>
 *    <li>{@literal Usename: john.smith (size >= 4) it will generate a path of type "j___/jo___/joh___/john.smith"}</li>
 *    <li>{@literal Usename: bob (size < 4) it will generate a path of type "b___/bo___/bob"}</li>
 * </ul>
 * 
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class DataDistributionByName extends AbstractDataDistributionType
{

   /**
    * The level of depth used by the algorithm
    */
   private int depth = 4;

   /**
    * The suffix used by the algorithm to indicate that they are sub nodes inside
    */
   private String suffix = "___";

   /**
    * {@inheritDoc}
    */
   @Override
   protected List<String> getAncestors(String dataId)
   {
      List<String> result = new ArrayList<String>(depth);
      int length = dataId.length();
      for (int i = 0; i < depth - 1 && i < length - 1; i++)
      {
         StringBuilder buffer = new StringBuilder();
         buffer.append(dataId, 0, i + 1);
         buffer.append(suffix);
         result.add(buffer.toString());
      }
      result.add(dataId);
      return result;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean useParametersOnLeafOnly()
   {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void migrate(Node rootNode) throws RepositoryException
   {
      migrate(rootNode, null, null, null);
   }

   /**
    * {@inheritDoc}
    */
   public void migrate(Node rootNode, String nodeType, List<String> mixinTypes, Map<String, String[]> permissions)
      throws RepositoryException
   {
      NodeIterator iter = ((NodeImpl)rootNode).getNodesLazily();
      while (iter.hasNext())
      {
         Node userNode = iter.nextNode();

         if (!alreadyMigrated(userNode))
         {
            Node ancestorNode = rootNode;

            Iterator<String> ancestors = getAncestors(userNode.getName()).iterator();
            while (ancestors.hasNext())
            {
               String ancestorName = ancestors.next();

               if (ancestors.hasNext())
               {
                  try
                  {
                     ancestorNode = ancestorNode.getNode(ancestorName);
                     continue;
                  }
                  catch (PathNotFoundException e)
                  {
                     ancestorNode =
                        createNode(ancestorNode, ancestorName, nodeType, mixinTypes, permissions, false, false);
                  }
               }
               else
               {
                  rootNode.getSession().move(userNode.getPath(), ancestorNode.getPath() + "/" + ancestorName);
               }
            }

            rootNode.getSession().save();
         }
      }
   }

   private boolean alreadyMigrated(Node userNode) throws RepositoryException
   {
      String nodeName = userNode.getName();

      return nodeName.length() == 1 || nodeName.endsWith(suffix);
   }
}
