%token IDENTIFIER CONSTANT STRING_LITERAL SIZEOF
%token PTR_OP INC_OP DEC_OP LEFT_OP RIGHT_OP LE_OP GE_OP EQ_OP NE_OP
%token AND_OP OR_OP MUL_ASSIGN DIV_ASSIGN MOD_ASSIGN ADD_ASSIGN
%token SUB_ASSIGN LEFT_ASSIGN RIGHT_ASSIGN AND_ASSIGN
%token XOR_ASSIGN OR_ASSIGN TYPE_NAME

%token TYPEDEF EXTERN STATIC AUTO REGISTER
%token CHAR SHORT INT LONG SIGNED UNSIGNED FLOAT DOUBLE CONST VOLATILE VOID
%token STRUCT UNION ENUM ELIPSIS

%token CASE DEFAULT IF ELSE SWITCH WHILE DO FOR GOTO CONTINUE BREAK RETURN

%%

declaration_specifiers
	: TYPEDEF
	| TYPEDEF declaration_specifiers
	| type_specifier declaration_specifiers
	;


postfix_expr
	: IDENTIFIER
	| postfix_expr '[' expr ']'
	;

unary_expr
	: postfix_expr
	| '&' cast_expr
	;

cast_expr
	: unary_expr
	;

assignment_expr
	: cast_expr
	| unary_expr '='
	;

expr
	: assignment_expr
	;

constant_expr
	: unary_expr
	;


type_specifier
	: CHAR
	| struct_or_union_specifier
	| enum_specifier
	;

struct_or_union_specifier
	: STRUCT IDENTIFIER '{' struct_declaration_list '}'
	| STRUCT IDENTIFIER
	;

struct_declaration_list
	: struct_declaration
	;

struct_declaration
	: type_specifier_list struct_declarator_list ';'
	;

struct_declarator_list
	: struct_declarator
	;

struct_declarator
	: declarator
	| ':' constant_expr
	| declarator ':' constant_expr
	;

enum_specifier
	: ENUM '{' enumerator '}'
	| ENUM IDENTIFIER '{' enumerator '}'
	| ENUM IDENTIFIER
	;

enumerator
	: IDENTIFIER
	| IDENTIFIER '=' constant_expr
	;

declarator
        : pointer IDENTIFIER
	;

pointer
	: '*'
	| '*' type_specifier_list
	| '*' pointer
	| '*' type_specifier_list pointer
	;

type_specifier_list
	: type_specifier
	;
