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
package org.exoplatform.services.script.groovy.jarjar;

import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents a package.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
class Package
{

   /** . */
   private final Package parent;

   /** . */
   private final Map<String, Package> subMappers;

   /** . */
   private List<String> target;

   public Package()
   {
      this.parent = null;
      this.target = null;
      this.subMappers = new HashMap<String, Package>();
   }

   private Package(Package parent)
   {
      this.parent = parent;
      this.target = null;
      this.subMappers = new HashMap<String, Package>();
   }

   public void add(List<String> source, List<String> target)
   {
      add(source.iterator(), target);
   }

   public Package getSubPackage(String name)
   {
      return subMappers.get(name);
   }

   private void add(Iterator<String> source, List<String> target)
   {
      if (source.hasNext())
      {
         String name = source.next();
         Package sub = subMappers.get(name);
         if (sub == null)
         {
            sub = new Package(this);
            subMappers.put(name, sub);
         }
         sub.add(source, target);
      }
      else
      {
         if (this.target != null)
         {
            throw new IllegalStateException();
         }
         this.target = Collections.unmodifiableList(target);
      }
   }

   public List<String> getTarget()
   {
      return target;
   }

   public String getName()
   {
      if (parent != null)
      {
         for (Map.Entry<String, Package> entry : parent.subMappers.entrySet())
         {
            if (entry.getValue() == this)
            {
               return entry.getKey();
            }
         }
      }
      return null;
   }

   public Package map(Expression exp)
   {
      Package mapmap = null;
      if (exp instanceof VariableExpression)
      {
         String name = ((VariableExpression)exp).getName();
         mapmap = getSubPackage(name);
      }
      else if (exp instanceof PropertyExpression)
      {
         PropertyExpression pe = (PropertyExpression)exp;
         if (pe.getObjectExpression() instanceof VariableExpression)
         {
            mapmap = map(pe.getObjectExpression());
         }
         else if (pe.getObjectExpression() instanceof PropertyExpression)
         {
            PropertyExpression pe_pe = (PropertyExpression)pe.getObjectExpression();
            mapmap = map(pe_pe);
         }
         else
         {
            //
         }
         if (mapmap != null)
         {
            ConstantExpression pe_ce = (ConstantExpression)pe.getProperty();
            if (pe_ce.getType().getName().equals("java.lang.String"))
            {
               Package sub_sub = mapmap.getSubPackage((String)pe_ce.getValue());
               if (sub_sub != null)
               {
                  mapmap = sub_sub;
               }
            }
         }
      }
      return mapmap;
   }

   private static class BiltoVisitor extends PackageNameVisitor
   {

      Package ref;

      List<String> bilto = new LinkedList<String>();

      boolean repackaged = false;

      private BiltoVisitor(Package mapper)
      {
         this.ref = mapper;
      }

      protected void accept(String name)
      {
         if (ref != null)
         {
            Package sub = ref.getSubPackage(name);
            if (sub != null)
            {
               List<String> target = sub.target;
               if (target != null)
               {
                  // Use the repackaging rule
                  bilto.clear();
                  bilto.addAll(target);
                  repackaged = true;
               }
               else
               {
                  // Add name
                  bilto.add(name);
               }
               ref = sub;
            }
            else
            {
               // Add name and set null as marker
               bilto.add(name);
               ref = null;
            }
         }
         else
         {
            // Add name
            bilto.add(name);
         }
      }
   }

   public List<String> map2(Expression expr)
   {
      BiltoVisitor visitor = new BiltoVisitor(this);
      visitor.visit(expr);
      return visitor.repackaged ? visitor.bilto : null;
   }

   public List<String> map2(String expr)
   {
      BiltoVisitor visitor = new BiltoVisitor(this);
      visitor.visit(expr);
      return visitor.repackaged ? visitor.bilto : null;
   }

   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder();
      toString(sb);
      return sb.toString();
   }

   private void toString(StringBuilder sb)
   {
      if (parent != null)
      {
         parent.toString(sb);
         String name = getName();
         sb.append(".").append(name);
      }
   }
}
