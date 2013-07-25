/* IZ Jul 24, 2013 (6:11 PM)

GUI for access to Synths created by playing a Function.
See method Function:l

*/

SynthList {
	classvar >default;
	classvar <>font;

	var <list;      // list of registered SynthModels

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
		this.addNotifier(model, \synthStarted, { this.changed(\list, model) });
		this.addNotifier(model, \synthEnded, { this.changed(\list, model) });
		this.addNotifier(model, \run, { this.changed(\list, model) });
		this.changed(\list, model);
	}

	*remove { | model | this.default.remove(model) }

	remove { | model |
		list remove: model;
		this.removeNotifier(model, \synthStarted);
		this.removeNotifier(model, \synthEnded);
		this.removeNotifier(model, \run);
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
	classvar <>runningColor, <>stoppedColor, <pausedColor;

	var list; // SynthList holding registered SynthModels
	var controller; // attached to selected SynthModel, translates notifications
	var window, listView, nameField, guiButton, startButton, stopButton, pauseButton;
	var <selected;

	*initClass {
		Class.initClassTree(Color);
		runningColor = Color.red;
		stoppedColor = Color.white;
		pausedColor = Color.gray;

	}
	*new { | list |
		^this.newCopyArgs(list).init;
	}

	init {
		// NOTE: The controller can be removed?
//		controller = SimpleController();
//		controller.put(\synthEnded, { this.synthEnded; this.updateListColors; });
//		controller.put(\synthStarted, { this.synthStarted; this.updateListColors; });
//		controller.put(\run, {
//			this.updateRunState; this.updateListColors;
//		});

		this.addNotifier(list, \list, { | model |
			listView.items = list.getItemNames;
			if (selected.isNil) {
				this.selectSynth(list.list indexOf: model)
			}{
				this.selectSynth(list.list indexOf: selected)
			};
			this.updateListColors;
		});
		window = Window("Synths", Rect(0, 0, 200, 300)).front;
		window.onClose = { this.objectClosed; selected !? { selected.removeDependant(controller) } };
		window.layout = VLayout(
			nameField = TextField(),
			HLayout(
				listView = ListView().minWidth_(120),
				VLayout(
					guiButton = Button().states_([["gui"]]),
					startButton = Button().states_([["start"], ["fade out"]]),
					stopButton = Button().states_([["stop"]]),
					pauseButton = Button().states_([["resume"], ["pause"]])
				)
			)
		);

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
				[{ selected.pause }, { selected.resume }][me.value].value;
			}
		};

		this.selectSynth(listView.value);
	}

	selectSynth { | index = 0 |
		listView.value = index;
//		selected !? { selected.removeDependant(controller); };
		selected = list.list[index ? 0];
//		selected !? { selected.addDependant(controller); };
		this.updateButtonStates;
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
			startButton.value = isPlaying.binaryValue;
			stopButton.enabled = isPlaying;
			this.updateRunState;
			pauseButton.enabled = isPlaying;
			this.updateListColors;
		};
	}

	synthStarted {
		startButton.enabled_(true).value_(1);
		stopButton.enabled = true;
		pauseButton.enabled_(true).value_(0);
	}

	synthEnded {
		startButton.enabled_(true).value_(0);
		stopButton.enabled = false;
		pauseButton.enabled_(false).value_(0);
	}

	updateRunState {
		pauseButton.value = selected.isRunning.binaryValue;
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