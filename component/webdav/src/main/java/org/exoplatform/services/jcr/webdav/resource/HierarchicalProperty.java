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
package org.exoplatform.services.jcr.webdav.resource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SAS .<br/>
 * DOM - like (but lighter) webdav property representation
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class HierarchicalProperty
{

   /**
    * The list of property's children.
    */
   protected List<HierarchicalProperty> children;

   /**
    * Property's name.
    */
   protected QName name;

   /**
    * Property's value.
    */
   protected String value;

   /**
    * Property's attributes.
    */
   protected HashMap<String, String> attributes = new HashMap<String, String>();

   /**
    * Constructor accepting String as property name, both prefixed (i.e.
    * prefix:local) and not (i.e. local) are accepted.
    * 
    * @param name property name
    * @param value property value (can be null)
    */
   public HierarchicalProperty(String name, String value)
   {
      String[] tmp = name.split(":");
      if (tmp.length > 1)
      {
         this.name = new QName(tmp[0], tmp[1]);
      }
      else
      {
         this.name = new QName(tmp[0]);
      }
      this.value = value;
      this.children = new ArrayList<HierarchicalProperty>();
   }

   /**
    * Constructor accepting QName as property name and String as value.
    * @param name property name
    * @param value property value
    */
   public HierarchicalProperty(QName name, String value)
   {
      this.name = name;
      this.value = value;
      this.children = new ArrayList<HierarchicalProperty>();
   }

   /**
    * Constructor accepting QName as property name and calendar as value.
    * 
    * @param name property name 
    * @param dateValue property value
    * @param formatPattern date format pattern
    */
   public HierarchicalProperty(QName name, Calendar dateValue, String formatPattern)
   {
      this(name, null);
      SimpleDateFormat dateFormat = new SimpleDateFormat(formatPattern, Locale.ENGLISH);
      dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      this.value = dateFormat.format(dateValue.getTime());
   }

   /**
    * Shortcut for XMLProperty(name, null).
    * 
    * @param name property name
    */
   public HierarchicalProperty(QName name)
   {
      this(name, null);
   }

   /**
    * adds prop as a children to this property.
    * 
    * @param prop property name
    * @return added property 
    */
   public HierarchicalProperty addChild(HierarchicalProperty prop)
   {
      children.add(prop);
      return prop;
   }

   /**
    * Returns this property children.
    * 
    * @return child properties of this property
    */
   public List<HierarchicalProperty> getChildren()
   {
      return this.children;
   }

   /**
    * retrieves children property by name.
    * 
    * @param name child name
    * @return property or null if not found
    */
   public HierarchicalProperty getChild(QName name)
   {
      for (HierarchicalProperty child : children)
      {
         if (child.getName().equals(name))
            return child;
      }
      return null;
   }

   /**
    * retrieves children property by 0 based index.
    * 
    * @param index the index of child
    * @return child with current index
    */
   public HierarchicalProperty getChild(int index)
   {
      return children.get(index);
   }

   /**
    * @return property name
    */
   public QName getName()
   {
      return name;
   }

   /**
    * @return property value
    */
   public String getValue()
   {
      return value;
   }

   /**
    * sets the property value.
    * 
    * @param value property value
    */
   public void setValue(String value)
   {
      this.value = value;
   }

   /**
    * sets the attribute.
    * 
    * @param attributeName attribute name
    * @param attributeValue attribute value
    */
   public void setAttribute(String attributeName, String attributeValue)
   {
      attributes.put(attributeName, attributeValue);
   }

   /**
    * @param attributeName attribute name
    * @return attribute attribute
    */
   public String getAttribute(String attributeName)
   {
      return attributes.get(attributeName);
   }

   /**
    * @return all attributes
    */
   public HashMap<String, String> getAttributes()
   {
      return attributes;
   }

}
