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
package org.exoplatform.services.jcr.impl.core.value;

import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;

import java.io.IOException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * a <code>PATH</code> value impl (an absolute or relative workspace path).
 * 
 * @author Gennady Azarenkov
 */
public class PathValue extends BaseValue
{

   public static final int TYPE = PropertyType.PATH;

   private final LocationFactory locationFactory;

   /**
    * PathValue constructor.
    */
   public PathValue(QPath path, LocationFactory locationFactory) throws IOException
   {
      super(TYPE, new TransientValueData(path));
      this.locationFactory = locationFactory;
   }

   /**
    * PathValue constructor.
    */
   public PathValue(ValueData data, LocationFactory locationFactory) throws IOException, RepositoryException
   {
      super(TYPE, data);
      this.locationFactory = locationFactory;
   }

   /**
    * {@inheritDoc}
    */
   public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      return locationFactory.createJCRPath(getQPath()).getAsString(false);
   }

   public QPath getQPath() throws RepositoryException
   {
      return ValueDataUtil.getPath(getInternalData());
   }
}
