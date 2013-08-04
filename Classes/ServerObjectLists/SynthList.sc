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
	var window, listView, stopButton, pauseButton;
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
			model
		});

		window = Window("Synths", Rect(0, 0, 200, 300)).front;
		window.layout = VLayout(
			TextField()
			.action_({ | me |
				modelSwitcher.notifier !? {
					modelSwitcher.notifier.name = me.string;
					list.changed(\list);
				}
			})
			.addNotifierSwitch(modelSwitcher, \name, { | me |
				modelSwitcher.notifier !? { me.string = modelSwitcher.notifier.name }
				}, { | model, me |
					if (model.isNil) { me.string = "" } { me.string = model.name }
			}),
			HLayout(
				VLayout(
					listView = ListView().minWidth_(120),
					ListView().minWidth_(120).font_(Font.default.size_(10))
					.items_(list.getItemNames)
					.action_({ | me | this.selectSynth(me.value) })
					.addNotifier(list, \list, { | model, notification |
						notification.listener.items = list.getItemNames;
						// TODO: Complete this!
						this.selectSynth();
					}),
				),
				VLayout(
					Button().states_([["gui"]])
					.action_({ modelSwitcher.notifier.gui })
					.addNotifierSetActions(modelSwitcher, _.enabled_(true), _.enabled_(false)),
					Button().states_([["start"], ["fade out"]])
					.addNotifierSetActions(modelSwitcher, { | v |
						v.states = modelSwitcher.notifier.makeStartButtonStates;
						v.action = modelSwitcher.notifier.makeStartButtonAction;
						v.enabled = true;
						v.value = modelSwitcher.notifier.isPlaying.binaryValue;
					}, _.enabled_(false)),

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
						modelSwitcher.notifier !? {
							modelSwitcher.notifier.eventModel.put(\amp, me.value)
						}
					}),
					NumberBox()
					.maxHeight_(20)
					.addNotifierSwitch(modelSwitcher, \amp, { | val, notification |
						notification.listener.value = val;
						}, { | synthModel, theNumBox |
							if (synthModel.isNil) {
								theNumBox = 0;
								nil
							} {
								theNumBox.value = synthModel.eventModel.event[\amp] ? 0;
								synthModel.eventModel;
							};
						}
					).action_({ | me |
						modelSwitcher.notifier !? {
							modelSwitcher.notifier.eventModel.put(\amp, me.value)
						}
					})
				)
			)
		);

		listView.items_(list.getItemNames)
		.font_(Font.default.size_(10))
		.action_({ | me | this.selectSynth(me.value) });

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
			stopButton.enabled = false;
			pauseButton.enabled = false;
			ampKnob.value = 0;
			ampNumBox.value = 0;
		}{
			isPlaying = selected.hasSynth;
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