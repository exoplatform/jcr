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

import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.datamodel.Identifier;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.JCRName;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedSystemHelper;
import org.exoplatform.services.jcr.impl.util.io.WorkspaceFileCleanerHolder;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

/**
 * Created by The eXo Platform SAS.<br/>
 * ValueFactory implementation
 * 
 * @author Gennady Azarenkov
 * @version $Id$
 */

public class ValueFactoryImpl implements ValueFactory
{

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ValueFactoryImpl");

   private LocationFactory locationFactory;

   private FileCleaner fileCleaner;

   private File tempDirectory;

   private int maxBufferSize;

   public ValueFactoryImpl(LocationFactory locationFactory, WorkspaceEntry workspaceConfig,
      WorkspaceFileCleanerHolder cleanerHolder)
   {

      this.locationFactory = locationFactory;
      this.fileCleaner = cleanerHolder.getFileCleaner();
      this.tempDirectory = new File(PrivilegedSystemHelper.getProperty("java.io.tmpdir"));

      // TODO we use WorkspaceDataContainer constants but is it ok?
      this.maxBufferSize =
         workspaceConfig.getContainer().getParameterInteger(WorkspaceDataContainer.MAXBUFFERSIZE_PROP,
            WorkspaceDataContainer.DEF_MAXBUFFERSIZE);
   }

   public ValueFactoryImpl(LocationFactory locationFactory)
   {
      this.locationFactory = locationFactory;
      this.maxBufferSize = WorkspaceDataContainer.DEF_MAXBUFFERSIZE;
      this.tempDirectory = new File(PrivilegedSystemHelper.getProperty("java.io.tmpdir"));
   }

   /**
    * {@inheritDoc}
    */
   public Value createValue(String value, int type) throws ValueFormatException
   {
      if (value == null)
         return null;

      try
      {
         switch (type)
         {
            case PropertyType.STRING :
               return createValue(new String(value));
            case PropertyType.BINARY :
               try
               {
                  return createValue(new ByteArrayInputStream(value.getBytes(Constants.DEFAULT_ENCODING)));
               }
               catch (UnsupportedEncodingException e)
               {
                  throw new RuntimeException("FATAL ERROR Charset " + Constants.DEFAULT_ENCODING + " is not supported!");
               }
            case PropertyType.BOOLEAN :
               return createValue(Boolean.parseBoolean(value));
            case PropertyType.LONG :
               return createValue(Long.parseLong(value));
            case PropertyType.DOUBLE :
               return createValue(Double.parseDouble(value));
            case PropertyType.DATE :
               Calendar cal = JCRDateFormat.parse(value);
               return createValue(cal);
            case PropertyType.PATH :
               try
               {
                  JCRPath path;
                  if (value.startsWith("/"))
                     path = locationFactory.parseAbsPath(value);
                  else
                     path = locationFactory.parseRelPath(value);
                  return createValue(path);
               }
               catch (RepositoryException e)
               {
                  throw new ValueFormatException("Path '" + value + "' is invalid");
               }
            case PropertyType.NAME :
               try
               {
                  JCRName name = locationFactory.parseJCRName(value);
                  return createValue(name);
               }
               catch (RepositoryException e)
               {
                  throw new ValueFormatException("Name '" + value + "' is invalid", e);
               }
            case PropertyType.REFERENCE :
               return createValue(new Identifier(value));
            case ExtendedPropertyType.PERMISSION :
               try
               {
                  return PermissionValue.parseValue(value);
               }
               catch (IOException e)
               {
                  new ValueFormatException("Cant create PermissionValue " + e);
               }
            default :
               throw new ValueFormatException("unknown type " + type);
         }
      }
      catch (IllegalArgumentException e)
      { // NumberFormatException
         throw new ValueFormatException("Cant create value from string '" + value + "' for type "
            + PropertyType.nameFromValue(type));
      }
   }

   /**
    * {@inheritDoc}
    */
   public Value createValue(String value)
   {
      if (value == null)
         return null;
      try
      {
         return new StringValue(value);
      }
      catch (IOException e)
      {
         LOG.warn("Cannot create Value from String " + value, e);
         return null;
      }

   }

   /**
    * {@inheritDoc}
    */
   public Value createValue(long value)
   {
      try
      {
         return new LongValue(value);
      }
      catch (IOException e)
      {
         LOG.warn("Cannot create Value from long " + value, e);
         return null;
      }

   }

   /**
    * {@inheritDoc}
    */
   public Value createValue(double value)
   {
      try
      {
         return new DoubleValue(value);
      }
      catch (IOException e)
      {
         LOG.warn("Cannot create Value from double " + value, e);
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   public Value createValue(boolean value)
   {
      try
      {
         return new BooleanValue(value);
      }
      catch (IOException e)
      {
         LOG.warn("Cannot create Value from boolean " + value, e);
         return null;
      }

   }

   /**
    * {@inheritDoc}
    */
   public Value createValue(Calendar value)
   {
      if (value == null)
         return null;
      try
      {
         return new DateValue(value);
      }
      catch (IOException e)
      {
         LOG.warn("Cannot create Value from Calendar " + value, e);
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   public Value createValue(InputStream value)
   {
      if (value == null)
         return null;
      try
      {
         return new BinaryValue(value, fileCleaner, tempDirectory, maxBufferSize);
      }
      catch (IOException e)
      {
         LOG.warn("Cannot create Value from InputStream " + value, e);
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   public Value createValue(Node value) throws RepositoryException
   {
      if (value == null)
         return null;
      if (!value.isNodeType("mix:referenceable"))
         throw new ValueFormatException("Node " + value.getPath() + " is not referenceable");
      try
      {
         if (value instanceof NodeImpl)
         {
            String jcrUuid = ((NodeImpl)value).getInternalIdentifier();
            return new ReferenceValue(new TransientValueData(jcrUuid));
         }
         else
         {
            throw new RepositoryException("Its need a NodeImpl instance of Node");
         }
      }
      catch (IOException e)
      {
         throw new RepositoryException("Cannot create REFERENE Value from Node", e);
      }
   }

   // /////////////////////////

   /**
    * Create Value from JCRName.
    * 
    * @param value
    *          JCRName
    * @return Value
    * @throws RepositoryException
    *           if error
    */
   public Value createValue(JCRName value) throws RepositoryException
   {
      if (value == null)
         return null;
      try
      {
         return new NameValue(value.getInternalName(), locationFactory);
      }
      catch (IOException e)
      {
         throw new RepositoryException("Cannot create NAME Value from JCRName", e);
      }
   }

   /**
    * Create Value from JCRPath.
    * 
    * @param value
    *          JCRPath
    * @return Value
    * @throws RepositoryException
    *           if error
    */
   public Value createValue(JCRPath value) throws RepositoryException
   {
      if (value == null)
         return null;
      try
      {
         return new PathValue(value.getInternalPath(), locationFactory);
      }
      catch (IOException e)
      {
         throw new RepositoryException("Cannot create PATH Value from JCRPath", e);
      }
   }

   /**
    * Create Value from Id.
    * 
    * @param value
    *          Identifier
    * @return Value Reference
    */
   public Value createValue(Identifier value)
   {
      if (value == null)
         return null;
      try
      {
         return new ReferenceValue(value);
      }
      catch (IOException e)
      {
         LOG.warn("Cannot create REFERENCE Value from Identifier " + value, e);
         return null;
      }
   }

   /**
    * Creates new Value object using ValueData
    * 
    * @param data
    *          ValueData
    * @param type
    *          int
    * @return Value
    * @throws RepositoryException
    *           if error
    */
   public Value loadValue(ValueData data, int type) throws RepositoryException
   {

      try
      {
         switch (type)
         {
            case PropertyType.STRING :
               return new StringValue(data);
            case PropertyType.BINARY :
               return new BinaryValue(data);
            case PropertyType.BOOLEAN :
               return new BooleanValue(data);
            case PropertyType.LONG :
               return new LongValue(data);
            case PropertyType.DOUBLE :
               return new DoubleValue(data);
            case PropertyType.DATE :
               return new DateValue(data);
            case PropertyType.PATH :
               return new PathValue(data, locationFactory);
            case PropertyType.NAME :
               return new NameValue(data, locationFactory);
            case PropertyType.REFERENCE :
               return new ReferenceValue(data);
            case PropertyType.UNDEFINED :
               return null;
            case ExtendedPropertyType.PERMISSION :
               return new PermissionValue(data);
            default :
               throw new ValueFormatException("unknown type " + type);
         }
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
   }

   public FileCleaner getFileCleaner()
   {
      return fileCleaner;
   }

   public int getMaxBufferSize()
   {
      return maxBufferSize;
   }

   public File getTempDirectory()
   {
      return tempDirectory;
   }

}
