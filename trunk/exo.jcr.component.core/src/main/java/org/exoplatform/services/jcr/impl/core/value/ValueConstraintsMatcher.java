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

import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.JCRPathMatcher;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id$
 */
public class ValueConstraintsMatcher extends ValueConstraintsValidator
{

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ValueConstraintsMatcher");

   private final LocationFactory locator;

   private final ItemDataConsumer itemDataConsumer;

   private final NodeTypeDataManager nodeTypeDataManager;

   public ValueConstraintsMatcher(String[] constraints, LocationFactory locator, ItemDataConsumer itemDataConsumer,
      NodeTypeDataManager nodeTypeDataManager)
   {
      super(constraints);
      this.locator = locator;
      this.itemDataConsumer = itemDataConsumer;
      this.nodeTypeDataManager = nodeTypeDataManager;
   }

   /**
    * Check given value on compatibility with a given type.  
    * 
    * @param value ValueData
    * @param type int, property type
    * @return boolean true if the value matches the type, false otherwise
    * @throws RepositoryException if gathering of match conditions meets errors (IOException or ItemNotFoundException)
    */
   public boolean match(ValueData value, int type) throws RepositoryException
   {

      if (constraints == null || constraints.length <= 0)
      {
         return true;
      }

      boolean invalid = true;

      // do not use getString because of string consuming
      ValueData valueData = value;
      if (type == PropertyType.STRING)
      {
         try
         {
            String strVal = new String(valueData.getAsByteArray(), Constants.DEFAULT_ENCODING);

            for (int i = 0; invalid && i < constraints.length; i++)
            {
               String constrString = constraints[i];
               if (strVal.matches(constrString))
               {
                  invalid = false;
               }
            }
         }
         catch (UnsupportedEncodingException e)
         {
            throw new RuntimeException("FATAL ERROR Charset " + Constants.DEFAULT_ENCODING + " is not supported!");
         }
         catch (IOException e)
         {
            throw new RepositoryException("FATAL ERROR Value data stream reading error " + e.getMessage(), e);
         }

      }
      else if (type == PropertyType.NAME)
      {
         NameValue nameVal;
         try
         {
            nameVal = new NameValue(valueData, locator);
         }
         catch (IOException e)
         {
            throw new RepositoryException(e);
         }
         for (int i = 0; invalid && i < constraints.length; i++)
         {
            String constrString = constraints[i];
            InternalQName constrName = locator.parseJCRName(constrString).getInternalName();
            if (nameVal.getQName().equals(constrName))
            {
               invalid = false;
            }
         }
      }
      else if (type == PropertyType.PATH)
      {
         PathValue pathVal;
         try
         {
            pathVal = new PathValue(valueData, locator);
         }
         catch (IOException e)
         {
            throw new RepositoryException(e);
         }
         for (int i = 0; invalid && i < constraints.length; i++)
         {
            JCRPathMatcher constrPath = parsePathMatcher(locator, constraints[i]);
            if (constrPath.match(pathVal.getQPath()))
            {
               invalid = false;
            }
         }
      }
      else if (type == PropertyType.REFERENCE)
      {
         try
         {
            ReferenceValue refVal = new ReferenceValue(valueData);
            NodeData refNode = (NodeData)itemDataConsumer.getItemData(refVal.getIdentifier().getString());
            for (int i = 0; invalid && i < constraints.length; i++)
            {
               String constrString = constraints[i];
               InternalQName constrName = locator.parseJCRName(constrString).getInternalName();
               if (nodeTypeDataManager
                  .isNodeType(constrName, refNode.getPrimaryTypeName(), refNode.getMixinTypeNames()))
               {
                  invalid = false;
               }
            }
         }
         catch (ItemNotFoundException e)
         {
            if (LOG.isDebugEnabled())
            {
               LOG.debug("Reference constraint node is not found: " + e.getMessage());
            }
            // But if it's a versionHisroy ref property for add mix:versionable...
            // we haven't a versionHisroy created until save method will be called
            // on this
            // session/item...
            // it's transient state here.
            invalid = false; // so, value is matched, we hope...
         }
         catch (RepositoryException e)
         {
            LOG.error("Reference constraint error: " + e.getMessage(), e);
            // [PN] Posible trouble is session.getNodeByUUID() call result,
            // till bug can be found in version restore operation.
            invalid = true;
         }
         catch (IOException e)
         {
            LOG.error("Reference constraint error: " + e.getMessage(), e);
            invalid = true;
         }
      }
      else if (type == PropertyType.BINARY)
      {
         long valueLength = valueData.getLength();
         for (int i = 0; invalid && i < constraints.length; i++)
         {
            String constrString = constraints[i];

            boolean minInvalid = true;
            boolean maxInvalid = true;

            MinMaxConstraint constraint = parseAsMinMax(constrString);

            long min =
               constraint.getMin().getThreshold().length() > 0 ? new Long(constraint.getMin().getThreshold())
                  : Long.MIN_VALUE;
            if (constraint.getMin().isExclusive())
            {
               if (valueLength > min)
               {
                  minInvalid = false;
               }
            }
            else
            {
               if (valueLength >= min)
               {
                  minInvalid = false;
               }
            }

            long max =
               constraint.getMax().getThreshold().length() > 0 ? new Long(constraint.getMax().getThreshold())
                  : Long.MAX_VALUE;
            if (constraint.getMax().isExclusive())
            {
               if (valueLength < max)
                  maxInvalid = false;
            }
            else
            {
               if (valueLength <= max)
                  maxInvalid = false;
            }
            invalid = maxInvalid | minInvalid;
         }
      }
      else if (type == PropertyType.DATE)
      {
         Calendar valueCalendar;
         try
         {
            valueCalendar = new DateValue(valueData).getDate();
         }
         catch (IOException e)
         {
            throw new RepositoryException(e);
         }
         for (int i = 0; invalid && i < constraints.length; i++)
         {
            boolean minInvalid = true;
            boolean maxInvalid = true;

            MinMaxConstraint constraint = parseAsMinMax(constraints[i]);
            try
            {
               if (constraint.getMin().getThreshold().length() > 0)
               {
                  Calendar min = JCRDateFormat.parse(constraint.getMin().getThreshold());
                  if (constraint.getMin().isExclusive())
                  {
                     if (valueCalendar.compareTo(min) > 0)
                     {
                        minInvalid = false;
                     }
                  }
                  else
                  {
                     if (valueCalendar.compareTo(min) >= 0)
                     {
                        minInvalid = false;
                     }
                  }
               }
               else
               {
                  minInvalid = false;
               }
            }
            catch (ValueFormatException e)
            {
               minInvalid = false;
            }

            try
            {
               if (constraint.getMax().getThreshold().length() > 0)
               {
                  Calendar max = JCRDateFormat.parse(constraint.getMax().getThreshold());
                  if (constraint.getMax().isExclusive())
                  {
                     if (valueCalendar.compareTo(max) < 0)
                        maxInvalid = false;
                  }
                  else
                  {
                     if (valueCalendar.compareTo(max) <= 0)
                        maxInvalid = false;
                  }
               }
               else
               {
                  maxInvalid = false;
               }
            }
            catch (ValueFormatException e)
            {
               maxInvalid = false;
            }

            invalid = maxInvalid | minInvalid;
         }
      }
      else if (type == PropertyType.LONG || type == PropertyType.DOUBLE)
      {
         // will be compared as double in any case
         Number valueNumber;
         try
         {
            valueNumber = new DoubleValue(valueData).getDouble();
         }
         catch (IOException e)
         {
            throw new RepositoryException(e);
         }
         for (int i = 0; invalid && i < constraints.length; i++)
         {
            boolean minInvalid = true;
            boolean maxInvalid = true;

            MinMaxConstraint constraint = parseAsMinMax(constraints[i]);

            Number min =
               constraint.getMin().getThreshold().length() > 0 ? new Double(constraint.getMin().getThreshold())
                  : Double.MIN_VALUE;
            if (constraint.getMin().isExclusive())
            {
               if (valueNumber.doubleValue() > min.doubleValue())
               {
                  minInvalid = false;
               }
            }
            else
            {
               if (valueNumber.doubleValue() >= min.doubleValue())
               {
                  minInvalid = false;
               }
            }

            Number max =
               constraint.getMax().getThreshold().length() > 0 ? new Double(constraint.getMax().getThreshold())
                  : Double.MAX_VALUE;
            if (constraint.getMax().isExclusive())
            {
               if (valueNumber.doubleValue() < max.doubleValue())
               {
                  maxInvalid = false;
               }
            }
            else
            {
               if (valueNumber.doubleValue() <= max.doubleValue())
               {
                  maxInvalid = false;
               }
            }
            invalid = maxInvalid | minInvalid;
         }
      }
      else if (type == PropertyType.BOOLEAN)
      {
         try
         {
            boolean bvalue = Boolean.parseBoolean(new String(valueData.getAsByteArray()));
            for (int i = 0; invalid && i < constraints.length; i++)
            {
               if (Boolean.parseBoolean(constraints[i]) == bvalue)
               {
                  invalid = false;
               }
            }
         }
         catch (IOException e)
         {
            throw new RepositoryException("FATAL ERROR Value data stream reading error " + e.getMessage(), e);
         }
      }

      return !invalid;
   }

   protected JCRPath parsePath(String path, LocationFactory locFactory) throws RepositoryException
   {
      try
      {
         return locFactory.parseAbsPath(path);
      }
      catch (RepositoryException e)
      {
         try
         {
            return locFactory.parseRelPath(path);
         }
         catch (RepositoryException e1)
         {
            throw e;
         }
      }
   }

   /**
    * Parses JCR path matcher from string.
    * 
    * @param path
    * @return
    * @throws RepositoryException
    */
   protected JCRPathMatcher parsePathMatcher(LocationFactory locFactory, String path) throws RepositoryException
   {

      JCRPath knownPath = null;
      boolean forDescendants = false;
      boolean forAncestors = false;

      if (path.equals("*") || path.equals(".*"))
      {
         // any
         forDescendants = true;
         forAncestors = true;
      }
      else if (path.endsWith("*") && path.startsWith("*"))
      {
         forDescendants = true;
         forAncestors = true;
         knownPath = parsePath(path.substring(1, path.length() - 1), locFactory);
      }
      else if (path.endsWith("*"))
      {
         forDescendants = true;
         knownPath = parsePath(path.substring(0, path.length() - 1), locFactory);
      }
      else if (path.startsWith("*"))
      {
         forAncestors = true;
         knownPath = parsePath(path.substring(1), locFactory);
      }
      else
      {
         knownPath = parsePath(path, locFactory);
      }

      return new JCRPathMatcher(knownPath.getInternalPath(), forDescendants, forAncestors);
   }
}
