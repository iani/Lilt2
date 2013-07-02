
SynthModel {
	var <template, <eventModel, <keys;
	var <target, <addAction = \addToHead;
	var <defName, <synthArray;
	var >font;

	*initClass {
		Class.initClassTree(ControlSpec);
		Spec.specs.addAll([
			\fadeTime -> ControlSpec(0.01, 30),
			\out -> ControlSpec(0, 127, \lin, step: 1, default: 0),
		])
	}

	*new { | template, eventModel, keys, target, addAction = \addToHead |
		^this.newCopyArgs(template, eventModel ?? { () }, keys, target.asTarget, addAction).init;
	}

	init {
		if (template.isKindOf(Symbol) or: { template.isKindOf(String) }) {
			defName = template;
		}{
			this.addSynthDef(template);
		};
		if (eventModel.isKindOf(Event)) {
			eventModel = EventModel(eventModel)
		};
		if (eventModel.event.size == 0) { this.makeEvent } { this.connectKeys; };
		synthArray = [];
	}

	addSynthDef { | synthDef |
		if (synthDef.isKindOf(Function)) {
			synthDef = synthDef.asFlexSynthDef(name: SystemSynthDefs.generateTempName);
		};
		synthDef.add;
		defName = synthDef.name.asSymbol;
	}

	makeEvent {
		var event, name;
		event = eventModel.event;
		this.getControlsFromSynthDesc do: { | c | event[c.name] = c.defaultValue };
		this.connectKeys;
	}

	getControlsFromSynthDesc {
		var desc, name;
		desc = SynthDescLib.global[defName];
		if (desc.notNil) {
			^desc.controls select: { | c |
				name = c.name.asString;
				name[..1] != "i_" and: { name != "gate" }
			}
		}{ ^nil };
	}

	connectKeys {
		var event;
		event = eventModel.event;
		(keys ?? { event.keys }) do: { | key |
			this.addNotifier(event, key, { | value | this.set(key, value) })
		}
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
			addAction: addAction
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

	getArgs {
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
		argKeys ?? { argKeys = keys ?? { eventModel.event.keys.asArray.sort } };
		rows = this.makeParameterControls(argKeys);
		rows[0] = rows[0] add: [ListView().minWidth_(120), rows: rows.size];
		layout = GridLayout.rows(
			[],
			*rows
		).addSpanning(this.makeStateControls, 0, 0, 1, 3)
		.addSpanning(this.makeOutputControls, 0, 3, 1, 1);
		^Window(defName, Rect(400, 400, 400, rows.size + 1 * 20 + 10)).front.view.layout = layout;
	}

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

	makeOutputControls {
		^HLayout(
			StaticText().string_("outputs").font_(this.font),
			StaticText().string_("group (0+-8)").font_(Font.default.size_(9)).maxWidth_(50),
			NumberBox().action_({ | me | this.setGroup(me.value) })
			.decimals_(0).step_(1).clipLo_(-8).clipHi_(8).fixedWidth_(25).font_(this.font),
		)
	}

	font {
		font ?? { font = Font.default.size_(10) };
		^font;
	}

	makeParameterControls { | argKeys |
		^argKeys collect: { | key |
			eventModel.numSlider(key, decoratorFunc: { | argKey, argView |
				[
					StaticText().string_(argKey).font_(this.font),
					argView.orientation_(\horizontal).maxHeight_(20),
					eventModel.numberBox(key).fixedWidth_(50).font_(this.font)
				];
			})
		}
	}

	makeOutputPane {
		^ListView().fixedWidth_(150).minHeight_(50)
	}

	setGroup { "not yet implemented".postln; }

	addView { | viewClass ... messagesActions |
		var view;
		view = viewClass.new;
		view.onClose = { view.objectClosed };
		messagesActions.pairsDo({ | message, action |
			view.addNotifier(this, message, action)
		})
		^view;
	}
}