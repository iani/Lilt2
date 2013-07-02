/*
AbstractPatcher: holds patches by name and manages creation of links between patches.

ControlPatcher also holds the server to which it belongs.
AudioPatcher lso

Patch: holds a SynthModel and its links.

Link: holds input and output processes and bus as well as signal copying link if needed.
*/

// ================== Patcher ====================

AbstractPatcher {
	var <nodes; // dict holding nodes by name. The types of nodes depend on subclass.
	*add { | name, template, server |
		this.for(server).add(name, template);
	}

	*for {
		// ....
	}

	*new { | server |
		^this.newCopyArgs(IdentityDictionary(), server.asTarget.server).init;
	}

	init {}

	*connect { | output, input, parameter = \in |
		/* output, input: names of ... */
	}

	add { | name, template |
		var node;
		node = this.makePatch(template, name);
		// TODO: check if node already exists under same name
		nodes[name] = node;
	}
}

ControlPatcher : AbstractPatcher {
	var <server;

	makePatch { | template, name |

	}
}

AudioPatcher : ControlPatcher {
	var <groups;
}


StreamPatcher : AbstractPatcher {
	classvar <streams; // holds all streams. Is global to all servers.

	*initClass {
		streams = this.new;
	}

	*for { ^streams }
}


// ================== Patch ====================

AbstractPatch {

}

ControlPatch : AbstractPatch {

}

AudioPatch : ControlPatch {

}

StreamPatch : AbstractPatch {

}

// ================== Link ====================


AbstractLink {

}

AudioLink : AbstractLink {

}

ControlLink : AudioLink {


}


StreamLink : AbstractLink {

}
