
// grammar on page 241 from dragon book
// used to verify LALR1 construction

%%

START : S
;

S : L '=' R
| R
;

L : '*' R
| 'i'
;

R : L
;
