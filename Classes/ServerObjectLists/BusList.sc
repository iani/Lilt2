/* IZ Jul 26, 2013 (6:13 PM)

Create gui for interconnecting Synths via busses. Uses SynthModel.

BusListGui();

*/


BusList {
	var synthList; // list of SynthModels for interconnecting

	*new { | synthList |
		^this.newCopyArgs(synthList ?? { SynthList.default }).init;
	}

	init {
		this.addNotifier(synthList, \list, { this.updateLists });
	}

	updateLists {

	}
}



BusListGui {
	var <list;
	var window;

	*new { | list |
		^this.newCopyArgs(list).init;
	}

	init {
		window = Window("Bus Browser", Rect(0, 500, 900, 300)).front;
		window.view.layout = HLayout(
			VLayout(
				StaticText().string_("Audio Outputs"),
				HLayout(
					StaticText().string_("Link:"),
					Button().states_([["-"], ["=>", Color.black, Color.red], ["=>", Color.black, Color.blue]]),
				),
				ListView()
			),
			VLayout(
				StaticText().string_("Audio Busses"),
				HLayout(
					Button().states_([["add bus"]]),
					StaticText().string_("chans:"),
					NumberBox()
				),
				ListView()
			),
			VLayout(
				StaticText().string_("Audio Inputs"),
				HLayout(
					Button().states_([["-"], ["=>", Color.black, Color.red], ["=>", Color.black, Color.blue]]),
					StaticText().string_("(Link)"),
				),
				ListView()
			),
			VLayout(
				StaticText().string_("Control Inputs"),
				HLayout(
					StaticText().string_("Link:"),
					Button().states_([["-"], ["<=", Color.black, Color.red], ["<=", Color.black, Color.blue]]),
				),
				ListView()
			),
			VLayout(
				StaticText().string_("Control Busses"),
				HLayout(
					Button().states_([["add bus"]]),
					StaticText().string_("chans:"),
					NumberBox()
				),
				ListView()
			),
			VLayout(
				StaticText().string_("Control Outputs"),
				HLayout(
					Button().states_([["-"], ["<=", Color.black, Color.red], ["<=", Color.black, Color.blue]]),
					StaticText().string_("(Link)"),
				),
				ListView()
			),

		)
	}
}