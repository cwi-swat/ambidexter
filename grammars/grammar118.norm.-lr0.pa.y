%token PROGRAM
%token PACKED
%token PROCEDURE
%token FUNCTION
%token SFILE
%token SET
%token RECORD
%token ARRAY
%token TYPE
%token VAR
%token WITH
%token END
%token SBEGIN
%token FOR
%token DO
%token WHILE
%token UNTIL
%token REPEAT
%token OF
%token CASE
%token ELSE
%token THEN
%token IF
%token DOTDOT
%token NOT
%token BECOMES
%token IDENTIFIER
%token STRING
%token UNSIGNED_INT

%%

program :  PROGRAM newident external_files ';' block '.'
;

record_var :  variable
;

ident :  IDENTIFIER
;

function_form : 
|  formal_params ':' ident
;

case_label :  constant
;

formal_params : 
|  '(' formal_p_sects ')'
;

unsigned_num :  UNSIGNED_INT
;

func_heading :  FUNCTION newident function_form
;

constant :  '-' unsigned_num
|  ident
|  '+' ident
|  STRING
|  '-' ident
|  unsigned_num
|  '+' unsigned_num
;

body :  IDENTIFIER
|  block
;

proc_heading :  PROCEDURE newident formal_params
;

case_list_elem : 
|  case_label_list ':' statement
;

proc_or_func :  proc_heading ';' body ';'
|  func_heading ';' body ';'
;

colon_things :  ':' expression
|  ':' expression ':' expression
;

new_id_list :  newident
|  new_id_list ',' newident
;

expressions :  expression
|  expressions ',' expression
;

label :  UNSIGNED_INT
;

actual_param :  expression colon_things
|  expression
;

variable_dcl :  newident_list ':' type
;

actuals_list :  actuals_list ',' actual_param
|  actual_param
;

variable_dcls :  variable_dcls ';' variable_dcl
|  variable_dcl
;

proc_dcl_part :  proc_or_func
;

var_dcl_part :  VAR variable_dcls ';'
;

actual_params : 
|  '(' actuals_list ')'
;

member :  expression
|  expression DOTDOT expression
;

case_label_list :  case_label_list ',' case_label
|  case_label
;

type_dcl_part :  TYPE type_defs ';'
;

variable :  ident actual_params
|  variable '[' expressions ']'
|  variable '^'
|  variable '.' ident
;

members :  members ',' member
|  member
;

variant :  case_label_list ':' '(' field_list ')'
| 
;

member_list : 
|  members
;

rec_var_list :  rec_var_list ',' record_var
|  record_var
;

direction :  DOWNTO
;

variants :  variants ';' variant
|  variant
;

declaration :  var_dcl_part
|  type_dcl_part
|  proc_dcl_part
;

tag_field :  ident
|  newident ':' ident
;

declarations :  declaration
|  declarations declaration
;

case_list :  case_list_elem
|  case_list ';' case_list_elem
;

set :  '[' member_list ']'
;

statement_part :  compound_stmt
;

expression :  simple_expr relational_op simple_expr
|  simple_expr
;

opt_declarations :  declarations
| 
;

unsigned_lit :  STRING
|  unsigned_num
;

record_section :  newident_list ':' type
| 
;

newident_list :  new_id_list
;

procedure_call :  ident actual_params
;

variant_part :  CASE tag_field OF variants
;

assignment :  variable BECOMES expression
;

fixed_part :  fixed_part ';' record_section
|  record_section
;

block :  opt_declarations statement_part
;

external_files :  '(' newident_list ')'
| 
;

newident :  IDENTIFIER
;

mult_op :  AND
;

statement :  IF expression THEN statement ELSE statement
|  procedure_call
|  label ':' statement
|  REPEAT statements UNTIL expression
|  WHILE expression DO statement
|  assignment
|  compound_stmt
| 
|  CASE expression OF case_list END
|  WITH rec_var_list DO statement
|  FOR ident BECOMES expression direction expression DO statement
;

field_list :  variant_part
|  fixed_part
|  fixed_part ';' variant_part
;

factor :  '(' expression ')'
|  variable
|  set
|  unsigned_lit
|  NOT factor
;

statements :  statements ';' statement
|  statement
;

index_t_list :  simple_type
|  index_t_list ',' simple_type
;

compound_stmt :  SBEGIN statements END
;

add_op :  '<'
;

index_spec :  newident DOTDOT newident ':' ident
;

index_specs :  index_specs ';' index_spec
|  index_spec
;

struct_type :  SET OF simple_type
|  SFILE OF type
|  RECORD field_list END
|  ARRAY '[' index_t_list ']' OF type
;

term :  term mult_op factor
|  factor
;

paramtype :  ident
|  ARRAY '[' index_specs ']' OF paramtype
|  PACKED ARRAY '[' index_spec ']' OF ident
;

simple_type :  constant DOTDOT constant
|  ident
|  '(' newident_list ')'
;

type :  PACKED struct_type
|  struct_type
|  simple_type
;

type_def :  newident '=' type
;

param_group :  newident_list ':' paramtype
;

type_defs :  type_defs ';' type_def
|  type_def
;

formal_p_sect :  func_heading
|  proc_heading
|  param_group
|  VAR param_group
;

formal_p_sects :  formal_p_sects ';' formal_p_sect
|  formal_p_sect
;

relational_op :  '='
|  '<'
;

simple_expr :  simple_expr add_op term
|  term
|  '+' term
|  '-' term
;
