%token CC99
%token CC97
%token CC100
%token CC98

%%

START : S 
;

S : B CC100
|
CC97 D CC100
|
CC97 B CC99 
;

B : CC98 
;

D : CC98 
;
