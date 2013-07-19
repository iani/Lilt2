/* IZ Jul 18, 2013 (2:29 AM)

Zillionth attempt at a practical Buffer manager

*/


BufferList {

	var <server;  // The server to which these buffers belong
	var <buffers; // Dictionary of buffers stored under names (symbols) for named access
	var <path;    // Path where this list is saved (or loaded from).
	var <loadingBuffers; // Set of buffers that are waiting to be loaded sequentially
	var <nextBufSpecs;   // the specs for the next buffer to be loaded
	var <bufferLoadedAction; // called by a new buffer's completionAction/Message

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
	}

	bufferLoaded { | buffer |
		this.changed(\bufferLoaded, buffer);
		if (this.buffersLoadingDone) {

		}{

		}
	}

	buffersLoadingDone { ^nextBufSpecs.size == 0 }

	loadAll {
		buffers keysValuesDo: { | name, buffer |
			if (buffer.path.isNil) {
				this.alloc(name, buffer.numFrames, buffer.numChannels);
			}{
				this.read(name, buffer.path);
			}
		};
	}

	alloc { | name, numFrames, numChannels |
		var buffer;
		buffer = Buffer.alloc(server, numFrames, numChannels, { | buf |
			this.makeNextBuffer;
		});
		buffers[name] = buffer;
		^buffer;
	}

	loadNewBuffer { | name, allocOrRead, specs |

	}

	makeNextBuffer {
		nextBufSpecs = loadingBuffers.pop;
		nextBufSpecs !? {
		//	this.
		}

	}

	read { | name, path |
		var buffer;
		buffer = Buffer.read(server, path, action: { | buf |
			this.changed(\newbuf, name, buf);
		});
		buffers[name] = buffer;
		^buffer;
	}

	free { | name |
		var buf;
		buf = buffers[name];
		buf ?! {
			buf.free;
			buffers[name] = nil
		};
		this.changed(\freebuf, name);
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

