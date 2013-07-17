/* IZ Jul 9, 2013 (9:06 PM)
Redoing Patch classes

*/

// ================== Patch ====================

SynthPatch : SynthModel {
	var <inputs; /* Array of Input instances. Created from synthDesc.inputs */
	var <outputs;  // Array of Output instances (kr or ar). Created from synthDesc.outputs
	var <controls; // Array of ControlMapper instances, created from synthDesc.controls

	init {
		super.init;
		// create inputs and outputs here from SynthDesc;
		inputs = synthDesc.inputs collect: Input(this, _);
		outputs = synthDesc.outputs collect: Output(this, _);
		controls = synthDesc.controls collect: ControlMapper(this, _);
	}


	addInput { | paramName, patch |
		/* make a link from the output of patch
		to parameter named paramName of yourself */
	}

	addOutput { | paramName, patch |
		/* make a link from the output of yourself
		to input parameter named paramName of patch */
	}
}
