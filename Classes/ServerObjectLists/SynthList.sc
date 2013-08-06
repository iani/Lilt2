/* IZ Jul 24, 2013 (6:11 PM)

GUI for access to Synths created by playing a Function.
See method Function:l

*/

SynthList {
	classvar <all;
	classvar >default;

	var <server;
	var <list;      // list of registered SynthModels
	var controller; // broadcast synthStarted, synthEnded update messages from all synth models
	var defResponder; // inform when one or more SynthDefs are loaded
	var defResponderAction; /* Performed when SynthDefs have been added.
	   Only performed once per second to avoid overloading the system with too many updates
	*/

	*initClass {
		all = IdentityDictionary();
		StartUp add: {
			(PathName(PathName(PathName(this.filenameSymbol.asString).parentPath).parentPath
			).parentPath +/+ "SynthDefs/*.scd").pathMatch do: _.load;
			(Platform.userAppSupportDir +/+ "SynthDefs/*.scd").pathMatch do: _.load;
			this.changed(\list);
		}
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
		controller = { | model | this.changed(\synthState, model); };
		defResponder = OSCFunc({ defResponderAction.value }, '/done', server.addr,
			argTemplate: ['/d_recv']
		);
		defResponderAction = DoOnceIn({ this.updateSynthDefs; }, 1);
	}

	updateSynthDefs {
		this.changed(\synthDefs,
			SynthDescLib.global.synthDescs.keys.asArray.collect(_.asString).reject({ | n |
				"freqScope*".matchRegexp(n) or: {
					"system_*".matchRegexp(n)
				}
			}).sort
		)
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
	classvar <>runningColor, <>stoppedColor, <pausedColor, font;

	var list; // SynthList holding registered SynthModels
	var window, pauseButton;
	var ampSpec;
	var <selected;

	var <modelSwitcher; // for new version using NotifierSwitch

	*initClass {
		Class.initClassTree(Color);
		runningColor = Color(1, 0.5, 0.5);
		stoppedColor = Color.white;
		pausedColor = Color.gray(0.7);
		StartUp add: { font = Font.default.size_(10); };
	}
	*new { | list |
		^this.newCopyArgs(list).init;
	}

	init {
		this.makeSpec;
		modelSwitcher = NotifierSwitch(this, \synthModel, { | model | model });
		window = Window("Synth Player", Rect(0, 0, 400, 290));
		window.layout = GridLayout.columns(
			[   // ======= Server boot/quit button and SynthDef list =======
				HLayout(
					StaticText().string_("SynthDefs").font_(Font.default.size_(10)),
					Button().states_([["boot server "], ["quit server"]])
					.font_(Font.default.size_(10))
					.action_({ | me |
						list.server.perform([\boot, \quit][1 - me.value])
					})
					.addNotifier(list.server, \serverRunning, { | notification |
						notification.listener.value = list.server.serverRunning.binaryValue;
					})
					.value_(list.server.serverRunning.binaryValue)
				),
				[
					ListView().font_(Font.default.size_(10))
					.addNotifier(list, \synthDefs, { | defNameList, notifier |
						notifier.listener.items = defNameList;
					})
					.enterKeyAction_({ | view |
						view.item !? { SynthModel(view.item).add }
					}),
					rows: 8
				]
			],

			[   // ======= List of SynthModels =======
				[HLayout(
					StaticText().string_("Synth Players").font_(Font.default.size_(10)),
					TextField().action_({ | me |
						modelSwitcher.notifier !? {
							modelSwitcher.notifier.name = me.string;
							list.changed(\list);
						}
					})
					.addNotifierSwitch(modelSwitcher, \name, { | me |
						modelSwitcher.notifier !? { me.string = modelSwitcher.notifier.name }
						}, { | model, me |
							if (model.isNil) { me.string = "" } { me.string = model.name }
						}
				)), columns: 2],
				[ListView().minWidth_(120).font_(Font.default.size_(10))
					.items_(list.getItemNames)
					.action_({ | me | this.changed(\synthModel, list.list[me.value]) })
					.addNotifier(list, \list, { | model, notification |
						notification.listener.items = list.getItemNames;
						this.colorSynthList(notification.listener);
						notification.listener.value = list.list.indexOf(model) ? 0;
						this.changed(\synthModel, model);
					})
					.addNotifier(list, \synthState, { | model, notification |
						this.colorSynthList(notification.listener);
					}),
					rows: 8
				]
			],

			[   // ======= SynthModel control items =======
				nil,
				// Open GUI window for selected SynthModel
				Button().states_([["gui"]])
				.enabled_(false)
				.action_({ modelSwitcher.notifier.gui })
				.addNotifierSetActions(modelSwitcher, _.enabled_(true), _.enabled_(false)),

				// Start / Release selected SynthModel
				Button().states_([["start"], ["fade out"]])
				.addNotifierSwitch(modelSwitcher, \synthStarted, { | notification |
					notification.listener.value = 1;
					}, { | model, view |
						if (model.isNil) {
							view.value = 0;
							view.enabled_(false);
						}{
							view.states = modelSwitcher.notifier.makeStartButtonStates;
							view.action = modelSwitcher.notifier.makeStartButtonAction;
							view.enabled_(true);
							view.value = model.isPlaying.binaryValue;

						};
						model
				})
				.addNotifierSwitch(modelSwitcher, \synthEnded, { | notification |
					notification.listener.value = 0;
				}),

				// Stop (free) synth of selected SynthModel
				Button().states_([["stop"]])
				.action_({ modelSwitcher.notifier !? { modelSwitcher.notifier.free } })
				.addNotifierSwitch(modelSwitcher, \synthStarted,
					{ | notification |
						notification.listener.enabled_(true);
					}, { | model, view |
						if (model.isNil) {
							view.enabled_(false);
						}{
							view.enabled_(model.isPlaying);
						};
						model
					}
				)
				.addNotifierSwitch(modelSwitcher, \synthEnded, { | notification |
					notification.listener.enabled_(false);
				}),

				// Pause or resume synth of selected SynthModel
				Button().states_([["resume"], ["pause"]])
				.addNotifierSwitch(modelSwitcher, \synthStarted, { | notification |
					notification.listener.enabled_(true).value_(1);
					}, { | model, view |
						if (model.isNil) {
							view.value = 0;
							view.enabled_(false);
						}{
							view.enabled = if (model.isPlaying) { true } { false };
							view.value = model.isRunning.binaryValue;
						};
						model
				})
				.addNotifierSwitch(modelSwitcher, \synthEnded, { | notification |
					notification.listener.value_(0).enabled_(false);
				})
				.action_({ | me |
					modelSwitcher.notifier.run(me.value);
				}),

				StaticText().string_("amplitude:"),

				// Set Amplitude of selected SynthModel
				Slider()
				.orientation_(\horizontal)
				.maxHeight_(20)
				.addNotifierSwitch(modelSwitcher, \amp, { | val, notification |
					notification.listener.value = ampSpec.unmap(val ? 0);
					}, { | model, view |
						if (model.isNil) {
							view.value = 0;
							view.enabled = false;
							nil
						}{
							view.enabled = true;
							view.value = ampSpec.unmap(model.eventModel.at(\amp) ? 0);
							model.eventModel;
						};
					}
				)
				.addNotifier(this, \spec, { | max, notification |
					notification.listener.clipHi = max;
				})
				.action_({ | me |
					modelSwitcher.notifier !? {
						modelSwitcher.notifier.eventModel.put(\amp, ampSpec.map(me.value))
					}
				}),


				// Display / Set Amplitude of selected SynthModel
				NumberBox()
				.maxHeight_(20).clipLo_(0).clipHi_(10).decimals_(5).enabled_(false)
				.addNotifierSwitch(modelSwitcher, \amp, { | val, notification |
					notification.listener.value = val;
					}, { | synthModel, theNumBox |
						if (synthModel.isNil) {
							theNumBox.enabled = false;
							theNumBox.value = 0;
							nil
						} {
							theNumBox.enabled = true;
							theNumBox.value = synthModel.eventModel.at(\amp) ? 0;
							synthModel.eventModel;
						};
					}
				)
				.addNotifier(this, \spec, { | max, notification |
					notification.listener.clipHi = max;
				})
				.action_({ | me |
					modelSwitcher.notifier !? {
						modelSwitcher.notifier.eventModel.put(\amp, me.value)
					}
				}),

				// Set maximum amplitude limit for Slider and Number Box.
				HLayout(
					StaticText().string_("max amp:").font_(Font.default.size_(10)).fixedWidth_(50),
					NumberBox().clipLo_(0.001).decimals_(3).fixedWidth_(35)
					.font_(Font.default.size_(10)).value_(ampSpec.maxval)
					.action_({ | me | this.makeSpec(me.value); })
			)]
		);
		window.onClose = {
			this.objectClosed;
			modelSwitcher.objectClosed;
		};
		list.updateSynthDefs;
		list.changed(\synthState, nil); // update SynthModel list colors
		window.front;
	}

	colorSynthList { | listView |
		listView.colors = list.list collect: { | sm |
			if (sm.isPlaying) {
				if (sm.isRunning) { runningColor } { pausedColor };
			}{
				stoppedColor
			}
		};
		listView.refresh;
	}

	makeSpec { | max = 2 |
		ampSpec = ControlSpec(0.0, max, \amp, 0, 0);
		this.changed(\spec, max);
	}
}