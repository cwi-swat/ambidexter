%token CC42
%token CC97

%%

START : S 
;

S : D
|
E 
;

E : I CC42 I 
;

D : T CC42 I 
;

I : CC97 
;

T : CC97 
;
