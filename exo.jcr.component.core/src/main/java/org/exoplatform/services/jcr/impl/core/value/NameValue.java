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

package org.exoplatform.services.jcr.impl.core.value;

import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;

import java.io.IOException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * a <code>NAME</code> value impl (a string that is namespace-qualified).
 * 
 * @author Gennady Azarenkov
 */
public class NameValue extends BaseValue
{

   public static final int TYPE = PropertyType.NAME;

   private final LocationFactory locationFactory;

   /**
    * NameValue constructor.
    */
   public NameValue(InternalQName name, LocationFactory locationFactory) throws IOException
   {
      super(TYPE, new TransientValueData(name));
      this.locationFactory = locationFactory;
   }

   /**
    * NameValue constructor.
    */
   public NameValue(ValueData data, LocationFactory locationFactory) throws IOException
   {
      super(TYPE, data);
      this.locationFactory = locationFactory;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      return locationFactory.createJCRName(getQName()).getAsString();
   }

   public InternalQName getQName() throws RepositoryException
   {
      return ValueDataUtil.getName(getInternalData());
   }
}
