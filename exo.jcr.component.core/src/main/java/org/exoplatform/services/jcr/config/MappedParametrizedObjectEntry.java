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
package org.exoplatform.services.jcr.config;

import org.exoplatform.services.jcr.util.StringNumberParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: MappedParametrizedObjectEntry.java 1807 2005-08-28 13:34:58Z geaz $
 */

public abstract class MappedParametrizedObjectEntry
{

   protected String type;

   protected List<SimpleParameterEntry> parameters = new ArrayList<SimpleParameterEntry>();

   public MappedParametrizedObjectEntry()
   {
   }

   public MappedParametrizedObjectEntry(String type, List parameters)
   {
      this.type = type;
      this.parameters = parameters;
   }

   public List<SimpleParameterEntry> getParameters()
   {
      return parameters;
   }

   /**
    * Parse named parameter.
    * 
    * @param name
    *          parameter name
    * @return String
    * @throws RepositoryConfigurationException
    */
   public String getParameterValue(String name) throws RepositoryConfigurationException
   {
      String value = getParameterValue(name, null);
      if (value == null)
         throw new RepositoryConfigurationException("Parameter " + name + " not found ");
      return value;
   }

   /**
    * Parse named parameter.
    * 
    * @param name
    *          parameter name
    * @param defaultValue
    *          default value
    * @return String
    */
   public String getParameterValue(String name, String defaultValue)
   {
      String value = defaultValue;
      for (int i = 0; i < parameters.size(); i++)
      {
         SimpleParameterEntry p = parameters.get(i);
         if (p.getName().equals(name))
         {
            value = p.getValue();
            break;
         }
      }
      return value;
   }

   public void putParameterValue(String name, String value)
   {
      for (SimpleParameterEntry p : parameters)
      {
         if (p.getName().equals(name))
         {
            p.setValue(value);
            return;
         }
      }

      SimpleParameterEntry newParam = new SimpleParameterEntry(name, value);
      parameters.add(newParam);
   }

   /**
    * Parse named parameter as Integer.
    * 
    * @param name
    *          parameter name
    * @param defaultValue
    *          default Integer value
    * @return Integer value
    */
   public Integer getParameterInteger(String name, Integer defaultValue)
   {
      for (int i = 0; i < parameters.size(); i++)
      {
         SimpleParameterEntry p = parameters.get(i);
         if (p.getName().equals(name))
         {
            try
            {
               return StringNumberParser.parseInt(p.getValue());
            }
            catch (NumberFormatException e)
            {
               //LOG.warn(name + ": unparseable Integer. " + e);
            }
         }
      }
      return defaultValue;
   }

   /**
    * Set parameter as integer
    * 
    * @param name
    * @param defaultValue
    */
   public void putIntegerParameter(String name, Integer value)
   {
      for (SimpleParameterEntry param : parameters)
      {
         if (param.getName().equals(name))
         {
            param.setValue(value.toString());
            return;
         }
      }

      SimpleParameterEntry newParam = new SimpleParameterEntry(name, value.toString());
      parameters.add(newParam);
   }

   /**
    * Parse named parameter as Integer.
    * 
    * @param name
    *          parameter name
    * @return Integer value
    * @throws RepositoryConfigurationException
    */
   public Integer getParameterInteger(String name) throws RepositoryConfigurationException
   {
      try
      {
         return StringNumberParser.parseInt(getParameterValue(name));
      }
      catch (NumberFormatException e)
      {
         throw new RepositoryConfigurationException(name + ": unparseable Integer. " + e, e);
      }
   }

   /**
    * Parse named parameter as Long.
    * 
    * @param name
    *          parameter name
    * @param defaultValue
    *          default Long value
    * @return Long value
    */
   public Long getParameterLong(String name, Long defaultValue)
   {
      for (int i = 0; i < parameters.size(); i++)
      {
         SimpleParameterEntry p = parameters.get(i);
         if (p.getName().equals(name))
         {
            try
            {
               return StringNumberParser.parseLong(p.getValue());
            }
            catch (NumberFormatException e)
            {
               //LOG.warn(name + ": unparseable Long. " + e);
            }
         }
      }
      return defaultValue;
   }

   /**
    * Parse named parameter as Long.
    * 
    * @param name
    *          parameter name
    * @return Long value
    * @throws RepositoryConfigurationException
    */
   public Long getParameterLong(String name) throws RepositoryConfigurationException
   {
      try
      {
         return StringNumberParser.parseLong(getParameterValue(name));
      }
      catch (NumberFormatException e)
      {
         throw new RepositoryConfigurationException(name + ": unparseable Long. " + e, e);
      }
   }

   /**
    * Parse named parameter as Double.
    * 
    * @param name
    *          parameter name
    * @param defaultValue
    *          default Double value
    * @return Double value
    */
   public Double getParameterDouble(String name, Double defaultValue)
   {
      for (int i = 0; i < parameters.size(); i++)
      {
         SimpleParameterEntry p = parameters.get(i);
         if (p.getName().equals(name))
         {
            try
            {
               return StringNumberParser.parseDouble(p.getValue());
            }
            catch (NumberFormatException e)
            {
               //LOG.warn(name + ": unparseable Long. " + e);
            }
         }
      }
      return defaultValue;
   }

   /**
    * Parse named parameter as Double.
    * 
    * @param name
    *          parameter name
    * @return Double value
    * @throws RepositoryConfigurationException
    */
   public Double getParameterDouble(String name) throws RepositoryConfigurationException
   {
      try
      {
         return StringNumberParser.parseDouble(getParameterValue(name));
      }
      catch (NumberFormatException e)
      {
         throw new RepositoryConfigurationException(name + ": unparseable Long. " + e, e);
      }
   }

   /**
    * Parse named parameter using {@link StringNumberParser.parseTime} and return time in
    * milliseconds (Long value).
    * 
    * @param name
    *          parameter name
    * @param defaultValue
    *          default time value
    * @return
    */
   public Long getParameterTime(String name, Long defaultValue)
   {
      for (int i = 0; i < parameters.size(); i++)
      {
         SimpleParameterEntry p = parameters.get(i);
         if (p.getName().equals(name))
         {
            try
            {
               return StringNumberParser.parseTime(p.getValue());
            }
            catch (NumberFormatException e)
            {
               //LOG.warn(name + ": unparseable time (as Long). " + e);
            }
         }
      }
      return defaultValue;
   }

   /**
    * Parse named parameter using {@link StringNumberParser.parseTime} and return time in
    * milliseconds (Long value).
    * 
    * @param name
    *          parameter name
    * @return Long value
    * @throws RepositoryConfigurationException
    */
   public Long getParameterTime(String name) throws RepositoryConfigurationException
   {
      try
      {
         return StringNumberParser.parseTime(getParameterValue(name));
      }
      catch (NumberFormatException e)
      {
         throw new RepositoryConfigurationException(name + ": unparseable time (as Long). " + e, e);
      }
   }

   /**
    * Parse named parameter as Boolean.
    * 
    * @param name
    *          parameter name
    * @param defaultValue
    *          default value
    * @return boolean value
    */
   public Boolean getParameterBoolean(String name, Boolean defaultValue)
   {
      for (int i = 0; i < parameters.size(); i++)
      {
         SimpleParameterEntry p = parameters.get(i);
         if (p.getName().equals(name))
         {
            return new Boolean(p.getValue());
         }
      }
      return defaultValue;
   }

   /**
    * Set parameter as boolean
    * 
    * @param name
    * @param value
    */
   public void putBooleanParameter(String name, Boolean value)
   {
      for (int i = 0; i < parameters.size(); i++)
      {
         SimpleParameterEntry p = parameters.get(i);
         if (p.getName().equals(name))
         {
            p.setValue(value.toString());
            return;
         }
      }

      SimpleParameterEntry newParam = new SimpleParameterEntry(name, value.toString());
      parameters.add(newParam);
   }

   /**
    * Parse named parameter as Boolean.
    * 
    * @param name
    * @return Boolean value
    * @throws RepositoryConfigurationException
    */
   public Boolean getParameterBoolean(String name) throws RepositoryConfigurationException
   {
      return new Boolean(getParameterValue(name));
   }

   public String getType()
   {
      return type;
   }

   public void setParameters(List<SimpleParameterEntry> parameters)
   {
      this.parameters = parameters;
   }

   public void setType(String type)
   {
      this.type = type;
   }

}
