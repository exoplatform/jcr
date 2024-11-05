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

/**
 * Implement visitor pattern for visiting a package name from left to right.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
abstract class PackageNameVisitor
{

   /**
    * Visit a name from its dotted string representation.
    *
    * @param packageName the package name
    */
   public void visit(String packageName)
   {
      for (String name : packageName.split("\\."))
      {
         accept(name);
      }
   }

   /**
    * Visit a name from its groovy expression AST representation.
    *
    * <p>
    * The AST structure we are recognizing in that method is:
    * <ul>
    * <li>Expression -> VariableExpression</li>
    * <li>Expression -> PropertyExpression[ObjectExpression,ConstantExpression]
    * </ul>
    * </p>
    *
    * <p>A VariableExpression is considered as a terminal that is the top left term. Its name
    * value is obtained by calling the <code>getName()</code> method.</p>
    *
    * <p>A PropertyExpression is composed of an ObjectExpression and a Property. The ObjectExpression is evaluated
    * recursively as an Expression. The Property must be a ConstantExpression and the name value is obtained
    * by calling the <code>getValue()</code> method.</p>
    *
    * @param expr the expression AST
    */
   public void visit(Expression expr)
   {
      if (expr instanceof VariableExpression)
      {
         String name = ((VariableExpression)expr).getName();
         accept(name);
      }
      else if (expr instanceof PropertyExpression)
      {
         PropertyExpression pe = (PropertyExpression)expr;
         visit(pe.getObjectExpression());
         ConstantExpression pe_ce = (ConstantExpression)pe.getProperty();
         accept((String)pe_ce.getValue());
      }
      else
      {
         throw new UnsupportedOperationException("Do not support expression of type" + expr + " with text "
            + expr.getText());
      }
   }

   /**
    * Implement this method to receive a name callback.
    *
    * @param name the name
    */
   protected abstract void accept(String name);

}
