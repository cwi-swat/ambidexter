
%%

S : 'a' A 'a' ;

A : 'a' 'b' C 'a' 
| B ;

B : 'a' D 'a' ;

D : 'b' C ;

C : 'c' E 'c' ;

E : E E 
| 'e' ;
