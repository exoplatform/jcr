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
 
/*Implementation of compact node type definition tree walker*/
tree grammar CNDWalker;

/*======================================================*/
/*			  HRADER       			*/
/*======================================================*/
/* Tree walker options */
options {
	ASTLabelType = CommonTree;
	tokenVocab = CND;	
}
/* Class header */
@header{
package org.exoplatform.services.jcr.impl.core.nodetype.registration;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;

import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.BitSet;
import org.antlr.runtime.DFA;
import org.antlr.runtime.EarlyExitException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.TreeNodeStream;
import org.antlr.runtime.tree.TreeParser;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.NodeTypeDataBuilder.NodeDefinitionDataBuilder;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.NodeTypeDataBuilder.PropertyDefinitionDataBuilder;


}
/* Class members */
@members{
/* registry and location factory objects */
private NamespaceRegistryImpl namespaceRegistry;
private LocationFactory locationFactory;

/* It is a list where all namespaces from current stream are placed */
private List<NameSpace>    nameSpaces = new ArrayList<NameSpace>();

/* It is a list where all nodeTypes from current stream are placed  */
private List<NodeTypeData> nodeTypes  = new ArrayList<NodeTypeData>();

/* If string is a literal then unQuoteed string is returned */
public String unQuote(String in){
	if ((in.length()>=2)&&(in.charAt(0)=='\'')) {
		in=in.substring(1,in.length()-1);
	} else if ((in.length()>=2)&&(in.charAt(0)=='\"')){
		in=in.substring(1,in.length()-1);
	}
	return in;
}	
/*Parsed namespaces are placed here*/
public List<NameSpace> getNameSpaces() {
		return nameSpaces;
}
/*Parsed nodeTypes are placed here*/
public List<NodeTypeData> getNodeTypes() {
		return nodeTypes;
}
/* Extend RecognitionException adding desired constructors */
class TreeWalkerException extends RecognitionException {

    private String    message;
    private Throwable cause;

    public TreeWalkerException(String message, Throwable cause) {
      this.message = message;
      this.cause = cause;
    }

    public TreeWalkerException(String message) {
      this.message = message;
    }

    public Throwable getCause() {
      return (cause == this ? null : cause);
    }

    public String getMessage() {
      return message;
    }
  }
/*Class containing namespace definition*/
class NameSpace{
	private String prefix;
	private String uri;
	public NameSpace() {
		super();
	}

	public NameSpace(String prefix, String uri) {
		this.prefix = prefix;
		this.uri = uri;
	}
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String toString(){
		return "<"+prefix+"='"+uri+"'>";
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		NameSpace other = (NameSpace) obj;
		if (!this.uri.equalsIgnoreCase(other.uri)){
			return false;
		}
		if (this.prefix.compareTo(other.prefix)!=0){
			return false;
		}
		return true;
	}
}
/*method for converting from string to InternalQName*/
private InternalQName toName(String name) throws RecognitionException{
	try {
		return locationFactory.parseJCRName(name).getInternalName();
	} catch (RepositoryException e) {
		throw new TreeWalkerException(e.getMessage(), e);
	}	
}
}

/* Main method. Parameter is an instance of NamespaceRegistryImpl to have 
 * an ability of registering namespaces and parsing strings to InternalQNames */
cnd	[NamespaceRegistryImpl namespaceRegistryImpl]
	@init	{
		    this.namespaceRegistry = namespaceRegistryImpl;
		    this.locationFactory =  new LocationFactory(namespaceRegistry);
		}
	:	(namespace|nodetypedef)*;
	
		catch[RecognitionException e]{
			throw e;
		}

/*==================== NAMESPACES ======================*/
/* Namespace definition rule */
namespace
	scope	{
			NameSpace ns; /*local for evry namespace variable*/
		}
	@init	{
			$namespace::ns = new NameSpace();
		}
	@after	{
			nameSpaces.add($namespace::ns);/*Adding to global list*/
			if (!namespaceRegistry.isUriRegistered($namespace::ns.getUri())){
				namespaceRegistry.registerNamespace($namespace::ns.getPrefix(), $namespace::ns.getUri());
			} else if (!namespaceRegistry.getPrefix($namespace::ns.getUri()).equals($namespace::ns.getPrefix())) 
			{
				namespaceRegistry.registerNamespace($namespace::ns.getPrefix(), $namespace::ns.getUri());
			}
		}
		
	: 	^(NAMESPACE prefix uri);
	
		catch[RepositoryException e]{
			throw new TreeWalkerException(e.getMessage(), e);
		}	
	  	catch[RecognitionException e]{
			throw e;
		}
		
prefix
	:	^(PREFIX name)
		{
			$namespace::ns.setPrefix(unQuote($name.text));
		};
		
		catch[RecognitionException e]{
			throw e;
		}
		
uri
	:	^(URI STRING)
		{
		$namespace::ns.setUri(unQuote($STRING.text));
		};
		
		catch[RecognitionException e]{
			throw e;
		}
/*==================== NODETYPES ======================*/
/*NODETYPE DEFINITION*/
nodetypedef
	scope	{
			/*local for every nodetypedef loop variable*/
			NodeTypeDataBuilder nodeTypeBuilder;
		}
	@init	{
			$nodetypedef::nodeTypeBuilder= new NodeTypeDataBuilder();
		}
	@after	{
			/* Add new NodeType after NodeTypeDataBuilder is filled up */
			nodeTypes.add($nodetypedef::nodeTypeBuilder.build());
		}
	
	:	^(NODETYPEDEF nodetypename supertypes? nodetypeattributes? propertydef* childnodedef*) ;
		
		catch[RecognitionException e]{
			throw e;
		}
	
/*NODE TYPE DEFINITION PARTS*/
nodetypename
	:	^(NODETYPENAME name)
		{
			$nodetypedef::nodeTypeBuilder.setName(toName($name.text));
		};
		
		catch[RecognitionException e]{
			throw e;
		}	

/*NODE TYPE SUPERTYPES*/
supertypes
		
	:	{
			List<InternalQName> l = new ArrayList<InternalQName>();
		}	
		
		^(SUPERTYPES (n=name{l.add(toName($n.text));})+)
		
		{
			InternalQName[] list = new InternalQName[l.size()];
	                l.toArray(list);
	                $nodetypedef::nodeTypeBuilder.setSupertypes(list);
                }
	
	|	^(SUPERTYPES VARIANT);
		
		catch[RecognitionException e]{
			throw e;
		}	

/*NODE TYPE ATTRIBUTES*/
nodetypeattributes
	:	^(NODETYPEATTRIBUTES nodetypeattribute+);
		
		catch[RecognitionException e]{
			throw e;
		}
		
nodetypeattribute
	:	opt_orderable
	|	opt_mixin
	|	opt_abstract
	|	opt_noquery
	|	opt_primaryitem;	
		
		catch[RecognitionException e]{
			throw e;
		}

/*NODETYPE OPTIONS*/
opt_orderable
	:	^(ORDERABLE VARIANT?)
		{
			$nodetypedef::nodeTypeBuilder.setOrderable(true);
		};
		
		catch[RecognitionException e]{
			throw e;
		}
		
opt_mixin
	:	^(MIXIN VARIANT?)
		{
			$nodetypedef::nodeTypeBuilder.setMixin(true);
		};
		
		catch[RecognitionException e]{
			throw e;
		}
opt_abstract
	:	^(ABSTRACT VARIANT?)
		{
			$nodetypedef::nodeTypeBuilder.setAbstract(true);
		};
		
		catch[RecognitionException e]{
			throw e;
		}
		
opt_noquery
	:	^(NOQUERY	VARIANT?)
		{
			$nodetypedef::nodeTypeBuilder.setQueryable(false);
		};
		
		catch[RecognitionException e]{
			throw e;
		}
		
opt_primaryitem
	:	^(PRIMARYITEM STRING)
		{
			$nodetypedef::nodeTypeBuilder.setPrimaryItemName(toName(unQuote($STRING.text)));
		}
		
	|	^(PRIMARYITEM VARIANT)
		{
			$nodetypedef::nodeTypeBuilder.setPrimaryItemName(null);
		};
		
		catch[RecognitionException e]{
			throw e;
		}

/*==================== PROPERTIES ======================*/
/*PROPERTY DEFINITION*/
propertydef
	scope	{
			PropertyDefinitionDataBuilder propertyDefinitionBuilder;
		}
	@init	{
			$propertydef::propertyDefinitionBuilder =$nodetypedef::nodeTypeBuilder.newPropertyDefinitionDataBuilder();
		}
	:	^(PROPERTYDEF propertyname propertytype? propertyparams*);
	
		catch[RecognitionException e]{
			throw e;
		}
		
/*PROPERTY PARAMETERS*/
propertyparams
	:	{
			List<String> l = new ArrayList<String>();
		}
		
		^(DEFAULTVALUES (s=STRING{l.add(unQuote($s.text));})+)
	
		{
			String[] list = new String[l.size()];
			l.toArray(list);
			$propertydef::propertyDefinitionBuilder.setDefaultValues(list);
                }
		
		
	|	^(DEFAULTVALUES VARIANT)
		/* Do nothing in this case */		
	|	^(PROPERTYATTRIBUTE propertyattribute)
		/* Do nothing in this case */		
	|  	{
			List<String> l = new ArrayList<String>();
		}
		
		^(VALUECONSTRAINTS (s=STRING{l.add(unQuote($s.text));})+)
		
		{
			String[] list = new String[l.size()];
			l.toArray(list);
			$propertydef::propertyDefinitionBuilder.setValueConstraints(list);
                }
		
	|  	^(VALUECONSTRAINTS VARIANT)
		{
		/*do nothing!*/
		};
	
		catch[RecognitionException e]{
			throw e;
		}
		
propertyname
	:	^(PROPERTYNAME pn=name)
		{
			$propertydef::propertyDefinitionBuilder.setName(toName($name.text));
		};
	
		catch[RecognitionException e]{
			throw e;
		}
			
propertytype 
	:	^(PROPERTYTYPE propertytypes)
		{
			$propertydef::propertyDefinitionBuilder.setRequiredType($propertytypes.type);
		};
	
		catch[RecognitionException e]{
			throw e;
		}	
		
propertytypes returns [int type]
	:	PT_STRING 
		{
			type = PropertyType.STRING;
		}
	|	PT_BINARY 
		{
			type = PropertyType.BINARY;
		}
	|	PT_LONG 
		{
			type = PropertyType.LONG;
		}
	|	PT_DOUBLE 
		{
			type = PropertyType.DOUBLE;
		}
	|	PT_BOOLEAN 
		{
			type = PropertyType.BOOLEAN;
		}
	|	PT_DATE 
		{
			type = PropertyType.DATE;
		}
	|	PT_NAME 
		{
			type = PropertyType.NAME;
		}
	|	PT_PATH 
		{	
			type = PropertyType.PATH;
		}
	|	PT_REFERENCE 
		{
			type = PropertyType.REFERENCE;
		}
	|	PT_WEAKREFERENCE
		{
			type = PropertyType.UNDEFINED;
		}
	|	PT_DECIMAL 
		{
			type = PropertyType.UNDEFINED;
		}
	|	PT_URI 
		{
			type = PropertyType.UNDEFINED;
		}
	|	PT_UNDEFINED
		{
			type = PropertyType.UNDEFINED;
		}
	|	VARIANT
		{
			type = PropertyType.STRING;
		};
		
		catch[RecognitionException e]{
			throw e;
		}
		
propertyattribute
	:	atr_autocreated 
		{
			$propertydef::propertyDefinitionBuilder.setAutoCreated(true);
		}
	|	atr_mandatory 
		{
			$propertydef::propertyDefinitionBuilder.setMandatory(true);
		}
	|	atr_protected
		{
			$propertydef::propertyDefinitionBuilder.setProtected(true);
		}
	|	atr_OPV
		{
			$propertydef::propertyDefinitionBuilder.setOnParentVersion($atr_OPV.action);
		}
	| 	atr_multiple 
		{
			$propertydef::propertyDefinitionBuilder.setMultiple(true);
		}
	|	atr_queryops 
		{
			$propertydef::propertyDefinitionBuilder.setQueryOperators($atr_queryops.list);		
		}
	|	atr_nofulltext 
		{
			$propertydef::propertyDefinitionBuilder.setFullTextSearchable(false);
		}
	|	atr_noqueryorder
		{
			$propertydef::propertyDefinitionBuilder.setQueryOrderable(false);
		};
		
		catch[RecognitionException e]{
			throw e;
		}	
		
/*PROPERTY ATTRIBUTES*/
atr_autocreated
	:	^(AUTOCREATED VARIANT?);
			
		catch[RecognitionException e]{
			throw e;
		}
		
atr_mandatory
	:	^(MANDATORY VARIANT?);
			
		catch[RecognitionException e]{
			throw e;
		}
atr_protected
	:	^(PROTECTED VARIANT?);
			
		catch[RecognitionException e]{
			throw e;
		}
		
atr_OPV	returns [int action]
	:	^(OPV T_OPV)
		{
			action = OnParentVersionAction.valueFromName($T_OPV.text.toUpperCase());
		}
		
	|	^(OPV VARIANT)
		{
			action = OnParentVersionAction.COPY;
		};
					
		catch[RecognitionException e]{
			throw e;
		}
		
atr_multiple
	:	^(MULTIPLE VARIANT?);
		
		catch[RecognitionException e]{
			throw e;
		}
atr_queryops returns [String[\] list]
	:	^(QUERYOPS operators)
		{
			list=$operators.list;
		};
		
		catch[RecognitionException e]{
			throw e;
		}
		
/*Operators*/
operators returns [String[\] list]
	:	o=STRING 
		{
			String opString = $o.text;
			opString=unQuote(opString.toUpperCase());
			String[] ops = opString.split(",");
			List<String> queryOps = new LinkedList<String>();
			for (String op : ops) {
				String s = op.trim();
				if (!Pattern.matches("(<=)|(>=)|(<)|(>)|(=)|(<>)|(LIKE)", s)){
					throw new TreeWalkerException("Wrong operator:"+s);
				}
				queryOps.add(s);
			}
			list=queryOps.toArray(new String[queryOps.size()]);
		}
		
	|	'?'
		{
			//list = Operator.getAllQueryOperators();
			list = new String[0];
		};
					
		catch[RecognitionException e]{
			throw e;
		}
		
atr_nofulltext
	:	^(NOFULLTEXT VARIANT?);
		catch[RecognitionException e]{
			throw e;
		}
atr_noqueryorder
	:	^(NOQUERYORDER VARIANT?);
		catch[RecognitionException e]{
			throw e;
		}
atr_sns
	:	^(SNS VARIANT?);
		catch[RecognitionException e]{
			throw e;
		}
		
/*==================== CHILDREN ======================*/		
/*CHILD NODE DEFINITION*/
childnodedef
	scope	{
			NodeDefinitionDataBuilder childDefinitionBuilder;
		}
	@init	{
			$childnodedef::childDefinitionBuilder = $nodetypedef::nodeTypeBuilder.newNodeDefinitionDataBuilder();
		}
	:	^(CHILDDEF nodename requiredtypes? defaulttype? nodeattributes*);
					
		catch[RecognitionException e]{
			throw e;
		}
		
nodename
	:	^(NODENAME name)
		{
			$childnodedef::childDefinitionBuilder.setName(toName($name.text));
		};
	
		catch[RecognitionException e]{
			throw e;
		}
		
requiredtypes
	:	{ 	
			List<InternalQName> l = new ArrayList<InternalQName>();
		}
		^(REQUIREDTYPES (n=name{l.add(toName($n.text));})+)

		{
			InternalQName[] list = new InternalQName[l.size()];
	                l.toArray(list);
	                $childnodedef::childDefinitionBuilder.setRequiredPrimaryTypes(list);
                }

	|	^(REQUIREDTYPES VARIANT);
		/* Do nothing in this case */		
				
		catch[RecognitionException e]{
			throw e;
		}
		
defaulttype	
	:	^(DEFAULTTYPE name)
		{
			$childnodedef::childDefinitionBuilder.setDefaultPrimaryType(toName($name.text));
		}
	|	^(DEFAULTTYPE VARIANT);
		/* Do nothing in this case */		

	
		catch[RecognitionException e]{
			throw e;
		}
		
nodeattributes
	:	^(NODEATTRIBUTE nodeattribute);
		
		catch[RecognitionException e]{
			throw e;
		}
	
nodeattribute
	:	atr_autocreated 
		{
			$childnodedef::childDefinitionBuilder.setAutoCreated(true);
		}
	|	atr_mandatory 
		{
			$childnodedef::childDefinitionBuilder.setMandatory(true);
		}
	|	atr_protected 
		{
			$childnodedef::childDefinitionBuilder.setProtected(true);
		}
	|	atr_OPV 
		{
			$childnodedef::childDefinitionBuilder.setOnParentVersion($atr_OPV.action);
		}
	|	atr_sns
		{
			$childnodedef::childDefinitionBuilder.setAllowsSameNameSiblings(true);
		};
		
		catch[RecognitionException e]{
			throw e;
		}

/*==================== GENERAL RULES ======================*/
/* name is a string that can be the same as registered words */			
name	returns [String text]
	:	STRING
		{
			text = unQuote($STRING.text);
		}
	|	registeredWords
		{
			text = $registeredWords.text;
		};
			
		catch[RecognitionException e]{
			throw e;
		}
/* List of registred words */		
registeredWords returns [String text]
	:	v=T_ORDERABLE 	{text=$v.getText();}
	|	v=T_ORD 	{text=$v.getText();}
	|	v=T_O 		{text=$v.getText();}
	|	v=T_MIXIN 	{text=$v.getText();}
	|	v=T_MIX 	{text=$v.getText();}
	|	v=T_M 		{text=$v.getText();}
	|	v=T_ABSTRACT 	{text=$v.getText();}
	|	v=T_ABS 	{text=$v.getText();}
	|	v=T_A 		{text=$v.getText();}
	|	v=T_NOQUERY 	{text=$v.getText();}
	|	v=T_NQ 		{text=$v.getText();}
	|	v=T_AUTOCREATED 	{text=$v.getText();}
	|	v=T_AUT 	{text=$v.getText();}
	|	v=T_MANDATORY 	{text=$v.getText();}
	|	v=T_MAN 	{text=$v.getText();}
	|	v=T_PROTECTED 	{text=$v.getText();}
	|	v=T_PRO 	{text=$v.getText();}
	|	v=T_P 		{text=$v.getText();}
	|	v=T_OPV 	{text=$v.getText();}
	|	v=T_OPV_OPV 	{text=$v.getText();}
	|	v=T_PRIMARY	{text=$v.getText();}
	| 	v=T_MULTIPLE 	{text=$v.getText();}
	|	v=T_MUL 	{text=$v.getText();}
	|	v=T_QUERYOPS 	{text=$v.getText();}
	|	v=T_QOP 	{text=$v.getText();}
	|	v=T_NOFULLTEXT 	{text=$v.getText();}
	|	v=T_NOF 	{text=$v.getText();}
	|	v=T_NOQUERYORDER 	{text=$v.getText();}
	|	v=T_NQORD 	{text=$v.getText();}
	|	v=T_SNS 	{text=$v.getText();}
	|	v=PT_STRING 	{text=$v.getText();}
	|	v=PT_BINARY 	{text=$v.getText();}
	|	v=PT_LONG 	{text=$v.getText();}
	|	v=PT_DOUBLE 	{text=$v.getText();}
	|	v=PT_BOOLEAN 	{text=$v.getText();}
	|	v=PT_DATE 	{text=$v.getText();}
	|	v=PT_NAME 	{text=$v.getText();}
	|	v=PT_PATH 	{text=$v.getText();}
	|	v=PT_REFERENCE 	{text=$v.getText();}
	|	v=PT_WEAKREFERENCE 	{text=$v.getText();}
	|	v=PT_DECIMAL 	{text=$v.getText();}
	|	v=PT_URI 	{text=$v.getText();}
	|	v=PT_UNDEFINED	{text=$v.getText();};
		
		catch[RecognitionException e]{
			throw e;
		}
