
// pico statements excerpt

%%

Program : 'b' Stats 'e' ;

Stats : Statsn | ;

Statsn : Stat ';' Statsn | Stat ;

// 'i' Id 't' Stats 'e' Stats 'f' 

Stat : 'w' Id 'd' Stats 'o'
     | Id '=' Id
;

Id : Char Id | Char ' ' ;

Char : 'w' | 'd' | 'o' ;


