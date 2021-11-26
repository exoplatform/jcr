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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.core.nodetype.ItemDefinitionData;
import org.exoplatform.services.jcr.datamodel.ItemData;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Created to avoid huge operations on PropertyImpl instance initialization, 
 * as they are not needed for trail audit.
 * 
 * @author <a href="mailto:dmi3.kuleshov@gmail.com">Dmitry Kuleshov</a>
 * @version $Id: $
 */
public class AuditPropertyImpl extends PropertyImpl
{

   AuditPropertyImpl(ItemData data, SessionImpl session) throws RepositoryException, ConstraintViolationException
   {
      super(data, session);
   }

   /**
    * The most expensive method. In parent class it fulfills useless operations
    * in context of trail audit.
    *
    * {@inheritDoc}
    */
   @Override
   void loadData(ItemData data) throws RepositoryException
   {
      this.qpath = data.getQPath();
      return;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ItemDefinitionData getItemDefinitionData()
   {
      throw new UnsupportedOperationException("getItemDefinitionData method is not supported by this class");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public PropertyDefinition getDefinition()
   {
      throw new UnsupportedOperationException("getItemDefinitionData method is not supported by this class");
   }

}
