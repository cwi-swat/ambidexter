
// to test empty skips over consecutive directly nullable nonterminals

%%

S : A B C F E ;

A : 'a' ;

B : | 'b' ;

C : | 'c' ;

F : D ;

D : | 'b' 'd' ;

E : 'e' ;

