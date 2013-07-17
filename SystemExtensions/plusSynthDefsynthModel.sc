/* under development

*/

+ SynthDef {

	synthModel { | eventModel, keys, target, addAction = \addToHead |
		^SynthModel(this, eventModel, keys, target, addAction)
	}

	synthGui { | eventModel, keys, target, addAction = \addToHead |
		^this.synthModel(eventModel, keys, target, addAction).gui;
	}

}

