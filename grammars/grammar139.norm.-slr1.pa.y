%token OP_TK
%token ASSIGN_TK
%token NEG_TK
%token NOT_TK
%token REL_CL_TK
%token REL_QM_TK
%token ASSERT_TK
%token NEW_TK
%token TRY_TK
%token SWITCH_TK
%token CLASS_TK
%token WHILE_TK
%token SUPER_TK
%token FINALLY_TK
%token CASE_TK
%token INTERFACE_TK
%token VOID_TK
%token RETURN_TK
%token INSTANCEOF_TK
%token ELSE_TK
%token THROW_TK
%token IF_TK
%token INCR_TK
%token DECR_TK
%token LTE_TK
%token LT_TK
%token GTE_TK
%token GT_TK
%token NEQ_TK
%token EQ_TK
%token BOOL_OR_TK
%token BOOL_AND_TK
%token OR_TK
%token XOR_TK
%token AND_TK
%token ZRS_TK
%token SRS_TK
%token LS_TK
%token DOT_TK
%token REM_TK
%token DIV_TK
%token C_TK
%token SC_TK
%token MULT_TK
%token CSB_TK
%token MINUS_TK
%token OSB_TK
%token PLUS_TK
%token CCB_TK
%token OCB_TK
%token CP_TK

%%

goal :  compilation_unit
;

try_statement :  TRY_TK block finally
|  TRY_TK block catches finally
|  TRY_TK block catches
;

variable_declarators :  variable_declarators C_TK variable_declarator
|  variable_declarator
;

throw_statement :  THROW_TK expression SC_TK
;

synchronized_statement :  synchronized OP_TK expression CP_TK block
;

return_statement :  RETURN_TK expression SC_TK
;

method_declaration :  method_header method_body
;

dim_expr :  OSB_TK expression CSB_TK
;

field_declaration :  modifiers type variable_declarators SC_TK
|  type variable_declarators SC_TK
;

block :  block_begin block_statements block_end
|  block_begin block_end
;

do_statement :  do_statement_begin statement WHILE_TK OP_TK expression CP_TK SC_TK
;

switch_statement :  switch_expression switch_block
;

constructor_declaration :  constructor_header constructor_body
;

expression_statement :  statement_expression SC_TK
;

static_initializer :  static_ block
;

class_member_declaration :  method_declaration
|  field_declaration
|  empty_statement
|  interface_declaration
|  class_declaration
;

dim_exprs :  dim_expr
|  dim_exprs dim_expr
;

for_statement_nsi :  for_begin SC_TK expression SC_TK for_update CP_TK statement_nsi
|  for_begin SC_TK SC_TK for_update CP_TK statement_nsi
;

class_body_declaration :  block
|  constructor_declaration
|  static_initializer
|  class_member_declaration
;

while_statement_nsi :  while_expression statement_nsi
;

class_body_declarations :  class_body_declarations class_body_declaration
|  class_body_declaration
;

if_then_else_statement_nsi :  IF_TK OP_TK expression CP_TK statement_nsi ELSE_TK statement_nsi
;

labeled_statement_nsi :  label_decl statement_nsi
;

statement_nsi :  statement_without_trailing_substatement
|  for_statement_nsi
|  while_statement_nsi
|  if_then_else_statement_nsi
|  labeled_statement_nsi
;

for_statement :  for_begin SC_TK expression SC_TK for_update CP_TK statement
|  for_begin SC_TK SC_TK for_update CP_TK statement
;

while_statement :  while_expression statement
;

something_dot_new :  primary DOT_TK NEW_TK
;

if_then_else_statement :  IF_TK OP_TK expression CP_TK statement_nsi ELSE_TK statement
;

anonymous_class_creation :  NEW_TK class_type OP_TK argument_list CP_TK class_body
|  NEW_TK class_type OP_TK CP_TK class_body
;

class_body :  OCB_TK class_body_declarations CCB_TK
|  OCB_TK CCB_TK
;

if_then_statement :  IF_TK OP_TK expression CP_TK statement
;

interfaces : 
;

super : 
;

labeled_statement :  label_decl statement
;

statement_without_trailing_substatement :  try_statement
|  empty_statement
|  do_statement
|  block
|  throw_statement
|  switch_statement
|  synchronized_statement
|  expression_statement
|  return_statement
|  assert_statement
;

modifiers :  MODIFIER_TK
;

empty_statement :  SC_TK
;

interface_declaration :  modifiers INTERFACE_TK identifier interface_body
|  INTERFACE_TK identifier interface_body
|  INTERFACE_TK identifier extends_interfaces interface_body
|  modifiers INTERFACE_TK identifier extends_interfaces interface_body
;

local_variable_declaration :  final type variable_declarators
|  type variable_declarators
;

type_literals :  primitive_type DOT_TK CLASS_TK
|  VOID_TK DOT_TK CLASS_TK
;

class_declaration :  modifiers CLASS_TK identifier super interfaces class_body
|  CLASS_TK identifier super interfaces class_body
;

array_access :  primary_no_new_array OSB_TK expression CSB_TK
|  name OSB_TK expression CSB_TK
;

statement :  labeled_statement
|  statement_without_trailing_substatement
|  for_statement
|  while_statement
|  if_then_else_statement
|  if_then_statement
;

local_variable_declaration_statement :  local_variable_declaration SC_TK
;

field_access :  primary DOT_TK identifier
;

block_statement :  class_declaration
|  statement
|  local_variable_declaration_statement
;

array_creation_expression :  NEW_TK class_or_interface_type dim_exprs
|  NEW_TK class_or_interface_type dims array_initializer
|  NEW_TK primitive_type dim_exprs
|  NEW_TK primitive_type dim_exprs dims
|  NEW_TK class_or_interface_type dim_exprs dims
|  NEW_TK primitive_type dims array_initializer
;

type_declaration :  empty_statement
|  interface_declaration
|  class_declaration
;

primary_no_new_array :  class_instance_creation_expression
|  field_access
|  method_invocation
|  OP_TK expression CP_TK
|  type_literals
|  array_access
;

primary :  array_creation_expression
|  primary_no_new_array
;

variable_initializers :  variable_initializer
|  variable_initializers C_TK variable_initializer
;

catch_clause_parameter :  CATCH_TK OP_TK FP_TK ID_TK CP_TK
;

type_declarations :  type_declarations type_declaration
|  type_declaration
;

catch_clause :  catch_clause_parameter block
;

import_declarations :  IMPORT_TK ID_TK SC_TK
;

finally :  FINALLY_TK block
;

package_declaration :  PACKAGE_TK ID_TK SC_TK
;

assignment_operator :  ASSIGN_TK
;

left_hand_side :  array_access
|  name
|  field_access
;

catches :  catches catch_clause
|  catch_clause
;

abstract_method_declaration :  method_header SC_TK
;

assignment_expression :  conditional_expression
|  assignment
;

constant_declaration :  field_declaration
;

synchronized :  MODIFIER_TK
;

identifier :  ID_TK
;

interface_member_declaration :  abstract_method_declaration
|  constant_declaration
|  interface_declaration
|  class_declaration
;

conditional_expression :  conditional_or_expression REL_QM_TK expression REL_CL_TK conditional_expression
|  conditional_or_expression
;

interface_member_declarations :  interface_member_declaration
|  interface_member_declarations interface_member_declaration
;

simple_name :  identifier
;

conditional_or_expression :  conditional_or_expression BOOL_OR_TK conditional_and_expression
|  conditional_and_expression
;

dims :  OSB_TK CSB_TK
|  dims OSB_TK CSB_TK
;

conditional_and_expression :  inclusive_or_expression
|  conditional_and_expression BOOL_AND_TK inclusive_or_expression
;

extends_interfaces :  EXTENDS_TK ID_TK
;

inclusive_or_expression :  exclusive_or_expression
|  inclusive_or_expression OR_TK exclusive_or_expression
;

class_type :  ID_TK
;

name :  simple_name
;

interface_body :  OCB_TK interface_member_declarations CCB_TK
;

exclusive_or_expression :  and_expression
|  exclusive_or_expression XOR_TK and_expression
;

class_or_interface_type :  ID_TK
;

and_expression :  and_expression AND_TK equality_expression
|  equality_expression
;

statement_expression_list :  statement_expression_list C_TK statement_expression
|  statement_expression
;

argument_list :  argument_list C_TK expression
|  expression
;

for_init :  statement_expression_list
|  local_variable_declaration
;

equality_expression :  equality_expression EQ_TK relational_expression
|  equality_expression NEQ_TK relational_expression
|  relational_expression
;

this_or_super :  THIS_TK
;

reference_type :  ID_TK
;

for_header :  FOR_TK OP_TK
;

block_end :  CCB_TK
;

primitive_type :  VOID_TK
;

type :  primitive_type
;

block_statements :  block_statements block_statement
|  block_statement
;

explicit_constructor_invocation :  name DOT_TK SUPER_TK OP_TK argument_list CP_TK SC_TK
|  this_or_super OP_TK argument_list CP_TK SC_TK
;

for_update :  statement_expression_list
;

relational_expression :  relational_expression GT_TK shift_expression
|  relational_expression GTE_TK shift_expression
|  relational_expression LT_TK shift_expression
|  relational_expression LTE_TK shift_expression
|  shift_expression
|  relational_expression INSTANCEOF_TK reference_type
;

for_begin :  for_header for_init
;

constructor_block_end :  block_end
;

block_begin :  OCB_TK
;

do_statement_begin :  DO_TK
;

shift_expression :  shift_expression ZRS_TK additive_expression
|  additive_expression
|  shift_expression LS_TK additive_expression
|  shift_expression SRS_TK additive_expression
;

compilation_unit :  package_declaration type_declarations
|  type_declarations
|  import_declarations type_declarations
|  package_declaration import_declarations type_declarations
;

while_expression :  WHILE_TK OP_TK expression CP_TK
;

constructor_body :  block_begin explicit_constructor_invocation constructor_block_end
|  block_begin explicit_constructor_invocation block_statements constructor_block_end
|  block_begin block_statements constructor_block_end
;

constructor_header :  ID_TK OP_TK CP_TK
;

additive_expression :  additive_expression PLUS_TK multiplicative_expression
|  additive_expression MINUS_TK multiplicative_expression
|  multiplicative_expression
;

constant_expression :  expression
;

static_ :  MODIFIER_TK
;

switch_label :  CASE_TK constant_expression REL_CL_TK
;

multiplicative_expression :  multiplicative_expression MULT_TK unary_expression
|  multiplicative_expression DIV_TK unary_expression
|  multiplicative_expression REM_TK unary_expression
|  unary_expression
;

switch_block_statement_group :  switch_labels block_statements
;

switch_block_statement_groups :  switch_block_statement_group
|  switch_block_statement_groups switch_block_statement_group
;

cast_expression :  OP_TK primitive_type CP_TK unary_expression
|  OP_TK primitive_type dims CP_TK unary_expression
|  OP_TK name dims CP_TK unary_expression_not_plus_minus
|  OP_TK expression CP_TK unary_expression_not_plus_minus
;

switch_labels :  switch_label
|  switch_labels switch_label
;

final :  modifiers
;

switch_block :  OCB_TK switch_block_statement_groups CCB_TK
|  OCB_TK switch_block_statement_groups switch_labels CCB_TK
|  OCB_TK CCB_TK
|  OCB_TK switch_labels CCB_TK
;

switch_expression :  SWITCH_TK OP_TK expression CP_TK
;

unary_expression_not_plus_minus :  postfix_expression
|  NOT_TK unary_expression
|  NEG_TK unary_expression
|  cast_expression
;

class_instance_creation_expression :  NEW_TK class_type OP_TK argument_list CP_TK
|  something_dot_new identifier OP_TK CP_TK
|  something_dot_new identifier OP_TK argument_list CP_TK class_body
|  something_dot_new identifier OP_TK argument_list CP_TK
|  something_dot_new identifier OP_TK CP_TK class_body
|  anonymous_class_creation
;

unary_expression :  trap_overflow_corner_case
|  MINUS_TK trap_overflow_corner_case
;

method_invocation :  SUPER_TK DOT_TK identifier OP_TK argument_list CP_TK
|  primary DOT_TK identifier OP_TK CP_TK
|  name OP_TK argument_list CP_TK
|  primary DOT_TK identifier OP_TK argument_list CP_TK
;

post_decrement_expression :  postfix_expression DECR_TK
;

trap_overflow_corner_case :  pre_decrement_expression
|  pre_increment_expression
|  unary_expression_not_plus_minus
|  PLUS_TK unary_expression
;

throws : 
;

method_declarator :  ID_TK OP_TK CP_TK
;

post_increment_expression :  postfix_expression INCR_TK
;

method_body :  SC_TK
|  block
;

pre_decrement_expression :  DECR_TK unary_expression
;

method_header :  modifiers type method_declarator throws
|  type method_declarator throws
|  modifiers VOID_TK method_declarator throws
|  VOID_TK method_declarator throws
;

pre_increment_expression :  INCR_TK unary_expression
;

array_initializer :  OCB_TK variable_initializers C_TK CCB_TK
|  OCB_TK variable_initializers CCB_TK
;

assignment :  left_hand_side assignment_operator assignment_expression
;

expression :  assignment_expression
;

postfix_expression :  post_decrement_expression
|  post_increment_expression
|  name
|  primary
;

statement_expression :  post_decrement_expression
|  post_increment_expression
|  pre_decrement_expression
|  pre_increment_expression
|  class_instance_creation_expression
|  assignment
|  method_invocation
;

variable_initializer :  array_initializer
|  expression
;

label_decl :  identifier REL_CL_TK
;

variable_declarator_id :  identifier
|  variable_declarator_id OSB_TK CSB_TK
;

assert_statement :  ASSERT_TK expression SC_TK
|  ASSERT_TK expression REL_CL_TK expression SC_TK
;

variable_declarator :  variable_declarator_id
|  variable_declarator_id ASSIGN_TK variable_initializer
;
