/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.query.ExecutableQuery;
import org.exoplatform.services.jcr.impl.core.query.PropertyTypeRegistry;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * <code>AbstractQueryImpl</code> provides a base class for executable queries
 * based on {@link SearchIndex}.
 */
public abstract class AbstractQueryImpl implements ExecutableQuery
{

   /**
    * The session of the user executing this query
    */
   protected final SessionImpl session;

   /**
    * The item manager of the user executing this query
    */
   protected final SessionDataManager itemMgr;

   /**
    * The actual search index
    */
   protected final SearchIndex index;

   /**
    * The property type registry for type lookup.
    */
   protected final PropertyTypeRegistry propReg;

   /**
    * If <code>true</code> the default ordering of the result nodes is in
    * document order.
    */
   private boolean documentOrder = true;

   /**
    * Indicates does we should use case insensitive sorting by field in order by clause or not.
    */
   private boolean caseInsensitiveOrder;

   /**
    * Set&lt;Name>, where Name is a variable name in the query statement.
    */
   private final Set<InternalQName> variableNames = new HashSet<InternalQName>();

   /**
    * Binding of variable name to value. Maps {@link Name} to {@link Value}.
    */
   private final Map<InternalQName, Value> bindValues = new HashMap<InternalQName, Value>();

   /**
    * Creates a new query instance from a query string.
    *
    * @param session the session of the user executing this query.
    * @param itemMgr the item manager of the session executing this query.
    * @param index   the search index.
    * @param propReg the property type registry.
    */
   public AbstractQueryImpl(SessionImpl session, SessionDataManager itemMgr, SearchIndex index, PropertyTypeRegistry propReg)
   {
      this.session = session;
      this.itemMgr = itemMgr;
      this.index = index;
      this.propReg = propReg;
   }

   /**
    * If set <code>true</code> the result nodes will be in document order
    * per default (if no order by clause is specified). If set to
    * <code>false</code> the result nodes are returned in whatever sequence
    * the index has stored the nodes. That sequence is stable over multiple
    * invocations of the same query, but will change when nodes get added or
    * removed from the index.
    * <br>
    * The default value for this property is <code>true</code>.
    * @return the current value of this property.
    */
   public boolean getRespectDocumentOrder()
   {
      return documentOrder;
   }

   /**
    * Sets a new value for this property.
    *
    * @param documentOrder if <code>true</code> the result nodes are in
    * document order per default.
    *
    * @see #getRespectDocumentOrder()
    */
   public void setRespectDocumentOrder(boolean documentOrder)
   {
      this.documentOrder = documentOrder;
   }

   /**
    * Setter for {@link #caseInsensitiveOrder} field.
    */
   public void setCaseInsensitiveOrder(boolean caseInsensitiveOrder)
   {
      this.caseInsensitiveOrder = caseInsensitiveOrder;
   }

   /**
    * Getter for {@link #caseInsensitiveOrder} field.
    */
   public boolean isCaseInsensitiveOrder()
   {
      return caseInsensitiveOrder;
   }

   /**
    * Binds the given <code>value</code> to the variable named
    * <code>varName</code>.
    *
    * @param varName name of variable in query
    * @param value   value to bind
    * @throws IllegalArgumentException if <code>varName</code> is not a valid
    *                                  variable in this query.
    * @throws RepositoryException      if an error occurs.
    */
   public void bindValue(InternalQName varName, Value value) throws IllegalArgumentException, RepositoryException
   {
      if (!variableNames.contains(varName))
      {
         throw new IllegalArgumentException("not a valid variable in this query");
      }
      else
      {
         bindValues.put(varName, value);
      }
   }

   /**
    * Adds a name to the set of variables.
    *
    * @param varName the name of the variable.
    */
   protected void addVariableName(InternalQName varName)
   {
      variableNames.add(varName);
   }

   /**
    * @return an unmodifiable map, which contains the variable names and their
    *         respective value.
    */
   protected Map<InternalQName, Value> getBindVariableValues()
   {
      return Collections.unmodifiableMap(bindValues);
   }

   /**
    * Returns <code>true</code> if this query node needs items under
    * /jcr:system to be queried.
    *
    * @return <code>true</code> if this query node needs content under
    *         /jcr:system to be queried; <code>false</code> otherwise.
    */
   public abstract boolean needsSystemTree();
}
