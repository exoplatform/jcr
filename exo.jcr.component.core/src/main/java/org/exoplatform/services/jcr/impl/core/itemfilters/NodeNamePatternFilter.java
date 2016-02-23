/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.itemfilters;

import org.exoplatform.services.jcr.impl.core.SessionImpl;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: NodeNamePatternFilter.java 111 12.05.2011 serg $
 */
public class NodeNamePatternFilter extends AbstractNamePatternFilter
{

   public NodeNamePatternFilter(String namePattern, SessionImpl session) throws RepositoryException
   {
      super(namePattern, session);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int getDefaultIndex()
   {
      return -1;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean isExactName(String token)
   {
      return (token.indexOf('*') == -1 && token.indexOf('[') != -1);
   }
}
