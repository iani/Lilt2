/* IZ Jul 24, 2013 (6:11 PM)

GUI for access to Synths created by playing a Function.
See method Function:l

*/

SynthList {
	classvar <all;
	classvar >default;

	var <server;
	var <list;      // list of registered SynthModels
	var controller; // relay update messages from synth models

	*initClass {
		all = IdentityDictionary();
	}

	*new { | server |
		var synthList;
		server = server.asTarget.server;
		synthList = all[server];
		synthList ?? {
			synthList = this.newCopyArgs(server, List()).init;
			all[server] = synthList;
		};
		^synthList;
	}

	init {
		controller = { | model | this.changed(\synthState, model); }
	}

	*add { | synthModel |
		^this.default.add(synthModel);
	}

	*default {
		default ?? { default = this.new(Server.default) };
		^default;
	}

	add { | model |
		list add: model;
		model.addDependant(controller);
		this.changed(\list, model);
	}

	*remove { | model | this.default.remove(model) }

	remove { | model |
		list remove: model;
		model.removeDependant(controller);
		this.changed(\list, model);
	}

	*gui { ^this.default.gui }

	gui {
		^SynthListGui(this);
	}

	getItemNames {
		^list collect: _.name
	}

	controlBusses {
		var controlBusses;
		controlBusses = Set();
		list do: { | synthModel | controlBusses addAll: synthModel.controlBusses };
		^controlBusses.asArray;
	}

	audioBusses {
		var audioBusses;
		audioBusses = Set();
		list do: { | synthModel | audioBusses addAll: synthModel.audioBusses };
		^audioBusses.asArray;
	}

}

SynthListGui {
	classvar <>runningColor, <>stoppedColor, <pausedColor;

	var list; // SynthList holding registered SynthModels
	var window, listView, nameField, guiButton, startButton, stopButton, pauseButton;
	var <selected;

	*initClass {
		Class.initClassTree(Color);
		runningColor = Color(1, 0.5, 0.5);
		stoppedColor = Color.white;
		pausedColor = Color.gray(0.7);

	}
	*new { | list |
		^this.newCopyArgs(list).init;
	}

	init {

		window = Window("Synths", Rect(0, 0, 200, 300)).front;
		window.layout = VLayout(
			nameField = TextField(),
			HLayout(
				listView = ListView().minWidth_(120),
				VLayout(
					guiButton = Button().states_([["gui"]]),
					// TODO: change button states according to whether synth has gate or not
					startButton = Button().states_([["start"], ["fade out"]]),
					stopButton = Button().states_([["stop"]]),
					pauseButton = Button().states_([["pause"], ["resume"]])
				)
			)
		);

		nameField.action = { | me | selected !? { selected.name = me.string } };

		listView.items_(list.getItemNames)
		.font_(Font.default.size_(10))
		.action_({ | me | this.selectSynth(me.value) });

		guiButton.action = { selected !? { selected.gui } };
		startButton.action = { | me |
			selected !? {
				[{ selected.release }, { selected.start(true) }][me.value].value;
			}
		};
		stopButton.action = { selected !? { selected.free } };
		pauseButton.action = { | me |
			selected !? {
				[{ selected.resume }, { selected.pause }][me.value].value;
			}
		};

		this.selectSynth(listView.value);

		this.addNotifier(list, \list, { | model |
			listView.items = list.getItemNames;
			if (selected.isNil) {
				this.selectSynth(list.list indexOf: model)
			}{
				this.selectSynth(list.list indexOf: selected)
			};
		});

		this.addNotifier(list, \synthState, { | model |
			this.updateListColors;
			if (model === selected) { this.updateButtonStates; }
		});
		window.onClose = { this.objectClosed; };
	}

	selectSynth { | index = 0 |
		listView.value = index;
		selected = list.list[index ? 0];
		this.updateButtonStates;
		this.updateListColors;
	}

	updateButtonStates {
		var isPlaying;
		if (selected.isNil) {
			nameField.string_("").enabled = false;
			guiButton.enabled = false;
			startButton.enabled = false;
			stopButton.enabled = false;
			pauseButton.enabled = false;
		}{
			isPlaying = selected.hasSynth;
			nameField.enabled_(true).string = selected.name;
			guiButton.enabled = true;
			startButton.enabled = true;
			startButton.value = isPlaying.binaryValue;
			stopButton.enabled = isPlaying;
			pauseButton.value = (isPlaying and: { selected.isRunning }.not).binaryValue;
			pauseButton.enabled = isPlaying;
		};
	}

	updateListColors {
		listView.colors = list.list collect: { | sm |
			if (sm.isPlaying) {
				if (sm.isRunning) { runningColor } { pausedColor };
			}{
				stoppedColor
			}
		};
		listView.refresh;
	}
}