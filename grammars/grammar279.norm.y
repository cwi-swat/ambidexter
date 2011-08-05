
// copy of ambiguous part of grammar 117

%token UNSIGNED_INT UNSIGNED_REAL STRING IDENTIFIER
%token NE LE GE BECOMES DIV MOD NIL IN OR AND NOT DOTDOT
%token IF THEN ELSE CASE OF REPEAT UNTIL WHILE DO FOR TO DOWNTO
%token SBEGIN END WITH GOTO CONST VAR TYPE ARRAY RECORD SET SFILE FUNCTION
%token PROCEDURE LABEL PACKED PROGRAM

%%

program		: PROGRAM ';' block '.'
		;

block		: opt_declarations  statement_part
		;
opt_declarations: 
		| declarations
		;
declarations	: declarations declaration	
		| declaration
		;
declaration	: label_dcl_part
		;

label_dcl_part	: LABEL labels ';'
		;
labels		: labels ',' label
		| label
		;
label		: UNSIGNED_INT		
		;

constant	: unsigned_num
		| STRING			
		;

unsigned_num	: UNSIGNED_INT
		| UNSIGNED_REAL
		;


statement_part	: compound_stmt
		;
compound_stmt	: SBEGIN statements END
		;
statements	: statements ';' statement
		| statement
		;
statement	: 
		| IF expression THEN statement ELSE statement
		| IF expression THEN statement
		;

expression	: simple_expr
		| simple_expr relational_op simple_expr
		;
relational_op	: '='
		| '<'
		| '>'
		| LE
		| GE
		| NE
		| IN
		;

simple_expr	: term
		| '+' term
		| '-' term
		| simple_expr add_op term
		;
add_op		: '+'
		| '-'
		| OR
		;

term		: factor
		| term mult_op factor
		;
mult_op		: '*'
		| '/'
		| DIV
		| MOD
		| AND
		;

factor		: variable		
		| unsigned_lit
		| '(' expression ')'
	
		| set
		| NOT factor
		;

unsigned_lit	: unsigned_num
		| STRING			
		| NIL
		;


set		: '[' member_list ']'
		;
member_list	: 
		| members
		;
members		: members ',' member
		| member
		;
member		: expression
		| expression DOTDOT expression
		;


variable	: ident actual_params	
		| variable '[' expressions ']'
		| variable '.' ident
		| variable '^'
		;
expressions	: expressions ',' expression
		| expression
		;
record_var	: variable
		;
ident		: IDENTIFIER
		;
newident	: IDENTIFIER
		    
		;
