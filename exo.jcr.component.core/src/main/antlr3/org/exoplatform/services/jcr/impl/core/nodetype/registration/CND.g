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
/*Compact NODE TYPE DEFINITION GRAMMAR*/

grammar CND;

options {
	output = AST;
}

/*DEFINING VIRTUAL TOKENS*/
tokens {
	NAMESPACE;
		PREFIX;
		URI;
	NODETYPEDEF;
		NODETYPENAME;
		SUPERTYPES;
		NODETYPEATTRIBUTES;
			ORDERABLE;
			MIXIN;
			ABSTRACT;
			NOQUERY;
			PRIMARYITEM;
		PROPERTYDEF;
			PROPERTYNAME;
			PROPERTYTYPE;
			DEFAULTVALUES;
			PROPERTYATTRIBUTE;
				AUTOCREATED;
				MANDATORY;
				PROTECTED;
				OPV;
				MULTIPLE;
				QUERYOPS;
				NOFULLTEXT;
				NOQUERYORDER;
				SNS;
			VALUECONSTRAINTS;
		CHILDDEF;
			NODENAME;
			REQUIREDTYPES;
			DEFAULTTYPE;
			NODEATTRIBUTE;
}
/* Parser's class header */
@parser::header {
package org.exoplatform.services.jcr.impl.core.nodetype.registration;

import java.util.LinkedList;
}
/* Lexer's class header */
@lexer::header{
package org.exoplatform.services.jcr.impl.core.nodetype.registration;

import java.util.LinkedList;
}
/* Parser's class members */
@parser::members{

private List<String> errors = new LinkedList<String>();
@Override
public void emitErrorMessage(String msg) {
	super.emitErrorMessage(msg);
	errors.add(msg);
}
public List<String> getErrors() {
	return errors;
}
public boolean hasError(){
	return (errors.size()>0);
}
    
class ParserRecognitionException extends RecognitionException {
	private Throwable cause;
	private String    message;

	public ParserRecognitionException(String message) {
		this.message = message;
	}

	public Throwable getCause() {
		return (cause==this ? null : cause);
	}

	public String getMessage() {
		return message;
	}
}   
}
/* Lexer's class members */
@lexer::members {
private List<String> errors = new LinkedList<String>();
@Override
public void emitErrorMessage(String msg) {
	super.emitErrorMessage(msg);
	errors.add(msg);
}
public List<String> getErrors() {
	return errors;
}
public boolean hasError(){
	return (errors.size()>0);
}
}


/*======================================================*/
/*			PARSER RULES			*/
/*======================================================*/

/*Main loop*/
cnd	:	(( namespace)|( nodetypedef))*  EOF!;

/*namespace definition*/
namespace
	:	'<'  prefix  '='  uri  '>' 
	-> 	^(NAMESPACE prefix uri);
prefix	:	name 	-> ^(PREFIX name);
uri	:	STRING 	-> ^(URI STRING);

/*NODETYPE DEFINITION*/
nodetypedef
	:	nodetypename ( supertypes)? ( nodetypeattributes)? (( propertydef) | ( childnodedef))* 
	-> 	^(NODETYPEDEF nodetypename supertypes? nodetypeattributes? propertydef* childnodedef*) ;
	
/*NODE TYPE DEFINITION PARTS*/
nodetypename
	:	'[' name ']' -> ^(NODETYPENAME name);	

/*NODE TYPE SUPERTYPES*/
supertypes
	:	'>'  supertypeVal 
	->	^(SUPERTYPES supertypeVal);
supertypeVal
	:	 (name ( ','!  name)*)
	|	 VARIANT;

/*NODE TYPE ATTRIBUTES*/
nodetypeattributes
	:	nodetypeattribute ( nodetypeattribute)* 
	->	^(NODETYPEATTRIBUTES nodetypeattribute+);
nodetypeattribute
	:	opt_orderable|opt_mixin|opt_abstract|opt_noquery|opt_primaryitem;	

/*NODETYPE OPTIONS*/
opt_orderable
	:	(T_ORDERABLE|T_ORD|T_O) (	VARIANT)? -> ^(ORDERABLE VARIANT?);
opt_mixin
	:	(T_MIXIN|T_MIX|T_M)	(	VARIANT)? -> ^(MIXIN VARIANT?);
opt_abstract
	:	(T_ABSTRACT|T_ABS|T_A)	(	VARIANT)? -> ^(ABSTRACT VARIANT?);
opt_noquery
	:	(T_NOQUERY|T_NQ)	(	VARIANT)? -> ^(NOQUERY	VARIANT?);
opt_primaryitem
	:	(T_PRIMARY | '!')  (v=STRING | v='?')  -> ^(PRIMARYITEM $v);

/*PROPERTY DEFINITION*/
propertydef
	:	propertyname ( propertytype)? ( propertyparams)* 
	->	^(PROPERTYDEF propertyname propertytype? propertyparams*);

/*PROPERTY PARAMETERS*/
propertyparams
	:	defaultvalues | propertyattributes | valueconstraints;
propertyname
	:	'-'  name -> ^(PROPERTYNAME name);
propertytype
	:	'('propertytypes')' -> ^(PROPERTYTYPE propertytypes);
propertytypes
	:	PT_STRING | PT_BINARY | PT_LONG | PT_DOUBLE | PT_BOOLEAN 
	|	PT_DATE | PT_NAME | PT_PATH | PT_REFERENCE | PT_WEAKREFERENCE
	|	PT_DECIMAL | PT_URI 
	|	PT_UNDEFINED
	|	'*' 	-> PT_UNDEFINED
	|	VARIANT;
defaultvalues
	:  	'='  defaultvalueslist -> ^(DEFAULTVALUES defaultvalueslist);
defaultvalueslist
	:	 (STRING ( ','!  STRING)*)
	|	 VARIANT;
propertyattributes
	:	propertyattribute -> ^(PROPERTYATTRIBUTE propertyattribute);

propertyattribute
	:	atr_autocreated | atr_mandatory | atr_protected | atr_OPV 
	| 	atr_multiple | atr_queryops | atr_nofulltext | atr_noqueryorder;

/*PROPERTY ATTRIBUTES*/
atr_autocreated
	:	(T_AUTOCREATED|T_AUT|T_A)( VARIANT)?	-> ^(AUTOCREATED VARIANT?);
atr_mandatory
	:	(T_MANDATORY|T_MAN|T_M)	 ( VARIANT)?	-> ^(MANDATORY VARIANT?);
atr_protected
	:	(T_PROTECTED|T_PRO|T_P)	 ( VARIANT)?	-> ^(PROTECTED VARIANT?);
atr_OPV	:	(T_OPV)			-> ^(OPV T_OPV)
	|	(T_OPV_OPV  v='?')	-> ^(OPV VARIANT);
atr_multiple
	:	(T_MULTIPLE|T_MUL|'*')	 ( VARIANT)?	-> ^(MULTIPLE VARIANT?);
atr_queryops
	:	(T_QUERYOPS|T_QOP)  (operators)	-> 	^(QUERYOPS operators);

/*Operators as strings.
Tey should be recognized in TreeWalker*/
operators:	STRING | '?';

atr_nofulltext
	:	(T_NOFULLTEXT|T_NOF)	( VARIANT)?	-> ^(NOFULLTEXT VARIANT?);
atr_noqueryorder
	:	(T_NOQUERYORDER|T_NQORD)( VARIANT)?	-> ^(NOQUERYORDER VARIANT?);
atr_sns
	:	(T_SNS|'*')		( VARIANT)?	-> ^(SNS VARIANT?);

/*Constraints*/
valueconstraints
	:  	'<'  valueconstraintslist -> ^(VALUECONSTRAINTS valueconstraintslist);
valueconstraintslist
	:	 (STRING ( ','!  STRING)*)
	|	 VARIANT;

/*CHILD NODE DEFINITION*/
childnodedef
	:	nodename ( requiredtypes)? ( defaulttype)? ( nodeattributes)* 
	->	^(CHILDDEF nodename requiredtypes? defaulttype? nodeattributes*);
nodename
	:	'+'  name -> ^(NODENAME name);
requiredtypes
	:	 '('  requiredtypesVal  ')' -> ^(REQUIREDTYPES requiredtypesVal);
requiredtypesVal
	:	 (name ( ','!  name)*)
	|	 VARIANT;
defaulttype
	:	 '='  defaulttypeVal -> ^(DEFAULTTYPE defaulttypeVal);
defaulttypeVal
	:	 name
	|	 VARIANT;
nodeattributes
	:	nodeattribute -> ^(NODEATTRIBUTE nodeattribute);
nodeattribute
	:	atr_autocreated | atr_mandatory | atr_protected | atr_OPV | atr_sns;	

/*Allow names be strings or registered words*/
name	:	STRING -> STRING
	|	registeredWords -> registeredWords;
/* List of registred words */	
registeredWords
	:	T_ORDERABLE | T_ORD | T_O | T_MIXIN | T_MIX | T_M | T_ABSTRACT 
	|	T_ABS | T_A | T_NOQUERY |T_NQ |	T_AUTOCREATED | T_AUT | T_MANDATORY 
	|	T_MAN | T_PROTECTED | T_PRO | T_P | T_OPV | T_OPV_OPV | T_PRIMARY
	| 	T_MULTIPLE | T_MUL | T_QUERYOPS | T_QOP | T_NOFULLTEXT | T_NOF 
	|	T_NOQUERYORDER | T_NQORD | T_SNS | PT_STRING | PT_BINARY | PT_LONG 
	|	PT_DOUBLE | PT_BOOLEAN | PT_DATE | PT_NAME | PT_PATH | PT_REFERENCE 
	|	PT_WEAKREFERENCE | PT_DECIMAL | PT_URI | PT_UNDEFINED;

/*======================================================*/
/*			LEXER RULES			*/
/*======================================================*/

/*TERMINATORS AND OPERATORS*/
EQUAL	:	'=';
LESS	:	'<';
MORE	:	'>';
VARIANT	:	'?';
LSBr	:	'[';
RSBr	:	']';
LPAR	:	'(';
RPAR	:	')';
PLUS	:	'+';
MINUS	:	'-';
QUOTE	:	'\'';
//DOUBLEQUOTE:	'"';
PRIM	:	'!';

/* Node type attributes */

/*TOKEN ORDERABLE*/
T_ORDERABLE
	:	O R D E R A B L E;//'orderable';
T_ORD	:	O R D;//'ord';
T_O	:	O;//'o';

/*TOKEN MIXIN*/
T_MIXIN	:	M I X I N;//
T_MIX	:	M I X;//'mix';
T_M	:	M;//'m';	//also: T_MANDATORY in short form

/*TOKEN ABSTRACT*/
T_ABSTRACT
	:	A B S T R A C T;//'abstract';
T_ABS	:	A B S;//'abs';
T_A	:	A;//'a';	//also:T_AUTOCREATED in short form

/*TOKEN NO_QUERY*/
T_NOQUERY
	:	N O Q U E R Y;//'noquery';
T_NQ	:	N Q;//'nq';

/*TOKEN PRIMARY*/
T_PRIMARY
	:	P R I M A R Y I T E M; //('primaryitem' | '!');

/*TOKEN AUTOCREATED*/
T_AUTOCREATED
	:	A U T O C R E A T E D;//'autocreated';
T_AUT	:	A U T;//'aut';

/*TOKEN MANDATORY*/
T_MANDATORY
	:	M A N D A T O R Y;//'mandatory';
T_MAN	:	M A N;//'man';

/*TOKEN PROTECTED*/
T_PROTECTED
	:	P R O T E C T E D;//'protected';
T_PRO	:	P R O;//'pro';
T_P	:	P;//'p';

/*TOKENS OPV*/
T_OPV	:	(C O P Y)
	|	(V E R S I O N)
	|	(I N I T I A L I Z E)
	|	(C O M P U T E)
	|	(I G N O R E)
	|	(A B O R T);
T_OPV_OPV
	:	(O P V);	

/*TOKEN MULTIPLE*/
T_MULTIPLE
	:	M U L T I P L E;//'multiple';
T_MUL	:	M U L;//'mul';

/*QUERRY OPS*/
T_QUERYOPS
	:	Q U E R Y O P S;//'queryops';
T_QOP
	:	Q O P;//'qop';

/*TOKEN NOFULLTEXT*/
T_NOFULLTEXT
	:	N O F U L L T E X T;//'nofulltext';
T_NOF	:	N O F;//'nof';

/*TOKEN NOQUERYORDER*/
T_NOQUERYORDER
	:	N O Q U E R Y O R D E R ;//'noqueryorder';
T_NQORD	:	N Q O R D;//'nqord';

/*SNS TOKEN*/
T_SNS	:	S N S;//'sns';


/*PROPERTY TYPES*/
PT_STRING
	:	(S T R I N G);
PT_BINARY
	:	(B I N A R Y);
PT_LONG	:	(L O N G);
PT_DOUBLE
	:	(D O U B L E);
PT_BOOLEAN
	:	(B O O L E A N);
PT_DATE	:	(D A T E);
PT_NAME	:	(N A M E);
PT_PATH	:	(P A T H);
PT_REFERENCE
	:	(R E F E R E N C E);
PT_WEAKREFERENCE
	:	(W E A K R E F E R E N C E);
PT_DECIMAL
	:	(D E C I M A L);
PT_URI	:	(U R I);
PT_UNDEFINED
	:	(U N D E F I N E D)|'*';
	
COMMENT
    :   '//' ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;}
    |   '/*' ( options {greedy=false;} : . )* '*/' {$channel=HIDDEN;}
    ;
    
STRING    : QUOTEDSTRING
          | UNQUOTEDSTRING
          | DOUBLEQUOTEDSTRING
          ;

fragment DOUBLEQUOTEDSTRING 
	:  	'"' ( ESC_SEQ | ~('\\'|'"') )* '"';
fragment QUOTEDSTRING 
	:  	QUOTE ( ESC_SEQ | ~('\\'|'\'') )* QUOTE;
fragment UNQUOTEDSTRING
	:	(ESC_SEQ|CHAR)+;
fragment CHAR
	:	(BASELATIN | DIGIT | ':' | '_');

fragment DIGIT
	:	('0'..'9');

fragment BASELATIN
	:	('a'..'z'|'A'..'Z');

fragment
HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

fragment
ESC_SEQ
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    |   UNICODE_ESC
    |   OCTAL_ESC;
fragment
OCTAL_ESC
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7');
fragment
UNICODE_ESC
    :   '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;
    
/*for case insensivity*/
fragment A:('a'|'A');
fragment B:('b'|'B');
fragment C:('c'|'C');
fragment D:('d'|'D');
fragment E:('e'|'E');
fragment F:('f'|'F');
fragment G:('g'|'G');
fragment H:('h'|'H');
fragment I:('i'|'I');
fragment J:('j'|'J');
fragment K:('k'|'K');
fragment L:('l'|'L');
fragment M:('m'|'M');
fragment N:('n'|'N');
fragment O:('o'|'O');
fragment P:('p'|'P');
fragment Q:('q'|'Q');
fragment R:('r'|'R');
fragment S:('s'|'S');
fragment T:('t'|'T');
fragment U:('u'|'U');
fragment V:('v'|'V');
fragment W:('w'|'W');
fragment X:('x'|'X');
fragment Y:('y'|'Y');
fragment Z:('z'|'Z');

WS  	:   ( ' ' | '\t' | '\r' | '\n')+{$channel=HIDDEN;};
