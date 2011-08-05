
%%

struct_declarator
	: pointer
	| unary_expr
	;

pointer
	: '*'
	| '*' pointer
	| '*' '{' struct_declarator '}' pointer
	;

unary_expr
	: unary_expr '='
	| '&' unary_expr
	;
