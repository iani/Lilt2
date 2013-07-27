
SynthDef("4ever", { |
	out = 0, buf = 0, rate = 1, startPos = 0, loop = 1,
	attack = 0.01, release = 1,
	gate = 1, trigger = 0,       // envelope release + loop trigger
	// processing parameters:
	magabove = 0, magaboveamp = 0,
	magbelow = 1, magbelowamp = 0,
	numteeth = 0, phase = 0, width = 0.5, combamp = 0,
	stretch = 1, shift = 0, shiftamp = 0,
	// panning and amplitude
	pos = 0, amp = 1 |

	var env, envgen, source;
	var fftsrc, magabovesrc, magbelowsrc, combsrc, shiftsrc;
	var fftsrcmagabove, fftsrcmagbelow, fftsrcshift, fftsrccomb;

	env = Env.asr(attack, 1, 3);
	envgen = EnvGen.kr(env, gate, amp, doneAction: 2);
	source = envgen *
	PlayBuf.ar(buf.numChannels, buf,
		rate * BufRateScale.kr(buf),
		trigger: trigger,
		startPos: startPos * BufFrames.kr(buf),
		loop: loop,
		doneAction: 2
	);
	fftsrcmagabove = FFT(LocalBuf(2048), source);
	fftsrcmagbelow = FFT(LocalBuf(2048), source);
	fftsrcshift = FFT(LocalBuf(2048), source);
	fftsrccomb = FFT(LocalBuf(2048), source);
	magabovesrc = PV_MagAbove(fftsrcmagabove, magabove);
	magbelowsrc = PV_MagBelow(fftsrcmagbelow, magbelow);
	shiftsrc = PV_MagShift(fftsrcshift, stretch, shift, 1);
	combsrc = PV_RectComb(fftsrccomb, numteeth, phase, width);
	Out.ar(out, Pan2.ar(
		(IFFT(magabovesrc) * magaboveamp)
		+ (IFFT(magbelowsrc) * magbelowamp)
		+ (IFFT(combsrc) * combamp)
		+ (IFFT(shiftsrc) * shiftamp)
		, pos, amp
	));
}).add;

SynthDef("vibsin", { | freq = 400, vfreq = 5, vwidth = 1.066, amp = 0.1, gate = 1,
	fadeTime = 0.2, out = 0 |
	var vs, env;
	env = Env([0, 1, 0], [0.1, 1], releaseNode: 1);
	vs = SinOsc.ar(SinOsc.kr(vfreq, 0, freq * vwidth - freq, freq), 0, amp);
	Out.ar(out, Pan2.ar(vs, LFNoise1.kr(0.1).range(-1, 1))
		* EnvGen.kr(env, gate, 1, 0, fadeTime, 2))
}).add;

