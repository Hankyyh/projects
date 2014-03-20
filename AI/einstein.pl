iright(L, R, [L | [R | _]]).
iright(L, R, [_ | Rest]) :- iright(L, R, Rest).
nextto(L, R, List) :- iright(L, R, List).
nextto(L, R, List) :- iright(R, L, List).
myprogram(Data) :-     =(Data,       [ [norwegian,_,_,_,_], 	[_,blue,_,_,_], 	[_,_,milk,_,_], 	[_,_,_,_,_], 	[_,_,_,_,_] ]),    
member([british,red,_,_,_], Data),    
member([swedish,_,_,_,dog], Data),    
member([danish,_,tea,_,_], Data),    
iright([_,green,coffee,_,_], [_,white,_,_,_], Data),    
member([_,_,_,pallmall,bird], Data),    
member([_,yellow,_,dunhill,_], Data),    
nextto([_,_,_,marlboro,_], [_,_,_,_,cat], Data),    
nextto([_,_,_,dunhill,_], [_,_,_,_,horse], Data),    
member([_,_,beer,winfield,_], Data),    
member([german,_,_,rothmans,_], Data),    
nextto([_,_,_,marlboro,_], [_,_,water,_,_], Data).