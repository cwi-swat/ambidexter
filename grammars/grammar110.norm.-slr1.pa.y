%token PROGRAM
%token PROCEDURE
%token FUNCTION
%token VAR

%%

program :  PROGRAM newident external_files ';' block '.'
;

statement_part :  SBEGIN END
;

opt_declarations :  declarations
;

variable_dcl :  newident_list ':' type
;

ident :  IDENTIFIER
;

newident_list :  IDENTIFIER
;

variable_dcls :  variable_dcls ';' variable_dcl
|  variable_dcl
;

proc_dcl_part :  proc_or_func
;

function_form : 
|  formal_params ':' ident
;

var_dcl_part :  VAR variable_dcls ';'
;

paramtype :  ident
;

simple_type :  ident
;

block :  opt_declarations statement_part
;

formal_params : 
|  '(' formal_p_sects ')'
;

external_files : 
;

func_heading :  FUNCTION newident function_form
;

newident :  IDENTIFIER
;

type :  simple_type
;

body :  block
;

proc_heading :  PROCEDURE newident formal_params
;

param_group :  newident_list ':' paramtype
;

proc_or_func :  proc_heading ';' body ';'
|  func_heading ';' body ';'
;

declaration :  var_dcl_part
|  proc_dcl_part
;

formal_p_sect :  func_heading
|  proc_heading
|  param_group
|  VAR param_group
;

declarations :  declaration
|  declarations declaration
;

formal_p_sects :  formal_p_sects ';' formal_p_sect
|  formal_p_sect
;
