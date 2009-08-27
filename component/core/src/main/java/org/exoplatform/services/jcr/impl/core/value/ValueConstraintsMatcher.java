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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.exoplatform.services.log.Log;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.JCRPathMatcher;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko
 * peter.nedonosko@exoplatform.com.ua 13.09.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: ValueConstraintsMatcher.java 12171 2008-03-20 15:37:28Z ksm $
 */
public class ValueConstraintsMatcher
{

   protected static Log log = ExoLogger.getLogger("jcr.ValueConstraintsMatcher");

   protected final static String DEFAULT_THRESHOLD = "";

   private final String[] constraints;

   private final LocationFactory locator;

   private final ItemDataConsumer itemDataConsumer;

   private final NodeTypeDataManager nodeTypeDataManager;

   public ValueConstraintsMatcher(String[] constraints, LocationFactory locator, ItemDataConsumer itemDataConsumer,
            NodeTypeDataManager nodeTypeDataManager)
   {
      this.constraints = constraints;
      this.locator = locator;
      this.itemDataConsumer = itemDataConsumer;
      this.nodeTypeDataManager = nodeTypeDataManager;

   }

   public boolean match(ValueData value, int type) throws ConstraintViolationException, IllegalStateException,
            RepositoryException
   {

      if (constraints == null || constraints.length <= 0)
         return true;

      boolean invalid = true;

      // do not use getString because of string consuming
      TransientValueData valueData = (TransientValueData) value;
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
            String constrString = constraints[i];

            JCRPathMatcher constrPath = parsePathMatcher(locator, constrString);
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
            // NodeImpl refNode = (NodeImpl)
            // session.getNodeByUUID(refVal.getIdentifier().getString());
            NodeData refNode = (NodeData) itemDataConsumer.getItemData(refVal.getIdentifier().getString());
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
            if (log.isDebugEnabled())
               log.debug("Reference constraint node is not found: " + e.getMessage());
            // But if it's a versionHisroy ref property for add mix:versionable...
            // we haven't a versionHisroy created until save method will be called
            // on this
            // session/item...
            // it's transient state here.
            invalid = false; // so, value is matched, we hope...
         }
         catch (RepositoryException e)
         {
            log.error("Reference constraint error: " + e.getMessage(), e);
            // [PN] Posible trouble is session.getNodeByUUID() call result,
            // till bug can be found in version restore operation.
            invalid = true;
         }
         catch (IOException e)
         {
            log.error("Reference constraint error: " + e.getMessage(), e);
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
                  minInvalid = false;
            }
            else
            {
               if (valueLength >= min)
                  minInvalid = false;
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
            String constrString = constraints[i];

            boolean minInvalid = true;
            boolean maxInvalid = true;

            MinMaxConstraint constraint = parseAsMinMax(constrString);

            try
            {
               if (constraint.getMin().getThreshold().length() > 0)
               {
                  Calendar min = JCRDateFormat.parse(constraint.getMin().getThreshold());
                  if (constraint.getMin().isExclusive())
                  {
                     if (valueCalendar.compareTo(min) > 0)
                        minInvalid = false;
                  }
                  else
                  {
                     if (valueCalendar.compareTo(min) >= 0)
                        minInvalid = false;
                  }
               }
               else
                  minInvalid = false;
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
                  maxInvalid = false;
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
            String constrString = constraints[i];

            boolean minInvalid = true;
            boolean maxInvalid = true;

            MinMaxConstraint constraint = parseAsMinMax(constrString);

            Number min =
                     constraint.getMin().getThreshold().length() > 0 ? new Double(constraint.getMin().getThreshold())
                              : Double.MIN_VALUE;
            if (constraint.getMin().isExclusive())
            {
               if (valueNumber.doubleValue() > min.doubleValue())
                  minInvalid = false;
            }
            else
            {
               if (valueNumber.doubleValue() >= min.doubleValue())
                  minInvalid = false;
            }

            Number max =
                     constraint.getMax().getThreshold().length() > 0 ? new Double(constraint.getMax().getThreshold())
                              : Double.MAX_VALUE;
            if (constraint.getMax().isExclusive())
            {
               if (valueNumber.doubleValue() < max.doubleValue())
                  maxInvalid = false;
            }
            else
            {
               if (valueNumber.doubleValue() <= max.doubleValue())
                  maxInvalid = false;
            }
            invalid = maxInvalid | minInvalid;
         }
      }
      else if (type == PropertyType.BOOLEAN)
      {
         // JSR-283, 4.7.17.6 BOOLEAN has no Constraint
         invalid = false;
      }

      return !invalid;
   }

   JCRPath parsePath(String path, LocationFactory locFactory) throws RepositoryException
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

   protected MinMaxConstraint parseAsMinMax(String constraint) throws ConstraintViolationException
   {

      // constraint as min,max range:
      // value constraints in the form of inclusive or exclusive ranges:
      // i.e., "[min, max]", "(min, max)", "(min, max]" or "[min, max)".
      // Where "[" and "]" indicate "inclusive", while "(" and ")" indicate
      // "exclusive".
      // A missing min or max value indicates no bound in that direction

      String[] parts = constraint.split(",");

      if (parts.length != 2)
         throw new ConstraintViolationException("Value constraint '" + constraint
                  + "' is invalid accrding the JSR-170 spec.");

      boolean exclusive = false;

      if (parts[0].startsWith("("))
         exclusive = true;
      else if (parts[0].startsWith("["))
         exclusive = false;
      else
         throw new ConstraintViolationException("Value constraint '" + constraint
                  + "' min exclusion rule is unefined accrding the JSR-170 spec.");

      ConstraintRange minValue =
               new ConstraintRange(parts[0].length() > 1 ? parts[0].substring(1) : DEFAULT_THRESHOLD, exclusive);

      if (parts[1].endsWith(")"))
         exclusive = true;
      else if (parts[1].endsWith("]"))
         exclusive = false;
      else
         throw new ConstraintViolationException("Value constraint '" + constraint
                  + "' max exclusion rule is unefined accrding the JSR-170 spec.");

      ConstraintRange maxValue =
               new ConstraintRange(parts[1].length() > 1 ? parts[1].substring(0, parts[1].length() - 1)
                        : DEFAULT_THRESHOLD, exclusive);

      return new MinMaxConstraint(minValue, maxValue);
   }

   /**
    * Parses JCR path matcher from string
    * 
    * @param path
    * @return
    * @throws RepositoryException
    */
   private JCRPathMatcher parsePathMatcher(LocationFactory locFactory, String path) throws RepositoryException
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

   public class ConstraintRange
   {

      private final String value;

      private final boolean exclusive;

      public ConstraintRange(String value)
      {
         this.value = value;
         this.exclusive = false;
      }

      public ConstraintRange(String value, boolean exclusive)
      {
         this.value = value;
         this.exclusive = exclusive;
      }

      protected String getThreshold()
      {
         return value;
      }

      protected boolean isExclusive()
      {
         return exclusive;
      }
   }

   public class MinMaxConstraint
   {

      private final ConstraintRange minValue;

      private final ConstraintRange maxValue;

      private final ConstraintRange singleValue;

      public MinMaxConstraint(ConstraintRange minValue, ConstraintRange maxValue)
      {
         this.minValue = minValue;
         this.maxValue = maxValue;
         this.singleValue = null;
      }

      public ConstraintRange getSingleValue()
      {
         return singleValue;
      }

      protected ConstraintRange getMax()
      {
         return maxValue;
      }

      protected ConstraintRange getMin()
      {
         return minValue;
      }
   }

}
