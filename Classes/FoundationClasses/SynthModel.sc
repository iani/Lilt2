/* IZ Jul 26, 2013 (6:12 PM)

Create gui for playing synths, by extracting controls from their synthdefs.

*/

SynthModel {
	var <template;   // a SynthDef name, SynthDef, or Function from which the synths are created
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
	var completionMsg;   /* Synth creation message, if starting synth immediately upon
	            SynthDef creation */
	var <name;

	classvar >font; // font for gui elements

	*initClass {
		Class.initClassTree(ControlSpec);
		Spec.specs.addAll([
			\fadeTime -> ControlSpec(0.01, 30),
			\out -> ControlSpec(0, 127, \lin, step: 1, default: 0),
		]);
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
			completionMsg = [];
			if (template.isKindOf(Function)) {
				synthDef = template.asFlexSynthDef;
			}{
				synthDef = template;
			};
			// If start is sent immediately, then it makes a new synth in completionMsg
			{
				synthDef.add(completionMsg: completionMsg);
				completionMsg = nil;
			}.defer(0.01);
			defName = synthDef.name.asSymbol;
			template = defName;
			synthDesc = synthDef.asSynthDesc;
		};
		if (eventModel.isKindOf(Event)) {
			eventModel = EventModel(eventModel)
		};
		specs !? { eventModel.addSpecs(specs); };
		this.makeControls;
		synthArray = [];
		name = format("%-%", defName, UniqueID.next - 1000);
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

	put { | key, value |
		eventModel.put(key, value);
	}

	at { | key | ^eventModel.at(key) }

	event { ^eventModel.event }

	toggle {
		if (this.hasSynth) { this.release } { this.start }
	}

	isPlaying { ^this.hasSynth }
	hasSynth { ^synthArray.size > 0 }

	isRunning { ^this.isPlaying and: { synthArray.first.isRunning } }

	start { | allowManySynths = false |
		if (allowManySynths.not and: { this.hasSynth }) {
			^"SynthModel already running. To add more synths, use .start(true)".postln;
		};
		if (target.server.serverRunning) {
			this.startSynth;
		}{
			target.server.waitForBoot({ this.startSynth });
		};
	}

	startSynth {
		var synth;
		if (completionMsg.notNil) {
			synth = Synth.basicNew(defName, this.server);
			completionMsg = synth.newMsg(target, this.getArgs, addAction)
		}{
			synth = Synth(defName,
				args: this.getArgs,
				target: target,
				addAction: addAction
			);
		};
		synthArray = synthArray add: synth;
		synth onEnd: { | theSynth |
			synthArray remove: theSynth;
			if (this.hasSynth.not) { { this.changed(\synthEnded); }.defer(0) };
		};
		synth.isRunning = true;
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

	resume { this.run(1) }
	pause { this.run(0) }
	run { | flag = 0 |
		var isRunning;
		isRunning = [false, true][flag];
		synthArray do: { | s | s.run(flag); s.isRunning = isRunning };
		this.changed(\run, flag);
	}

	gui { | argKeys |
		var rows, layout;
		rows = this.makeControlsGui(argKeys);
		layout = GridLayout.rows(
			[],
			*rows
		).addSpanning(this.makeStateControls, 0, 0, 1, 3)
		^Window(name, Rect(400, 400, 400, rows.size + 1 * 20 + 10)).front.view.layout = layout;
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
				[{ this.release(eventModel.event[\releaseTime]) }, { this.start(true) }]
				[me.value].value
			}).font_(this.font).value_(this.hasSynth.binaryValue),
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

	// Display im lists and other GUIs
	server { ^target.asTarget.server }

	name_ { | argName | name = argName; this.changed(\name, argName); }

	add { SynthList.default add: this }

	// Linking to other SynthModels

	controlOutputs {
		^connectors select: { | c | c.isKindOf(OutputConnector) and: { c.rate === \control } }
	}

	controlInputs { ^connectors; }

	audioOutputs {
		^connectors select: { | c | (c.class === OutputConnector) and: { c.rate === \audio } }
	}

	audioInputs {
		^connectors select: { | c | (c.class === InputConnector) and: { c.rate === \audio } }
	}

	audioBusses {
		^connectors.collect(_.audioBusses).flat;
	}
	controlBusses {
		^connectors.collect(_.controlBusses).flat;
	}
}