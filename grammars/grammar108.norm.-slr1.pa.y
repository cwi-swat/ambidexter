%token INT
%token COLUMN
%token CREATE
%token ADD
%token TTABLE
%token ALTER
%token NAME

%%

y_sql :  y_create
|  y_alter
;

y_columndef :  NAME INT
|  NAME INT ',' NAME INT
;

y_table :  NAME
;

y_create :  CREATE TTABLE y_table '(' y_columndefs ')'
;

y_alter :  ALTER TTABLE y_table ADD COLUMN y_columndef
|  ALTER TTABLE y_table ADD y_columndef
;

y_columndefs :  y_columndef
|  y_columndefs ',' y_columndef
;
