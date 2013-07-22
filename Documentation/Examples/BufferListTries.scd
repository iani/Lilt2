

\test.addNotifier(BufferList(), 'allBuffersLoaded', { | buflist |
	"Following is a list of buffers loaded".postln;
	buflist.postln;
});
{ | i | BufferList().read(format("b%", i), "/Users/iani2/Music/sounds/1307SyrosSounds/02SoundsSyrosDanae/bells and kitchen 1.wav") } ! 10;

SynthModel({ | buf = 0 | Out.ar(0, PlayBuf.ar(1, buf)) }).gui