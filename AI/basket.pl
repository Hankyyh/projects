iright(L, R, [L | [R | _]]).
iright(L, R, [_ | Rest]) :- iright(L, R, Rest).

next_to(L, R, List) :- iright(L, R, List).
next_to(L, R, List) :- iright(R, L, List).

not_next_to(L, R, List) :- member(L,List),member(R,List),\+ next_to(L, R, List).

to_right(L,R,Data) :- iright(L,R,Data).
to_right(L,R,Data) :- iright(L,Z,Data),to_right(Z,R,Data).

three_b_between(L,R,[L | [_,_,_,R|_]]).
three_b_between(L,R,[_|Rest]) :- three_b_between(L,R,Rest).

away_four(X,Y,Data) :- three_b_between(X,Y,Data).
away_four(X,Y,Data) :- three_b_between(Y,X,Data).

between(X,Y,Z,Data) :- in_between(X,Y,Z,Data).
between(X,Y,Z,Data) :- in_between(Z,Y,X,Data).

in_between(X,Y,Z,[X | [Y |[Z |_]]]).
in_between(X,Y,Z, [_|Data]) :- in_between(X,Y,Z,Data).

basket(Sol) :- =(Sol,[red,_,_,_,_,_,_]),%Clue-6:The Red basket is the first basket on the left.
away_four(brown,purple,Sol),%Clue-1:The Brown Basket is 4 baskets away from the Purple Basket.
between(green,blue,yellow,Sol),%Clue-2: Blue basket lies between the Green and Yellow Baskets
not_next_to(blue,orange,Sol),%Clue-3:The Blue Basket is not next to the Orange basket
next_to(brown,red,Sol),%Clue-4:The Brown and Red Baskets are next to each other
to_right(brown,orange,Sol).%Clue-5:The Orange basket is somewhere to the right of the Brown Basket

basket(Sol) :- =(Sol,[red,_,_,_,_,_,_]),
away_four(brown,purple,Sol),
between(green,orange,yellow,Sol),%Clue-2: Orange basket lies between the Green and Yellow Baskets 
not_next_to(blue,orange,Sol),
next_to(brown,red,Sol),
to_right(brown,orange,Sol).
