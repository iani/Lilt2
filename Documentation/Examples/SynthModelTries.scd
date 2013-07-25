
SynthList.gui;

a = { WhiteNoise.ar(0.1) }.l;

a.addDependant({ | ... args | args.postln });

a.gui;

(
a = SynthModel({ | test = 0, test2 = 10 | WhiteNoise.ar(0.1) });
a.gui;
)

(
a = SynthModel({ WhiteNoise.ar(0.1) });
a.gui;
)

a.synthArray.first.isPlaying

(
a = SynthModel('default');
a.gui;
)

a.isRunning
a.isPlaying

a.synthArray.first.inspect;

a.stop;

a.start(true);

a.release(5);
a.release;
a.stop;
a.free;

a.gui([\amp]);
a.start;
a.stop;

a.set(\amp, 0.15)
a.release(5);

{ XFade(WhiteNoise.ar(0.1)) }.play;

a.eventModel;
a.eventModel.event;
a.makeEvent;


a.release;
a.release(9.1);




nil.asTarget.server.serverRunning;

a = { XFade(WhiteNoise.ar(0.1)) }.asSynthDef;

a.add;

a.inspect;

Notification.all;


a.synth.release

View

Synth('temp__2');


SynthDescLib.global.browse;

(
{
	var a;
	12 do: { | i |
		a = Synth(format("temp__%", i + 12)).postln;
		5.wait;
		a.release(1);
	}
}.fork
)