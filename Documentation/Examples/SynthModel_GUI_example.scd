/* IZ Jul 18, 2013 (2:12 AM)

Example of SynthModel gui generation with custom specs.

Tgrain example adapted from Thanassis Ikonomidis.

*/

b = Buffer.alloc(Server.default, Server.default.sampleRate * 4, 1); // 4 sec buffer allocation

(
SynthDef('simple_tgrain', { | bufnum = 0, trigRate = 10, pitchshift = 1, graindur = 0.1, pan = 0, amp = 1 |
	var frames;
	var recpos;
	var input;
	var playpos;
	var recorder;
	var trig;
	var output;

	frames = BufFrames.kr(bufnum);

	input = SoundIn.ar(0, 2);
	recpos = Phasor.ar(1, 1, 0, frames, 0);
	recorder = BufWr.ar(input, bufnum, recpos);

	trig = Impulse.kr(trigRate);
	playpos = Wrap.ar(recpos - (graindur * BufRateScale.kr(bufnum) *
		pitchshift * 0.5), 0, frames - 1);
	output = TGrains.ar(2, trig, bufnum, BufRateScale.kr(bufnum) * pitchshift,
		playpos / BufSampleRate.kr(bufnum), graindur, pan, amp, 4);

	Out.ar(0, output);

}).synthGui(specs: [
	bufnum: ControlSpec(0, 1024, \lin, 1, b.bufnum),
	trigRate: [0.1, 50],
	pitchshift: [-50, 50],
	graindur: [0.05, 2]
])
)
