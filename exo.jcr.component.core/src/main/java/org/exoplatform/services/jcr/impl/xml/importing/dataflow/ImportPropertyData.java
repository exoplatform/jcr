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
package org.exoplatform.services.jcr.impl.xml.importing.dataflow;

import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: ImportPropertyData.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class ImportPropertyData extends TransientPropertyData implements ImportItemData
{
   /**
    * Logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.core.ImportPropertyData");

   /**
    * Default constructor.
    */
   public ImportPropertyData()
   {

   }

   /**
    * Full constructor.
    * 
    * @param path
    *          - property path.
    * @param identifier
    *          - identifier
    * @param version
    *          - version
    * @param type
    *          - property type
    * @param parentIdentifier
    *          - parent node identifier
    * @param multiValued
    *          - is property multivalued
    */
   public ImportPropertyData(QPath path, String identifier, int version, int type, String parentIdentifier,
      boolean multiValued)
   {
      super(path, identifier, version, type, parentIdentifier, multiValued);
   }

   /**
    * Set parent identifier.
    * 
    * @param identifer
    *          - identifier
    */
   public void setParentIdentifer(String identifer)
   {
      this.parentIdentifier = identifer;
   }
   
   /**
    * Set multiValued;
    * 
    * @param multiValue
    *          - multi value
    */
   public void setMultivalue(boolean multiValue)
   {
      this.multiValued = multiValue;
   }

   /**

   /**
    * Set path of item.
    * 
    * @param path
    *          - property path.
    */
   public void setQPath(QPath path)
   {
      this.qpath = path;
   }

   /**
    * Set property values.
    *
    * @param values
    *          values property
    */
   public void setValues(List<ValueData> values)
   {
      this.values = values;
   }

   /**
    * Set type of property
    *
    * @param type
    *          property type
    */
   public void setType(int type)
   {
      this.type = type;
   }

   /**
    * Set property value.
    * 
    * @param value
    *          property value
    */
   public void setValue(ValueData value)
   {
      this.values = new ArrayList<ValueData>();
      values.add(value);
   }

}
