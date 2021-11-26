/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.core.nodetype.registration;

import org.exoplatform.services.jcr.core.nodetype.ItemDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.Constants;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public abstract class AbstractDefinitionComparator<T extends ItemDefinitionData>
{
   public abstract PlainChangesLog compare(NodeTypeData registeredNodeType, T[] ancestorDefinition,
      T[] recipientDefinition) throws ConstraintViolationException, RepositoryException;

   /**
    * Compare definitions
    * 
    * @param ancestorDefinition
    * @param recipientDefinition
    * @param sameDefinitionData
    * @param changedDefinitionData
    * @param newDefinitionData
    * @param removedDefinitionData
    */
   protected void init(T[] ancestorDefinition, T[] recipientDefinition, List<T> sameDefinitionData,
      List<RelatedDefinition<T>> changedDefinitionData, List<T> newDefinitionData, List<T> removedDefinitionData)
   {
      for (int i = 0; i < recipientDefinition.length; i++)
      {
         boolean isNew = true;
         for (int j = 0; j < ancestorDefinition.length; j++)
         {
            if (recipientDefinition[i].getName().equals(ancestorDefinition[j].getName()))
            {
               isNew = false;
               if (recipientDefinition[i].equals(ancestorDefinition[j]))
                  sameDefinitionData.add(recipientDefinition[i]);
               else
               {
                  RelatedDefinition<T> relatedDefinition =
                     new RelatedDefinition<T>(ancestorDefinition[j], recipientDefinition[i]);
                  changedDefinitionData.add(relatedDefinition);
               }
            }
         }
         if (isNew)
            newDefinitionData.add(recipientDefinition[i]);
      }
      for (int i = 0; i < ancestorDefinition.length; i++)
      {
         boolean isRemoved = true;
         for (int j = 0; j < recipientDefinition.length && isRemoved; j++)
         {
            if (recipientDefinition[j].getName().equals(ancestorDefinition[i].getName()))
            {
               isRemoved = false;
               break;
            }
         }
         if (isRemoved)
            removedDefinitionData.add(ancestorDefinition[i]);
      }
   }

   /**
    * Return true if recipientDefinition contains Constants.JCR_ANY_NAME and
    * doesn't contain definition with name itemName.
    * 
    * @param itemName
    * @param recipientDefinition
    * @return
    */
   protected boolean isResidualMatch(InternalQName itemName, T[] recipientDefinition)
   {
      boolean containsResidual = false;
      for (int i = 0; i < recipientDefinition.length; i++)
      {
         if (itemName.equals(recipientDefinition[i].getName()))
            return false;
         else if (Constants.JCR_ANY_NAME.equals(recipientDefinition[i].getName()))
            containsResidual = true;
      }
      return containsResidual;
   }

   protected boolean isNonResidualMatch(InternalQName itemName, T[] recipientDefinition)
   {
      boolean isMatch = false;
      for (int i = 0; i < recipientDefinition.length; i++)
      {
         if (itemName.equals(recipientDefinition[i].getName()))
            return true;
      }
      return isMatch;
   }
}
