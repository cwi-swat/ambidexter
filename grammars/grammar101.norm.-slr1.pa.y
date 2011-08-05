%token DELETE
%token WHERE
%token FROM
%token SELECT
%token NOT
%token OR
%token AND
%token SET
%token BY
%token UPDATE
%token ORDER

%%

y_sql :  y_select
|  y_delete
|  y_update
;

y_order :  NAME
;

y_assignments :  NAME EQUAL NULL_VALUE
;

y_boolean :  NOT y_boolean
|  NAME COMPARISON_OPERATOR NAME
|  '(' y_sub_condition ')'
;

y_condition :  y_sub_condition
;

y_table :  NAME
;

y_delete :  DELETE FROM y_table WHERE y_condition
;

y_columns :  '*'
;

y_sub_condition2 :  y_sub_condition2 AND y_boolean
|  y_boolean
;

y_update :  UPDATE y_table SET y_assignments WHERE y_condition
;

y_sub_condition :  y_sub_condition OR y_sub_condition
|  y_sub_condition2
;

y_select :  SELECT y_columns FROM y_table WHERE y_condition
|  SELECT y_columns FROM y_table WHERE y_condition ORDER BY y_order
;
