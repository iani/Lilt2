/* IZ Jul 24, 2013 (6:24 PM)

Do an action when a Synth ends (is freed).

This implementation does not use Notification because it does not want to have to remove notifications either one by one or all at once (using objectClosed).


a = { WhiteNoise.ar(0.1) }.play;

a.onEnd({ | x | [x, \ended].postln; });
a.onEnd({ | x | [x, \ended_AGAIN].postln; });

a.free;

a.dependants;

*/

+ Synth {
	onEnd { | function |
		var controller;
		controller = { | who, msg |
			if (msg === \n_end) {
				function.(who);
				this.removeDependant(controller);
			};
		};
		NodeWatcher.register(this);
		this.addDependant(controller);
	}
}

