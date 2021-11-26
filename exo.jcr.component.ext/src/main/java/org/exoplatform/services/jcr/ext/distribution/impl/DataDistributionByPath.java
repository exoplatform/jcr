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

package org.exoplatform.services.jcr.ext.distribution.impl;

import java.util.Arrays;
import java.util.List;

/**
 * This distribution doesn't do anything specific, it only stores the content as
 * requested, which means that the data id expected is simply a relative path. It will
 * mainly help to create a tree of nodes in a reliable way.
 * 
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class DataDistributionByPath extends AbstractDataDistributionType
{

   /**
    * {@inheritDoc}
    */
   @Override
   protected List<String> getAncestors(String relativePath)
   {
      if (relativePath.startsWith("/"))
         relativePath = relativePath.substring(1);
      return Arrays.asList(relativePath.split("/"));
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean useParametersOnLeafOnly()
   {
      return false;
   }
}
