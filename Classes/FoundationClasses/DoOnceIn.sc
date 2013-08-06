/* IZ Aug 6, 2013 (10:54 AM)

Perform an action only once within a certain interval.
Useful for reducing the number of updates in order to limit cpu load.

*/


DoOnceIn {
	var <>action, <>seconds;
	var scheduler;

	*new { | action, seconds = 1 |
		^this.newCopyArgs(action, seconds);
	}

	value {
		scheduler ?? {
			scheduler = {
				action.value;
				scheduler = nil;
			}.defer(seconds);
		}
	}

}