
%token   PLUS_TK         MINUS_TK        MULT_TK         DIV_TK    REM_TK
%token   LS_TK           SRS_TK          ZRS_TK
%token   AND_TK          XOR_TK          OR_TK
%token   BOOL_AND_TK BOOL_OR_TK
%token   EQ_TK NEQ_TK GT_TK GTE_TK LT_TK LTE_TK


%token   MODIFIER_TK


%token   DECR_TK INCR_TK



%token   DEFAULT_TK      IF_TK              THROW_TK
%token   BOOLEAN_TK      DO_TK              IMPLEMENTS_TK
%token   THROWS_TK       BREAK_TK           IMPORT_TK
%token   ELSE_TK         INSTANCEOF_TK      RETURN_TK
%token   VOID_TK         CATCH_TK           INTERFACE_TK
%token   CASE_TK         EXTENDS_TK         FINALLY_TK
%token   SUPER_TK        WHILE_TK           CLASS_TK
%token   SWITCH_TK       TRY_TK
%token   FOR_TK          NEW_TK             CONTINUE_TK
%token   PACKAGE_TK         THIS_TK
%token   ASSERT_TK

%token   INTEGRAL_TK

%token   FP_TK

%token   ID_TK

%token   REL_QM_TK         REL_CL_TK NOT_TK  NEG_TK

%token   ASSIGN_ANY_TK   ASSIGN_TK
%token   OP_TK  CP_TK  OCB_TK  CCB_TK  OSB_TK  CSB_TK  SC_TK  C_TK DOT_TK

%token   STRING_LIT_TK   CHAR_LIT_TK        INT_LIT_TK        FP_LIT_TK
%token   BOOL_LIT_TK       NULL_TK

%%


class_declaration:
	super interfaces class_body_declarations
;

class_body_declarations:
	class_body_declarations class_body_declaration
;

class_body_declaration:
	class_declaration
|	block			
;


interfaces:
	IMPLEMENTS_TK interface_type_list
;

interface_type_list:
	interface_type_list C_TK class_or_interface_type
;


class_or_interface_type:
	simple_name
;

name:
	simple_name		
;

simple_name:
	identifier		
;

variable_declarators:
	variable_declarators C_TK identifier ASSIGN_TK expression
;


block:
	block_begin block_statements block_end
;

block_statements:
	block_statement
|	block_statements block_statement
;

block_statement:
	local_variable_declaration_statement
|	statement
;

local_variable_declaration_statement:
	class_or_interface_type variable_declarators
;

statement:
	statement_without_trailing_substatement
;

statement_without_trailing_substatement:
	empty_statement
|	statement_expression SC_TK
;

statement_expression:
	assignment
|	post_increment_expression
;

primary:
	primary_no_new_array
;

primary_no_new_array:
	OP_TK expression CP_TK
|	array_access
;


array_access:
	primary_no_new_array OSB_TK
;

postfix_expression:
	primary
|	name
|	post_increment_expression
;

post_increment_expression:
	postfix_expression INCR_TK
;


trap_overflow_corner_case:
	PLUS_TK trap_overflow_corner_case
|	postfix_expression
;


assignment_expression:
	trap_overflow_corner_case
|	assignment
;

assignment:
	left_hand_side ASSIGN_TK assignment_expression
;

left_hand_side:
	name
|	array_access
;


expression:
	assignment_expression
;
