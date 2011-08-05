%token PROGRAM
%token PACKED
%token PROCEDURE
%token FUNCTION
%token SFILE
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
%token BECOMES
%token IDENTIFIER
%token STRING
%token UNSIGNED_INT

%%

program :  PROGRAM newident external_files ';' block '.'
;

statement_part :  compound_stmt
;

opt_declarations :  declarations
| 
;

expression :  simple_expr
|  simple_expr relational_op simple_expr
;

unsigned_lit :  STRING
|  unsigned_num
;

record_section :  newident_list ':' type
| 
;

ident :  IDENTIFIER
;

newident_list :  new_id_list
;

procedure_call :  ident actual_params
;

variant_part :  CASE tag_field OF variants
;

function_form : 
|  formal_params ':' ident
;

case_label :  constant
;

fixed_part :  fixed_part ';' record_section
|  record_section
;

block :  opt_declarations statement_part
;

formal_params : 
|  '(' formal_p_sects ')'
;

unsigned_num :  UNSIGNED_INT
;

external_files :  '(' newident_list ')'
| 
;

func_heading :  FUNCTION newident function_form
;

newident :  IDENTIFIER
;

constant :  '-' unsigned_num
|  ident
|  '+' ident
|  STRING
|  '-' ident
|  unsigned_num
|  '+' unsigned_num
;

case_list_elem : 
|  case_label_list ':' statement
;

body :  IDENTIFIER
|  block
;

proc_heading :  PROCEDURE newident formal_params
;

statement :  IF expression THEN statement ELSE statement
|  procedure_call
|  label ':' statement
|  FOR ident BECOMES expression direction expression DO statement
|  REPEAT statements UNTIL expression
|  WHILE expression DO statement
|  compound_stmt
|  IF expression THEN statement
| 
|  WITH rec_var_list DO statement
|  CASE expression OF case_list END
;

field_list :  variant_part
|  fixed_part
|  fixed_part ';' variant_part
;

proc_or_func :  proc_heading ';' body ';'
|  func_heading ';' body ';'
;

factor :  '(' expression ')'
|  variable
|  unsigned_lit
;

statements :  statements ';' statement
|  statement
;

colon_things :  ':' expression
|  ':' expression ':' expression
;

index_t_list :  IDENTIFIER
;

compound_stmt :  SBEGIN statements END
;

new_id_list :  newident
|  new_id_list ',' newident
;

actual_param :  expression
|  expression colon_things
;

label :  UNSIGNED_INT
;

actuals_list :  actual_param
|  actuals_list ',' actual_param
;

variable_dcl :  newident_list ':' type
;

variable_dcls :  variable_dcls ';' variable_dcl
|  variable_dcl
;

term :  factor
;

proc_dcl_part :  proc_or_func
;

actual_params : 
|  '(' actuals_list ')'
;

struct_type :  SFILE OF type
|  RECORD field_list END
|  ARRAY '[' index_t_list ']' OF type
;

var_dcl_part :  VAR variable_dcls ';'
;

paramtype :  ident
;

variable :  ident actual_params
;

simple_type :  ident
|  '(' newident_list ')'
;

case_label_list :  case_label_list ',' case_label
|  case_label
;

type_dcl_part :  TYPE type_defs ';'
;

type :  PACKED struct_type
|  struct_type
|  simple_type
;

variant :  case_label_list ':' '(' field_list ')'
| 
;

rec_var_list :  IDENTIFIER
;

type_def :  newident '=' type
;

param_group :  newident_list ':' paramtype
;

direction :  DOWNTO
;

type_defs :  type_defs ';' type_def
|  type_def
;

variants :  variants ';' variant
|  variant
;

declaration :  var_dcl_part
|  type_dcl_part
|  proc_dcl_part
;

formal_p_sect :  func_heading
|  proc_heading
|  param_group
|  VAR param_group
;

tag_field :  ident
|  newident ':' ident
;

relational_op :  '='
;

declarations :  declaration
|  declarations declaration
;

case_list :  case_list ';' case_list_elem
|  case_list_elem
;

formal_p_sects :  formal_p_sects ';' formal_p_sect
|  formal_p_sect
;

simple_expr :  term
|  '+' term
|  '-' term
;
