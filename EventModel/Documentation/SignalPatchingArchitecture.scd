/* IZ Jul 3, 2013 (2:46 PM)

=== Notes for porting the synth-linking procedure mechanism from Lilt1 to Lilt2 ===

The algorithms for cycle-checking and signal-diverting bus creation of Lilt1 as well as the class organization are good solutions, so they will be adopted here. Those parts shich relate to the gui will be removed.

=== Principles ===

The output of a Synth (w) is linked to an input of another synth (r) via a Bus (l).
Synth w _writes_ its output to the bus (hence it is called the "writer").
Synth r _reads_ an input from the bus (hence it is called the "reader").

Many synths can write to the same bus, and many synths can read from the same bus.
However, when many readers nr want to share a single bus with many writers nw, care must be taken so that the bus only contains the mixed outputs from exactly those writers that are required by all readers nr. This is explained in more detail in the document Connecting Scripts (Implementation Notes).pdf.

Tbe handling of the synth processes (w and r) is implemented through classes ControlPatch (for control busses) and AudioPatch (for audio busses).

The handling of the bus and its connections to synth processes is handled by class LinkedBus. A LinkedBus holds a single bus, one or more writers (in dictionary "inputs"), and one or more readers (in dictionary "outputs"). Thus, theoretically the following configurations are possible:

1. one-to-one:

writer1 -> linkedbus -> reader1

2. one-to-many:

                      / reader1
                     /
writer1 -> linkedbus -- reader 2
                     \
                      \ reader 3

3. many-to-one

writer1 \
         \
writer2 -- linkedbus -> reader1
         /
writer3 /

4. many-to-many

writer1 \             / reader1
         \           /
writer2 -- linkedbus -- reader2
         /           \
writer3 /             \ reader3


However, as indicated above, in the case of many-to-many, to ensure that all readers only receive the outputs of the writers that they specify, the following rule is applied:

A. If an input bus A of a reader r has more than one readers, then to add a writer w to r one must:
1. Create a new bus B, that will connect the output of w to the input of r
2. Remove r from the outputs of A
3. Copy the contents of A to B

B Conversely, if an output bus A of a writer w has more than one writers, then to add a reader r to w one must:

1. Create a new bus B that will connect the output of w to the input of r.
2. Remove w from the inputs of A.
3. Copy the contents of B to A.

The copying of contents (signal) from one bus to another is managed by class BusLink.

A direct consequence of the above rules is that whatever the number and complexity of interconnections added, the resulting graph will always have busses interconnected in one of the following two ways:

(a) The bus has one writer only and one or more readers
 or:
(b) The bus has one or more writers and one reader only.

Thus, the class components involved in the general scheme of many-to-many links between synths are:

1. ControlPatch (for control rate synths) and AudioPatch (for audio rate synths): handle the processes of "writer" and "reader" synths.

2. Output: links the output of a synth to a LinkedBus instance.

3. LinkedBus: links one or more Output/BusLink instances to one or more Input/Buslink instances

4. Input: links one LinkedBus instance to an input of a synth.

5. BusLink: is inserted between Output and LinkedBus or between LinkedBus and Input to copy bus contents as required by the rule explained above.


=== Classes ===

* ComtrolPatch, AudioPatch:

Holds the SynthModel which produces the output, and inputs and output for linking it to other patches.

Inputs are implemented as a Dictionary (Array???) of Input instances.

Output is implemented as a single Output instance.

* Link (previously Parameter class)

Superclass of Input and Output.
Its subclasses implement the linking of an input or output to other outputs or inputs.


var <>patch;	// the Audio/ControlPatch to which this parameter belongs

* Input (new class): subclass of Link, implements input link

var <input;		// a LinkedBus with links to all my writers
var <numChannels = 1;

* OutputParameter : subclass of Link, implements output -> rename to Output

var <name;		// name of parameter controlled. Is set by name_
var <output; 	// a LinkedBus with links to all my readers

* LinkedBus : Bus
var <writers; // array of parameters of scripts writing to this bus
var <readers; // array of parameters of scripts reading from this bus


*/


AbstractPatch {
	var inputs; // dictionary (array?) of Parameters -> rename class to Input
    var output; // Instance of OutputParameter -> Rename class to Output

}

