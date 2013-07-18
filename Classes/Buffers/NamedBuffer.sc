/*
TODO:
- Buffers are loaded on demand - whenever a buffer is assigned to an input of a SynthModel.
- NamedBuffer's buffer var is set to nil whenever its server quits.

*/


NamedBuffer {
	var <path, <name, <buffer;
}
