/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.webdav;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.services.jcr.webdav.util.InitParamsDefaults;
import org.exoplatform.services.jcr.webdav.util.InitParamsNames;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;

/**
 * Provides means to parse WebDAV service initial parameters and gain access to their values.
 * 
 * @author  <a href="mailto:dmi3.kuleshov@gmail.com">Dmitry Kuleshov</a>
 * @version $Id$
 */
public class WebDavServiceInitParams
{
   /**
    * Logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.webdav.WebDavServiceInitParams");

   /**
    * Default folder node type.
    */
   private String defaultFolderNodeType = InitParamsDefaults.FOLDER_NODE_TYPE;

   /**
    * Default file node type.
    */
   private String defaultFileNodeType = InitParamsDefaults.FILE_NODE_TYPE;

   /**
    * Default file mime type.
    */
   private String defaultFileMimeType = InitParamsDefaults.FILE_MIME_TYPE;

   /**
    * Update policy.
    */
   private String defaultUpdatePolicyType = InitParamsDefaults.UPDATE_POLICY;

   /**
    * Auto-version default value.
    */
   private String defaultAutoVersionType = InitParamsDefaults.AUTO_VERSION;

   /**
    * XSLT parameters.
    */
   private Map<String, String> xsltParams = new HashMap<String, String>();

   /**
    * Set of untrusted user agents. Special rules are applied for listed agents.
    */
   private Set<String> untrustedUserAgents = new HashSet<String>();

   /**
    * Set of allowed file node types.
    */
   private Set<String> allowedFileNodeTypes = new HashSet<String>();

   /**
    * Set of allowed folder node types.
    */
   private Set<String> allowedFolderNodeTypes = new HashSet<String>();

   /**
    * Cache control parameters.
    */
   private Map<MediaType, String> cacheControlMap = new HashMap<MediaType, String>();

   /**
    * Default constructor, all initial parameters take default values.
    * The list of default parameters values may be obtained from {@link InitParamsDefaults}.
    */
   public WebDavServiceInitParams()
   {
      allowedFileNodeTypes.add(InitParamsDefaults.FILE_NODE_TYPE);
      allowedFolderNodeTypes.add(InitParamsDefaults.FOLDER_NODE_TYPE);
   }

   /**
    * Create an instance of WebDAV service initial parameters from {@link InitParams} 
    * which are obtained from configuration file.
    * @param params initial parameters
    */
   public WebDavServiceInitParams(InitParams params)
   {
      this();
      Map<String, String> parametersMap = new HashMap<String, String>();

      Iterator<ValueParam> valueParamIterator = params.getValueParamIterator();
      ValueParam valueParam;
      while (valueParamIterator.hasNext())
      {
         valueParam = valueParamIterator.next();
         parametersMap.put(valueParam.getName(), valueParam.getValue());
      }

      Iterator<ValuesParam> valuesParamIterator = params.getValuesParamIterator();
      ValuesParam valuesParam;
      while (valuesParamIterator.hasNext())
      {
         valuesParam = valuesParamIterator.next();

         ArrayList<String> values = valuesParam.getValues();
         StringBuffer sb = new StringBuffer();
         for (int i = 0; i < values.size(); i++)
         {
            sb.append(values.get(i));
            sb.append(',');
         }

         parametersMap.put(valuesParam.getName(), sb.toString());
      }

      processParametersMap(parametersMap);
   }

   /**
    * Create an instance of WebDAV service initial parameters from parameters map.
    * Please note, that this constructor receives Map<String, String> instead of InitParams
    * and we cannot pass multi-valued parameters in the form of 
    * {@link String} -> {@link Collection}  
    * To overcome this shortage we pass a set of parameter values as a single {@link String}
    * with each value separated by comma (",") i.e. "agent1, agent2, agent3"
    * @param params initial parameters
    */
   public WebDavServiceInitParams(Map<String, String> params)
   {
      this();
      processParametersMap(params);
   }

   private void processParametersMap(Map<String, String> parameters)
   {
      ParametersMapProcessor pmp = new ParametersMapProcessor(parameters);

      pmp.processSingleParameter(defaultFolderNodeType, InitParamsNames.DEF_FOLDER_NODE_TYPE);
      pmp.processSingleParameter(defaultFileNodeType, InitParamsNames.DEF_FILE_NODE_TYPE);
      pmp.processSingleParameter(defaultFileMimeType, InitParamsNames.DEF_FILE_MIME_TYPE);
      pmp.processSingleParameter(defaultUpdatePolicyType, InitParamsNames.UPDATE_POLICY);
      pmp.processSingleParameter(defaultAutoVersionType, InitParamsNames.AUTO_VERSION);

      pmp.processMultiParameter(untrustedUserAgents, InitParamsNames.UNTRUSTED_USER_AGENTS);
      pmp.processMultiParameter(allowedFileNodeTypes, InitParamsNames.ALLOWED_FILE_NODE_TYPES);
      pmp.processMultiParameter(allowedFolderNodeTypes, InitParamsNames.ALLOWED_FOLDER_NODE_TYPES);

      pmp.processSingleParameterToMap(xsltParams, InitParamsNames.FILE_ICON_PATH);
      pmp.processSingleParameterToMap(xsltParams, InitParamsNames.FOLDER_ICON_PATH);

      pmp.processCacheControlParameter();
   }



   public String getDefaultFolderNodeType()
   {
      return defaultFolderNodeType;
   }

   public String getDefaultFileNodeType()
   {
      return defaultFileNodeType;
   }

   public String getDefaultFileMimeType()
   {
      return defaultFileMimeType;
   }

   public String getDefaultUpdatePolicyType()
   {
      return defaultUpdatePolicyType;
   }

   public String getDefaultAutoVersionType()
   {
      return defaultAutoVersionType;
   }

   public Map<String, String> getXsltParams()
   {
      return xsltParams;
   }

   public Set<String> getUntrustedUserAgents()
   {
      return untrustedUserAgents;
   }

   public Set<String> getAllowedFileNodeTypes()
   {
      return allowedFileNodeTypes;
   }

   public Set<String> getAllowedFolderNodeTypes()
   {
      return allowedFolderNodeTypes;
   }

   public Map<MediaType, String> getCacheControlMap()
   {
      return cacheControlMap;
   }

   public void setDefaultFolderNodeType(String defaultFolderNodeType)
   {
      this.defaultFolderNodeType = defaultFolderNodeType;
   }

   public void setDefaultFileNodeType(String defaultFileNodeType)
   {
      this.defaultFileNodeType = defaultFileNodeType;
   }

   public void setDefaultFileMimeType(String defaultFileMimeType)
   {
      this.defaultFileMimeType = defaultFileMimeType;
   }

   public void setDefaultUpdatePolicyType(String defaultUpdatePolicyType)
   {
      this.defaultUpdatePolicyType = defaultUpdatePolicyType;
   }

   public void setDefaultAutoVersionType(String defaultAutoVersionType)
   {
      this.defaultAutoVersionType = defaultAutoVersionType;
   }

   public void setXsltParams(Map<String, String> xsltParams)
   {
      this.xsltParams = xsltParams;
   }

   public void setUntrustedUserAgents(Set<String> untrustedUserAgents)
   {
      this.untrustedUserAgents = untrustedUserAgents;
   }

   public void setAllowedFileNodeTypes(Set<String> allowedFileNodeTypes)
   {
      this.allowedFileNodeTypes = allowedFileNodeTypes;
   }

   public void setAllowedFolderNodeTypes(Set<String> allowedFolderNodeTypes)
   {
      this.allowedFolderNodeTypes = allowedFolderNodeTypes;
   }

   public void setCacheControlMap(Map<MediaType, String> cacheControlMap)
   {
      this.cacheControlMap = cacheControlMap;
   }

   private class ParametersMapProcessor
   {
      Map<String, String> parameters;

      public ParametersMapProcessor(Map<String, String> parameters)
      {
         this.parameters = parameters;
      }

      private void processSingleParameterToMap(Map<String, String> parameterMap, String parameterName)
      {
         String paramValue = parameters.get(parameterName);
         if (paramValue != null)
         {
            parameterMap.put(parameterName, paramValue);
            log.info(parameterName + " = " + paramValue);
         }
      }

      private void processCacheControlParameter()
      {
         String paramValue = parameters.get(InitParamsNames.CACHE_CONTROL);
         if (paramValue != null)
         {
            try
            {
               String[] elements = paramValue.split(";");
               for (String element : elements)
               {
                  String cacheValue = element.split(":")[1];
                  String keys = element.split(":")[0];
                  for (String key : keys.split(","))
                  {
                     MediaType mediaType = new MediaType(key.split("/")[0], key.split("/")[1]);
                     cacheControlMap.put(mediaType, cacheValue);
                  }
               }
            }
            catch (Exception e)
            {
               log.warn("Invalid " + InitParamsNames.CACHE_CONTROL + " parameter");
            }

         }
      }

      private void processSingleParameter(String parameter, String parameterName)
      {
         String paramValue = parameters.get(parameterName);
         if (paramValue != null)
         {
            parameter = paramValue;
            log.info(parameterName + " = " + parameter);
         }
      }

      private void processMultiParameter(Set<String> valuesSet, String parameterName)
      {
         String parameterMultivalue = parameters.get(parameterName);
         if (parameterMultivalue != null)
         {
            valuesSet.clear();
            for (String value : parameterMultivalue.split(","))
            {
               valuesSet.add(value.trim());
               log.info(parameterName + " = " + value.trim());
            }
         }
      }
   }
}
