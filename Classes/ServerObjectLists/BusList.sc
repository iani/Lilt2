/* IZ Jul 26, 2013 (6:13 PM)

Create gui for interconnecting Synths via busses. Uses SynthModel.

BusListGui();

*/


BusList {
	classvar <all;
	classvar default;

	var <synthList; // list of SynthModels for interconnecting
	/* Note: Control-rate InputConnectors are not supported in the present implementation.
	controlInputs get their input only by mapping: aSynth.map(parameter, busNumber)
	*/
	var <controlOutputs, <controlBusses, <controlInputs;
	var <audioOutputs, <audioBusses, <audioInputs;

	*initClass {
		all = IdentityDictionary();
	}

	*gui { BusListGui(this.default) }

	*default {
		default ?? { default = this.new; };
		^default;
	}

	*new { | server |
		var busList, synthList;
		synthList = SynthList(server.asTarget.server);
		busList = all[synthList];
		busList ?? {
			busList = this.newCopyArgs(synthList).init;
			all[synthList] = busList;
		};
		^busList;
	}

	init {
		this.addNotifier(synthList, \list, { this.updateSynthLists });
		controlOutputs = [];
		controlBusses = [];
		controlInputs = [];
		audioOutputs = [];
		audioBusses = [];
		audioInputs = [];
	}

	updateSynthLists {
		controlOutputs = synthList.list.collect(_.controlOutputs).flat; // Out.kr
		controlBusses = synthList.controlBusses;
		controlInputs = synthList.list.collect(_.controlInputs).flat;  // N.B.: In.kr not supported!
		audioOutputs = synthList.list.collect(_.audioOutputs).flat;
		audioBusses = synthList.audioBusses;
		audioInputs = synthList.list.collect(_.audioInputs).flat;

		this.changed(\synthLists,
			controlOutputs collect: _.asString,
//			controlBusses collect: _.asString,
			controlInputs collect: _.asString,
			audioOutputs collect: _.asString,
//			audioBusses collect: _.asString,
			audioInputs collect: _.asString
		)
	}

	addAudio { | busConnector |
		audioBusses = audioBusses add: busConnector;
		this.changed(\audioBusses);
	}

	addControl { | busConnector |
		controlBusses = controlBusses add: busConnector;
		this.changed(\controlBusses);
	}
}

/*

Note: The order of lists should be (from left to right, bus lists omitted:):

Control Outputs -> Control Inputs ! Audio Outputs -> Audio Inputs

BusListGui();
*/


BusListGui {
	classvar font;
	var <list; // a BusList
	var window;
	var <controlOutputs, <controlBusses, <controlInputs;
	var <audioOutputs, <audioBusses, <audioInputs;

	var selCtlOut, selCtlBus, selCtlIn, selAudOut, selAudBus, selAudIn;
	var ctlChansNumBox, audChansNumBox;
	var ctlOutSetButton, ctlOutBranchButton, ctlLinkButton, ctlBusButton, ctlInSetButton, ctlInBranchButton;
	var audOutSetButton, audOutBranchButton, audLinkButton, audBusButton, audInSetButton, audInBranchButton;

	*new { | list |
		font ?? { font = Font.default.size_(10) };
		^this.newCopyArgs(list ?? { BusList.default }).init;
	}

	init {
		window = Window("Bus Browser", Rect(0, 550, 900, 300)).front;
		window.view.palette = QPalette.dark;
		window.onClose = { this.objectClosed };
		window.view.layout = HLayout(
			[VLayout(
				StaticText().string_("Control Outputs").font_(font).align_(\center),
				HLayout(
					ctlOutSetButton = Button().states_([["set"], ["remove"]]).maxWidth_(50).font_(font),
					ctlOutBranchButton = Button().states_([["branch"]]).maxWidth_(50).font_(font),
				).spacing_(1),
				controlOutputs = ListView()
				.action_({ | me | this.selectControlOutput(me.value) })
			), s: 2],
			[VLayout(
				StaticText().string_("Control Busses").font_(font).align_(\center),
				VLayout(
					HLayout(
						ctlLinkButton = Button().states_([["+auto link", Color.black, Color.red]])
						.maxWidth_(60).font_(font),
						ctlBusButton = Button().states_([["+bus"]]).font_(font).maxWidth_(40),
					).spacing_(1),
					HLayout(
						StaticText().string_("num chans:").font_(font),
						ctlChansNumBox = NumberBox().clipLo_(1).clipHi_(64).decimals_(0)
						.font_(font).maxWidth_(25).background_(Color.white)
					)
				),
				controlBusses = ListView()
//				.action_({ | me | this.selectAudioOutput(me.value) })
			), s: 1],
			[VLayout(
				StaticText().string_("Control Inputs").font_(font).align_(\center),
				HLayout(
					ctlInSetButton = Button().states_([["set"], ["remove"]])
					.maxWidth_(50).font_(font),
					ctlInBranchButton = Button().states_([["branch"]])
					.maxWidth_(50).font_(font),
				).spacing_(1),
				controlInputs = ListView()
				.action_({ | me | this.selectControlInput(me.value) })
			), s: 2],
			[VLayout(
				StaticText().string_("Audio Outputs").font_(font).align_(\center),
				HLayout(
					Button().states_([["set"], ["remove"]]).maxWidth_(50).font_(font),
					Button().states_([["branch"]]).maxWidth_(50).font_(font),
				).spacing_(1),
				audioOutputs = ListView()
				.enterKeyAction_({
					selAudOut !? {
						selCtlIn = selAudOut.patch.connectors[0];
						controlInputs.value = list.controlInputs.indexOf(selCtlIn) ? 0;
					};
				})
				.action_({ | me | this.selectAudioOutput(me.value) })
			), s: 2],
			[VLayout(
				StaticText().string_("Audio Busses").font_(font).align_(\center),
				VLayout(
					HLayout(
						Button().states_([["+auto link", Color.black, Color.red]])
						.maxWidth_(60).font_(font),
						Button().states_([["+bus"]]).font_(font).maxWidth_(40),
					).spacing_(1),
					HLayout(
						StaticText().string_("num chans:").font_(font),
						audChansNumBox = NumberBox().clipLo_(1).clipHi_(64).decimals_(0)
						.font_(font).maxWidth_(25).background_(Color.white)
					)
				),
				audioBusses = ListView()
//				.action_({ | me | this.selectControlOutput(me.value) })
			), s: 1],
			[VLayout(
				StaticText().string_("Audio Inputs").font_(font).align_(\center),
				HLayout(
					Button().states_([["set"], ["remove"]]).maxWidth_(50).font_(font),
					Button().states_([["branch"]]).maxWidth_(50).font_(font),
				).spacing_(1),
				audioInputs = ListView()
				.action_({ | me | this.selectAudioInput(me.value) })
			), s: 2]
		);
		this.addNotifier(list, \synthLists, { | cOut, cIn, aOut, aIn |
			this.updateSynthLists(cOut, cIn, aOut, aIn);
		});
		this.addNotifier(list, \audioBusses, { this.updateAudioBusses; });
		this.addNotifier(list, \controlBusses, { this.updateControlBusses; });

		this.updateSynthLists;
		this.updateAudioBusses;
		this.updateControlBusses;
	}

	updateSynthLists { | cOut, cIn, aOut, aIn |
		controlOutputs.items = cOut;
		controlInputs.items = cIn;
		audioOutputs.items = aOut;
		audioInputs.items = aIn;

		// controlOutputs.postln;
		// controlOutputs.value = 0;
		// list.controlOutputs.indexOf(selCtlOut).postln;
		controlOutputs.value = list.controlOutputs.indexOf(selCtlOut) ? 0;
		controlInputs.value = list.controlInputs.indexOf(selCtlIn) ? 0;
		audioOutputs.value = list.audioOutputs.indexOf(selAudOut) ? 0;
		audioInputs.value = list.audioInputs.indexOf(selAudIn) ? 0;
		// Following initializes the selected items at gui creation time
		if (list.audioOutputs.size > 0) { selAudOut = list.audioOutputs[audioOutputs.value]; };
		if (list.audioInputs.size > 0) { selAudIn = list.audioInputs[audioInputs.value]; };
		if (list.controlOutputs.size > 0) { selCtlOut = list.controlOutputs[controlOutputs.value]; };
		if (list.controlInputs.size > 0) { selCtlIn = list.controlInputs[controlInputs.value]; };
	}

	selectControlOutput { | index |
		selCtlOut = list.controlOutputs[index];
		selCtlOut !? {
			selCtlOut.readerBusses
		}
	}

	selectControlBus { | index |
//		selCtlBus = list.audioOutputs[index];
	}

	selectControlInput { | index |
		selCtlIn = list.controlInputs[index];
	}

	selectAudioOutput { | index |
		selAudOut = list.audioOutputs[index];
		selAudOut !? {
			selAudOut.readerBusses; // .postln;
		}
	}

	selectAudioBus { | index |
//		selAudBus = list.audioOutputs[index];
	}

	selectAudioInput { | index |
		selAudIn = list.audioInputs[index];
	}

	addReaderAudioBus {
		selAudOut !? {
			selAudBus = selAudOut.addReaderAudioBus;
			this.updateAudioBusList;
		}
	}

	updateAudioBusses {
		audioBusses.items = []; // TODO
		audioBusses.value; // ...
	}

	updateControlBusses { // TODO

	}
}