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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: MappedParametrizedObjectEntry.java 1807 2005-08-28 13:34:58Z geaz $
 */

public abstract class MappedParametrizedObjectEntry
{

   private static final Log LOG = ExoLogger
      .getLogger("org.exoplatform.services.jcr.config.MappedParametrizedObjectEntry");

   protected String type;

   protected Map<String, SimpleParameterEntry> parameters = new LinkedHashMap<String, SimpleParameterEntry>();

   public MappedParametrizedObjectEntry()
   {
   }

   public MappedParametrizedObjectEntry(String type, List<SimpleParameterEntry> parameters)
   {
      this.type = type;
      setParameters(parameters);
   }

   public Collection<SimpleParameterEntry> getParameters()
   {
      return parameters.values();
   }

   public Iterator<SimpleParameterEntry> getParameterIterator()
   {
      return parameters.values().iterator();
   }

   public boolean hasParameters()
   {
      return !parameters.isEmpty();
   }

   public boolean hasParameter(String name)
   {
      return parameters.containsKey(name);
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
      SimpleParameterEntry p = parameters.get(name);
      if (p != null)
      {
         value = p.getValue();
      }
      return value;
   }

   public void putParameterValue(String name, String value)
   {
      SimpleParameterEntry p = parameters.get(name);
      if (p != null)
      {
         p.setValue(value);
         return;
      }
      SimpleParameterEntry newParam = new SimpleParameterEntry(name, value);
      addParameter(newParam);
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
      String value = getParameterValue(name, null);

      if (value != null)
      {
         try
         {
            return StringNumberParser.parseInt(value);
         }
         catch (NumberFormatException e)
         {
            if (LOG.isTraceEnabled())
            {
               LOG.trace("An exception occurred: " + e.getMessage());
            }
         }
      }

      return defaultValue;
   }

   /**
    * Set parameter as integer
    * 
    * @param name
    * @param value
    */
   public void putIntegerParameter(String name, Integer value)
   {
      putParameterValue(name, value.toString());
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
      String value = getParameterValue(name, null);

      if (value != null)
      {
         try
         {
            return StringNumberParser.parseLong(value);
         }
         catch (NumberFormatException e)
         {
            if (LOG.isTraceEnabled())
            {
               LOG.trace("An exception occurred: " + e.getMessage());
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
      String value = getParameterValue(name, null);

      if (value != null)
      {
         try
         {
            return StringNumberParser.parseDouble(value);
         }
         catch (NumberFormatException e)
         {
            if (LOG.isTraceEnabled())
            {
               LOG.trace("An exception occurred: " + e.getMessage());
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
    * Parse named parameter using {@link StringNumberParser#parseTime(String)} and return time in
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
      String value = getParameterValue(name, null);

      if (value != null)
      {
         try
         {
            return StringNumberParser.parseTime(value);
         }
         catch (NumberFormatException e)
         {
            if (LOG.isTraceEnabled())
            {
               LOG.trace("An exception occurred: " + e.getMessage());
            }
         }
      }
      return defaultValue;
   }

   /**
    * Parse named parameter using {@link StringNumberParser#parseTime(String)} and return time in
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
      String value = getParameterValue(name, null);

      if (value != null)
      {
         return new Boolean(value);
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
      putParameterValue(name, value.toString());
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
      Map<String, SimpleParameterEntry> mParameters = new LinkedHashMap<String, SimpleParameterEntry>();
      if (parameters != null)
      {
         for (SimpleParameterEntry param : parameters)
         {
            mParameters.put(param.getName(), param);
         }
      }
      this.parameters = mParameters;
   }

   public void addParameter(SimpleParameterEntry param)
   {
      parameters.put(param.getName(), param);
   }

   public SimpleParameterEntry getParameter(String name)
   {
      return parameters.get(name);
   }

   public void setType(String type)
   {
      this.type = type;
   }

}
