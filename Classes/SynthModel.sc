
SynthModel {
	var <template; // a SynthDef name, SynthDef, or Function from which the synths are created
	var <eventModel; // an EventModel containing keys and parameter values for synth controls
	var <target;     // Target to which synths are added
	var <addAction = \addToHead; // Where to add the synths relative to the target
	var <defName;    // The name of the SynthDef that creates synths for this model.
	var <synthDef;
	var <synthDesc;  // Derived from the SynthDef. Used by SynthPatch to create inputs and outputs
	var <connectors; // Array of ControlConnectors handling mapping, bus patching, buffer setting
	var <synthArray; /* Array of synths created by this model.
	Several synths can be running, because one may start a new synth while
	the previous has been released but not yet freed. Communication is kept
	alive for all synths created by me, until they are freed. */

	classvar >font; // font for gui elements

	*initClass {
		Class.initClassTree(ControlSpec);
		Spec.specs.addAll([
			\fadeTime -> ControlSpec(0.01, 30),
			\out -> ControlSpec(0, 127, \lin, step: 1, default: 0),
		])
	}

	*new { | template, eventModel, target, addAction = \addToHead, specs |
		^this.newCopyArgs(
			template, (eventModel ?? { () }),
			target.asTarget, addAction
		).init(specs);
	}

	init { | specs |
		if (template.isKindOf(Symbol) or: { template.isKindOf(String) }) {
			defName = template.asSymbol;
			synthDesc = SynthDescLib.global.at(defName);
		}{
			if (template.isKindOf(Function)) {
				synthDef = template.asFlexSynthDef;
			}{
				synthDef = template;
			};
			synthDef.add;
			defName = synthDef.name.asSymbol;
			synthDesc = synthDef.asSynthDesc;
		};
		if (eventModel.isKindOf(Event)) {
			eventModel = EventModel(eventModel)
		};
		eventModel.addSpecs(specs);
		this.makeControls;
		synthArray = [];
	}

	makeControls {
		var event, controls, inputs, outputs, control, descriptor;
		event = eventModel.event;
		controls = synthDesc.controls;
		inputs = synthDesc.inputs;
		outputs = synthDesc.outputs;
		controls.collect({ | cn | cn.name.asString; }) do: { | nameString, i |
			if (nameString[..1] != "i_" and: { nameString != "gate" }) {
				var nameSymbol;
				control = controls[i];
				nameSymbol = control.name;
				event[nameSymbol] = control.defaultValue;
				this.addNotifier(event, nameSymbol, { | value | this.set(nameSymbol, value) });
				case
				{ nameString[..2] == "buf" } {
					connectors = connectors add: BufferConnector(this, control);
				}
				{ (descriptor = inputs.detect({ | i | i.startingChannel === nameSymbol })).notNil } {
					connectors = connectors add: InputConnector(this, control, descriptor)
				}
				{ (descriptor = outputs.detect({ | i | i.startingChannel === nameSymbol })).notNil } {
					connectors = connectors add: OutputConnector(this, control, descriptor)
				}
				{ 	connectors = connectors add: ControlConnector(this, control) }
			};
		};
//		event keysDo: { | key | this.addNotifier(event, key, { | value | this.set(key, value) }); };
	}

	set { | key, value |
		synthArray do: _.set(key, value);
	}

	toggle {
		if (this.hasSynth) { this.release } { this.start }
	}

	hasSynth { ^synthArray.size > 0 }

	start {
		if (target.server.serverRunning) {
			this.startSynth;
		}{
			target.server.waitForBoot({ this.startSynth });
		};
	}

	startSynth {
		var synth;
		synth = Synth(defName,
			args: this.getArgs,
			target: target,
//			addAction: addAction
		);
		synthArray = synthArray add: synth;
		NodeWatcher.register(synth);
		this.addNotifier(synth, \n_end, { | notification |
			notification.notifier.objectClosed;
			synthArray remove: notification.notifier;
			if (this.hasSynth.not) { { this.changed(\synthEnded); }.defer(0) };
		});
		this.changed(\synthStarted);
	}

	getArgs { | keys |
		^eventModel.event.getPairs(keys);
	}

	stop { this.free; }

	free { synthArray do: _.free }

	release { | fadeTime |
		synthArray do: _.release(fadeTime);
		this.changed(\release);
	}

	run { | flag = 0 |
		synthArray do: _.run(flag);
		this.changed(\run, flag);
	}

	gui { | argKeys |
		// basic gui - under development
		var rows, layout;
		rows = this.makeControlsGui(argKeys);
		// rows[0] = rows[0] add: [ListView().minWidth_(120), rows: rows.size];
		layout = GridLayout.rows(
			[],
			*rows
		).addSpanning(this.makeStateControls, 0, 0, 1, 3)
		// .addSpanning(this.makeOutputControls, 0, 3, 1, 1);
		^Window(defName, Rect(400, 400, 400, rows.size + 1 * 20 + 10)).front.view.layout = layout;
	}

	makeControlsGui { ^connectors collect: _.makeGui; }

	makeStateControls { | argKeys |
		^HLayout(
			this.addView(Button,
				\synthEnded, { | n |
					n.listener.value = 0;
				},
				\synthStarted, { | n |
					n.listener.value = 1;
				}
			)
			.states_([["start"], ["fade out"]]).action_({ | me |
				[{ this.release(eventModel.event[\releaseTime]) }, { this.start }][me.value].value
			}).font_(this.font), // .fixedWidth_(100)
			this.addView(Button,
				\synthEnded, { | n | n.listener.enabled = 0 },
				\synthStarted, { | n | n.listener.enabled = 1 }
			).font_(this.font)
			.states_([["stop"]]).action_({ this.free })
			.enabled_(this.hasSynth),
			this.addView(Button,
				\synthEnded, { | n | n.listener.value_(0).enabled = 0 },
				\synthStarted, { | n | n.listener.value_(0).enabled = 1 },
				\run, { | val, n | n.listener.value_(1 - val) }
			).font_(this.font)
			.states_([["pause"], ["resume"]]).action_({ | me | this.run(1 - me.value); })
			.value_(this.hasSynth.and({ synthArray[0].isRunning.not }).binaryValue)
			.enabled_(this.hasSynth)
		)
	}
	font {
		font ?? { font = Font.default.size_(10) };
		^font;
	}

	addView { | viewClass ... messagesActions |
		var view;
		view = viewClass.new;
		view.onClose = { view.objectClosed };
		messagesActions.pairsDo({ | message, action |
			view.addNotifier(this, message, action)
		})
		^view;
	}

	// ========== Patching
	audioPatch { | name |
		^AudioPatch(name ?? { defName.asSymbol }, this);
	}

}