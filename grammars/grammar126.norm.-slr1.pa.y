%token RETURN
%token FOR
%token DO
%token WHILE
%token SWITCH
%token ELSE
%token IF
%token DEFAULT
%token CASE
%token ELIPSIS
%token ENUM
%token OR_OP
%token AND_OP
%token NE_OP
%token EQ_OP
%token GE_OP
%token LE_OP
%token RIGHT_OP
%token LEFT_OP
%token DEC_OP
%token INC_OP
%token PTR_OP
%token SIZEOF

%%

file :  file external_definition
|  external_definition
;

external_definition :  declaration
|  function_definition
;

and_expr :  and_expr '&' equality_expr
|  equality_expr
;

pointer :  '*' pointer
|  '*' type_specifier_list pointer
|  '*' type_specifier_list
|  '*'
;

declaration_list :  declaration
|  declaration_list declaration
;

abstract_declarator2 :  '[' constant_expr ']'
|  abstract_declarator2 '(' parameter_type_list ')'
|  '(' abstract_declarator ')'
|  abstract_declarator2 '[' constant_expr ']'
|  '(' parameter_type_list ')'
|  abstract_declarator2 '(' ')'
|  abstract_declarator2 '[' ']'
;

declarator2 :  declarator2 '(' parameter_type_list ')'
|  identifier
|  declarator2 '(' parameter_identifier_list ')'
|  declarator2 '[' constant_expr ']'
|  '(' declarator ')'
|  declarator2 '(' ')'
|  declarator2 '[' ']'
;

type_name :  type_specifier_list abstract_declarator
|  type_specifier_list
;

equality_expr :  equality_expr EQ_OP relational_expr
|  equality_expr NE_OP relational_expr
|  relational_expr
;

statement_list :  statement
|  statement_list statement
;

abstract_declarator :  pointer abstract_declarator2
|  pointer
|  abstract_declarator2
;

function_body :  compound_statement
|  declaration_list compound_statement
;

cast_expr :  unary_expr
|  '(' type_name ')' cast_expr
;

unary_operator :  '+'
|  '*'
|  '&'
|  '-'
;

enumerator :  identifier '=' constant_expr
|  identifier
;

function_definition :  declaration_specifiers declarator function_body
|  declarator function_body
;

jump_statement :  RETURN expr ';'
;

unary_expr :  SIZEOF unary_expr
|  INC_OP unary_expr
|  DEC_OP unary_expr
|  unary_operator cast_expr
|  postfix_expr
|  SIZEOF '(' type_name ')'
;

relational_expr :  shift_expr
|  relational_expr LE_OP shift_expr
|  relational_expr GE_OP shift_expr
|  relational_expr '<' shift_expr
|  relational_expr '>' shift_expr
;

parameter_declaration :  type_name
|  type_specifier_list declarator
;

enumerator_list :  enumerator_list ',' enumerator
|  enumerator
;

iteration_statement :  FOR '(' ';' ';' expr ')' statement
|  FOR '(' expr ';' ';' ')' statement
|  WHILE '(' expr ')' statement
|  FOR '(' expr ';' expr ';' expr ')' statement
|  FOR '(' expr ';' expr ';' ')' statement
|  FOR '(' ';' ';' ')' statement
|  FOR '(' expr ';' ';' expr ')' statement
|  FOR '(' ';' expr ';' expr ')' statement
|  DO statement WHILE '(' expr ')' ';'
|  FOR '(' ';' expr ';' ')' statement
;

selection_statement :  SWITCH '(' expr ')' statement
|  IF '(' expr ')' statement
|  IF '(' expr ')' statement ELSE statement
;

assignment_expr :  unary_expr assignment_operator assignment_expr
|  conditional_expr
;

parameter_list :  parameter_declaration
|  parameter_list ',' parameter_declaration
;

expression_statement :  expr ';'
|  ';'
;

shift_expr :  additive_expr
|  shift_expr LEFT_OP additive_expr
|  shift_expr RIGHT_OP additive_expr
;

assignment_operator :  '='
;

compound_statement :  '{' statement_list '}'
|  '{' declaration_list '}'
|  '{' declaration_list statement_list '}'
;

struct_declarator :  declarator ':' constant_expr
|  declarator
|  ':' constant_expr
;

labeled_statement :  identifier ':' statement
|  DEFAULT ':' statement
|  CASE constant_expr ':' statement
;

initializer :  assignment_expr
|  '{' initializer_list '}'
|  '{' initializer_list ',' '}'
;

struct_declarator_list :  struct_declarator_list ',' struct_declarator
|  struct_declarator
;

additive_expr :  additive_expr '-' multiplicative_expr
|  multiplicative_expr
|  additive_expr '+' multiplicative_expr
;

statement :  iteration_statement
|  selection_statement
|  expression_statement
|  compound_statement
|  labeled_statement
|  jump_statement
;

identifier_list :  identifier_list ',' identifier
|  identifier
;

type_specifier_list :  type_specifier_list type_specifier
|  type_specifier
;

declarator :  pointer declarator2
|  declarator2
;

conditional_expr :  logical_or_expr '?' logical_or_expr ':' conditional_expr
|  logical_or_expr
;

argument_expr_list :  argument_expr_list ',' assignment_expr
|  assignment_expr
;

struct_declaration :  type_specifier_list struct_declarator_list ';'
;

init_declarator :  declarator '=' initializer
|  declarator
;

logical_or_expr :  logical_or_expr OR_OP logical_and_expr
|  logical_and_expr
;

initializer_list :  initializer
|  initializer_list ',' initializer
;

multiplicative_expr :  multiplicative_expr '/' cast_expr
|  multiplicative_expr '%' cast_expr
|  multiplicative_expr '*' cast_expr
|  cast_expr
;

type_specifier :  enum_specifier
|  struct_or_union_specifier
;

postfix_expr :  postfix_expr PTR_OP identifier
|  postfix_expr '[' expr ']'
|  postfix_expr DEC_OP
|  postfix_expr INC_OP
|  postfix_expr '.' identifier
|  primary_expr
|  postfix_expr '(' ')'
|  postfix_expr '(' argument_expr_list ')'
;

logical_and_expr :  inclusive_or_expr
|  logical_and_expr AND_OP inclusive_or_expr
;

parameter_identifier_list :  identifier_list
|  identifier_list ',' ELIPSIS
;

expr :  expr ',' assignment_expr
|  assignment_expr
;

storage_class_specifier :  REGISTER
;

struct_declaration_list :  struct_declaration
|  struct_declaration_list struct_declaration
;

parameter_type_list :  parameter_list
|  parameter_list ',' ELIPSIS
;

inclusive_or_expr :  exclusive_or_expr
|  inclusive_or_expr '|' exclusive_or_expr
;

struct_or_union :  UNION
;

init_declarator_list :  init_declarator_list ',' init_declarator
|  init_declarator
;

declaration_specifiers :  type_specifier
|  storage_class_specifier declaration_specifiers
|  type_specifier declaration_specifiers
;

identifier :  IDENTIFIER
;

enum_specifier :  ENUM '{' enumerator_list '}'
|  ENUM identifier '{' enumerator_list '}'
;

primary_expr :  '(' expr ')'
|  identifier
;

exclusive_or_expr :  and_expr
|  exclusive_or_expr '^' and_expr
;

declaration :  declaration_specifiers init_declarator_list ';'
|  declaration_specifiers ';'
;

struct_or_union_specifier :  struct_or_union identifier '{' struct_declaration_list '}'
|  struct_or_union '{' struct_declaration_list '}'
;

constant_expr :  conditional_expr
;
