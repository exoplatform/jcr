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

import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * Created by The eXo Platform SAS. 
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ValueConstraintsValidator.java 12171 2008-03-20 15:37:28Z ksm $
 */
public class ValueConstraintsValidator
{

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ValueConstraintsValidator");

   protected final static String DEFAULT_THRESHOLD = "";

   protected final String[] constraints;

   public ValueConstraintsValidator(String[] constraints)
   {
      this.constraints = constraints;
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
      {
         throw new ConstraintViolationException("Value constraint '" + constraint
            + "' is invalid accrding the JSR-170 spec.");
      }

      boolean exclusive = false;

      if (parts[0].startsWith("("))
      {
         exclusive = true;
      }
      else if (parts[0].startsWith("["))
      {
         exclusive = false;
      }
      else
      {
         throw new ConstraintViolationException("Value constraint '" + constraint
            + "' min exclusion rule is unefined accrding the JSR-170 spec.");
      }

      ConstraintRange minValue =
         new ConstraintRange(parts[0].length() > 1 ? parts[0].substring(1) : DEFAULT_THRESHOLD, exclusive);

      if (parts[1].endsWith(")"))
      {
         exclusive = true;
      }
      else if (parts[1].endsWith("]"))
      {
         exclusive = false;
      }
      else
      {
         throw new ConstraintViolationException("Value constraint '" + constraint
            + "' max exclusion rule is unefined accrding the JSR-170 spec.");
      }

      ConstraintRange maxValue =
         new ConstraintRange(parts[1].length() > 1 ? parts[1].substring(0, parts[1].length() - 1) : DEFAULT_THRESHOLD,
            exclusive);

      return new MinMaxConstraint(minValue, maxValue);
   }

   /**
    * Validate this matcher constraints for a validity to a given type.
    *   
    * @param type int, property type
    * @throws ValueFormatException if the constraint (at least one) incompatible with the given type
    */
   public void validateFor(final int type) throws ValueFormatException
   {
      if (constraints == null || constraints.length <= 0)
      {
         return;
      }

      if (type == PropertyType.DATE)
      {
         for (String constrString : constraints)
         {
            try
            {
               MinMaxConstraint constraint = parseAsMinMax(constrString);
               if (constraint.getMin().getThreshold().length() > 0)
               {
                  JCRDateFormat.parse(constraint.getMin().getThreshold());
               }

               if (constraint.getMax().getThreshold().length() > 0)
               {
                  JCRDateFormat.parse(constraint.getMax().getThreshold());
               }
            }
            catch (ValueFormatException e)
            {
               throw new ValueFormatException("Wrong DATE constraint format " + constrString, e);
            }
            catch (ConstraintViolationException e)
            {
               throw new ValueFormatException("Wrong DATE constraint format " + constrString, e);
            }
         }
      }
      else if (type == PropertyType.DOUBLE)
      {
         for (String constrString : constraints)
         {
            try
            {
               MinMaxConstraint constraint = parseAsMinMax(constrString);
               if (constraint.getMin().getThreshold().length() > 0)
               {
                  Double.parseDouble(constraint.getMin().getThreshold());
               }
               if (constraint.getMax().getThreshold().length() > 0)
               {
                  Double.parseDouble(constraint.getMax().getThreshold());
               }
            }
            catch (NumberFormatException e)
            {
               throw new ValueFormatException("Wrong DOUBLE constraint format " + constrString, e);
            }
            catch (ConstraintViolationException e)
            {
               throw new ValueFormatException("Wrong DOUBLE constraint format " + constrString, e);
            }
         }
      }
      else if (type == PropertyType.LONG || type == PropertyType.BINARY)
      {
         for (String constrString : constraints)
         {
            try
            {
               MinMaxConstraint constraint = parseAsMinMax(constrString);
               if (constraint.getMin().getThreshold().length() > 0)
               {
                  Long.parseLong(constraint.getMin().getThreshold());
               }
               if (constraint.getMax().getThreshold().length() > 0)
               {
                  Long.parseLong(constraint.getMax().getThreshold());
               }
            }
            catch (NumberFormatException e)
            {
               throw new ValueFormatException("Wrong LONG (or BINARY size) constraint format " + constrString, e);
            }
            catch (ConstraintViolationException e)
            {
               throw new ValueFormatException("Wrong LONG (or BINARY size) constraint format " + constrString, e);
            }
         }
      }
      else if (type == PropertyType.BOOLEAN)
      {
         for (String constrString : constraints)
         {
            if (!constrString.equalsIgnoreCase("true") && !constrString.equalsIgnoreCase("false"))
            {
               throw new ValueFormatException("Wrong BOOLEAN constraint format: " + constrString);
            }
         }
      }
   }

   protected class ConstraintRange
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

   protected class MinMaxConstraint
   {
      private final ConstraintRange minValue;

      private final ConstraintRange maxValue;

      public MinMaxConstraint(ConstraintRange minValue, ConstraintRange maxValue)
      {
         this.minValue = minValue;
         this.maxValue = maxValue;
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
