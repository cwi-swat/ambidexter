%token Term_y_alter
%token BY
%token Term_y_order
%token Term_y_create
%token Term_y_table
%token Term_y_sql
%token Term_y_select
%token Term_y_insert
%token Term_y_assignments
%token UPDATE
%token Term_y_drop
%token WHERE
%token ')'
%token Term_y_term
%token Term_y_value_list
%token Term_y_delete
%token SET
%token Term_y_columndefs
%token SELECT
%token Term_y_values
%token Term_y_value
%token AND
%token Term_y_product
%token Term_y_columndef
%token NOT
%token Term_y_expression
%token Term_y_column
%token Term_y_sub_condition2
%token Term_y_boolean
%token ORDER
%token Term_y_column_list
%token DELETE
%token FROM
%token Term_y_atom
%token '('
%token Term_y_assignment
%token Term_y_columns
%token OR
%token Term_y_sub_condition
%token Term_y_comparison
%token Term_y_update
%token Term_y_condition

%%

y_sql :  Term_y_sql
|  y_update
|  y_select
|  y_delete
;

y_update :  Term_y_update
|  UPDATE Term_y_table SET Term_y_assignments WHERE y_condition
;

y_sub_condition2 :  Term_y_sub_condition2
|  y_sub_condition2 AND y_boolean
|  y_boolean
;

y_sub_condition :  Term_y_sub_condition
|  y_sub_condition2
|  y_sub_condition OR y_sub_condition
;

y_condition :  Term_y_condition
|  y_sub_condition
;

y_select :  Term_y_select
|  SELECT Term_y_columns FROM Term_y_table WHERE y_condition ORDER BY Term_y_order
|  SELECT Term_y_columns FROM Term_y_table WHERE y_condition
;

y_boolean :  Term_y_boolean
|  NOT y_boolean
|  '(' y_sub_condition ')'
;

y_delete :  Term_y_delete
|  DELETE FROM Term_y_table WHERE y_condition
;
