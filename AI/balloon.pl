next_to(L, R, List) :- iright(L, R, List).
next_to(L, R, List) :- iright(R, L, List).

iright(L, R, [L | [R | _]]).
iright(L, R, [_ | Rest]) :- iright(L, R, Rest).

not_next_to(X,Y,Data) :- member(X,Data),member(Y,Data),\+ next_to(X,Y,Data).

one_b_between(X,Y,[X |[_,Y|_]]).
one_b_between(X,Y,[_|Data]) :- one_b_between(X,Y,Data).

away_two(X,Y,Data) :- one_b_between(X,Y,Data).
away_two(X,Y,Data) :- one_b_between(Y,X,Data).

in_between(X,Y,Z,[X |[Y |[Z|_]]]).
in_between(X,Y,Z,[_|Rest]) :- in_between(X,Y,Z,Rest).

between(X,Y,Z,Data) :- in_between(X,Y,Z,Data).
between(X,Y,Z,Data) :- in_between(Z,Y,X,Data).


to_right(L,R,List) :- iright(L,R,List).
to_right(L,Z,List) :- iright(L,Z,List),to_right(Z,R,List).

balloon(Sol) :- =(Sol,[_,orange,_,_,_,_,_]),%Clue-8:The 2nd Balloon from left is Orange.
away_two(orange,green,Sol),%Clue-1:The Orange Balloon is two Balloons away from the Green Balloon.
between(orange,red,green,Sol),%Clue-2:The Red Balloon is between the Orange and Green Balloon.
next_to(blue,orange,Sol),%Clue-3:The Blue Balloon is next to the Orange Balloon.
next_to(yellow,green,Sol),%Clue-4:The Yellow Balloon is next to the Green Balloon.
to_right(brown,purple,Sol),%Clue-7:The Purple Balloon is somewhere to the right of the Brown Balloon.
not_next_to(yellow,orange,Sol),%Clue-5:The Yellow Balloon is NOT next to the Orange Balloon.
not_next_to(brown,blue,Sol).%Clue-6:The Brown Balloon and the Blue Balloon are NOT next to each other.
