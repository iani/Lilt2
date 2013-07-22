 /* IZ Jul 10, 2013 (9:28 PM)

Provide links between a SynthPatch and a LinkedBus.

ControlConnector instances represent Control instances in a Synth process and connect these instances to a bus by mapping the control to the bus. Example : aSynth.map(\controName, busIndex)

Each InputConnector, OutputConnector or ControlConnector can only be linked to one LinkedBus. But a LinkedBus can be written to or read from many instances of Input or Output.

InputConnector and OutputConnector are created from instances of IoDescriptors, while ControlMappers are created from instances of ControlName.

InputConnector and OutputConnector instances represent UGens which read from or write to buses. They are connected to a bus by setting their value to correspond to the index of the bus.  Example: aSynth.set(\out, 1);

*/

ControlConnector {
	var <patch;         // The patch containing this link.
	var <controlName;   // Instance of ControlName from the SynthDesc
	var <busConnector;  // BusConnector.

	*new { | patch, controlName |
		^this.newCopyArgs(patch, controlName);
	}

	makeGui {
		^patch.eventModel.numSlider(controlName.name, decoratorFunc: { | argKey, argView |
			[   // TODO: must put a useful object here as drag source
				DragBoth().string_(argKey).font_(patch.font),
				argView.orientation_(\horizontal).maxHeight_(20),
				patch.eventModel.numberBox(controlName.name).fixedWidth_(50).font_(patch.font)
			];
		})
	}

	server { ^patch.server }

	set { | value |
		patch.set(this.name, value);
	}

	name { ^controlName.name }
}

BufferConnector : ControlConnector {
	var <bufferName, <buffer;
	makeGui {
		var name, menu;
		name = controlName.name;
		menu = PopUpMenu().maxHeight_(20).items_(["-"])
		.action_({ | me | this.setBuffer(me.item.asSymbol) })
		.addNotifier(BufferList(this.server), \bufferList, { | buflist | this.updateMenu(buflist) })
		.releaseOnClose;

		^patch.eventModel.numSlider(controlName.name, decoratorFunc: { | argKey, argView |
			[   // TODO: must put a useful object here as drag source
				DragBoth().string_(argKey).font_(patch.font),
				menu,
				patch.eventModel.numberBox(controlName.name)
				.fixedWidth_(50).font_(patch.font).decimals_(0)
				.clipLo_(0).clipHi_(patch.server.options.numBuffers)
			];
		})
	}

	updateMenu { | buflist |
	}

	setBuffer { | bufName |
		var buffer;
		buffer = BufferList(this.server).buffers[bufName];
		if (buffer.isNil) { this.set(0); }{ this.set(buffer.bufnum) }
	}
}

InputConnector : ControlConnector {
	var <descriptor; /* IODesc from SynthDesc.
	Holds rate, numberOfChannels, startingChannel, type.
	The name of the control is stored in startingChannel. */

	*new { | patch, controlName, descriptor |
		^this.newCopyArgs(patch, controlName, nil, descriptor);
	}
}

OutputConnector : InputConnector {


}
