
// indirectly nullable nonterminal
// used to check out skipping of empty rules

%%

S : 'a' A 'b' | 'b' A 'b' ;

A : B ;

B : 'b' | ;

