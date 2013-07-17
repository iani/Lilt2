/* under development

*/

+ SynthDef {

	synthModel { | eventModel, target, addAction = \addToHead, specs |
		^SynthModel(this, eventModel, target, addAction, specs)
	}

	synthGui { | eventModel, target, addAction = \addToHead, specs |
		^this.synthModel(eventModel, target, addAction, specs).gui;
	}

}

