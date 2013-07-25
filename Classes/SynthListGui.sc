/* IZ Jul 24, 2013 (6:11 PM)

GUI for access to Synths created by playing a Function.


See method Function:l

*/

SynthList {
	classvar >default;
	classvar <>font;

	var <list;

	*new { ^this.newCopyArgs(List()) }

	*add { | synthModel |
		^this.default.add(synthModel);
	}

	*default {
		default ?? { default = this.new };
		^default;
	}

	add { | model |
		list add: model;
		this.changed(\list, model);
	}

	*gui { ^this.default.gui }

	gui {
		^SynthListGui(this);
	}

	getItemNames {
		^list collect: _.name
	}
}

SynthListGui {

	var list;
	var window, listView, nameField, guiButton, startButton, stopButton, pauseButton;
	var <selected;

	*new { | list |
		^this.newCopyArgs(list).init;
	}

	init {
		this.addNotifier(list, \list, { | model |
			listView.items = list.getItemNames;
			selected ?? { this.selectSynth(list.list indexOf: model) }
		});
		window = Window("Synths", Rect(0, 0, 200, 300)).front;
		window.onClose = { this.objectClosed };
		window.layout = VLayout(
			nameField = TextField(),
			HLayout(
				listView = ListView().minWidth_(120),
				VLayout(
					guiButton = Button().states_([["gui"]]),
					startButton = Button().states_([["start"], ["fade out"]]),
					stopButton = Button().states_([["stop"]]),
					pauseButton = Button().states_([["pause"], ["run"]])
				)
			)
		);

		listView.items_(list.getItemNames)
		.font_(Font.default.size_(10))
		.releaseOnClose
		.action_({ | me | this.selectSynth(me.value) })
		.addNotifier(this, \list, { listView.items = list.getItemNames });

		nameField.releaseOnClose
		.addNotifier(this, \selectedSynth, { | model, notification |
			model !? { notification.listener.string_(model.name) }
		});

		guiButton.action = { selected !? { selected.gui } };
		this.selectSynth(listView.value);
	}

	selectSynth { | index = 0 |
		selected = list.list[index ? 0];
		selected !? {
			nameField.string = selected.name;
		};
	}

	/*
	if (selected.isNil) { this.selectSynth(list indexOf: model) };
	*/
}