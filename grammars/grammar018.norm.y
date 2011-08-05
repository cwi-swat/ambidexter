%token CC97
%token CC98
%token CC101
%token CC100

%%

START : S 
;

S : P Q 
;

P : CC97
|
R O T 
;

R : M P 
;

O : CC97 CC98
|
CC97 
;

T : CC98 CC98
|
CC98 
;

M : CC97 
;

Q : C CC101 D 
;

C : CC97 CC98
|
CC97 
;

D : CC101 CC100
|
CC100 
;
