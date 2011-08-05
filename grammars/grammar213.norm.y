%token Td
%token Te

%%

START : S
;

S : '(' A ')' '(' B ')'
;

A : C
| D
;

C : Td Td
;

D : Td Td
;

B : E
| F
;

E : Te Te
;

F : Te Te
;

