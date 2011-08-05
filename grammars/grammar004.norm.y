%token CC97
%token CC100
%token CC98
%token CC99

%%

START : S 
;

S : C
|
A B 
;

A : CC97 CC98
|
CC97 A CC98 
;

B : CC99 CC100
|
CC99 B CC100 
;

C : CC97 D CC100
|
CC97 C CC100 
;

D : CC98 CC99
|
CC98 D CC99 
;
