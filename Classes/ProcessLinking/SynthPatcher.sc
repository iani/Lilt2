 /* IZ Jul 9, 2013 (9:01 PM)
Redesign of Pathers currently in "consideration" phase.

*/

// ================== Patcher ====================

AbstractPatcher {
	var <nodes; // dict holding nodes by name. The types of nodes depend on subclass.

	*new { | server |
		^this.newCopyArgs(IdentityDictionary()).init(server);
	}

	init {}

	*connect { | output, input, parameter = \in |
		/* output, input: names of ... */
	}

	add { | name, template |
		var node;
		node = this.makePatch(name, template);
		// TODO: check if node already exists under same name
		nodes[name] = node;
	}
}

SynthPatcher : AbstractPatcher {
	var <server;

	*add { | name, template, server |
		^this.for(server).add(name, template);
	}

	*for { | server |
		var patcher;
		server = server.asTarget.server;
		patcher = Library.global.at(this, server);
		if (patcher.isNil) {
			patcher = this.new(server);
			Library.put(this, server, patcher);
		};
		^patcher;
	}

	init { | argServer |
		server = argServer.asTarget.server;
	}

	makePatch { | name, template |
		var patch;
		patch = ControlPatch(name, template);
		nodes[name] = patch;
		^patch;
	}
}

