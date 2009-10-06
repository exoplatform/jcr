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
package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause.Occur;
import org.exoplatform.services.jcr.core.NamespaceAccessor;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.InvalidQueryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34027 2009-07-15 23:26:43Z
 *          aheritier $
 */
public class LuceneVirtualTableResolver extends
	NodeTypeVirtualTableResolver<Query> {

    private final LocationFactory locationFactory;

    /**
     * Class logger.
     */
    private final Log log = ExoLogger
	    .getLogger(LuceneVirtualTableResolver.class);

    private final String mixinTypesField;

    private final String primaryTypeField;

    /**
     * @param nodeTypeDataManager
     * @throws RepositoryException
     */
    public LuceneVirtualTableResolver(
	    final NodeTypeDataManager nodeTypeDataManager,
	    final NamespaceAccessor namespaceAccessor)
	    throws RepositoryException {
	super(nodeTypeDataManager);

	locationFactory = new LocationFactory(namespaceAccessor);
	mixinTypesField = locationFactory.createJCRName(
		Constants.JCR_MIXINTYPES).getAsString();
	primaryTypeField = locationFactory.createJCRName(
		Constants.JCR_PRIMARYTYPE).getAsString();

    }

    /**
     * {@inheritDoc}
     */
    public Query resolve(final InternalQName tableName,
	    final boolean includeInheritedTables) throws InvalidQueryException,
	    RepositoryException {

	final List<Term> terms = new ArrayList<Term>();

	Query query = null;
	try {
	    final String nodeTypeStringName = locationFactory.createJCRName(
		    tableName).getAsString();

	    if (isMixin(tableName)) {
		// search for nodes where jcr:mixinTypes is set to this mixin
		Term t = new Term(FieldNames.PROPERTIES, FieldNames
			.createNamedValue(mixinTypesField, nodeTypeStringName));
		terms.add(t);

	    } else {
		// search for nodes where jcr:primaryType is set to this type

		Term t = new Term(FieldNames.PROPERTIES, FieldNames
			.createNamedValue(primaryTypeField, nodeTypeStringName));
		terms.add(t);
	    }
	    if (includeInheritedTables) {
		// now search for all node types that are derived from base
		final Set<InternalQName> allTypes = getSubTypes(tableName);
		for (final InternalQName descendantNt : allTypes) {
		    final String ntName = locationFactory.createJCRName(
			    descendantNt).getAsString();

		    Term t;
		    if (isMixin(descendantNt)) {
			// search on jcr:mixinTypes
			t = new Term(FieldNames.PROPERTIES, FieldNames
				.createNamedValue(mixinTypesField, ntName));
		    } else {
			// search on jcr:primaryType
			t = new Term(FieldNames.PROPERTIES, FieldNames
				.createNamedValue(primaryTypeField, ntName));
		    }
		    terms.add(t);
		}
	    }
	} catch (final NoSuchNodeTypeException e) {
	    throw new InvalidQueryException(e.getMessage(), e);
	}

	if (terms.size() == 0) {
	    // exception occured
	    query = new BooleanQuery();

	} else if (terms.size() == 1) {
	    query = new JcrTermQuery(terms.get(0));

	} else {
	    final BooleanQuery b = new BooleanQuery();
	    for (final Object element : terms) {
		// b.add(new TermQuery((Term) element), Occur.SHOULD);
		b.add(new JcrTermQuery((Term) element), Occur.SHOULD);
	    }
	    query = b;
	}

	return query;
    }
}
