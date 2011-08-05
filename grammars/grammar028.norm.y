%token CC98
%token CC97
%token CC122
%token CC121

%%

START : Z 
;

Z : CC122
|
CC98 X Z
|
CC97 X Y 
;

X : CC121
|
CC97 CC122
|
CC97 Y 
;

Y : CC121 
;
