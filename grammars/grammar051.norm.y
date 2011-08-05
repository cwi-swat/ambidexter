%token CC43
%token CC97

%%

START : S 
;

S : E 
;

E : CC97
|
E CC43 E 
;
