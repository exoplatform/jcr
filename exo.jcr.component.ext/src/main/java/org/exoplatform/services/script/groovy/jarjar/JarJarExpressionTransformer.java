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

import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.control.SourceUnit;

import java.util.List;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
class JarJarExpressionTransformer extends ClassCodeExpressionTransformer
{

   /** . */
   private final SourceUnit source;

   /** . */
   private final Package root;

   JarJarExpressionTransformer(SourceUnit source, Package root)
   {
      this.source = source;
      this.root = root;
   }

   protected SourceUnit getSourceUnit()
   {
      return source;
   }

   @Override
   public Expression transform(Expression expression)
   {

      // Handle method call expressions
      if (expression instanceof MethodCallExpression)
      {
         MethodCallExpression mce = (MethodCallExpression)expression;

         //
         if (mce.getObjectExpression() instanceof PropertyExpression)
         {
            PropertyExpression mce_pe = (PropertyExpression)mce.getObjectExpression();

            //
            Expression expr = bilto(mce_pe);
            if (expr != null)
            {
               mce.setObjectExpression(expr);
            }
         }
      }
      return super.transform(expression);
   }

   private Expression bilto(Expression exp)
   {

      String s = exp.getText();

      List<String> ss = root.map2(exp);

      if (ss != null)
      {
         System.out.println(s + " -> " + ss); //NOSONAR
         return createExpr(ss);
      }

      return null;
   }

   private static Expression createExpr(List<String> packageName)
   {
      if (packageName.isEmpty())
      {
         throw new IllegalStateException("Root does not have prefix");
      }

      //
      if (packageName.size() == 1)
      {
         String name = packageName.get(0);
         ClassNode objectCN = getClassNode(Object.class);
         return new VariableExpression(name, objectCN);
      }
      else
      {
         Expression left = createExpr(packageName.subList(0, packageName.size() - 1));
         ConstantExpression right = new ConstantExpression(packageName.get(packageName.size() - 1));
         return new PropertyExpression(left, right);
      }
   }

   private static ClassNode getClassNode(Class<?> clazz)
   {
      return ClassHelper.make(clazz);
   }
}
