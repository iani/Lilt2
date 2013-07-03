/* IZ Jul 3, 2013 (11:36 AM)

Classes for linking between Synths (audio or control rate) and between routines that change parameters of synths or write to events.

The linking between Synth instances requires several things:
1. Create a bus where one or "writer" synths write their output,
   and one or more "reader" synths read that signal as their input
2. In the case of many-to-many interconnections between readers and writers
   the signal of an output as to be copied to a separate
   bus.
3. Move synths so that they are in the correct audio-computation-graph order.
   each "writer" synth must be placed before any "reader" synths that accept
   its input.

For 1 and 2 we are going to re-use the mechanism devised in 2007, and copied here (classes Parameter, LinkedBus, BusLink etc). See documentation in: Documentation/Connecting Scripts (Implementation Notes).pdf

For 3 we are going to use graph traversal. The classes used for 1 and 2 also provide an algorithm that checks for cycles in the link graph. So before adding any This means that the system will check before adding a link and will not permit to add any links which would create a cycle in the graph. So graph traversal will be safe.

1. When adding a reader to a writer:
Move the reader

AbstractPatcher: holds patches by name and manages creation of links between patches.

ControlPatcher also holds the server to which it belongs.
AudioPatcher also list of groups for putting the patches in the correct node order.

Patch: holds a SynthModel and its links.

Link: holds input and output processes and bus as well as signal copying link if needed.
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

ControlPatcher : AbstractPatcher {
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

AudioPatcher : ControlPatcher {
	classvar <maxGroups = 16;
	var <groups;

	init { | argServer |
		super.init(argServer);
		groups = List().add(Group(server));
		ServerBoot.add({ this.makeGroups });
	}

	makeGroups {
		groups = List().addAll({ Group(server) } ! groups.size);
		nodes do: _.setGroup(this);
	}

	growGroups { | level = 0 |
		while { groups.size - 1 < level } { groups.add(Group(server)) };
	}
}


StreamPatcher : AbstractPatcher {
	classvar <streams; // holds all StreamPatches. Is global to all servers.

	*initClass {
		streams = this.new;
	}

	*add { | name, template, server |
		^streams.add(name, template);
	}

	*for { ^streams }
}


// ================== Patch ====================

AbstractPatch {
	var <name;    // name under which patch is stored. Also used for gui
	var <process; /* the process that is being patched to inputs and outputs
	can be a SynthModel (or StreamModel : will this class be defined later ? */
	var <inputs, <outputs; /* inputs and ouputs
	Inputs: Dict of inputs by name of parameter.
	Each input is a dictionary of Links, each stored by the name of the patch sending
	its output to this patch
	Outputs: A dictionary of of Links, each stored by the name of the patch receiving
	the output of this patch.
	*/

	*new { | name, process |
		^this.newCopyArgs(name, process).init;
	}

	init {
		this.patcherClass.add(name, this);
		inputs = IdentityDictionary();
		outputs = IdentityDictionary();
	}

	addInput { | paramName, patch |
		/* make a link from the output of patch
		to parameter named paramName of yourself */
	}

	addOutput { | paramName, patch |
		/* make a link from the output of yourself
		to input parameter named paramName of patch */
	}
}

ControlPatch : AbstractPatch {

	patcherClass { ^ControlPatcher }

	/* Rule for adding inputs to ControlPatch/AudioPatch:
	An input bus must receive only the inputs intended for it.

	For a given link l, it must be checked that:
	For each of its outputs o, the inputs defined to be received by o
	are exactly those that write to that link l, not more not less.

	Therefore:

	For a given link from output o1 to input i1:
	If i1 receives from an output o2 which also sends to another input i2,
	and that input i2 does not (want to) receive input from o1,
	then make a copy of the signal from the bus linking o2 to i2,
	and send that signal to the bus linking o1 to i1.
	So i1 will continue to receive the signal from o2,
	but i2 will only receive from o2.

	Attempt at an algorithm formulation:

	Starting point: Request made to connect output o to input i.
	Steps:
	find the



	*/
}

AudioPatch : ControlPatch {
	var <groupLevel = 0;

	patcherClass { ^AudioPatcher }

	setGroup { | patcher |
		patcher.growGroups(groupLevel);
		process.setTarget(patcher.groups[groupLevel]);
	}
}

StreamPatch : AbstractPatch {

	patcherClass { ^StreamPatcher }

}


Output {
	var <process;
	var <readers;
}

Input {
	var <process;
	var <writers;
}


// ================== Link ====================


AbstractLink {
	/* Note: many processes can write to and read from the same bus
	However, it must be guaranteed that the bus only receives
	input from those processes that are required by its outputs.
	For this, a rule/algorithm is described in ControlPatch above.
	*/
	var <inputs, <outputs;
}

ControlLink : AbstractLink {
	var <bus;  //
	var <linkSynth;  // synth copying input to bus

}

AudioLink : ControlLink {
}


StreamLink : AbstractLink {

}
