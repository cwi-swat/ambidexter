%token WHERE
%token NULL_VALUE
%token EQUAL
%token NAME
%token SET
%token UPDATE

%%

y_sql :  y_update
;

y_assignments :  y_assignment
|  y_assignments ',' y_assignment
;

y_condition :  NAME COMPARISON_OPERATOR NAME
;

y_table :  NAME
;

y_assignment :  NAME EQUAL NULL_VALUE
|  NAME EQUAL y_expression
;

y_term :  y_atom
;

y_value :  NULL_VALUE
;

y_update :  UPDATE y_table SET y_assignments
|  UPDATE y_table SET y_assignments WHERE y_condition
;

y_expression :  y_product
;

y_atom :  y_value
;

y_product :  y_term
;
