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
package org.exoplatform.services.jcr.core.nodetype;

import javax.jcr.nodetype.ItemDefinition;

/**
 * Created by The eXo Platform SAS.<br/> ItemDefinition value object.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady
 *         Azarenkov</a>
 * @version $Id: ItemDefinitionValue.java 11907 2008-03-13 15:36:21Z ksm $
 */

public abstract class ItemDefinitionValue
{

   protected String name;

   protected boolean autoCreate;

   protected int onVersion;

   protected boolean readOnly;

   protected boolean mandatory;

   public ItemDefinitionValue()
   {
   }

   public ItemDefinitionValue(ItemDefinition itemDefinition)
   {
      this.autoCreate = itemDefinition.isAutoCreated();
      this.mandatory = itemDefinition.isMandatory();
      this.name = itemDefinition.getName();
      this.onVersion = itemDefinition.getOnParentVersion();
      this.readOnly = itemDefinition.isProtected();
   }

   /**
    * @param autoCreate
    * @param mandatory
    * @param name
    * @param onVersion
    * @param readOnly
    */
   public ItemDefinitionValue(String name, boolean autoCreate, boolean mandatory, int onVersion, boolean readOnly)
   {
      super();
      this.autoCreate = autoCreate;
      this.mandatory = mandatory;
      this.name = name;
      this.onVersion = onVersion;
      this.readOnly = readOnly;
   }

   /**
    * @return Returns the autoCreate.
    */
   public boolean isAutoCreate()
   {
      return autoCreate;
   }

   /**
    * @param autoCreate The autoCreate to set.
    */
   public void setAutoCreate(boolean autoCreate)
   {
      this.autoCreate = autoCreate;
   }

   /**
    * @return Returns the mandatory.
    */
   public boolean isMandatory()
   {
      return mandatory;
   }

   /**
    * @param mandatory The mandatory to set.
    */
   public void setMandatory(boolean mandatory)
   {
      this.mandatory = mandatory;
   }

   /**
    * @return Returns the name.
    */
   public String getName()
   {
      return name;
   }

   /**
    * @param name The name to set.
    */
   public void setName(String name)
   {
      this.name = name;
   }

   /**
    * @return Returns the onVersion.
    */
   public int getOnVersion()
   {
      return onVersion;
   }

   /**
    * @param onVersion The onVersion to set.
    */
   public void setOnVersion(int onVersion)
   {
      this.onVersion = onVersion;
   }

   /**
    * @return Returns the readOnly.
    */
   public boolean isReadOnly()
   {
      return readOnly;
   }

   /**
    * @param readOnly The readOnly to set.
    */
   public void setReadOnly(boolean readOnly)
   {
      this.readOnly = readOnly;
   }
}
