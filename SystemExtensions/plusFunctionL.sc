/* IZ Jul 24, 2013 (6:11 PM)

GUI for access to Synths created by playing a Function.

see also Synth:onEnd

*/


+ Function {

	l { | target, outbus = 0, fadeTime = 0.02, addAction = 'addToHead', args |
		// add a SynthModel from self to SynthList and play
		var synthModel;
		synthModel = this.addSynthModel(target, outbus, fadeTime, addAction, args);
		synthModel.start;
		^synthModel;
	}

	addSynthModel { | target, outbus = 0, fadeTime = 0.02, addAction = 'addToHead', args |
		// add a SynthModel from self to SynthList
		var synthModel;
		synthModel = SynthModel(this, (out: outbus, fadeTime: fadeTime), target, addAction);
		args pairsDo: { | key, value | synthModel.put(key, value) };
		SynthList add: synthModel;
		^synthModel;
	}
}

