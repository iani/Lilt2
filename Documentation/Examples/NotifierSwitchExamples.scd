/* IZ Jul 31, 2013 (7:47 PM)

Using NotifierSwitch

*/


n = NotifierSwitch(\switcher, \switch, { | notifier ... args | notifier });
n.notifier;
n.addListener(\listener, \new, { | ... args | ["listener this:", args].postln });


\switcher.changed(\switch, \notifier1, 2, 3);
\switcher.changed(\switch, \notifier2, 2, 3);


\notifier1.changed(\new, 1, 2, 3, 4, 5);

\notifier2.changed(\new, 100, 1, 2, 3, 4, 5);

////////////////////////////////////////////


a = EventModel();


\switcher.changed(\switch, a);

a.put(\new, 1);

a.event.addDependant({ | ... args | args.postln; });