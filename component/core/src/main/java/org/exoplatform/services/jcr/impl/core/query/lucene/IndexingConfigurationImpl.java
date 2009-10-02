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
import org.exoplatform.services.jcr.core.NamespaceAccessor;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.query.AdditionalNamespaceResolver;
import org.exoplatform.services.jcr.impl.core.query.QueryHandlerContext;
import org.exoplatform.services.jcr.impl.core.query.misc.Pattern;
import org.exoplatform.services.jcr.impl.util.ISO9075;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * <code>IndexingConfigurationImpl</code> implements a concrete indexing
 * configuration.
 */
public class IndexingConfigurationImpl implements IndexingConfiguration {

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory
	    .getLogger(IndexingConfigurationImpl.class);

    /**
     * The path factory instance.
     */
    // private static final PathFactory PATH_FACTORY =
    // PathFactoryImpl.getInstance();
    /**
     * A namespace resolver for parsing QNames in the configuration.
     */
    private LocationFactory resolver;

    /**
     * The item state manager to retrieve additional item states.
     */
    private ItemDataConsumer ism;

    /**
     * A hierarchy resolver for the item state manager.
     */
    // private HierarchyManager hmgr;
    /**
     * The {@link IndexingRule}s inside this configuration.
     */
    private Map<InternalQName, List<IndexingRule>> configElements = new HashMap<InternalQName, List<IndexingRule>>();

    /**
     * The indexing aggregates inside this configuration.
     */
    private AggregateRule[] aggregateRules;

    /**
     * The configured analyzers for indexing properties.
     */
    private Map<String, Analyzer> analyzers = new HashMap<String, Analyzer>();

    /**
     * {@inheritDoc}
     */
    public void init(Element config, QueryHandlerContext context,
	    NamespaceMappings nsMappings) throws Exception {
	ism = context.getItemStateManager();

	NamespaceAccessor nsResolver = new AdditionalNamespaceResolver(
		getNamespaces(config));
	resolver = new LocationFactory(nsResolver);// new
	// ParsingNameResolver(NameFactoryImpl.getInstance(),
	// nsResolver);

	NodeTypeDataManager ntReg = context.getNodeTypeDataManager();
	List<NodeTypeData> ntNames = ntReg.getAllNodeTypes();
	List<AggregateRuleImpl> idxAggregates = new ArrayList<AggregateRuleImpl>();
	NodeList indexingConfigs = config.getChildNodes();
	for (int i = 0; i < indexingConfigs.getLength(); i++) {
	    Node configNode = indexingConfigs.item(i);
	    if (configNode.getNodeName().equals("index-rule")) {
		IndexingRule element = new IndexingRule(configNode);
		// register under node type and all its sub types
		log.debug("Found rule '{}' for NodeType '{}'", element, element
			.getNodeTypeName());
		for (NodeTypeData nodeTypeData : ntNames) {

		    if (ntReg.isNodeType(element.getNodeTypeName(),
			    nodeTypeData.getName())) {
			List<IndexingRule> perNtConfig = configElements
				.get(nodeTypeData);
			if (perNtConfig == null) {
			    perNtConfig = new ArrayList<IndexingRule>();
			    configElements.put(nodeTypeData.getName(),
				    perNtConfig);
			}
			log.debug("Registering it for name '{}'", nodeTypeData);
			perNtConfig.add(new IndexingRule(element, nodeTypeData
				.getName()));
		    }
		}
	    } else if (configNode.getNodeName().equals("aggregate")) {
		idxAggregates.add(new AggregateRuleImpl(configNode, resolver,
			ism));
	    } else if (configNode.getNodeName().equals("analyzers")) {
		NodeList childNodes = configNode.getChildNodes();
		for (int j = 0; j < childNodes.getLength(); j++) {
		    Node analyzerNode = childNodes.item(j);
		    if (analyzerNode.getNodeName().equals("analyzer")) {
			String analyzerClassName = analyzerNode.getAttributes()
				.getNamedItem("class").getNodeValue();
			try {
			    Class clazz = Class.forName(analyzerClassName);
			    if (clazz == JcrStandartAnalyzer.class) {
				log
					.warn("Not allowed to configure "
						+ JcrStandartAnalyzer.class
							.getName()
						+ " for a property. "
						+ "Using default analyzer for that property.");
			    } else if (Analyzer.class.isAssignableFrom(clazz)) {
				Analyzer analyzer = (Analyzer) clazz
					.newInstance();
				NodeList propertyChildNodes = analyzerNode
					.getChildNodes();
				for (int k = 0; k < propertyChildNodes
					.getLength(); k++) {
				    Node propertyNode = propertyChildNodes
					    .item(k);
				    if (propertyNode.getNodeName().equals(
					    "property")) {
					// get property name
					InternalQName propName = resolver
						.parseJCRName(
							getTextContent(propertyNode))
						.getInternalName();
					String fieldName = nsMappings
						.translateName(propName);
					// set analyzer for the fulltext
					// property fieldname
					int idx = fieldName.indexOf(':');
					fieldName = fieldName.substring(0,
						idx + 1)
						+ FieldNames.FULLTEXT_PREFIX
						+ fieldName.substring(idx + 1);
					Object prevAnalyzer = analyzers.put(
						fieldName, analyzer);
					if (prevAnalyzer != null) {
					    log
						    .warn("Property "
							    + propName
								    .getName()
							    + " has been configured for multiple analyzers. "
							    + " Last configured analyzer is used");
					}
				    }
				}
			    } else {
				log
					.warn("org.apache.lucene.analysis.Analyzer is not a superclass of "
						+ analyzerClassName
						+ ". Ignoring this configure analyzer");
			    }
			} catch (ClassNotFoundException e) {
			    log.warn("Analyzer class not found: "
				    + analyzerClassName, e);
			}
		    }
		}
	    }

	}
	aggregateRules = idxAggregates.toArray(new AggregateRule[idxAggregates
		.size()]);
    }

    /**
     * Returns the configured indexing aggregate rules or <code>null</code> if
     * none exist.
     * 
     * @return the configured rules or <code>null</code> if none exist.
     */
    public AggregateRule[] getAggregateRules() {
	return aggregateRules;
    }

    /**
     * Returns <code>true</code> if the property with the given name is fulltext
     * indexed according to this configuration.
     * 
     * @param state
     *            the node state.
     * @param propertyName
     *            the name of a property.
     * @return <code>true</code> if the property is fulltext indexed;
     *         <code>false</code> otherwise.
     */
    public boolean isIndexed(NodeData state, InternalQName propertyName) {
	IndexingRule rule = getApplicableIndexingRule(state);
	if (rule != null) {
	    return rule.isIndexed(propertyName);
	}
	// none of the configs matches -> index property
	return true;
    }

    /**
     * Returns the boost value for the given property name. If there is no
     * configuration entry for the property name the {@link #DEFAULT_BOOST} is
     * returned.
     * 
     * @param state
     *            the node state.
     * @param propertyName
     *            the name of a property.
     * @return the boost value for the property.
     */
    public float getPropertyBoost(NodeData state, InternalQName propertyName) {
	IndexingRule rule = getApplicableIndexingRule(state);
	if (rule != null) {
	    return rule.getBoost(propertyName);
	}
	return DEFAULT_BOOST;
    }

    /**
     * Returns the boost for the node scope fulltext index field.
     * 
     * @param state
     *            the node state.
     * @return the boost for the node scope fulltext index field.
     */
    public float getNodeBoost(NodeData state) {
	IndexingRule rule = getApplicableIndexingRule(state);
	if (rule != null) {
	    return rule.getNodeBoost();
	}
	return DEFAULT_BOOST;
    }

    /**
     * Returns <code>true</code> if the property with the given name should be
     * included in the node scope fulltext index. If there is not configuration
     * entry for that propery <code>false</code> is returned.
     * 
     * @param state
     *            the node state.
     * @param propertyName
     *            the name of a property.
     * @return <code>true</code> if the property should be included in the node
     *         scope fulltext index.
     */
    public boolean isIncludedInNodeScopeIndex(NodeData state,
	    InternalQName propertyName) {
	IndexingRule rule = getApplicableIndexingRule(state);
	if (rule != null) {
	    return rule.isIncludedInNodeScopeIndex(propertyName);
	}
	// none of the config elements matched -> default is to include
	return true;
    }

    /**
     * Returns <code>true</code> if the content of the property with the given
     * name should show up in an excerpt. If there is no configuration entry for
     * that property <code>true</code> is returned.
     * 
     * @param state
     *            the node state.
     * @param propertyName
     *            the name of a property.
     * @return <code>true</code> if the content of the property should be
     *         included in an excerpt; <code>false</code> otherwise.
     */
    public boolean useInExcerpt(NodeData state, InternalQName propertyName) {
	IndexingRule rule = getApplicableIndexingRule(state);
	if (rule != null) {
	    return rule.useInExcerpt(propertyName);
	}
	// none of the config elements matched -> default is to include
	return true;
    }

    /**
     * Returns the analyzer configured for the property with this fieldName (the
     * string representation ,JCR-style name, of the given
     * <code>InternalQName</code> prefixed with
     * <code>FieldNames.FULLTEXT_PREFIX</code>)), and <code>null</code> if none
     * is configured, or the configured analyzer cannot be found. If
     * <code>null</code> is returned, the default Analyzer is used.
     * 
     * @param fieldName
     *            the string representation ,JCR-style name, of the given
     *            <code>InternalQName</code> prefixed with
     *            <code>FieldNames.FULLTEXT_PREFIX</code>))
     * @return the <code>analyzer</code> to use for indexing this property
     */
    public Analyzer getPropertyAnalyzer(String fieldName) {
	if (analyzers.containsKey(fieldName)) {
	    return analyzers.get(fieldName);
	}
	return null;
    }

    // ---------------------------------< internal
    // >-----------------------------

    /**
     * Returns the first indexing rule that applies to the given node
     * <code>state</code>.
     * 
     * @param state
     *            a node state.
     * @return the indexing rule or <code>null</code> if none applies.
     */
    private IndexingRule getApplicableIndexingRule(NodeData state) {
	List<IndexingRule> rules = null;
	List<IndexingRule> r = configElements.get(state.getPrimaryTypeName());
	if (r != null) {
	    rules = new ArrayList<IndexingRule>();
	    rules.addAll(r);
	}

	InternalQName[] mixTypes = state.getMixinTypeNames();
	for (InternalQName mixType : mixTypes) {
	    r = configElements.get(mixType);
	    if (r != null) {
		if (rules == null) {
		    rules = new ArrayList<IndexingRule>();
		}
		rules.addAll(r);
	    }
	}

	if (rules != null) {
	    for (IndexingRule ir : rules) {
		if (ir.appliesTo(state)) {
		    return ir;
		}
	    }
	}

	// no applicable rule
	return null;
    }

    /**
     * Returns the namespaces declared on the <code>node</code>.
     * 
     * @param node
     *            a DOM node.
     * @return the namespaces
     */
    private Properties getNamespaces(Node node) {
	Properties namespaces = new Properties();
	NamedNodeMap attributes = node.getAttributes();
	for (int i = 0; i < attributes.getLength(); i++) {
	    Attr attribute = (Attr) attributes.item(i);
	    if (attribute.getName().startsWith("xmlns:")) {
		namespaces.setProperty(attribute.getName().substring(6),
			attribute.getValue());
	    }
	}
	return namespaces;
    }

    /**
     * Creates property configurations defined in the <code>config</code>.
     * 
     * @param config
     *            the fulltext indexing configuration.
     * @param propConfigs
     *            will be filled with exact <code>InternalQName</code> to
     *            <code>PropertyConfig</code> mappings.
     * @param namePatterns
     *            will be filled with <code>NamePattern</code>s.
     * @throws IllegalNameException
     *             if the node type name contains illegal characters.
     * @throws RepositoryException
     */
    private void createPropertyConfigs(Node config,
	    Map<InternalQName, PropertyConfig> propConfigs,
	    List<NamePattern> namePatterns) throws IllegalNameException,
	    RepositoryException {
	NodeList childNodes = config.getChildNodes();
	for (int i = 0; i < childNodes.getLength(); i++) {
	    Node n = childNodes.item(i);
	    if (n.getNodeName().equals("property")) {
		NamedNodeMap attributes = n.getAttributes();
		// get boost value
		float boost = 1.0f;
		Node boostAttr = attributes.getNamedItem("boost");
		if (boostAttr != null) {
		    try {
			boost = Float.parseFloat(boostAttr.getNodeValue());
		    } catch (NumberFormatException e) {
			// use default
		    }
		}

		// get nodeScopeIndex flag
		boolean nodeScopeIndex = true;
		Node nsIndex = attributes.getNamedItem("nodeScopeIndex");
		if (nsIndex != null) {
		    nodeScopeIndex = Boolean.valueOf(nsIndex.getNodeValue())
			    .booleanValue();
		}

		// get isRegexp flag
		boolean isRegexp = false;
		Node regexp = attributes.getNamedItem("isRegexp");
		if (regexp != null) {
		    isRegexp = Boolean.valueOf(regexp.getNodeValue())
			    .booleanValue();
		}

		// get useInExcerpt flag
		boolean useInExcerpt = true;
		Node excerpt = attributes.getNamedItem("useInExcerpt");
		if (excerpt != null) {
		    useInExcerpt = Boolean.valueOf(excerpt.getNodeValue())
			    .booleanValue();
		}

		PropertyConfig pc = new PropertyConfig(boost, nodeScopeIndex,
			useInExcerpt);

		if (isRegexp) {
		    namePatterns.add(new NamePattern(getTextContent(n), pc,
			    resolver));
		} else {
		    InternalQName propName = resolver.parseJCRName(
			    getTextContent(n)).getInternalName();
		    propConfigs.put(propName, pc);
		}
	    }
	}
    }

    /**
     * Gets the condition expression from the configuration.
     * 
     * @param config
     *            the config node.
     * @return the condition expression or <code>null</code> if there is no
     *         condition set on the <code>config</code>.
     * @throws MalformedPathException
     *             if the condition string is malformed.
     * @throws IllegalNameException
     *             if a name contains illegal characters.
     * @throws RepositoryException
     */
    private PathExpression getCondition(Node config)
	    throws IllegalNameException, RepositoryException {
	Node conditionAttr = config.getAttributes().getNamedItem("condition");
	if (conditionAttr == null) {
	    return null;
	}
	String conditionString = conditionAttr.getNodeValue();
	int idx;
	int axis;
	InternalQName elementTest = null;
	InternalQName nameTest = null;
	InternalQName propertyName;
	String propertyValue;

	// parse axis
	if (conditionString.startsWith("ancestor::")) {
	    axis = PathExpression.ANCESTOR;
	    idx = "ancestor::".length();
	} else if (conditionString.startsWith("parent::")) {
	    axis = PathExpression.PARENT;
	    idx = "parent::".length();
	} else if (conditionString.startsWith("@")) {
	    axis = PathExpression.SELF;
	    idx = "@".length();
	} else {
	    axis = PathExpression.CHILD;
	    idx = 0;
	}

	try {
	    if (conditionString.startsWith("element(", idx)) {
		int colon = conditionString.indexOf(',', idx
			+ "element(".length());
		String name = conditionString.substring(
			idx + "element(".length(), colon).trim();
		if (!name.equals("*")) {
		    nameTest = resolver.parseJCRName(ISO9075.decode(name))
			    .getInternalName();
		}
		idx = conditionString.indexOf(")/@", colon);
		String type = conditionString.substring(colon + 1, idx).trim();
		elementTest = resolver.parseJCRName(ISO9075.decode(type))
			.getInternalName();
		idx += ")/@".length();
	    } else {
		if (axis == PathExpression.ANCESTOR
			|| axis == PathExpression.CHILD
			|| axis == PathExpression.PARENT) {
		    // simple name test
		    String name = conditionString.substring(idx,
			    conditionString.indexOf('/', idx));
		    if (!name.equals("*")) {
			nameTest = resolver.parseJCRName(ISO9075.decode(name))
				.getInternalName();
		    }
		    idx += name.length() + "/@".length();
		}
	    }

	    // parse property name
	    int eq = conditionString.indexOf('=', idx);
	    String name = conditionString.substring(idx, eq).trim();
	    propertyName = resolver.parseJCRName(ISO9075.decode(name))
		    .getInternalName();

	    // parse string value
	    int quote = conditionString.indexOf('\'', eq) + 1;
	    propertyValue = conditionString.substring(quote, conditionString
		    .indexOf('\'', quote));
	} catch (IndexOutOfBoundsException e) {
	    throw new RepositoryException(conditionString);
	}

	return new PathExpression(axis, elementTest, nameTest, propertyName,
		propertyValue);
    }

    /**
     * @param node
     *            a node.
     * @return the text content of the <code>node</code>.
     */
    private static String getTextContent(Node node) {
	StringBuffer content = new StringBuffer();
	NodeList nodes = node.getChildNodes();
	for (int i = 0; i < nodes.getLength(); i++) {
	    Node n = nodes.item(i);
	    if (n.getNodeType() == Node.TEXT_NODE) {
		content.append(((CharacterData) n).getData());
	    }
	}
	return content.toString();
    }

    /**
     * A property name pattern.
     */
    private static final class NamePattern {

	/**
	 * The pattern to match.
	 */
	private final Pattern pattern;

	/**
	 * The associated configuration.
	 */
	private final PropertyConfig config;

	/**
	 * Creates a new name pattern.
	 * 
	 * @param pattern
	 *            the pattern as read from the configuration file.
	 * @param config
	 *            the associated configuration.
	 * @param resolver
	 *            a namespace resolver for parsing name from the
	 *            configuration.
	 * @throws IllegalNameException
	 *             if the prefix of the name pattern is illegal.
	 * @throws RepositoryException
	 */
	private NamePattern(String pattern, PropertyConfig config,
		LocationFactory resolver) throws IllegalNameException,
		RepositoryException {
	    String uri = Constants.NS_DEFAULT_URI;
	    String localPattern = pattern;
	    int idx = pattern.indexOf(':');
	    if (idx != -1) {
		// use a dummy local name to get namespace uri
		uri = resolver.parseJCRName(pattern.substring(0, idx) + ":a")
			.getNamespace();
		localPattern = pattern.substring(idx + 1);
	    }
	    this.pattern = Pattern.name(uri, localPattern);
	    this.config = config;
	}

	/**
	 * @param path
	 *            the path to match.
	 * @return <code>true</code> if <code>path</code> matches this name
	 *         pattern; <code>false</code> otherwise.
	 */
	boolean matches(QPath path) {
	    return pattern.match(path).isFullMatch();
	}

	/**
	 * @return the property configuration for this name pattern.
	 */
	PropertyConfig getConfig() {
	    return config;
	}
    }

    private class IndexingRule {

	/**
	 * The node type of this fulltext indexing rule.
	 */
	private final InternalQName nodeTypeName;

	/**
	 * Map of {@link PropertyConfig}. Key=InternalQName of property.
	 */
	private final Map<InternalQName, PropertyConfig> propConfigs;

	/**
	 * List of {@link NamePattern}s.
	 */
	private final List<NamePattern> namePatterns;

	/**
	 * An expression based on a relative path.
	 */
	private final PathExpression condition;

	/**
	 * The boost value for this config element.
	 */
	private final float boost;

	/**
	 * Creates a new indexing rule base on an existing one, but for a
	 * different node type name.
	 * 
	 * @param original
	 *            the existing rule.
	 * @param nodeTypeName
	 *            the node type name for the rule.
	 */
	IndexingRule(IndexingRule original, InternalQName nodeTypeName) {
	    this.nodeTypeName = nodeTypeName;
	    this.propConfigs = original.propConfigs;
	    this.namePatterns = original.namePatterns;
	    this.condition = original.condition;
	    this.boost = original.boost;
	}

	/**
	 * 
	 * @param config
	 *            the configuration for this rule.
	 * @throws MalformedPathException
	 *             if the condition expression is malformed.
	 * @throws IllegalNameException
	 *             if a name contains illegal characters.
	 * @throws RepositoryException
	 */
	IndexingRule(Node config) throws IllegalNameException,
		RepositoryException {
	    this.nodeTypeName = getNodeTypeName(config);
	    this.condition = getCondition(config);
	    this.boost = getNodeBoost(config);
	    this.propConfigs = new HashMap<InternalQName, PropertyConfig>();
	    this.namePatterns = new ArrayList<NamePattern>();
	    createPropertyConfigs(config, propConfigs, namePatterns);
	}

	/**
	 * Returns the name of the node type where this rule applies to.
	 * 
	 * @return name of the node type.
	 */
	public InternalQName getNodeTypeName() {
	    return nodeTypeName;
	}

	/**
	 * @return the value for the node boost.
	 */
	public float getNodeBoost() {
	    return boost;
	}

	/**
	 * Returns <code>true</code> if the property with the given name is
	 * indexed according to this rule.
	 * 
	 * @param propertyName
	 *            the name of a property.
	 * @return <code>true</code> if the property is indexed;
	 *         <code>false</code> otherwise.
	 */
	public boolean isIndexed(InternalQName propertyName) {
	    return getConfig(propertyName) != null;
	}

	/**
	 * Returns the boost value for the given property name. If there is no
	 * configuration entry for the property name the default boost value is
	 * returned.
	 * 
	 * @param propertyName
	 *            the name of a property.
	 * @return the boost value for the property.
	 */
	public float getBoost(InternalQName propertyName) {
	    PropertyConfig config = getConfig(propertyName);
	    if (config != null) {
		return config.boost;
	    } else {
		return DEFAULT_BOOST;
	    }
	}

	/**
	 * Returns <code>true</code> if the property with the given name should
	 * be included in the node scope fulltext index. If there is no
	 * configuration entry for that propery <code>false</code> is returned.
	 * 
	 * @param propertyName
	 *            the name of a property.
	 * @return <code>true</code> if the property should be included in the
	 *         node scope fulltext index.
	 */
	public boolean isIncludedInNodeScopeIndex(InternalQName propertyName) {
	    PropertyConfig config = getConfig(propertyName);
	    if (config != null) {
		return config.nodeScopeIndex;
	    } else {
		return false;
	    }
	}

	/**
	 * Returns <code>true</code> if the content of the property with the
	 * given name should show up in an excerpt. If there is no configuration
	 * entry for that property <code>true</code> is returned.
	 * 
	 * @param propertyName
	 *            the name of a property.
	 * @return <code>true</code> if the content of the property should be
	 *         included in an excerpt; <code>false</code> otherwise.
	 */
	public boolean useInExcerpt(InternalQName propertyName) {
	    PropertyConfig config = getConfig(propertyName);
	    if (config != null) {
		return config.useInExcerpt;
	    } else {
		return true;
	    }
	}

	/**
	 * Returns <code>true</code> if this rule applies to the given node
	 * <code>state</code>.
	 * 
	 * @param state
	 *            the state to check.
	 * @return <code>true</code> the rule applies to the given node;
	 *         <code>false</code> otherwise.
	 */
	public boolean appliesTo(NodeData state) {
	    if (!nodeTypeName.equals(state.getPrimaryTypeName())) {
		return false;
	    }
	    if (condition == null) {
		return true;
	    } else {
		return condition.evaluate(state);
	    }
	}

	// -------------------------< internal
	// >---------------------------------

	/**
	 * @param propertyName
	 *            name of a property.
	 * @return the property configuration or <code>null</code> if this
	 *         indexing rule does not contain a configuration for the given
	 *         property.
	 */
	private PropertyConfig getConfig(InternalQName propertyName) {
	    PropertyConfig config = propConfigs.get(propertyName);
	    if (config != null) {
		return config;
	    } else if (namePatterns.size() > 0) {
		QPath path = new QPath(new QPathEntry[] { new QPathEntry(
			propertyName, 1) });
		// check patterns
		for (Iterator<NamePattern> it = namePatterns.iterator(); it
			.hasNext();) {
		    NamePattern np = it.next();
		    if (np.matches(path)) {
			return np.getConfig();
		    }
		}
	    }
	    return null;
	}

	/**
	 * Reads the node type of the root node of the indexing rule.
	 * 
	 * @param config
	 *            the configuration.
	 * @return the name of the node type.
	 * @throws IllegalNameException
	 *             if the node type name contains illegal characters.
	 * @throws RepositoryException
	 */
	private InternalQName getNodeTypeName(Node config)
		throws IllegalNameException, RepositoryException {
	    String ntString = config.getAttributes().getNamedItem("nodeType")
		    .getNodeValue();
	    return resolver.parseJCRName(ntString).getInternalName();
	}

	/**
	 * Returns the node boost from the <code>config</code>.
	 * 
	 * @param config
	 *            the configuration.
	 * @return the configured node boost or the default boost if none is
	 *         configured.
	 */
	private float getNodeBoost(Node config) {
	    Node boost = config.getAttributes().getNamedItem("boost");
	    if (boost != null) {
		try {
		    return Float.parseFloat(boost.getNodeValue());
		} catch (NumberFormatException e) {
		    // return default boost
		}
	    }
	    return DEFAULT_BOOST;
	}
    }

    /**
     * Simple class that holds boost and nodeScopeIndex flag.
     */
    private class PropertyConfig {

	/**
	 * The boost value for a property.
	 */
	final float boost;

	/**
	 * Flag that indicates whether a property is included in the node scope
	 * fulltext index of its parent.
	 */
	final boolean nodeScopeIndex;

	/**
	 * Flag that indicates whether the content of a property should be used
	 * to create an excerpt.
	 */
	final boolean useInExcerpt;

	PropertyConfig(float boost, boolean nodeScopeIndex, boolean useInExcerpt) {
	    this.boost = boost;
	    this.nodeScopeIndex = nodeScopeIndex;
	    this.useInExcerpt = useInExcerpt;
	}
    }

    private class PathExpression {

	static final int SELF = 0;

	static final int CHILD = 1;

	static final int ANCESTOR = 2;

	static final int PARENT = 3;

	private final int axis;

	private final InternalQName elementTest;

	private final InternalQName nameTest;

	private final InternalQName propertyName;

	private final String propertyValue;

	PathExpression(int axis, InternalQName elementTest,
		InternalQName nameTest, InternalQName propertyName,
		String propertyValue) {
	    this.axis = axis;
	    this.elementTest = elementTest;
	    this.nameTest = nameTest;
	    this.propertyName = propertyName;
	    this.propertyValue = propertyValue;
	}

	/**
	 * Evaluates this expression and returns <code>true</code> if the
	 * condition matches using <code>state</code> as the context node state.
	 * 
	 * @param context
	 *            the context from where the expression should be evaluated.
	 * @return expression result.
	 */
	boolean evaluate(final NodeData context) {
	    // get iterator along specified axis
	    Iterator nodeStates;
	    if (axis == SELF) {
		nodeStates = Collections.singletonList(context).iterator();
	    } else if (axis == CHILD) {
		List<NodeData> childs;
		try {
		    childs = ism.getChildNodesData(context);
		    nodeStates = childs.iterator();
		} catch (RepositoryException e) {
		    nodeStates = Collections.<NodeData> emptyList().iterator();
		}
	    } else if (axis == ANCESTOR) {
		try {
		    nodeStates = new Iterator() {

			private NodeData next = (NodeData) ism
				.getItemData(context.getParentIdentifier());

			public void remove() {
			    throw new UnsupportedOperationException();
			}

			public boolean hasNext() {
			    return next != null;
			}

			public Object next() {
			    NodeData tmp = next;
			    try {
				if (next.getParentIdentifier() != null) {
				    next = (NodeData) ism.getItemData(next
					    .getParentIdentifier());
				} else {
				    next = null;
				}
			    } catch (RepositoryException e) {
				next = null;
			    }
			    return tmp;
			}
		    };
		} catch (RepositoryException e) {
		    nodeStates = Collections.EMPTY_LIST.iterator();
		}
	    } else if (axis == PARENT) {
		try {
		    if (context.getParentIdentifier() != null) {
			NodeData state = (NodeData) ism.getItemData(context
				.getParentIdentifier());
			nodeStates = Collections.singletonList(state)
				.iterator();
		    } else {
			nodeStates = Collections.EMPTY_LIST.iterator();
		    }
		} catch (RepositoryException e) {
		    nodeStates = Collections.EMPTY_LIST.iterator();
		}
	    } else {
		// unsupported axis
		nodeStates = Collections.EMPTY_LIST.iterator();
	    }

	    // check node type, name and property value for each
	    while (nodeStates.hasNext()) {
		try {
		    NodeData current = (NodeData) nodeStates.next();
		    if ((elementTest != null)
			    && !current.getPrimaryTypeName()
				    .equals(elementTest)) {
			continue;
		    }
		    if ((nameTest != null)
			    && !current.getQPath().getName().equals(nameTest)) {
			continue;
		    }

		    List<PropertyData> childProps = ism
			    .getChildPropertiesData(current);

		    PropertyData propState = null;
		    for (PropertyData propertyData : childProps) {
			if (propertyData.getQPath().getName().equals(
				propertyName)) {
			    propState = propertyData;
			    break;
			}

		    }
		    if (propState == null) {
			continue;
		    }

		    List<ValueData> values = propState.getValues();

		    // if (values.get(i).toString().equals(propertyValue)) {
		    // return true;
		    // }

		    if (propState.getType() == PropertyType.BINARY) {
			// skip binary values
			continue;
		    }

		    try {
			for (int i = 0; i < values.size(); i++) {
			    byte[] bytes = values.get(i).getAsByteArray();
			    String val = new String(bytes,
				    Constants.DEFAULT_ENCODING);
			    if (val.equals(propertyValue)) {
				return true;
			    }
			}
		    } catch (IOException e) {
			log.error(e.getLocalizedMessage());
		    }

		} catch (RepositoryException e) {
		    log.error(e.getLocalizedMessage());
		}
	    }
	    return false;
	}
    }
}
