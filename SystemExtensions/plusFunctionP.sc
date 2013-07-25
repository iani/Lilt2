/* IZ Jul 24, 2013 (6:11 PM)

GUI for access to Synths created by playing a Function.

see also Synth:onEnd

*/


+ Function {

	l { | target, outbus = 0, fadeTime = 0.02, addAction = 'addToHead', args |
		var synthModel;
		synthModel = SynthModel(this, (out: outbus, fadeTime: fadeTime), target, addAction);
		args pairsDo: { | key, value | synthModel.put(key, value) };
		synthModel.start;
		^synthModel;
/*		synth = this.play(target, outbus, fadeTime, addAction, args);
		NodeWatcher.register(synth);
		NodeWatcherGui.addNotifier(synth, \n_end, { | notification |
			notification.notifier.objectClosed;
			synthArray remove: notification.notifier;
			if (this.hasSynth.not) { { this.changed(\synthEnded); }.defer(0) };
		});
		this.changed(\synthStarted)
*/
	}
}

