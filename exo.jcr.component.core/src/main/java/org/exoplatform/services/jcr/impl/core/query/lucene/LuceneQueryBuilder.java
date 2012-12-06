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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.query.AndQueryNode;
import org.exoplatform.services.jcr.impl.core.query.DefaultQueryNodeVisitor;
import org.exoplatform.services.jcr.impl.core.query.DerefQueryNode;
import org.exoplatform.services.jcr.impl.core.query.ExactQueryNode;
import org.exoplatform.services.jcr.impl.core.query.LocationStepQueryNode;
import org.exoplatform.services.jcr.impl.core.query.NodeTypeQueryNode;
import org.exoplatform.services.jcr.impl.core.query.NotQueryNode;
import org.exoplatform.services.jcr.impl.core.query.OrQueryNode;
import org.exoplatform.services.jcr.impl.core.query.OrderQueryNode;
import org.exoplatform.services.jcr.impl.core.query.PathQueryNode;
import org.exoplatform.services.jcr.impl.core.query.PropertyFunctionQueryNode;
import org.exoplatform.services.jcr.impl.core.query.PropertyTypeRegistry;
import org.exoplatform.services.jcr.impl.core.query.QueryConstants;
import org.exoplatform.services.jcr.impl.core.query.QueryNode;
import org.exoplatform.services.jcr.impl.core.query.QueryNodeVisitor;
import org.exoplatform.services.jcr.impl.core.query.QueryRootNode;
import org.exoplatform.services.jcr.impl.core.query.RelationQueryNode;
import org.exoplatform.services.jcr.impl.core.query.TextsearchQueryNode;
import org.exoplatform.services.jcr.impl.util.ISO9075;
import org.exoplatform.services.jcr.impl.xml.XMLChar;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;

/**
 * Implements a query builder that takes an abstract query tree and creates a
 * lucene {@link org.apache.lucene.search.Query} tree that can be executed on an
 * index. todo introduce a node type hierarchy for efficient translation of
 * NodeTypeQueryNode
 */
public class LuceneQueryBuilder implements QueryNodeVisitor
{
   /**
    * Namespace URI for xpath functions
    */
   private static final String NS_FN_PREFIX = "fn";

   public static final String NS_FN_URI = "http://www.w3.org/2005/xpath-functions";

   /**
    * Deprecated namespace URI for xpath functions
    */
   private static final String NS_FN_OLD_PREFIX = "fn_old";

   public static final String NS_FN_OLD_URI = "http://www.w3.org/2004/10/xpath-functions";

   /**
    * Namespace URI for XML schema
    */
   private static final String NS_XS_PREFIX = "xs";

   public static final String NS_XS_URI = "http://www.w3.org/2001/XMLSchema";

   /**
    * Logger for this class
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.LuceneQueryBuilder");

   /**
    * Root node of the abstract query tree
    */
   private final QueryRootNode root;

   /**
    * Session of the user executing this query
    */
   private final SessionImpl session;

   /**
    * The shared item state manager of the workspace.
    */
   private final ItemDataConsumer sharedItemMgr;

   // /**
   // * A hierarchy manager based on {@link #sharedItemMgr} to resolve paths.
   // */
   // private final HierarchyManager hmgr;

   /**
    * Namespace mappings to internal prefixes
    */
   private final NamespaceMappings nsMappings;

   /**
    * Name and Path resolver
    */
   private final LocationFactory resolver;

   /**
    * The analyzer instance to use for contains function query parsing
    */
   private final Analyzer analyzer;

   /**
    * The property type registry.
    */
   private final PropertyTypeRegistry propRegistry;

   /**
    * The synonym provider or <code>null</code> if none is configured.
    */
   private final SynonymProvider synonymProvider;

   /**
    * Wether the index format is new or old.
    */
   private final IndexFormatVersion indexFormatVersion;

   /**
    * Exceptions thrown during tree translation
    */
   private final List exceptions = new ArrayList();

   private final NodeTypeDataManager nodeTypeDataManager;

   private final VirtualTableResolver<Query> virtualTableResolver;

   private IndexingConfiguration indexConfig;

   /**
    * Creates a new <code>LuceneQueryBuilder</code> instance.
    * 
    * @param root
    *            the root node of the abstract query tree.
    * @param session
    *            of the user executing this query.
    * @param sharedItemMgr
    *            the shared item state manager of the workspace.
    * @param hmgr
    *            a hierarchy manager based on sharedItemMgr.
    * @param nsMappings
    *            namespace resolver for internal prefixes.
    * @param analyzer
    *            for parsing the query statement of the contains function.
    * @param propReg
    *            the property type registry.
    * @param synonymProvider
    *            the synonym provider or <code>null</code> if node is
    *            configured.
    * @param indexFormatVersion
    *            the index format version for the lucene query.
    * @param virtualTableResolver
    * @throws RepositoryException
    */
   private LuceneQueryBuilder(QueryRootNode root, SessionImpl session,
      ItemDataConsumer sharedItemMgr,
      // HierarchyManager hmgr,
      NamespaceMappings nsMappings, Analyzer analyzer, PropertyTypeRegistry propReg, SynonymProvider synonymProvider,
      IndexFormatVersion indexFormatVersion, VirtualTableResolver<Query> virtualTableResolver,
      IndexingConfiguration indexConfig) throws RepositoryException
   {
      this.root = root;
      this.session = session;
      this.sharedItemMgr = sharedItemMgr;
      // this.hmgr = hmgr;
      this.nsMappings = nsMappings;
      this.analyzer = analyzer;
      this.propRegistry = propReg;
      this.synonymProvider = synonymProvider;
      this.indexFormatVersion = indexFormatVersion;
      this.virtualTableResolver = virtualTableResolver;
      this.nodeTypeDataManager = session.getWorkspace().getNodeTypesHolder();
      this.resolver = new LocationFactory(nsMappings);
      this.indexConfig = indexConfig;
   }

   /**
    * Creates a lucene {@link org.apache.lucene.search.Query} tree from an
    * abstract query tree.
    * 
    * @param root
    *            the root node of the abstract query tree.
    * @param session
    *            of the user executing the query.
    * @param sharedItemMgr
    *            the shared item state manager of the workspace.
    * @param nsMappings
    *            namespace resolver for internal prefixes.
    * @param analyzer
    *            for parsing the query statement of the contains function.
    * @param propReg
    *            the property type registry to lookup type information.
    * @param synonymProvider
    *            the synonym provider or <code>null</code> if node is
    *            configured.
    * @param indexFormatVersion
    *            the index format version to be used
    * @return the lucene query tree.
    * @throws RepositoryException
    *             if an error occurs during the translation.
    */
   public static Query createQuery(QueryRootNode root, SessionImpl session, ItemDataConsumer sharedItemMgr,
      NamespaceMappings nsMappings, Analyzer analyzer, PropertyTypeRegistry propReg, SynonymProvider synonymProvider,
      IndexFormatVersion indexFormatVersion, VirtualTableResolver<Query> virtualTableResolver,
      IndexingConfiguration indexConfig) throws RepositoryException
   {

      LuceneQueryBuilder builder =
         new LuceneQueryBuilder(root, session, sharedItemMgr, nsMappings, analyzer, propReg, synonymProvider,
            indexFormatVersion, virtualTableResolver, indexConfig);

      Query q = builder.createLuceneQuery();
      if (builder.exceptions.size() > 0)
      {
         StringBuffer msg = new StringBuffer();
         for (Iterator it = builder.exceptions.iterator(); it.hasNext();)
         {
            msg.append(it.next().toString()).append('\n');
         }
         throw new RepositoryException("Exception building query: " + msg.toString());
      }
      return q;
   }

   /**
    * Starts the tree traversal and returns the lucene
    * {@link org.apache.lucene.search.Query}.
    * 
    * @return the lucene <code>Query</code>.
    * @throws RepositoryException
    */
   private Query createLuceneQuery() throws RepositoryException
   {
      return (Query)root.accept(this, null);
   }

   // ---------------------< QueryNodeVisitor interface
   // >-----------------------

   public Object visit(QueryRootNode node, Object data) throws RepositoryException
   {
      BooleanQuery root = new BooleanQuery();

      Query wrapped = root;
      if (node.getLocationNode() != null)
      {
         wrapped = (Query)node.getLocationNode().accept(this, root);
      }

      return wrapped;
   }

   public Object visit(OrQueryNode node, Object data) throws RepositoryException
   {
      BooleanQuery orQuery = new BooleanQuery();
      Object[] result = node.acceptOperands(this, null);
      for (int i = 0; i < result.length; i++)
      {
         Query operand = (Query)result[i];
         orQuery.add(operand, Occur.SHOULD);
      }
      return orQuery;
   }

   public Object visit(AndQueryNode node, Object data) throws RepositoryException
   {
      Object[] result = node.acceptOperands(this, null);
      if (result.length == 0)
      {
         return null;
      }
      BooleanQuery andQuery = new BooleanQuery();
      for (int i = 0; i < result.length; i++)
      {
         Query operand = (Query)result[i];
         andQuery.add(operand, Occur.MUST);
      }
      return andQuery;
   }

   public Object visit(NotQueryNode node, Object data) throws RepositoryException
   {
      Object[] result = node.acceptOperands(this, null);
      if (result.length == 0)
      {
         return data;
      }
      // join the results
      BooleanQuery b = new BooleanQuery();
      for (int i = 0; i < result.length; i++)
      {
         b.add((Query)result[i], Occur.SHOULD);
      }
      // negate
      return new NotQuery(b);
   }

   public Object visit(ExactQueryNode node, Object data)
   {
      String field = "";
      String value = "";
      try
      {
         field = resolver.createJCRName(node.getPropertyName()).getAsString();
         value = resolver.createJCRName(node.getValue()).getAsString();
      }
      catch (RepositoryException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }
      return new JcrTermQuery(new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(field, value)));
   }

   public Object visit(NodeTypeQueryNode node, Object data)
   {

      try
      {
         return virtualTableResolver.resolve(node.getValue(), true);
      }
      catch (InvalidQueryException e1)
      {
         exceptions.add(e1);
      }
      catch (RepositoryException e1)
      {
         exceptions.add(e1);
      }
      return new BooleanQuery();
      // // (result)
      // List terms = new ArrayList();
      // try {
      // String mixinTypesField = resolver.createJCRName(
      // Constants.JCR_MIXINTYPES).getAsString();
      // String primaryTypeField = resolver.createJCRName(
      // Constants.JCR_PRIMARYTYPE).getAsString();
      //
      // NodeTypeData base = nodeTypeDataManager.findNodeType(node
      // .getValue());
      //
      // if (base.isMixin()) {
      // // search for nodes where jcr:mixinTypes is set to this mixin
      // Term t = new Term(FieldNames.PROPERTIES, FieldNames
      // .createNamedValue(mixinTypesField, resolver
      // .createJCRName(node.getValue()).getAsString()));
      // terms.add(t);
      // } else {
      // // search for nodes where jcr:primaryType is set to this type
      // Term t = new Term(FieldNames.PROPERTIES, FieldNames
      // .createNamedValue(primaryTypeField, resolver
      // .createJCRName(node.getValue()).getAsString()));
      // terms.add(t);
      // }
      //
      // // now search for all node types that are derived from base
      // Collection<NodeTypeData> allTypes = nodeTypeDataManager
      // .getAllNodeTypes();
      // for (NodeTypeData nodeTypeData : allTypes) {
      // InternalQName[] superTypes = nodeTypeData
      // .getDeclaredSupertypeNames();
      // if (Arrays.asList(superTypes).contains(base.getName())) {
      // String ntName = nsMappings.translateName(nodeTypeData
      // .getName());
      // Term t;
      // if (nodeTypeData.isMixin()) {
      // // search on jcr:mixinTypes
      // t = new Term(FieldNames.PROPERTIES, FieldNames
      // .createNamedValue(mixinTypesField, ntName));
      // } else {
      // // search on jcr:primaryType
      // t = new Term(FieldNames.PROPERTIES, FieldNames
      // .createNamedValue(primaryTypeField, ntName));
      // }
      // terms.add(t);
      // }
      // }
      // } catch (IllegalNameException e) {
      // exceptions.add(e);
      // } catch (RepositoryException e) {
      // exceptions.add(e);
      // }
      // if (terms.size() == 0) {
      // // exception occured
      // return new BooleanQuery();
      // } else if (terms.size() == 1) {
      // return new JackrabbitTermQuery((Term) terms.get(0));
      // } else {
      // BooleanQuery b = new BooleanQuery();
      // for (Iterator it = terms.iterator(); it.hasNext();) {
      // b.add(new JackrabbitTermQuery((Term) it.next()), Occur.SHOULD);
      // }
      // return b;
      // }
   }

   public Object visit(TextsearchQueryNode node, Object data)
   {
      try
      {
         QPath relPath = node.getRelativePath();
         String fieldname;
         if (relPath == null || !node.getReferencesProperty())
         {
            // fulltext on node
            fieldname = FieldNames.FULLTEXT;
         }
         else
         {
            // final path element is a property name

            fieldname = resolver.createJCRName(relPath.getName()).getAsString();
            int idx = fieldname.indexOf(':');
            fieldname = fieldname.substring(0, idx + 1) + FieldNames.FULLTEXT_PREFIX + fieldname.substring(idx + 1);

         }
         QueryParser parser = new JcrQueryParser(fieldname, analyzer, synonymProvider);
         Query context = parser.parse(node.getQuery());
         if (relPath != null && (!node.getReferencesProperty() || relPath.getEntries().length > 1))
         {
            // text search on some child axis
            QPathEntry[] elements = relPath.getEntries();
            for (int i = elements.length - 1; i >= 0; i--)
            {
               QPathEntry name = null;
               if (!elements[i].equals(RelationQueryNode.STAR_NAME_TEST))
               {
                  name = elements[i];
               }
               // join text search with name test
               // if path references property that's elements.length - 2
               // if path references node that's elements.length - 1
               if (name != null
                        && ((node.getReferencesProperty() && i == elements.length - 2) || (!node
                                 .getReferencesProperty() && i == elements.length - 1)))
               {
                  Query q = new NameQuery(name, indexFormatVersion, nsMappings);
                  BooleanQuery and = new BooleanQuery();
                  and.add(q, Occur.MUST);
                  and.add(context, Occur.MUST);
                  context = and;
               }
               else if ((node.getReferencesProperty() && i < elements.length - 2)
                  || (!node.getReferencesProperty() && i < elements.length - 1))
               {
                  // otherwise do a parent axis step
                  context = new ParentAxisQuery(context, name, indexFormatVersion, nsMappings);
               }
            }
            // finally select parent
            context = new ParentAxisQuery(context, null, indexFormatVersion, nsMappings);
         }
         return context;
      }
      catch (NamespaceException e)
      {
         exceptions.add(e);
      }
      catch (ParseException e)
      {
         exceptions.add(e);
      }
      catch (RepositoryException e)
      {
         LOG.error(e.getLocalizedMessage(), e);
      }
      return null;
   }

   public Object visit(PathQueryNode node, Object data) throws RepositoryException
   {
      Query context = null;
      LocationStepQueryNode[] steps = node.getPathSteps();
      if (steps.length > 0)
      {
         if (node.isAbsolute() && !steps[0].getIncludeDescendants())
         {
            // eat up first step
            InternalQName nameTest = steps[0].getNameTest();
            if (nameTest == null)
            {
               // this is equivalent to the root node
               context = new JcrTermQuery(new Term(FieldNames.UUID, Constants.ROOT_UUID));
            }
            else if (nameTest.getName().length() == 0)
            {
               // root node
               context = new JcrTermQuery(new Term(FieldNames.UUID, Constants.ROOT_UUID));
            }
            else
            {
               // then this is a node != the root node
               // will never match anything!
               BooleanQuery and = new BooleanQuery();
               and.add(new JcrTermQuery(new Term(FieldNames.UUID, Constants.ROOT_UUID)), Occur.MUST);
               and.add(new NameQuery(nameTest, indexFormatVersion, nsMappings), Occur.MUST);
               context = and;
            }
            LocationStepQueryNode[] tmp = new LocationStepQueryNode[steps.length - 1];
            System.arraycopy(steps, 1, tmp, 0, steps.length - 1);
            steps = tmp;
         }
         else
         {
            // path is 1) relative or 2) descendant-or-self
            // use root node as context
            context = new JcrTermQuery(new Term(FieldNames.UUID, Constants.ROOT_UUID));
         }
      }
      else
      {
         exceptions.add(new InvalidQueryException("Number of location steps must be > 0"));
      }
      // loop over steps
      for (int i = 0; i < steps.length; i++)
      {
         context = (Query)steps[i].accept(this, context);
      }
      if (data instanceof BooleanQuery)
      {
         BooleanQuery constraint = (BooleanQuery)data;
         if (constraint.getClauses().length > 0)
         {
            constraint.add(context, Occur.MUST);
            context = constraint;
         }
      }
      return context;
   }

   public Object visit(LocationStepQueryNode node, Object data) throws RepositoryException
   {
      Query context = (Query)data;
      BooleanQuery andQuery = new BooleanQuery();

      if (context == null)
      {
         exceptions.add(new IllegalArgumentException("Unsupported query"));
      }

      // predicate on step?
      Object[] predicates = node.acceptOperands(this, data);
      for (int i = 0; i < predicates.length; i++)
      {
         andQuery.add((Query)predicates[i], Occur.MUST);
      }

      // check for position predicate
      QueryNode[] pred = node.getPredicates();
      for (int i = 0; i < pred.length; i++)
      {
         if (pred[i].getType() == QueryNode.TYPE_RELATION)
         {
            RelationQueryNode pos = (RelationQueryNode)pred[i];
            if (pos.getValueType() == QueryConstants.TYPE_POSITION)
            {
               node.setIndex(pos.getPositionValue());
            }
         }
      }

      NameQuery nameTest = null;
      if (node.getNameTest() != null)
      {
         nameTest = new NameQuery(node.getNameTest(), indexFormatVersion, nsMappings);
      }

      if (node.getIncludeDescendants())
      {
         if (nameTest != null)
         {
            andQuery.add(new DescendantSelfAxisQuery(context, nameTest, false, indexConfig), Occur.MUST);
         }
         else
         {
            // descendant-or-self with nametest=*
            if (predicates.length > 0)
            {
               // if we have a predicate attached, the condition acts as
               // the sub query.

               // only use descendant axis if path is not //*
               // otherwise the query for the predicate can be used itself
               PathQueryNode pathNode = (PathQueryNode)node.getParent();
               if (pathNode.getPathSteps()[0] != node)
               {
                  Query subQuery = new DescendantSelfAxisQuery(context, andQuery, false, indexConfig);
                  andQuery = new BooleanQuery();
                  andQuery.add(subQuery, Occur.MUST);
               }
            }
            else
            {
               // todo this will traverse the whole index, optimize!
               // only use descendant axis if path is not //*
               PathQueryNode pathNode = (PathQueryNode)node.getParent();
               if (pathNode.getPathSteps()[0] != node)
               {
                  if (node.getIndex() == LocationStepQueryNode.NONE)
                  {
                     context = new DescendantSelfAxisQuery(context, false, indexConfig);
                     andQuery.add(context, Occur.MUST);
                  }
                  else
                  {
                     context = new DescendantSelfAxisQuery(context, true, indexConfig);
                     andQuery.add(new ChildAxisQuery(sharedItemMgr, context, null, node.getIndex(), indexFormatVersion,
                        nsMappings, indexConfig), Occur.MUST);
                  }
               }
               else
               {
                  andQuery.add(new MatchAllDocsQuery(indexConfig), Occur.MUST);
               }
            }
         }
      }
      else
      {
         // name test
         if (nameTest != null)
         {
            andQuery.add(new ChildAxisQuery(sharedItemMgr, context, nameTest.getName(), node.getIndex(),
               indexFormatVersion, nsMappings, indexConfig), Occur.MUST);
         }
         else
         {
            // select child nodes
            andQuery.add(new ChildAxisQuery(sharedItemMgr, context, null, node.getIndex(), indexFormatVersion,
               nsMappings, indexConfig), Occur.MUST);
         }
      }

      return andQuery;
   }

   public Object visit(DerefQueryNode node, Object data) throws RepositoryException
   {
      Query context = (Query)data;
      if (context == null)
      {
         exceptions.add(new IllegalArgumentException("Unsupported query"));
      }

      try
      {
         String refProperty = resolver.createJCRName(node.getRefProperty()).getAsString();

         if (node.getIncludeDescendants())
         {
            Query refPropQuery = Util.createMatchAllQuery(refProperty, indexFormatVersion);
            context = new DescendantSelfAxisQuery(context, refPropQuery, false, indexConfig);
         }

         context = new DerefQuery(context, refProperty, node.getNameTest(), indexFormatVersion, nsMappings);

         // attach predicates
         Object[] predicates = node.acceptOperands(this, data);
         if (predicates.length > 0)
         {
            BooleanQuery andQuery = new BooleanQuery();
            for (int i = 0; i < predicates.length; i++)
            {
               andQuery.add((Query)predicates[i], Occur.MUST);
            }
            andQuery.add(context, Occur.MUST);
            context = andQuery;
         }

      }
      catch (NamespaceException e)
      {
         // should never happen
         exceptions.add(e);
      }

      return context;
   }

   public Object visit(RelationQueryNode node, Object data) throws RepositoryException
   {
      Query query;
      String[] stringValues = new String[1];
      switch (node.getValueType())
      {
         case 0 :
            // not set: either IS NULL or IS NOT NULL
            break;
         case QueryConstants.TYPE_DATE :
            stringValues[0] = DateField.dateToString(node.getDateValue());
            break;
         case QueryConstants.TYPE_DOUBLE :
            stringValues[0] = DoubleField.doubleToString(node.getDoubleValue());
            break;
         case QueryConstants.TYPE_LONG :
            stringValues[0] = LongField.longToString(node.getLongValue());
            break;
         case QueryConstants.TYPE_STRING :
            if (node.getOperation() == QueryConstants.OPERATION_EQ_GENERAL
               || node.getOperation() == QueryConstants.OPERATION_EQ_VALUE
               || node.getOperation() == QueryConstants.OPERATION_NE_GENERAL
               || node.getOperation() == QueryConstants.OPERATION_NE_VALUE)
            {
               // only use coercing on non-range operations
               InternalQName propertyName = node.getRelativePath().getName();
               stringValues = getStringValues(propertyName, node.getStringValue());
            }
            else
            {
               stringValues[0] = node.getStringValue();
            }
            break;
         case QueryConstants.TYPE_POSITION :
            // ignore position. is handled in the location step
            return null;
         default :
            throw new IllegalArgumentException("Unknown relation type: " + node.getValueType());
      }

      if (node.getRelativePath() == null && node.getOperation() != QueryConstants.OPERATION_SIMILAR
         && node.getOperation() != QueryConstants.OPERATION_SPELLCHECK)
      {
         exceptions.add(new InvalidQueryException("@* not supported in predicate"));
         return data;
      }

      // get property transformation
      final int[] transform = new int[]{TransformConstants.TRANSFORM_NONE};
      node.acceptOperands(new DefaultQueryNodeVisitor()
      {
         @Override
         public Object visit(PropertyFunctionQueryNode node, Object data)
         {
            if (node.getFunctionName().equals(PropertyFunctionQueryNode.LOWER_CASE))
            {
               transform[0] = TransformConstants.TRANSFORM_LOWER_CASE;
            }
            else if (node.getFunctionName().equals(PropertyFunctionQueryNode.UPPER_CASE))
            {
               transform[0] = TransformConstants.TRANSFORM_UPPER_CASE;
            }
            return data;
         }
      }, null);

      QPath relPath = node.getRelativePath();

      InternalQName propName;

      if (node.getOperation() == QueryConstants.OPERATION_SIMILAR)
      {
         // this is a bit ugly:
         // add the name of a dummy property because relPath actually
         // references a property. whereas the relPath of the similar
         // operation references a node
         //relPath = QPath.makeChildPath(relPath, Constants.JCR_PRIMARYTYPE);
         propName = Constants.JCR_PRIMARYTYPE;
      }
      else
      {
         propName = relPath.getName();
      }

      String field = "";
      try
      {
         field = resolver.createJCRName(propName).getAsString();
      }
      catch (NamespaceException e)
      {
         // should never happen
         exceptions.add(e);
      }

      // support for fn:name()
      //InternalQName propName = relPath.getName();
      if (propName.getNamespace().equals(NS_FN_URI) && propName.getName().equals("name()"))
      {
         if (node.getValueType() != QueryConstants.TYPE_STRING)
         {
            exceptions.add(new InvalidQueryException("Name function can "
               + "only be used in conjunction with a string literal"));
            return data;
         }
         if (node.getOperation() != QueryConstants.OPERATION_EQ_VALUE
            && node.getOperation() != QueryConstants.OPERATION_EQ_GENERAL)
         {
            exceptions.add(new InvalidQueryException("Name function can "
               + "only be used in conjunction with an equals operator"));
            return data;
         }
         // check if string literal is a valid XML Name
         if (XMLChar.isValidName(node.getStringValue()))
         {
            // parse string literal as JCR Name
            try
            {
               InternalQName n =
                  session.getLocationFactory().parseJCRName(ISO9075.decode(node.getStringValue())).getInternalName();
               query = new NameQuery(n, indexFormatVersion, nsMappings);
            }
            catch (RepositoryException e)
            {
               exceptions.add(e);
               return data;
            }
         }
         else
         {
            // will never match -> create dummy query
            query = new BooleanQuery();
         }
      }
      else
      {
         switch (node.getOperation())
         {
            case QueryConstants.OPERATION_EQ_VALUE : // =
            case QueryConstants.OPERATION_EQ_GENERAL :
               BooleanQuery or = new BooleanQuery();
               for (int i = 0; i < stringValues.length; i++)
               {
                  Term t = new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(field, stringValues[i]));
                  Query q;
                  if (transform[0] == TransformConstants.TRANSFORM_UPPER_CASE)
                  {
                     q = new CaseTermQuery.Upper(t);
                  }
                  else if (transform[0] == TransformConstants.TRANSFORM_LOWER_CASE)
                  {
                     q = new CaseTermQuery.Lower(t);
                  }
                  else
                  {
                     q = new JcrTermQuery(t);
                  }
                  or.add(q, Occur.SHOULD);
               }
               query = or;
               if (node.getOperation() == QueryConstants.OPERATION_EQ_VALUE)
               {
                  query = createSingleValueConstraint(or, field);
               }
               break;
            case QueryConstants.OPERATION_GE_VALUE : // >=
            case QueryConstants.OPERATION_GE_GENERAL :
               or = new BooleanQuery();
               for (int i = 0; i < stringValues.length; i++)
               {
                  Term lower = new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(field, stringValues[i]));
                  Term upper = new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(field, "\uFFFF"));
                  or.add(new RangeQuery(lower, upper, true, transform[0]), Occur.SHOULD);
               }
               query = or;
               if (node.getOperation() == QueryConstants.OPERATION_GE_VALUE)
               {
                  query = createSingleValueConstraint(or, field);
               }
               break;
            case QueryConstants.OPERATION_GT_VALUE : // >
            case QueryConstants.OPERATION_GT_GENERAL :
               or = new BooleanQuery();
               for (int i = 0; i < stringValues.length; i++)
               {
                  Term lower = new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(field, stringValues[i]));
                  Term upper = new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(field, "\uFFFF"));
                  or.add(new RangeQuery(lower, upper, false, transform[0]), Occur.SHOULD);
               }
               query = or;
               if (node.getOperation() == QueryConstants.OPERATION_GT_VALUE)
               {
                  query = createSingleValueConstraint(or, field);
               }
               break;
            case QueryConstants.OPERATION_LE_VALUE : // <=
            case QueryConstants.OPERATION_LE_GENERAL : // <=
               or = new BooleanQuery();
               for (int i = 0; i < stringValues.length; i++)
               {
                  Term lower = new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(field, ""));
                  Term upper = new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(field, stringValues[i]));
                  or.add(new RangeQuery(lower, upper, true, transform[0]), Occur.SHOULD);
               }
               query = or;
               if (node.getOperation() == QueryConstants.OPERATION_LE_VALUE)
               {
                  query = createSingleValueConstraint(query, field);
               }
               break;
            case QueryConstants.OPERATION_LIKE : // LIKE
               // the like operation always has one string value.
               // no coercing, see above
               if (stringValues[0].equals("%"))
               {
                  query = Util.createMatchAllQuery(field, indexFormatVersion);
               }
               else
               {
                  query = new WildcardQuery(FieldNames.PROPERTIES, field, stringValues[0], transform[0]);
               }
               break;
            case QueryConstants.OPERATION_LT_VALUE : // <
            case QueryConstants.OPERATION_LT_GENERAL :
               or = new BooleanQuery();
               for (int i = 0; i < stringValues.length; i++)
               {
                  Term lower = new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(field, ""));
                  Term upper = new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(field, stringValues[i]));
                  or.add(new RangeQuery(lower, upper, false, transform[0]), Occur.SHOULD);
               }
               query = or;
               if (node.getOperation() == QueryConstants.OPERATION_LT_VALUE)
               {
                  query = createSingleValueConstraint(or, field);
               }
               break;
            case QueryConstants.OPERATION_NE_VALUE : // !=
               // match nodes with property 'field' that includes svp and mvp
               BooleanQuery notQuery = new BooleanQuery();
               notQuery.add(Util.createMatchAllQuery(field, indexFormatVersion), Occur.SHOULD);
               // exclude all nodes where 'field' has the term in question
               for (int i = 0; i < stringValues.length; i++)
               {
                  Term t = new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(field, stringValues[i]));
                  Query q;
                  if (transform[0] == TransformConstants.TRANSFORM_UPPER_CASE)
                  {
                     q = new CaseTermQuery.Upper(t);
                  }
                  else if (transform[0] == TransformConstants.TRANSFORM_LOWER_CASE)
                  {
                     q = new CaseTermQuery.Lower(t);
                  }
                  else
                  {
                     q = new JcrTermQuery(t);
                  }
                  notQuery.add(q, Occur.MUST_NOT);
               }
               // and exclude all nodes where 'field' is multi valued
               notQuery.add(new JcrTermQuery(new Term(FieldNames.MVP, field)), Occur.MUST_NOT);
               query = notQuery;
               break;
            case QueryConstants.OPERATION_NE_GENERAL : // !=
               // that's:
               // all nodes with property 'field'
               // minus the nodes that have a single property 'field' that is
               // not equal to term in question
               // minus the nodes that have a multi-valued property 'field' and
               // all values are equal to term in question
               notQuery = new BooleanQuery();
               notQuery.add(Util.createMatchAllQuery(field, indexFormatVersion), Occur.SHOULD);
               for (int i = 0; i < stringValues.length; i++)
               {
                  // exclude the nodes that have the term and are single
                  // valued
                  Term t = new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(field, stringValues[i]));
                  Query svp = new NotQuery(new JcrTermQuery(new Term(FieldNames.MVP, field)));
                  BooleanQuery and = new BooleanQuery();
                  Query q;
                  if (transform[0] == TransformConstants.TRANSFORM_UPPER_CASE)
                  {
                     q = new CaseTermQuery.Upper(t);
                  }
                  else if (transform[0] == TransformConstants.TRANSFORM_LOWER_CASE)
                  {
                     q = new CaseTermQuery.Lower(t);
                  }
                  else
                  {
                     q = new JcrTermQuery(t);
                  }
                  and.add(q, Occur.MUST);
                  and.add(svp, Occur.MUST);
                  notQuery.add(and, Occur.MUST_NOT);
               }
               // todo above also excludes multi-valued properties that contain
               // multiple instances of only stringValues. e.g. text={foo, foo}
               query = notQuery;
               break;
            case QueryConstants.OPERATION_NULL :
               query = new NotQuery(Util.createMatchAllQuery(field, indexFormatVersion));
               break;
            case QueryConstants.OPERATION_SIMILAR :
               String uuid = "x";
               try
               {
                  // throw new UnsupportedOperationException();
                  QPath path = resolver.parseJCRPath(node.getStringValue()).getInternalPath();
                  NodeData parent = (NodeData)sharedItemMgr.getItemData(Constants.ROOT_UUID);

                  if (path.equals(Constants.ROOT_PATH))
                  {
                     uuid = Constants.ROOT_UUID;
                  }
                  else
                  {
                     QPathEntry[] relPathEntries = path.getRelPath(path.getDepth());
                     ItemData item = parent;
                     for (int i = 0; i < relPathEntries.length; i++)
                     {
                        item = sharedItemMgr.getItemData(parent, relPathEntries[i], ItemType.UNKNOWN);

                        if (item == null)
                           break;

                        if (item.isNode())
                           parent = (NodeData)item;
                        else if (i < relPathEntries.length - 1)
                           throw new IllegalPathException(
                              "Path can not contains a property as the intermediate element");
                     }
                     if (item == null)
                     {
                        throw new IllegalStateException("The item cannot be found");
                     }
                     uuid = item.getIdentifier();
                  }

               }
               catch (RepositoryException e)
               {
                  exceptions.add(e);
               }
               query = new SimilarityQuery(uuid, analyzer);
               break;
            case QueryConstants.OPERATION_NOT_NULL :
               query = Util.createMatchAllQuery(field, indexFormatVersion);
               break;
            case QueryConstants.OPERATION_SPELLCHECK :
               query = Util.createMatchAllQuery(field, indexFormatVersion);
               break;
            default :
               throw new IllegalArgumentException("Unknown relation operation: " + node.getOperation());
         }
      }

      if (relPath != null && relPath.getEntries().length > 1)
      {
         // child axis in relation
         QPathEntry[] elements = relPath.getEntries();
         // elements.length - 1 = property name
         // elements.length - 2 = last child axis name test
         for (int i = elements.length - 2; i >= 0; i--)
         {
            QPathEntry name = null;
            if (!elements[i].equals(RelationQueryNode.STAR_NAME_TEST))
            {
               name = elements[i];
            }
            if (i == elements.length - 2)
            {
               // join name test with property query if there is one
               if (name != null)
               {
                  Query nameTest = new NameQuery(name, indexFormatVersion, nsMappings);
                  BooleanQuery and = new BooleanQuery();
                  and.add(query, Occur.MUST);
                  and.add(nameTest, Occur.MUST);
                  query = and;
               }
               else
               {
                  // otherwise the query can be used as is
               }
            }
            else
            {
               query = new ParentAxisQuery(query, name, indexFormatVersion, nsMappings);
            }
         }
         // finally select the parent of the selected nodes
         query = new ParentAxisQuery(query, null, indexFormatVersion, nsMappings);
      }

      return query;
   }

   public Object visit(OrderQueryNode node, Object data)
   {
      return data;
   }

   public Object visit(PropertyFunctionQueryNode node, Object data)
   {
      return data;
   }

   // ---------------------------< internal
   // >-----------------------------------

   /**
    * Wraps a constraint query around <code>q</code> that limits the nodes to
    * those where <code>propName</code> is the name of a single value property
    * on the node instance.
    * 
    * @param q
    *            the query to wrap.
    * @param propName
    *            the name of a property that only has one value.
    * @return the wrapped query <code>q</code>.
    */
   private Query createSingleValueConstraint(Query q, String propName)
   {
      // get nodes with multi-values in propName
      Query mvp = new JcrTermQuery(new Term(FieldNames.MVP, propName));
      // now negate, that gives the nodes that have propName as single
      // values but also all others
      Query svp = new NotQuery(mvp);
      // now join the two, which will result in those nodes where propName
      // only contains a single value. This works because q already restricts
      // the result to those nodes that have a property propName
      BooleanQuery and = new BooleanQuery();
      and.add(q, Occur.MUST);
      and.add(svp, Occur.MUST);
      return and;
   }

   /**
    * Returns an array of String values to be used as a term to lookup the
    * search index for a String <code>literal</code> of a certain property
    * name. This method will lookup the <code>propertyName</code> in the node
    * type registry trying to find out the {@link javax.jcr.PropertyType}s. If
    * no property type is found looking up node type information, this method
    * will guess the property type.
    * 
    * @param propertyName
    *            the name of the property in the relation.
    * @param literal
    *            the String literal in the relation.
    * @return the String values to use as term for the query.
    */
   private String[] getStringValues(InternalQName propertyName, String literal)
   {
      PropertyTypeRegistry.TypeMapping[] types = propRegistry.getPropertyTypes(propertyName);
      Set<String> values = new HashSet<String>();
      for (int i = 0; i < types.length; i++)
      {
         switch (types[i].type)
         {
            case PropertyType.NAME :
               // try to translate name
               try
               {
                  InternalQName n = session.getLocationFactory().parseJCRName(literal).getInternalName();
                  values.add(nsMappings.translateName(n));
                  LOG.debug("Coerced " + literal + " into NAME.");
               }
               catch (RepositoryException e)
               {
                  if (types.length == 1)
                  {
                     LOG.warn("Unable to coerce '" + literal + "' into a NAME: " + e.toString());
                  }
               }
               catch (IllegalNameException e)
               {
                  if (types.length == 1)
                  {
                     LOG.warn("Unable to coerce '" + literal + "' into a NAME: " + e.toString());
                  }
               }
               break;
            case PropertyType.PATH :
               // try to translate path
               try
               {
                  QPath p = session.getLocationFactory().parseJCRPath(literal).getInternalPath();
                  values.add(resolver.createJCRPath(p).getAsString(false));
                  LOG.debug("Coerced " + literal + " into PATH.");
               }
               catch (RepositoryException e)
               {
                  if (types.length == 1)
                  {
                     LOG.warn("Unable to coerce '" + literal + "' into a PATH: " + e.toString());
                  }
               }
               break;
            case PropertyType.DATE :
               // try to parse date
               Calendar c = ISO8601.parse(literal);
               if (c != null)
               {
                  values.add(DateField.timeToString(c.getTimeInMillis()));
                  LOG.debug("Coerced " + literal + " into DATE.");
               }
               else
               {
                  if (types.length == 1)
                  {
                     LOG.warn("Unable to coerce '" + literal + "' into a DATE.");
                  }
               }
               break;
            case PropertyType.DOUBLE :
               // try to parse double
               try
               {
                  double d = Double.parseDouble(literal);
                  values.add(DoubleField.doubleToString(d));
                  LOG.debug("Coerced " + literal + " into DOUBLE.");
               }
               catch (NumberFormatException e)
               {
                  if (types.length == 1)
                  {
                     LOG.warn("Unable to coerce '" + literal + "' into a DOUBLE: " + e.toString());
                  }
               }
               break;
            case PropertyType.LONG :
               // try to parse long
               try
               {
                  long l = Long.parseLong(literal);
                  values.add(LongField.longToString(l));
                  LOG.debug("Coerced " + literal + " into LONG.");
               }
               catch (NumberFormatException e)
               {
                  if (types.length == 1)
                  {
                     LOG.warn("Unable to coerce '" + literal + "' into a LONG: " + e.toString());
                  }
               }
               break;
            case PropertyType.STRING :
               values.add(literal);
               LOG.debug("Using literal " + literal + " as is.");
               break;
         }
      }

      if (values.size() == 0)
      {
         // use literal as is then try to guess other types
         values.add(literal);

         // try to guess property type
         if (literal.indexOf('/') > -1)
         {
            // might be a path
            try
            {
               QPath p = session.getLocationFactory().parseJCRPath(literal).getInternalPath();
               values.add(resolver.createJCRPath(p).getAsString(false));
               LOG.debug("Coerced " + literal + " into PATH.");
            }
            catch (Exception e)
            {
               if (LOG.isTraceEnabled())
               {
                  LOG.trace("An exception occurred: " + e.getMessage());
               }
            }
         }
         if (XMLChar.isValidName(literal))
         {
            // might be a name
            try
            {
               InternalQName n = session.getLocationFactory().parseJCRName(literal).getInternalName();
               values.add(nsMappings.translateName(n));
               LOG.debug("Coerced " + literal + " into NAME.");
            }
            catch (Exception e)
            {
               if (LOG.isTraceEnabled())
               {
                  LOG.trace("An exception occurred: " + e.getMessage());
               }
            }
         }
         if (literal.indexOf(':') > -1)
         {
            // is it a date?
            Calendar c = ISO8601.parse(literal);
            if (c != null)
            {
               values.add(DateField.timeToString(c.getTimeInMillis()));
               LOG.debug("Coerced " + literal + " into DATE.");
            }
         }
         else
         {
            // long or double are possible at this point
            try
            {
               values.add(LongField.longToString(Long.parseLong(literal)));
               LOG.debug("Coerced " + literal + " into LONG.");
            }
            catch (NumberFormatException e)
            {
               // not a long
               // try double
               try
               {
                  values.add(DoubleField.doubleToString(Double.parseDouble(literal)));
                  LOG.debug("Coerced " + literal + " into DOUBLE.");
               }
               catch (NumberFormatException e1)
               {
                  if (LOG.isTraceEnabled())
                  {
                     LOG.trace("An exception occurred: " + e.getMessage());
                  }
               }
            }
         }
      }
      // if still no values use literal as is
      if (values.size() == 0)
      {
         values.add(literal);
         LOG.debug("Using literal " + literal + " as is.");
      }
      return values.toArray(new String[values.size()]);
   }
}
