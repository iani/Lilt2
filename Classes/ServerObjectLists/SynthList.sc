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
	classvar <>runningColor, <>stoppedColor, <pausedColor, ampSpec;

	var list; // SynthList holding registered SynthModels
	var window, listView, nameField, guiButton, startButton, stopButton, pauseButton;
	var ampKnob, ampNumBox;
	var <selected;

	var <modelSwitcher; // for new version using NotifierSwitch

	*initClass {
		Class.initClassTree(Color);
		runningColor = Color(1, 0.5, 0.5);
		stoppedColor = Color.white;
		pausedColor = Color.gray(0.7);
		Class.initClassTree(ControlSpec);
		ampSpec = ControlSpec(0, 8, \lin, 0, 0);

	}
	*new { | list |
		^this.newCopyArgs(list).init;
	}

	init {
		modelSwitcher = NotifierSwitch(this, \synthModel, { | model |
			[this, thisMethod.name, "experimental use of NotifierSwitch", model].postln;
			// if (model.isNil) { model } { model.eventModel.event };
			model
		});

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
					pauseButton = Button().states_([["pause"], ["resume"]]),
					ampKnob = Knob(),
					ampNumBox = NumberBox(),
					Slider()
					.orientation_(\horizontal)
					.maxHeight_(20)
					.addNotifierSwitch(modelSwitcher, \amp, { | val, notification |
						notification.listener.value = val;
						}, { | synthModel |

							if (synthModel.isNil) { nil } { synthModel.eventModel.event; };
						}
					).action_({ | me |
						me.value.postln;
						modelSwitcher.notifier !? {
							modelSwitcher.notifier.eventModel.put(\amp, me.value)
						}
					}),
					NumberBox()
					.maxHeight_(20)
					.addNotifierSwitch(modelSwitcher, \amp, { | val, notification |
						notification.listener.value = val;
						}, { | synthModel |

							if (synthModel.isNil) { nil } { synthModel.eventModel.event; };
						}
					).action_({ | me |
						me.value.postln;
						modelSwitcher.notifier !? {
							modelSwitcher.notifier.eventModel.put(\amp, me.value)
						}
					})
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

		ampKnob.action_({ | me |
			var val;
			val = ampSpec.map(me.value);
			ampNumBox.value = val;
			selected !? { selected.put(\amp, val); };
		});

		ampNumBox.action_({ | me |
			var val;
			val = me.value;
			ampKnob.value = ampSpec.unmap(val);
			selected !? { selected.put(\amp, me.val) };
		});

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
		this.changed(\synthModel, selected);
	}

	updateButtonStates {
		var isPlaying, amp;
		if (selected.isNil) {
			nameField.string_("").enabled = false;
			guiButton.enabled = false;
			startButton.enabled = false;
			stopButton.enabled = false;
			pauseButton.enabled = false;
			ampKnob.value = 0;
			ampNumBox.value = 0;
		}{
			isPlaying = selected.hasSynth;
			nameField.enabled_(true).string = selected.name;
			guiButton.enabled = true;
			startButton.enabled = true;
			startButton.value = isPlaying.binaryValue;
			stopButton.enabled = isPlaying;
			pauseButton.value = (isPlaying and: { selected.isRunning }.not).binaryValue;
			pauseButton.enabled = isPlaying;
			// todo: connect to eventModel of synth ...
			amp = selected.at('amp');
//			[this, thisMethod.name, selected, selected.at('amp')].postln;
			amp !? {
				ampKnob.value = ampSpec.map(amp);
				ampNumBox.value = amp;
			};
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