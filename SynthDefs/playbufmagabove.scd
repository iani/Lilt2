

(
SynthDef("playbufmagabove", { | out = 0, buf = 0, rate = 1, startPos = 0, loop = 1,
	attack = 0.01, release = 1,
	gate = 1, amp = 1,
	magabove = 0, magabovelag = 0,
	pos = 0, width = 2, trigger = 0, binshift_amp = 0, stretch = 1, shift = 0 |
	var env, envgen, source, chain, shiftchain, output;
	env = Env.asr(attack, 1, 3);

	envgen = EnvGen.kr(env, gate, 1, doneAction: 2);
	source = envgen *
	PlayBuf.ar(buf.numChannels, buf,
		rate * BufRateScale.kr(buf),
		trigger: trigger,
		startPos: startPos * BufFrames.kr(buf),
		loop: loop,
		doneAction: 2
	);
	chain = FFT(LocalBuf(2048), source);
	chain = PV_MagAbove(chain, Lag.kr(magabove, magabovelag));
	shiftchain = PV_BinShift(FFT(LocalBuf(2048), source), stretch, shift, 0);
	output = (IFFT(chain) * amp * 5) + (IFFT(shiftchain) * binshift_amp * 5);
	Out.ar(out, Pan2.ar(output, pos));
}).add;
)