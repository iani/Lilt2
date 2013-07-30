/* IZ Jul 24, 2013 (6:11 PM)

GUI for access to Synths created by playing a Function.

see also Synth:onEnd

*/


+ Function {

	// add a SynthModel from self to SynthList and play
	l { | target, outbus = 0, fadeTime = 0.02, addAction = 'addToHead', args |
		var synthModel;
		synthModel = this.addSynthModel(target, outbus, fadeTime, addAction, args);
		synthModel.start;
		^synthModel;
	}

	// add a SynthModel from self to SynthList
	addSynthModel { | target, outbus = 0, fadeTime = 0.02, addAction = 'addToHead', args |
		var synthModel;
		synthModel = this.synthModel(
			target, outbus = 0, fadeTime = 0.02, addAction = 'addToHead', args
		);
		SynthList add: synthModel;
		^synthModel;
	}

	synthModel { | target, outbus = 0, fadeTime = 0.02, addAction = 'addToHead', args |
		var synthModel;
		synthModel = SynthModel(this, (out: outbus, fadeTime: fadeTime), target, addAction);
		args pairsDo: { | key, value | synthModel.put(key, value) };
		^synthModel;
	}
}

