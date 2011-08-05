%token Term_InterfacesOpt
%token Term_SuperOpt
%token Term_ExtendsInterfacesOpt
%token Term_PrimitiveType
%token Term_ImportDeclarationsOpt
%token LSHR
%token SHR
%token Term_PackageDeclarationOpt
%token SHL
%token MOD
%token CARET
%token OR
%token AND
%token DIV
%token MUL
%token MINUS
%token PLUS
%token DEC
%token INC
%token OROR
%token ANDAND
%token GE
%token LE
%token NE
%token EQ
%token COLON
%token QUEST
%token TILDE
%token BANG
%token GT
%token LT
%token Term_Type
%token ASN
%token DOT
%token CM
%token SM
%token RB
%token LB
%token RC
%token LC
%token RP
%token LP
%token TRY
%token IDENT
%token WHILE
%token VOID
%token THROWS
%token THROW
%token THIS
%token SYNCHRONIZED
%token SWITCH
%token SUPER
%token STATIC
%token SUSPEND
%token RETURN
%token NEW
%token INTERFACE
%token INSTANCEOF
%token IF
%token FOR
%token FINALLY
%token ELSE
%token DO
%token CLASS
%token CATCH
%token CASE

%%

Goal :  CompilationUnit
;

IfThenElseStatementNoShortIf :  IF LP Expression RP StatementNoShortIf ELSE StatementNoShortIf
;

PostFixExpression :  PostDecrementExpression
|  PostIncrementExpression
|  Primary
|  Name
;

LabeledStatementNoShortIf :  IDENT COLON StatementNoShortIf
;

StatementNoShortIf :  ForStatementNoShortIf
|  WhileStatementNoShortIf
|  IfThenElseStatementNoShortIf
|  LabeledStatementNoShortIf
|  StatementWithoutTrailingSubstatement
;

ForStatement :  FOR LP ForInitOpt SM ExpressionOpt SM ForUpdateOpt RP Statement
;

WhileStatement :  WHILE LP Expression RP Statement
;

IfThenElseStatement :  IF LP Expression RP StatementNoShortIf ELSE Statement
;

IfThenStatement :  IF LP Expression RP Statement
;

LabeledStatement :  IDENT COLON Statement
;

InterfaceDeclaration :  ModifiersOpt INTERFACE IDENT InterfaceBody
;

StatementWithoutTrailingSubstatement :  TryStatement
|  DoStatement
|  ThrowStatement
|  SwitchStatement
|  SynchronizedStatement
|  Block
|  ExpressionStatement
|  ReturnStatement
|  EmptyStatement
;

ClassDeclaration :  ModifiersOpt CLASS IDENT ClassBody
;

LocalVariableDeclaration :  Term_Type VariableDeclarators
;

DimExpr :  LB Expression RB
;

Statement :  IfThenStatement
|  LabeledStatement
|  StatementWithoutTrailingSubstatement
|  ForStatement
|  WhileStatement
|  IfThenElseStatement
;

DimExprs :  DimExprs DimExpr
|  DimExpr
;

LocalVariableDeclarationStatement :  LocalVariableDeclaration SM
;

Dims :  LB RB
|  Dims LB RB
;

BlockStatement :  Statement
|  LocalVariableDeclarationStatement
;

DimsOpt : 
|  Dims
;

TypeDeclaration :  InterfaceDeclaration
|  ClassDeclaration
;

ArrayAccess :  PrimaryNoNewArray LB Expression RB
|  Name LB Expression RB
;

TypeDeclarations :  TypeDeclaration
|  TypeDeclarations TypeDeclaration
;

FieldAccess :  Primary DOT IDENT
;

CMOpt : 
;

VariableInitializers :  VariableInitializers CM VariableInitializer
|  VariableInitializer
;

VariableInitializersOpt :  VariableInitializers
;

TypeDeclarationsOpt :  TypeDeclarations
;

AbstractMethodDeclaration :  MethodHeader SM
;

ArrayCreationExpression :  NEW ClassOrInterfaceType DimExprs DimsOpt
|  NEW Term_PrimitiveType DimExprs DimsOpt
;

ConstantDeclaration :  FieldDeclaration
;

PrimaryNoNewArray :  FieldAccess
|  LP Expression RP
|  ClassInstanceCreationExpression
|  MethodInvocation
|  ArrayAccess
;

Primary :  ArrayCreationExpression
|  PrimaryNoNewArray
;

InterfaceMemberDeclaration :  AbstractMethodDeclaration
|  ConstantDeclaration
;

SimpleName :  IDENT
;

CatchClause :  CATCH LP FormalParameter RP Block
;

InterfaceMemberDeclarations :  InterfaceMemberDeclarations InterfaceMemberDeclaration
|  InterfaceMemberDeclaration
;

Finally :  FINALLY Block
;

InterfaceMemberDeclarationsOpt :  InterfaceMemberDeclarations
;

InterfaceBody :  LC InterfaceMemberDeclarationsOpt RC
;

Catches :  CatchClause
|  Catches CatchClause
;

CatchesOpt :  Catches
| 
;

ClassType :  ClassOrInterfaceType
;

Name :  SimpleName
;

ArrayType :  Name LB RB
|  ArrayType LB RB
;

ClassOrInterfaceType :  Name
;

ArgumentList :  ArgumentList CM Expression
|  Expression
;

ArgumentListOpt :  ArgumentList
| 
;

BlockStatements :  BlockStatements BlockStatement
|  BlockStatement
;

BlockStatementsOpt : 
|  BlockStatements
;

ExplicitConstructorInvocation :  THIS LP ArgumentListOpt RP SM
|  SUPER LP ArgumentListOpt RP SM
;

StatementExpressionList :  StatementExpressionList CM StatementExpression
|  StatementExpression
;

ExplicitConstructorInvocationOpt : 
|  ExplicitConstructorInvocation
;

ConstructorBody :  LC ExplicitConstructorInvocationOpt BlockStatementsOpt RC
;

ConstructorDeclarator :  SimpleName LP FormalParameterListOpt RP
;

ReferenceType :  ArrayType
|  ClassOrInterfaceType
;

ForUpdate :  StatementExpressionList
;

Block :  LC BlockStatementsOpt RC
;

ForUpdateOpt :  ForUpdate
;

AssignmentOperator :  ASN
;

LeftHandSide :  FieldAccess
|  Name
|  ArrayAccess
;

ExpressionOpt : 
|  Expression
;

ClassTypeList :  ClassType
|  ClassTypeList CM ClassType
;

ForInit :  LocalVariableDeclaration
|  StatementExpressionList
;

AssignmentExpression :  Assignment
|  ConditionalExpression
;

ForInitOpt :  ForInit
;

ConditionalExpression :  ConditionalOrExpression
|  ConditionalOrExpression QUEST Expression COLON ConditionalExpression
;

FormalParameter :  Term_Type VariableDeclaratorId
;

CompilationUnit :  TypeDeclarationsOpt
;

ConditionalOrExpression :  ConditionalAndExpression
|  ConditionalOrExpression OROR ConditionalAndExpression
;

ConstantExpression :  Expression
;

ConditionalAndExpression :  InclusiveOrExpression
|  ConditionalAndExpression ANDAND InclusiveOrExpression
;

FormalParameterList :  FormalParameterList CM FormalParameter
|  FormalParameter
;

SwitchLabel :  CASE ConstantExpression COLON
;

FormalParameterListOpt :  FormalParameterList
;

InclusiveOrExpression :  ExclusiveOrExpression
|  InclusiveOrExpression OR ExclusiveOrExpression
;

Throws :  THROWS ClassTypeList
;

SwitchBlockStatementGroup :  SwitchLabels BlockStatements
;

ExclusiveOrExpression :  ExclusiveOrExpression CARET AndExpression
|  AndExpression
;

ThrowsOpt : 
|  Throws
;

MethodDeclarator :  IDENT LP FormalParameterListOpt RP
;

MethodBody :  Block
;

AndExpression :  AndExpression AND EqualityExpression
|  EqualityExpression
;

SwitchLabels :  SwitchLabel
|  SwitchLabels SwitchLabel
;

MethodHeader :  ModifiersOpt Term_Type MethodDeclarator ThrowsOpt
|  ModifiersOpt VOID MethodDeclarator ThrowsOpt
;

SwitchLabelsOpt :  SwitchLabels
| 
;

ArrayInitializer :  LC VariableInitializersOpt CMOpt RC
;

SwitchBlockStatementGroups :  SwitchBlockStatementGroup
|  SwitchBlockStatementGroups SwitchBlockStatementGroup
;

EqualityExpression :  EqualityExpression EQ RelationalExpression
|  EqualityExpression NE RelationalExpression
|  RelationalExpression
;

Expression :  AssignmentExpression
;

SwitchBlockStatementGroupsOpt : 
|  SwitchBlockStatementGroups
;

SwitchBlock :  LC SwitchBlockStatementGroupsOpt SwitchLabelsOpt RC
;

VariableInitializer :  ArrayInitializer
|  Expression
;

VariableDeclaratorId :  IDENT
|  VariableDeclaratorId LB RB
;

RelationalExpression :  RelationalExpression LE ShiftExpression
|  RelationalExpression GE ShiftExpression
|  ShiftExpression
|  RelationalExpression INSTANCEOF ReferenceType
|  RelationalExpression LT ShiftExpression
|  RelationalExpression GT ShiftExpression
;

ClassInstanceCreationExpression :  NEW ClassType LP ArgumentListOpt RP
;

VariableDeclarator :  VariableDeclaratorId ASN VariableInitializer
|  VariableDeclaratorId
;

MethodInvocation :  Name LC ArgumentListOpt RC
|  Primary DOT IDENT LC ArgumentListOpt RC
|  SUPER DOT IDENT LC ArgumentListOpt RC
|  Name LP ArgumentListOpt RP
|  SUPER DOT IDENT LP ArgumentListOpt RP
|  Primary DOT IDENT LP ArgumentListOpt RP
;

VariableDeclarators :  VariableDeclarator
|  VariableDeclarators CM VariableDeclarator
;

PostDecrementExpression :  PostFixExpression DEC
;

MethodDeclaration :  MethodHeader MethodBody
;

PostIncrementExpression :  PostFixExpression INC
;

ShiftExpression :  ShiftExpression SHL AdditiveExpression
|  AdditiveExpression
|  ShiftExpression SHR AdditiveExpression
|  ShiftExpression LSHR AdditiveExpression
;

FieldDeclaration :  ModifiersOpt Term_Type VariableDeclarators SM
;

PreDecrementExpression :  DEC UnaryExpression
;

ConstructorDeclaration :  ModifiersOpt ConstructorDeclarator ThrowsOpt ConstructorBody
;

PreIncrementExpression :  INC UnaryExpression
;

AdditiveExpression :  AdditiveExpression PLUS MultiplicativeExpression
|  MultiplicativeExpression
|  AdditiveExpression MINUS MultiplicativeExpression
;

StaticInitializer :  STATIC Block
;

Assignment :  LeftHandSide AssignmentOperator AssignmentExpression
;

ClassMemberDeclaration :  MethodDeclaration
|  FieldDeclaration
;

StatementExpression :  PostDecrementExpression
|  PostIncrementExpression
|  PreDecrementExpression
|  PreIncrementExpression
|  ClassInstanceCreationExpression
|  Assignment
|  MethodInvocation
;

ClassBodyDeclaration :  ConstructorDeclaration
|  StaticInitializer
|  ClassMemberDeclaration
;

MultiplicativeExpression :  MultiplicativeExpression MUL UnaryExpression
|  MultiplicativeExpression DIV UnaryExpression
|  MultiplicativeExpression MOD UnaryExpression
|  UnaryExpression
;

TryStatement :  TRY Block Catches
|  TRY Block CatchesOpt Finally
;

ClassBodyDeclarations :  ClassBodyDeclaration
|  ClassBodyDeclarations ClassBodyDeclaration
;

CastExpression :  LP Name Dims RP UnaryExpressionNotPlusMinus
|  LP Expression RP UnaryExpressionNotPlusMinus
|  LP Term_PrimitiveType DimsOpt RP UnaryExpression
;

ClassBodyDeclarationsOpt :  ClassBodyDeclarations
;

ThrowStatement :  THROW Expression SM
;

SynchronizedStatement :  SYNCHRONIZED LP Expression RP Block
;

ReturnStatement :  RETURN ExpressionOpt SM
|  SUSPEND ExpressionOpt SM
;

UnaryExpressionNotPlusMinus :  CastExpression
|  PostFixExpression
|  BANG UnaryExpression
|  TILDE UnaryExpression
;

DoStatement :  DO Statement WHILE LP Expression RP SM
;

SwitchStatement :  SWITCH LP Expression RP SwitchBlock
;

ExpressionStatement :  StatementExpression SM
;

UnaryExpression :  PreDecrementExpression
|  UnaryExpressionNotPlusMinus
|  PreIncrementExpression
|  PLUS UnaryExpression
|  MINUS UnaryExpression
;

EmptyStatement :  SM
;

ClassBody :  LC ClassBodyDeclarationsOpt RC
;

ForStatementNoShortIf :  FOR LP ForInitOpt SM ExpressionOpt SM ForUpdateOpt RP StatementNoShortIf
;

ModifiersOpt : 
;

WhileStatementNoShortIf :  WHILE LP Expression RP StatementNoShortIf
;
