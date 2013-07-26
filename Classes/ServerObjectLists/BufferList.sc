/* IZ Jul 18, 2013 (2:29 AM)

Zillionth attempt at a practical Buffer manager

*/


BufferList {

	var <server;  // The server to which these buffers belong
	var <buffers; // Dictionary of buffers stored under names (symbols) for named access
	var <path;    // Path where this list is saved (or loaded from).
	var <loadingBuffers; // Set of buffer specs that are waiting to be loaded sequentially
	var <nextBufSpecs;   // the specs for the next buffer to be loaded
	var <bufferLoadedAction; // called by a new buffer's completionAction/Message
	var <bufferNames, <bufferNameIndexByBufnum;
	var <spec;

	*new { | server |
		var new;
		server = server.asTarget.server;
		new = Library.global.at('Buffers', server);
		if (new.isNil) {
			new = this.newCopyArgs(server, IdentityDictionary(), nil, Set()).init;
			ServerBoot.add({ new.loadAll }, server);
			Library.global.put('Buffers', server, new);
		};
		^new;
	}

	init {
		bufferLoadedAction = { | buffer | this.bufferLoaded(buffer) };
		bufferNames = [];
		bufferNameIndexByBufnum = IdentityDictionary();
		spec = ControlSpec(0, 1, \lin, 1);
	}

	bufferLoaded { | buffer |
		if (this.buffersLoadingDone) { // Note: This test here may be superfluous
		}{
			this.makeNextBuffer;
		}
	}

	buffersLoadingDone { ^nextBufSpecs.size == 0 }

	makeNextBuffer {
		var name, specs;
		nextBufSpecs = loadingBuffers.pop;
		if (nextBufSpecs.notNil) {
			#name ... specs = nextBufSpecs;
			buffers[name.asSymbol] = Buffer.performList(*specs);
		}{
			this.updateBufferList;
		}
	}

	updateBufferList {
		bufferNames = buffers.keys.asArray.sort;
		bufferNameIndexByBufnum = IdentityDictionary(buffers.size);
		buffers keysValuesDo: { | name, buffer |
			bufferNameIndexByBufnum[buffer.bufnum] = bufferNames indexOf: name;
		};
		spec = ControlSpec(0, buffers.size - 1, \lin, 1);
		{ this.changed(\bufferList, bufferNames, spec) }.defer(0);
	}

	nameIndex { | bufnum = 0 | ^bufferNameIndexByBufnum[bufnum.asInteger] ? 0 }

	loadAll {
		buffers keysValuesDo: { | name, buffer |
			if (buffer.path.isNil) {
				this.alloc(name, buffer.numFrames, buffer.numChannels);
			}{
				this.read(name, buffer.path);
			}
		};
	}

	alloc { | name, numFrames = 1024, numChannels = 1 |
		loadingBuffers add: [name, \alloc, server, numFrames, numChannels, bufferLoadedAction];
		if (this.buffersLoadingDone) { this.makeNextBuffer };
	}

	read { | name, path, startFrame = 0, numFrames = -1 |
		loadingBuffers add: [
			name, \read, server, path, startFrame, numFrames, bufferLoadedAction];
		if (this.buffersLoadingDone) { this.makeNextBuffer };
	}

	free { | name |
		var buf;
		buf = buffers[name];
		buf ?! {
			buf.free;
			buffers[name] = nil
		};
		this.changed(\freebuf, name);
		this.updateBufferList;
	}

	save { | argPath |
		path = argPath ?? { this.defaultPath };
		buffers.getPairs.clump(2).collect(this.bufferSaveData(*_)).writeArchive(path);
	}

	defaultPath { ^Platform.userAppSupportDir +/+ "Buffers.sctxar"; }

	bufferSaveData { | name, buffer |
		buffer.path !? { ^[name, buffer.path] };
		^[name, buffer.numFrames, buffer.numChannels];
	}

	*load { | path, server |
		this.new(server).load(path);
	}

	load { | argPath |
		var buflist;
		argPath ?? { argPath = this.defaultPath };
		path ?? { path = argPath };
		buflist = Object.readArchive(argPath);
		buflist do: { | b |
			if (b[1].isKindOf(String)) {
				this.read(*b);
			}{
				this.alloc(*b);
			}
		}
	}



}

