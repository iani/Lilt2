/* IZ Jul 18, 2013 (2:29 AM)

Zillionth attempt at a practical Buffer manager

*/


BufferList {

	var <server;
	var <buffers;
	var <path;

	*new { | server |
		var new;
		server = server.asTarget.server;
		new = Library.global.at('Buffers', server);
		if (new.isNil) {
			new = this.newCopyArgs(server, IdentityDictionary());
			ServerBoot.add({ new.loadAll }, server);
			Library.global.put('Buffers', server, new);
		};
		^new;
	}

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
			this.changed(name, buf);
		});
		buffers[name] = buffer;
		^buffer;
	}

	read { | name, path |
		var buffer;
		buffer = Buffer.read(server, path, action: { | buf |
			this.changed(name, buf);
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
		this.changed(name);
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

