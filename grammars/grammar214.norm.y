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
;

E : Te Te
;


