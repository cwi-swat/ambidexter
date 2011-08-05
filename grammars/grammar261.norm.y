
%%

// grammar for LDTA presentation

E : E '*' E 
| E '+' E
| N ;

N : D N | D ;

D : '0' | '1' | '2' | '3' ;
