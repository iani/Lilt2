/* IZ Jul 26, 2013 (6:13 PM)

Create gui for interconnecting Synths via busses. Uses SynthModel.

BusListGui();

*/


BusList {
	classvar default;

	var synthList; // list of SynthModels for interconnecting
	/* Note: Control-rate InputConnectors are not supported in the present implementation.
	controlInputs get their input only by mapping: aSynth.map(parameter, busNumber)
	*/
	var <controlOutputs, <controlBusses, <controlInputs;
	var <audioOutputs, <audioBusses, <audioInputs;

	*default {
		default ?? { default = this.new; };
		^default;
	}

	*new { | synthList |
		^this.newCopyArgs(synthList ?? { SynthList.default }).init;
	}

	init {
		this.addNotifier(synthList, \list, { this.updateLists });
	}

	updateLists {
		controlOutputs = synthList.list.collect(_.controlOutputs).flat; // Out.kr
		controlInputs = synthList.list.collect(_.controlInputs).flat;  // N.B.: In.kr not supported!
		audioOutputs = synthList.list.collect(_.audioOutputs).flat;
		audioInputs = synthList.list.collect(_.audioInputs).flat;

		this.changed(\lists,
			controlOutputs collect: _.asString,
			controlBusses collect: _.asString,
			controlInputs collect: _.asString,
			audioOutputs collect: _.asString,
			audioBusses collect: _.asString,
			audioInputs collect: _.asString
		)
	}
}

/*

Note: The order of lists should be (from left to right, bus lists omitted:):

Control Outputs -> Control Inputs ! Audio Outputs -> Audio Inputs

BusListGui();
*/


BusListGui {
	var <list;
	var window;

	*new { | list |
		^this.newCopyArgs(list ?? { BusList.default }).init;
	}

	init {
		window = Window("Bus Browser", Rect(0, 500, 900, 300)).front;
		window.view.layout = HLayout(
			[VLayout(
				StaticText().string_("Control Outputs"),
				HLayout(
					StaticText().string_("Links:"),
//					Button().states_([["-"], ["=>", Color.black, Color.red], ["=>", Color.black, Color.blue]]),
					Button().states_([["set"], ["remove"]]).maxWidth_(50),
					Button().states_([["branch"]]).maxWidth_(50),
//					Button().states_([["remove"]]).maxWidth_(50),
				),
				HLayout(
					Button().states_([["show ctls"], ["show all"]]),
					Button().states_([["auto link+", Color.black, Color.red]]),
				),
				ListView()
			), s: 2],
			[VLayout(
				StaticText().string_("Control Busses"),
				VLayout(
					Button().states_([["add bus"]]),
					HLayout(StaticText().string_("chans:"), NumberBox().clipLo_(1).clipHi_(64).decimals_(0))
				),
				ListView()
			), s: 1],
			[VLayout(
				StaticText().string_("Control Inputs"),
				HLayout(
//					Button().states_([["-"], ["=>", Color.black, Color.red], ["=>", Color.black, Color.blue]]),
//					StaticText().string_("(Link)"),
					Button().states_([["set"], ["remove"]]).maxWidth_(50),
					Button().states_([["branch"]]).maxWidth_(50),
				),
				ListView()
			), s: 2],
			[VLayout(
				StaticText().string_("Audio Outputs"),
				HLayout(
//					StaticText().string_("Link:"),
//					Button().states_([["-"], ["=>", Color.black, Color.red], ["=>", Color.black, Color.blue]]),
					Button().states_([["set"], ["remove"]]).maxWidth_(50),
					Button().states_([["branch"]]).maxWidth_(50),
				),
				HLayout(
					Button().states_([["show ctls"], ["show all"]]),
					Button().states_([["auto link+", Color.black, Color.red]]),
				),
				ListView()
			), s: 2],
			[VLayout(
				StaticText().string_("Audio Busses"),
				VLayout(
					Button().states_([["add bus"]]),
					HLayout(StaticText().string_("chans:"), NumberBox().clipLo_(1).clipHi_(64).decimals_(0))
				),
				ListView()
			), s: 1],
			[VLayout(
				StaticText().string_("Audio Inputs"),
				HLayout(
//					Button().states_([["-"], ["=>", Color.black, Color.red], ["=>", Color.black, Color.blue]]),
//					StaticText().string_("(Link)"),
					Button().states_([["set"], ["remove"]]).maxWidth_(50),
					Button().states_([["branch"]]).maxWidth_(50),
				),
				ListView()
			), s: 2]

		)
	}
}