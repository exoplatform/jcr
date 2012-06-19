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

import org.exoplatform.services.jcr.datamodel.Identifier;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;

import java.io.IOException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * a <code>REFERENCE</code> value impl (a Identifier of an existing node).
 * 
 * @author Gennady Azarenkov
 */
public class ReferenceValue extends BaseValue
{

   public static final int TYPE = PropertyType.REFERENCE;

   private final Identifier identifier;

   /**
    * ReferenceValue constructor. 
    */
   public ReferenceValue(Identifier identifier) throws IOException
   {
      super(TYPE, new TransientValueData(identifier));
      this.identifier = identifier;
   }

   /**
    * ReferenceValue constructor. 
    */
   public ReferenceValue(ValueData data) throws IOException, RepositoryException
   {
      super(TYPE, data);
      this.identifier = new Identifier(getString());
   }

   /**
    * Returns {@link Identifier}.
    */
   public Identifier getIdentifier()
   {
      return identifier;
   }
}
