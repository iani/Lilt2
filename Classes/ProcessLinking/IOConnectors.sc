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
		patch.put(this.name, value);
	}

	get { ^patch.at(this.name) }

	name { ^controlName.name }

}

BufferConnector : ControlConnector {
	var <bufferName, <buffer, <bufferList;

	*new { | patch, controlName |
		^super.new(patch, controlName).getBufferList;
	}

	getBufferList { bufferList = BufferList(this.server) }

	makeGui {
		var name, menu, numBox;
		name = controlName.name;
		menu = PopUpMenu().maxHeight_(20).items_(["-"])
		.action_({ | me | this.setBuffer(me.item.asSymbol) })
		.addNotifier(bufferList, \bufferList, { | buflist | this.updateMenu(menu, buflist) })
		.addNotifier(patch.event, this.name, { | val |
			menu.value = bufferList.nameIndex(val);
		})
		.releaseOnClose
		.items_(bufferList.bufferNames)
		.value_(bufferList.nameIndex(this.get));

		numBox = patch.eventModel.numberBox(controlName.name)
		.fixedWidth_(50).font_(patch.font).decimals_(0)
		.clipLo_(0).clipHi_(patch.server.options.numBuffers);

		^patch.eventModel.numSlider(controlName.name, decoratorFunc: { | argKey, argView |
			[   // TODO: must put a useful object here as drag source
				DragBoth().string_(argKey).font_(patch.font),
				menu,
				numBox
			];
		})
	}

	updateMenu { | menu, buflist |
		var bufItemNum;
		menu.items = buflist;
		menu.value = buflist.indexOf(bufferName) ? 0;
	}

	setBuffer { | bufName |
		var buffer;
		bufferName = bufName;
		buffer = BufferList(this.server).buffers[bufName];
		if (buffer.isNil) { this.set(0); }{ this.set(buffer.bufnum) };
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
